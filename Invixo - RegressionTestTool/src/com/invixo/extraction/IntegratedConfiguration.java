package com.invixo.extraction;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import com.invixo.common.GeneralException;
import com.invixo.common.IcoOverviewInstance;
import com.invixo.common.IntegratedConfigurationMain;
import com.invixo.common.StateException;
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
			
			// State Handling: prepare
			StateHandler.init(this.getName());
			
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
			
			// State Handling: persist
			StateHandler.storeIcoState();
		} catch (ExtractorException|HttpException|StateException e) {
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
	        Map<String, String> messageIdMap = StateHandler.getMessageIdsFromFile();
	        logger.writeInfo(LOCATION, SIGNATURE, "Number of entries (matching ICO) fetched from ICO State file: " + messageIdMap.size());
			
	        // Split and process map in batches
	        Map<String, String> currentBatch = new HashMap<String, String>();
	        for (Entry<String, String> entry : messageIdMap.entrySet()) {
	        	// Process batches if max batch size is reached
	        	if (currentBatch.size() >= 100) {
	        		logger.writeDebug(LOCATION, SIGNATURE, "Batch size reached. Current batch is being processed...");
	        		processNonInitInBatch(currentBatch.values());
	        		currentBatch.clear();
	        	}
	        	
	        	// Add current entry to batch
	        	currentBatch.put(entry.getKey(), entry.getValue());
	        }
	        
	        // Process remaining/leftover maps
        	logger.writeDebug(LOCATION, SIGNATURE, "Batch leftovers to be processed: " + currentBatch.size());
        	if (currentBatch.size() > 0) {
    	        processNonInitInBatch(currentBatch.values());        		
        	}
		} catch (IllegalStateException|StateException e) {
			String msg = "Error reading Message Id Map file: " + StateHandler.getIcoPath() + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);			
		}
	}


	/**
	 * Extract data (LAST payloads) for a single non-init batch run.
	 * @param injectMessageIds				List of Inject Message IDs
	 * @throws ExtractorException
	 * @throws HttpException
	 */
	private void processNonInitInBatch(Collection<String> injectMessageIds) throws ExtractorException, HttpException {		
		for(String injectMessageId : injectMessageIds) {
			// Lookup Parent MessageInfo for current Inject ID
			MessageInfo msgInfo = WebServiceUtil.lookupParentMessageInfo(injectMessageId, this.getName(), this.getReceiverInterface());
			this.responseMessageKeys = msgInfo.getObjectKeys();
					
			// Process extracted message keys
			processMessageKeysMultiple(injectMessageId, this.responseMessageKeys, this.internalObjectId);	
		}
	}


	/**
	 * Method extracts messages from SAP PO.
	 * The messages extracted are which ever messages present in SAP PO matching the requests made by this tool.
	 * @throws ExtractorException
	 * @throws HttpException
	 */
	private void extractModeInit() throws ExtractorException, HttpException, StateException {
		final String SIGNATURE = "extractModeInit()";
		
		// Initialize State handling
		StateHandler.reset();						// Delete existing ICO state file

		// Lookup Messages in SAP PO and extract MessageInfo from response
		MessageInfo msgInfo = WebServiceUtil.lookupMessages(this);
		
		// Set MessageKeys from web Service response
		this.responseMessageKeys = msgInfo.getObjectKeys();
		logger.writeDebug(LOCATION, SIGNATURE, "Number of MessageKeys contained in Web Service response: " + this.responseMessageKeys.size());
		
		// Process extracted message keys
		processMessageKeysMultiple(null, this.responseMessageKeys, this.internalObjectId);
	}

	
	/**
	 * Extract payloads for a list of SAP Message Keys and store these on the file system.
	 * @param injectMessageId				Inject message id.
	 * @param messageKeys					List of SAP Message Keys to be processed.
	 * @param internalObjectId				Internal counter. Used to track which MessageKey number is being processed in the log.
	 * @throws ExtractorException
	 */
	private void processMessageKeysMultiple(String injectMessageId, HashSet<String> messageKeys, int internalObjectId) throws ExtractorException {
		final String SIGNATURE = "processMessageKeysMultiple(HashSet<String>, int)";
		
		// For each MessageKey fetch payloads (first and/or last)
		int counter = 1;
		for (String key : messageKeys) {
			// Process a single Message Key
			logger.writeInfo(LOCATION, SIGNATURE, "-----> [ICO " + internalObjectId + "], [MSG KEY " + counter + "] MessageKey processing started for key: " + key);
			this.processMessageKeySingle(injectMessageId, key);
			logger.writeInfo(LOCATION, SIGNATURE, "-----> [ICO " + internalObjectId + "], [MSG KEY " + counter + "] MessageKey processing finished");
			counter++;
		}
	}
	
	
	/**
	 * Processes a single MessageKey returned in Web Service response for service GetMessageList.
	 * It extracts payloads and stores the state in case of successfully finding payloads.
	 * This method can/will generate FIRST/LAST payloads.
	 * @param inectMessageId
	 * @param key
	 */
	void processMessageKeySingle(String injectMessageId, String key) {
		try {
			// Create a new MessageKey object
			MessageKey msgKey = new MessageKey(this, key);
			
			// Attach a reference to newly created MessageKey object to this ICO
			this.messageKeys.add(msgKey);
			
			// Extract FIRST and/or LAST payloads
			msgKey.extractAllPayloads(key);
			
			// Store state
			msgKey.storeState(injectMessageId, msgKey.getPayloadFirst(), msgKey.getPayloadLast());
		} catch (ExtractorException e) {
			// Do nothing, exception already logged
			// Exceptions at this point are used to terminate further processing of current messageKey
		}
	}
	
}
