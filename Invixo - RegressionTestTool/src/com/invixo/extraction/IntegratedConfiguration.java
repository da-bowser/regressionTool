package com.invixo.extraction;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import com.invixo.common.GeneralException;
import com.invixo.common.IcoOverviewInstance;
import com.invixo.common.IntegratedConfigurationMain;
import com.invixo.common.StateHandler;
import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.common.util.HttpException;
import com.invixo.consistency.FileStructure;
import com.invixo.main.GlobalParameters;

public class IntegratedConfiguration extends IntegratedConfigurationMain {
	/*====================================================================================
	 *------------- Class variables
	 *====================================================================================*/
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = IntegratedConfiguration.class.getName();

	
	/*====================================================================================
	 *------------- Instance variables
	 *====================================================================================*/
	private HashSet<String> responseMessageKeys = new HashSet<String>();			// MessageKey IDs returned by Web Service GetMessageList
	private ArrayList<MessageKey> messageKeys = new ArrayList<MessageKey>();		// List of FIRST MessageKeys created/processed
	private ArrayList<String> multiMapMessageKeys = new ArrayList<String>();		// List of MessageKeys processed for MultiMapping interfaces

	
	
	/*====================================================================================
	 *------------- Constructors
	 *====================================================================================*/
	IntegratedConfiguration(IcoOverviewInstance icoInstance) throws GeneralException {
		super(icoInstance);
	}

	
	IntegratedConfiguration(IcoOverviewInstance icoInstance, String mapfilePath, String sourceEnv, String targetEnv) throws GeneralException {
		super(icoInstance, mapfilePath, sourceEnv, targetEnv);
	}
	
	
	
	/*====================================================================================
	 *------------- Getters and Setters
	 *====================================================================================*/
	public ArrayList<String> getMultiMapMessageKeys() {
		return multiMapMessageKeys;
	}

	public ArrayList<MessageKey> getMessageKeys() {
		return messageKeys;
	}
	
	
	
	/*====================================================================================
	 *------------- Instance methods
	 *====================================================================================*/
	/**
	 * Process a single Integrated Configuration object.
	 * This also includes all MessageKeys related to this object.
	 */
	void startExtraction() {
		final String SIGNATURE = "startExtraction()";
		try {
			logger.writeInfo(LOCATION, SIGNATURE, "*********** (" + this.internalObjectId + ") Start processing ICO: " + this.getName());
			
			// Housekeeping: Delete old ICO extract data
			if (GlobalParameters.PARAM_VAL_ALLOW_SAME_ENV) {
				// Do not delete data (this is a special test parameter)
				logger.writeDebug(LOCATION, SIGNATURE, "Deletion of existing files in Extract output directory disabled due to program parameter.");
			} else {
				// Delete data
				deleteOldRunData();				
			}
			
			// Check: execution can take place in 2 modes: 
			// 1) init (first extraction of data from a given system environment (DEV, TST, PRD))
			// 2) non-init (extract injected messages, so that a comparison can be made at later stage)
			if (Boolean.parseBoolean(GlobalParameters.PARAM_VAL_EXTRACT_MODE_INIT)) {
				// Extract whatever data is in SAP PO matching the ICO
				extractModeInit();
			} else {
				// Extract only Message IDs previously injected for ICO 
				extractModeNonInit();
			}
		} catch (ExtractorException|HttpException e) {
			this.setEx(e);
		} finally {
			this.endTime = Util.getTime();
			logger.writeInfo(LOCATION, SIGNATURE, "*********** Finished processing ICO request file");
		}
	}
	
	
	/**
	 * Makes sure all old run data for target environment is deleted before a new run.
	 */
	private void deleteOldRunData() {
		final String SIGNATURE = "deleteOldRunData()";
		try {       
			// Build output directory path
			String outputDirWithIcoName = FileStructure.DIR_EXTRACT_OUTPUT_PRE + "\\" + this.getName();
			
			// Cleanup: delete all files contained in "Extract Output" for current ico. Only done for sub-directories part of the specified target environment
			deletePayloadFiles(outputDirWithIcoName, GlobalParameters.PARAM_VAL_TARGET_ENV);
			logger.writeDebug(LOCATION, SIGNATURE, "Housekeeping: all old payload files deleted from: " + outputDirWithIcoName + " for environment: " + GlobalParameters.PARAM_VAL_TARGET_ENV);
		} catch (Exception e) {
			String ex = "Housekeeping terminated with error! " + e;
			logger.writeError(LOCATION, SIGNATURE, ex);
			throw new RuntimeException(e);
		}       
	}


	private void deletePayloadFiles(String rootDirectory, String environment) {
		// Create pathMatcher which will match all files and directories (in the world of this tool, only files) that
		// are located in FIRST or LAST directories for the specified environment.
		String pattern = "^(?=.*\\\\" + environment + "\\\\.*\\\\.*\\\\.*\\\\).*$";
		PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("regex:" + pattern);
		
		// Find all matches to above regex starting from the specified DIR
		try (Stream<Path> paths = Files.find(Paths.get(rootDirectory), 100, (path, f)->pathMatcher.matches(path))) {
			// Delete all matches
			paths.forEach(path -> {
				try {
					Files.delete(path);
				} catch (IOException e) {
					throw new RuntimeException("*deletePayloadFiles* Error deleting file '" + path + "'\n" + e);
				}
			});
		} catch (IOException e) {
			throw new RuntimeException("*deletePayloadFiles* Error finding files." + e);
		}	
	}


	/**
	 * Method extracts messages from SAP PO.
	 * The messages extracted are those that has already been injected previously by this tool.
	 * List of Message IDs previously injected is contained in the Message Mapping Id file.
	 * NB: messages resulting from a Message Split is also extracted.
	 * @throws ExtractorException
	 * @throws HttpException
	 */
	private void extractModeNonInit() throws ExtractorException, HttpException {
		final String SIGNATURE = "extractModeNonInit()";
		try {
			// Get list of Message IDs to be extracted (Map<FIRST msgId, Inject Id>)
	        Map<String, String> messageIdMap = StateHandler.getMessageIdsFromFile(this.getName());
	        logger.writeInfo(LOCATION, SIGNATURE, "Number of entries (matching ICO) fetched from Message Id Mapping file: " + messageIdMap.size());
			
	        // Split and process map in batches
	        Map<String, String> currentBatch = new HashMap<String, String>();
	        for (Entry<String, String> entry : messageIdMap.entrySet()) {
	        	// Process batches if max batch size is reached
	        	if (currentBatch.size() >= 100) {
	        		logger.writeDebug(LOCATION, SIGNATURE, "Batch size reached. Current batch is being processed...");
	        		processNonInitInBatch(currentBatch);
	        		currentBatch.clear();
	        	}
	        	
	        	// Add current entry to batch
	        	currentBatch.put(entry.getKey(), entry.getValue());
	        }
	        
	        // Process remaining/leftover maps
        	logger.writeDebug(LOCATION, SIGNATURE, "Batch leftovers to be processed: " + currentBatch.size());
        	if (currentBatch.size() > 0) {
    	        processNonInitInBatch(currentBatch);        		
        	}
		} catch (IllegalStateException|IOException e) {
			String msg = "Error reading Message Id Map file: " + FileStructure.FILE_MSG_ID_MAPPING + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);			
		}
	}


	/**
	 * Extract data (LAST payloads) for a single non-init batch run.
	 * In non-init mode data extracted comes from the Message ID mapping file generated during injection.
	 * As some ICOs may split messages, this needs to be handled, since split messages are given a new Message ID by SAP at runtime.
	 * This means that a Message ID in the Message ID mapping file cannot be used to get LAST payloads for split messages.
	 * As such all Message IDs in the Message Mapping file must be checked to see if they are in fact 'Parent ID' to other messages.
	 * The Web Service 'GetMessagesWithSuccessors' is used to determine this, since it returns details for the message itself along with 
	 * any messages spawned (split) by it (messages that the message is parent to).
	 * @param messageIdMap					Message Id map build from Message Id mapping file created during injection.
	 * 										Map<FIRST message ID, Inject message ID>
	 * @throws ExtractorException
	 * @throws HttpException
	 */
	private void processNonInitInBatch(Map<String, String> messageIdMap) throws ExtractorException, HttpException {
		final String SIGNATURE = "processNonInitInBatch(Map<String, String>)";
		
		for(Entry<String, String> entry : messageIdMap.entrySet()) {
			// Lookup Parent MessageInfo for current Inject ID
			MessageInfo msgInfo = WebServiceUtil.lookupParentMessageInfo(entry.getValue(), this.getName(), this.getReceiverInterface());
			
			// Special handling for multimapping interfaces(multiplicity 1:n)
			if (this.isUsingMultiMapping()) {
				logger.writeDebug(LOCATION, SIGNATURE, "Special handling for MultiMapping scenario");
				this.responseMessageKeys = handleScenarioMultiMapping(entry.getValue(), msgInfo.getObjectKeys());
			}
			
			// Special processing for split interfaces (multiplicity 1:1)
			if (msgInfo.getSplitMessageIds().size() > 0 && !this.isUsingMultiMapping()) {
				logger.writeDebug(LOCATION, SIGNATURE, "Special handling for split scenario");
				this.responseMessageKeys = handleScenarioSplit(msgInfo);
			}
					
			// Process extracted message keys
			processMessageKeysMultiple(this.responseMessageKeys, this.internalObjectId);	
		}
	}


	/**
	 * 
	 * @param injectMessageId					Inject Message ID (FIRST)
	 * @param lastMessageKeys					Children of @param injectMessageId (LAST Message Keys). Can be 1:n for multimapping scenario.
	 * @return
	 * @throws HttpException
	 * @throws ExtractorException
	 */
	private HashSet<String> handleScenarioMultiMapping(String injectMessageId, HashSet<String> lastMessageKeys) throws HttpException, ExtractorException {
		final String SIGNATURE = "handleScenarioMultiMapping(HashSet<String>, Collection<String>";

		try {
			// Read file
			List<String> lines = StateHandler.getLinesMatchingIco(this.getName());

			// Filter/remove all lines not containing Inject Message Id
			lines.removeIf(line -> !line.contains(injectMessageId));
			
			// Get Message ID from source (FIRST) extract
			String[] lineParts = lines.get(0).split(GlobalParameters.FILE_DELIMITER);	
			String sourceFirstMessageId = lineParts[1];
			
			// Lookup MessageInfo for Message ID
			MessageInfo extractKeys = WebServiceUtil.lookupParentMessageInfo(sourceFirstMessageId, this.getName(), this.getReceiverInterface());
			
			// Build new map entries
			for (String targetMessagKey : lastMessageKeys) {
				String targetSequence = targetMessagKey.substring(targetMessagKey.indexOf("EOIO"), targetMessagKey.length());
				
				for (String sourceMessageKey : extractKeys.getObjectKeys() ) {
					String sourceSequence = sourceMessageKey.substring(sourceMessageKey.indexOf("EOIO"), sourceMessageKey.length());
					
					if (targetSequence.equals(sourceSequence)) {
						String sourceMsgId = Util.extractMessageIdFromKey(sourceMessageKey);
						String targetMsgId = Util.extractMessageIdFromKey(targetMessagKey);
						String mapEntryLine = com.invixo.injection.IntegratedConfiguration.createMappingEntryLine(sourceMsgId, targetMsgId, this.getName());
						logger.writeDebug(LOCATION, SIGNATURE, mapEntryLine);
						Files.write(Paths.get(FileStructure.FILE_MSG_ID_MAPPING), mapEntryLine.getBytes(), StandardOpenOption.APPEND);
						break;
					}
				}
			}
			
			// Read all lines from map file except inject id
//			lines = Files.readAllLines(mapFilePath);
//			lines.removeIf(line -> line.contains(injectMessageId));
			
			// Clear map file
//			Files.newBufferedWriter(Paths.get(FileStructure.FILE_MSG_ID_MAPPING), StandardOpenOption.TRUNCATE_EXISTING);
			
			// Recreate map file
//			for (String line : lines) {
//				Files.write(Paths.get(FileStructure.FILE_MSG_ID_MAPPING), (line + "\n").getBytes(), StandardOpenOption.APPEND);
//			}
			
			// Return original object keys to be extracted
			return lastMessageKeys;
		} catch (IOException e) {
			String msg = "Error recreating Message Id Mapping file." + "\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);
		}
	}


	private HashSet<String> handleScenarioSplit(MessageInfo msgInfo) throws ExtractorException {
		final String SIGNATURE = "handleScenarioSplit(MessageInfo)";
		
		// Correct Message Mapping Id's in file. This is a special situation. 
		// EXPLANATION: If an extract is made (after an injection) for an ICO performing message split, then 
		// the Message Id Mapping file must be corrected for existing entries made during injection.
		// This is because during a message split, SAP assigns the split interfaces new message ID's. 
		// These new interfaces will have a Parent Message Id matching the original request (inject ID).
		// For this reason the Message ID Mapping must be corrected, so that the new, SAP Message IDs generated by SAP
		// for split cases is replaced with the original "Parent ID's" from the injection.
		// If this is not done then the Message ID Mapping file will not make sense and cannot be used during Comparison.
		correctMessageMappingFile(msgInfo.getSplitMessageIds());
			
		// Set list of Message Keys to be extracted. This list is consist of all MessageKeys extracted from WS response matching 
		// the ICO receiver interface at hand. Should any of the MessageKeys be a parent Message, then the key is replaced with 
		// the Message ID from the split message.
		HashSet<String> responseMessageKeys = buildListOfMessageIdsToBeExtracted(msgInfo.getObjectKeys(), msgInfo.getSplitMessageIds());
		logger.writeDebug(LOCATION, SIGNATURE, "Number of MessageKeys to be extracted: " + responseMessageKeys.size());
		
		return responseMessageKeys;
	}


	/**
	 * Build a list of message IDs to be extracted.
	 * This method adds all message keys to the result list. IFF any of these message keys have a parent id (= message split), then
	 * the message ID part of the message key is replaced (parent id replaced with message id):
	 * 		Parent ID 	= inject message ID
	 * 		Message ID 	= for a split message, this is the new message id created by SAP during message split.
	 * @param objectKeys
	 * @param splitMessageIds
	 * @return
	 */
	static HashSet<String> buildListOfMessageIdsToBeExtracted(HashSet<String> objectKeys, HashMap<String, String> splitMessageIds) {
		HashSet<String> result = new HashSet<String>();
		
		for (Object messageKey : objectKeys.toArray()) {
			String currentMsgKey = messageKey.toString(); 
			
			for (Entry<String, String> entry : splitMessageIds.entrySet()) {
				// Check if current Message Key should be replaced
				if (currentMsgKey.contains(entry.getKey())) {
					String newKey = currentMsgKey.replace(entry.getKey(), entry.getValue());
					result.add(newKey);
					break;
				}				
			}
			result.add(currentMsgKey);
		}
		
		// Set list of message IDs to fetch LAST payloads from
		return result;
	}


	/**
	 * Method extracts messages from SAP PO.
	 * The messages extracted are which ever messages present in SAP PO matching the requests made by this tool.
	 * @throws ExtractorException
	 * @throws HttpException
	 */
	private void extractModeInit() throws ExtractorException, HttpException {
		final String SIGNATURE = "extractModeInit()";
		
		// Reset (delete) state
		StateHandler.reset();
		
		// Lookup Messages in SAP PO and extract MessageInfo from response
		MessageInfo msgInfo = WebServiceUtil.lookupMessages(this);
		
		// Set MessageKeys from web Service response
		this.responseMessageKeys = msgInfo.getObjectKeys();
		logger.writeDebug(LOCATION, SIGNATURE, "Number of MessageKeys contained in Web Service response: " + this.responseMessageKeys.size());
		
		// Process extracted message keys
		processMessageKeysMultiple(this.responseMessageKeys, this.internalObjectId);
	}

	
	/**
	 * Extract payloads for a list of SAP Message Keys and store these on the file system.
	 * @param messageKeys					List of SAP Message Keys to be processed.
	 * @param internalObjectId				Internal counter. Used to track which MessageKey number is being processed in the log.
	 * @throws ExtractorException
	 */
	private void processMessageKeysMultiple(HashSet<String> messageKeys, int internalObjectId) throws ExtractorException {
		final String SIGNATURE = "processMessageKeysMultiple(HashSet<String>, int)";
		
		// For each MessageKey fetch payloads (first and/or last)
		int counter = 1;
		for (String key : messageKeys) {
			// Process a single Message Key
			logger.writeInfo(LOCATION, SIGNATURE, "-----> [ICO " + internalObjectId + "], [MSG KEY " + counter + "] MessageKey processing started for key: " + key);
			this.processMessageKeySingle(key);
			logger.writeInfo(LOCATION, SIGNATURE, "-----> [ICO " + internalObjectId + "], [MSG KEY " + counter + "] MessageKey processing finished");
			counter++;
		}
	}
	
	
	/**
	 * Processes a single MessageKey returned in Web Service response for service GetMessageList.
	 * It extracts payloads and stores the state in case of successfully finding payloads.
	 * This method can/will generate FIRST/LAST payloads.
	 * @param key
	 */
	void processMessageKeySingle(String key) {
		try {
			// Create a new MessageKey object
			MessageKey msgKey = new MessageKey(this, key);
			
			// Attach a reference to newly created MessageKey object to this ICO
			this.messageKeys.add(msgKey);
			
			// Extract FIRST and/or LAST payloads
			msgKey.extractAllPayloads(key);
			
			// Store state
			msgKey.storeState(msgKey.getPayloadFirst(), msgKey.getPayloadLast());
		} catch (ExtractorException e) {
			// Do nothing, exception already logged
			// Exceptions at this point are used to terminate further processing of current messageKey
		}
	}


	/**
	 * Replace parent message ids with actual message ids in Message Id Mapping file.
	 * NB: Parent ID is ONLY EVAR exiting in response in the case of a message split for async messages.
	 * 
	 * This method updates/modifies the Message Mapping file.
	 * @param messageIds
	 * @throws ExtractorException
	 */
	private void correctMessageMappingFile(HashMap<String, String> messageIds) throws ExtractorException {
    	// Check: skip processing as message split is not relevant
        if (this.getQualityOfService().equals("BE")) {
        	return;
        }
        
        // Modify relevant, existing entries in Message Mapping file
        updateMessageIdMappingFile(messageIds);
	}


	private void updateMessageIdMappingFile(HashMap<String, String> updatedMessageIds) {
		final String SIGNATURE = "updateMessageIdMappingFile(HashMap<String, String>)";
		String messageId;
		String parentId;
		File file = new File(FileStructure.FILE_MSG_ID_MAPPING);
		
		// Update Message Mapping file
		if (updatedMessageIds.size() > 0 && file.exists()) {
			String content = new String(Util.readFile(FileStructure.FILE_MSG_ID_MAPPING));
			for (HashMap.Entry<String, String> entry : updatedMessageIds.entrySet()) {
				parentId = entry.getKey();
				messageId = entry.getValue();
				logger.writeDebug(LOCATION, SIGNATURE, "Message split scenario found!");
				logger.writeDebug(LOCATION, SIGNATURE, "Key: " + parentId + " will be replaced with key: " + messageId);
				content = content.replace(entry.getKey(), entry.getValue());
			}

			logger.writeDebug(LOCATION, SIGNATURE, "Changed file will be written to: " + FileStructure.FILE_MSG_ID_MAPPING);
			Util.writeFileToFileSystem(FileStructure.FILE_MSG_ID_MAPPING, content.getBytes());
		}
	}
	
}
