package com.invixo.extraction;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.stream.Stream;

import com.invixo.common.GeneralException;
import com.invixo.common.IcoOverviewInstance;
import com.invixo.common.IntegratedConfigurationMain;
import com.invixo.common.XiMessage;
import com.invixo.common.XiMessageException;
import com.invixo.common.StateException;
import com.invixo.common.StateHandler;
import com.invixo.common.util.Logger;
import com.invixo.common.util.PropertyAccessor;
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
	private static final int BATCH_SIZE = Integer.parseInt(PropertyAccessor.getProperty("BATCH_SIZE"));

	
	
	/*====================================================================================
	 *------------- Instance variables
	 *====================================================================================*/
	private ArrayList<MessageKey> messageKeys = new ArrayList<MessageKey>();		// List of FIRST MessageKeys created/processed
	private ArrayList<String> multiMapFirstMsgKeys = new ArrayList<String>();		// List of MessageKeys processed for MultiMapping interfaces
	private ArrayList<XiMessages> xiMessagesLinkList = new ArrayList<XiMessages>();
	


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
	public ArrayList<String> getMultiMapFirstMsgKeys() {
		return multiMapFirstMsgKeys;
	}

	public ArrayList<MessageKey> getMessageKeys() {
		return messageKeys;
	}
	
	public ArrayList<XiMessages> getPayloadsLinkList() {
		return xiMessagesLinkList;
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
			StateHandler.init(GlobalParameters.Operation.valueOf(GlobalParameters.PARAM_VAL_OPERATION), this.getName());
			
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
			logger.writeInfo(LOCATION, SIGNATURE, "*********** Finished processing ICO");
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
	 * List of Message IDs previously injected is contained in the State ICO file during injection.
	 * @throws ExtractorException
	 * @throws HttpException
	 */
	private void extractModeNonInit() throws ExtractorException, HttpException {
		final String SIGNATURE = "extractModeNonInit()";
		try {
			// Get list of Message IDs to be extracted (Inject Id)
	        HashSet<String> uniqueInjectIds = StateHandler.getUniqueInjectIdsFromStateFile();
			logger.writeInfo(LOCATION, SIGNATURE, "Number of entries (matching ICO) fetched from ICO State file: " + uniqueInjectIds.size());

			// Get Message Keys related to Inject IDs 
			HashSet<String> injectIds = new HashSet<String>();
			for (String injectMessageId : uniqueInjectIds) {
				String currentMsgKey = WebServiceUtil.lookupMessageKey(injectMessageId, this.getName());
				injectIds.add(currentMsgKey);
			}
			logger.writeDebug(LOCATION, SIGNATURE, "Number of unique Inject Message Keys to be processed: " + injectIds.size());
			
			// Call common
			this.xiMessagesLinkList = commonGround(injectIds, false, this.internalObjectId);
			
			// Handle STATE file
			logger.writeDebug(LOCATION, SIGNATURE, "Start building internal STATE list (template replacement)");
			for (XiMessages currentPayloadsLink : xiMessagesLinkList) {
				XiMessage firstPayload = currentPayloadsLink.getFirstMessage();
				ArrayList<XiMessage> lastPayloads = currentPayloadsLink.getLastMessageList();

				// Sort Last Payloads
				sortLastMessagesBySequenceId(lastPayloads);
				
				int total = lastPayloads.size()-1;
				for (int i=total; i>=0; i--) {
					XiMessage currentLast = lastPayloads.get(i);
					StateHandler.nonInitReplaceTemplates(firstPayload, currentLast, (i)+"");
				}
			}
			logger.writeDebug(LOCATION, SIGNATURE, "Finished building internal STATE list (template replacement)");
		} catch (IllegalStateException|StateException e) {
			String msg = "Error reading Message Id Map file: " + StateHandler.getIcoPath() + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);			
		}
	}
	
	
	private ArrayList<XiMessages> commonGround(HashSet<String> firstMessageKeys, boolean isInit, int currentIcoCount) throws ExtractorException, HttpException {
		final String SIGNATURE = "commonGround(HashSet<String>, boolean, int)";
		
		// Collect basic FIRST info
		ArrayList<XiMessage> firstMessages = collectBasicFirstInfoForAllKeys(firstMessageKeys, currentIcoCount);
		int firstMessagesTotal = firstMessages.size();
		logger.writeInfo(LOCATION, SIGNATURE, "FIRST: basic info collected. Number of FIRST XI Messages: " + firstMessagesTotal);
		
		// Find and add LAST messages
		ArrayList<XiMessages> payloadsLinkList = attachLastMessages(firstMessages);
		 
		// Persist payload (FIRST and/or LAST)
		storePayloads(payloadsLinkList, isInit);
		
		return payloadsLinkList;
	}
	
	
	private ArrayList<XiMessages> attachLastMessages(ArrayList<XiMessage> firstMessages) throws HttpException, ExtractorException {
		final String SIGNATURE = "attachLastMessages(ArrayList<XiMessages>)";
		int xiMessagesTotal = firstMessages.size();
		int batchesTotal = getBatchCount(xiMessagesTotal, BATCH_SIZE);
		logger.writeDebug(LOCATION, SIGNATURE, "Total number of FIRST to be processed: " + xiMessagesTotal);
		logger.writeDebug(LOCATION, SIGNATURE, "Number of batches required: " + batchesTotal);
		
		ArrayList<XiMessages> xiMessagesLinkListTotal = new ArrayList<XiMessages>();
		ArrayList<XiMessage> currentBatch = new ArrayList<XiMessage>();
		int counter = 0;
		
		for (XiMessage currentFirst : firstMessages) {
        	// Add current entry to batch
        	currentBatch.add(currentFirst);
			
			// Process batches if max batch size is reached
        	if (currentBatch.size() >= BATCH_SIZE) {
        		counter++;
        		logger.writeDebug(LOCATION, SIGNATURE, "Process batch [" + counter + "/" + batchesTotal + "]...");
        		xiMessagesLinkListTotal.addAll(attachLastMessagesBatch(currentBatch));
        		currentBatch.clear();
        	}
		}
		
		// Process remaining/leftover maps
    	logger.writeDebug(LOCATION, SIGNATURE, "Process batch [" + batchesTotal + "/" + batchesTotal + "] (leftovers)");
    	if (currentBatch.size() > 0) {
    		xiMessagesLinkListTotal.addAll(attachLastMessagesBatch(currentBatch));        		
    	}
    	
    	return xiMessagesLinkListTotal;
	}
	
	
	private static int getBatchCount(double maxMessages, double batchSize) {
		double batches = Math.ceil( maxMessages / batchSize );
		return (int) batches;	
	}
	
	
	private ArrayList<XiMessages> attachLastMessagesBatch(ArrayList<XiMessage> firstXiMessages) throws HttpException, ExtractorException {
		final String SIGNATURE = "attachLastMessagesBatch(ArrayList<XiMessage>)";
		logger.writeInfo(LOCATION, SIGNATURE, "Batch of FIRST to be processed. Batch size: " + firstXiMessages.size());
		
		// Get successors (children) of current FIRST message
		byte[] successorResponse = WebServiceUtil.lookupSuccessorsBatch(getMessageIdsFromList(firstXiMessages), this.getName());
		HashMap<String, String> rawResponseMap = WebServiceUtil.extractSuccessorsBatch(successorResponse, this.getSenderInterface(), this.getReceiverInterface());
		
		// Divide raw response info into LAST messages referring to proper FIRST message 
		ArrayList<XiMessages> payloadsLinkList = new ArrayList<XiMessages>();
		for (XiMessage firstXiMessage : firstXiMessages) {
			XiMessages currentPayloads = getLastMessagesForFirstEntry(rawResponseMap, firstXiMessage);
			payloadsLinkList.add(currentPayloads);
		}

		return payloadsLinkList;
	}
		
	
	/**
	 * Get LAST payloads for FIRST payload based on extracted data WS response of service GetMessagesWithSuccessors 
	 * @param rawResponseMap
	 * @param firstXiMessage
	 * @return
	 */
	static XiMessages getLastMessagesForFirstEntry(HashMap<String, String> rawResponseMap, XiMessage firstXiMessage) {
		String currentFirstMsgId = firstXiMessage.getSapMessageId(); 
		
		// Find all records in WS response with a Parent value matching the FIRST message id
		HashMap<String, String> parentMap = new HashMap<String, String>();
		for (Entry<String, String> entry : rawResponseMap.entrySet()) {
			String currentMsgKey = entry.getKey();
			String currentMsgId = Util.extractMessageIdFromKey(currentMsgKey);
			String currentParentId = entry.getValue();
			
			if (currentFirstMsgId.equals(currentParentId)) {
				parentMap.put(currentMsgId, currentMsgKey);
			}
		}
		
		// Find successor (LAST) messages
		ArrayList<XiMessage> currentLastList = new ArrayList<XiMessage>();
		if (parentMap.size() == 1) {
			Entry<String, String> parentEntry = parentMap.entrySet().iterator().next();
			String parentMessageId = parentEntry.getKey();
			
			// Collect all messages that refers to parent
			ArrayList<String> matchList = new ArrayList<String>();
			for (Entry<String, String> entry : rawResponseMap.entrySet()) {
				String currentMsgKey = entry.getKey();
				String currentParentId = entry.getValue();
				
				if (parentMessageId.equals(currentParentId)) {
					matchList.add(currentMsgKey);
				}
			}
			
			// Handle LAST messages according to number of matches found
			if (matchList.size() == 0) {
				// No matches. This means the message itself was a LAST message
				XiMessage lastXiMessage = new XiMessage();
				lastXiMessage.setSapMessageKey(parentEntry.getValue());
				currentLastList.add(lastXiMessage);

				// FIRST message: correct version used to fetch payloads
				switchPayloadVersions(firstXiMessage);
			} else {
				// More than 1 match (all matches are LAST messages)
				for (String currentMessageKey : matchList) {
					XiMessage lastXiMessage = new XiMessage();
					lastXiMessage.setSapMessageKey(currentMessageKey);
					currentLastList.add(lastXiMessage);
				}
			}
		} else {
			// More than 1 parent message. All of these is a LAST message
			for (Entry<String, String> parentEntry : parentMap.entrySet()) {
				XiMessage lastXiMessage = new XiMessage();
				lastXiMessage.setSapMessageKey(parentEntry.getValue());
				currentLastList.add(lastXiMessage);
				
				// FIRST message: correct version used to fetch payloads
				switchPayloadVersions(firstXiMessage);
			}
		}

		// Create and add coupling between first and last payloads
		XiMessages currentXiMessages = new XiMessages();
		currentXiMessages.setFirstMessage(firstXiMessage);
		currentXiMessages.setLastPayloadList(currentLastList);
		return currentXiMessages;
	}
	
	
	private static void switchPayloadVersions(XiMessage message) {
		message.setVersionFirst(-1);	// Correct unusual behavior. Version -1 contains before mapping.
		message.setVersionLast(0);		// Correct unusual behavior. Version 0 contains after mapping.
	}
	
	
	private ArrayList<String> getMessageIdsFromList(ArrayList<XiMessage> payloads) {
		ArrayList<String> messageIds = new ArrayList<String>();
		for (XiMessage payload : payloads) {
			messageIds.add(payload.getSapMessageId());
		}
		return messageIds;
	}


	/**
	 * Method extracts messages from SAP PO.
	 * The messages extracted are which ever messages present in SAP PO matching the requests made by this tool.
	 * @throws ExtractorException
	 * @throws HttpException
	 * @throws StateException
	 */
	private void extractModeInit() throws ExtractorException, HttpException, StateException {
		final String SIGNATURE = "extractModeInit()";
		
		// Initialize State handling
		StateHandler.reset();

		// Lookup Messages in SAP PO and extract MessageInfo from response
		MessageInfo msgInfo = WebServiceUtil.lookupMessages(this);
		
		// Get MessageKeys from web Service response
		HashSet<String> responseMessageKeys = msgInfo.getObjectKeys();
		logger.writeDebug(LOCATION, SIGNATURE, "Number of MessageKeys contained in Web Service response: " + responseMessageKeys.size());
		
		// Call common ground
		this.xiMessagesLinkList = commonGround(responseMessageKeys, true,  this.internalObjectId);
		
		// Handle STATE file
		logger.writeDebug(LOCATION, SIGNATURE, "Start building internal STATE list (template replacement)");
		for (XiMessages currentXiMessagesLink : xiMessagesLinkList) {
			XiMessage firstPayload = currentXiMessagesLink.getFirstMessage();
			ArrayList<XiMessage> lastXiMessages = currentXiMessagesLink.getLastMessageList();

			// Sort Last Payloads
			sortLastMessagesBySequenceId(lastXiMessages);
			
			// Write to temp (internal) storage
			for (int i=0; i < lastXiMessages.size(); i++) {
				XiMessage currentLast = lastXiMessages.get(i);
				String currentIcoLine = StateHandler.createExtractEntry(this.getName(), firstPayload, currentLast, i+"");
				StateHandler.addEntryToInternalList(currentIcoLine);
			}
		}
		logger.writeDebug(LOCATION, SIGNATURE, "Finished building internal STATE list (template replacement)");
	}
	
	
	private void sortLastMessagesBySequenceId(ArrayList<XiMessage> lastXiMessages) {
		Collections.sort(lastXiMessages, new Comparator<XiMessage>() {
		    @Override
		    public int compare(XiMessage msg1, XiMessage msg2) {
		    	int seq1 = msg1.getSequenceIdFromMessageKey();
		    	int seq2 = msg2.getSequenceIdFromMessageKey(); 
		        return Integer.compare(seq1, seq2);
		    }});
	}
	
	
	private void storePayloads(ArrayList<XiMessages> xiMessagesLinkList, boolean isInit) throws ExtractorException {
		final String SIGNATURE = "storePayloads(ArrayList<XiMessages>, boolean)";
		logger.writeDebug(LOCATION, SIGNATURE, "Start persisting payloads to file system");
		logger.writeDebug(LOCATION, SIGNATURE, "Number of FIRST payloads to lookup and persist: " + xiMessagesLinkList.size());
		
		for (XiMessages currentXiMessagesLink : xiMessagesLinkList) {
			XiMessage firstXiMessage = currentXiMessagesLink.getFirstMessage();
			ArrayList<XiMessage> lastXiMessages = currentXiMessagesLink.getLastMessageList();
			logger.writeDebug(LOCATION, SIGNATURE, "Number of LAST paylods to lookup and persist for current FIRST message: " + lastXiMessages.size());
			
			// Persist FIRST
			if (Boolean.parseBoolean(GlobalParameters.PARAM_VAL_EXTRACT_MODE_INIT)) {
				persist(firstXiMessage, true);				
			}

			// Persist LAST
			for (XiMessage lastPaylad : lastXiMessages) {
				persist(lastPaylad, false);
			}
		}
		logger.writeDebug(LOCATION, SIGNATURE, "Finished persisting payloads to file system");
	}
	

	private void persist(XiMessage xiMessage, boolean isFirst)  throws ExtractorException {
		final String SIGNATURE = "persist(XiMessage, boolean)";
		try {
			// Lookup Payload in SAP system (sets internal variables)
			xiMessage.extractPayloadFromSystem(isFirst);
			
			// Persist on file system
			if (isFirst) {
				xiMessage.persistMessage(this.getFilePathFirstPayloads());	
			} else {
				xiMessage.persistMessage(this.getFilePathLastPayloads());
			}
			
			// Clear payload so we do not carry around large objects
			xiMessage.clearPayload();
		} catch (XiMessageException e) {
			String msg = "Error during persist of payload: " + xiMessage.getSapMessageKey() + ". " + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);
		}
	}
	
	
	/**
	 * Extract payloads for a list of SAP Message Keys and store these on the file system.
	 * @param messageKeys					List of SAP Message Keys to be processed.
	 * @param internalObjectId				Internal counter. Used to track which MessageKey number is being processed in the log.
	 * @return
	 * @throws ExtractorException
	 */
	private ArrayList<XiMessage> collectBasicFirstInfoForAllKeys(HashSet<String> messageKeys, int internalObjectId) throws ExtractorException {
		final String SIGNATURE = "collectBasicFirstInfoForAllKeys(HashSet<String>, int)";
		
		// For each MessageKey fetch XI Messages (first and/or last)
		ArrayList<XiMessage> firstXiMessages = new ArrayList<XiMessage>();
		int counter = 1;
		for (String key : messageKeys) {
			// Process a single Message Key
			logger.writeInfo(LOCATION, SIGNATURE, "-----> [ICO " + internalObjectId + "], [MSG KEY " + counter + "] MessageKey processing started for key: " + key);
			XiMessage firstXiMessage = this.processMessageKeySingle(key);
			
			// Only add FIRST message if it was new (this related to MultiMapping handling
			// where many LAST keys (which is what is contained in @messageKeys for a multimap) can have the same parent (FIRST).
			// This is why in a multimap scenario the FIRST message can be 'null'.
			if (firstXiMessage == null) {
				logger.writeDebug(LOCATION, SIGNATURE, "MultiMap scenario: XI message was null (already processed by a previous MessageKey");
			} else {
				firstXiMessages.add(firstXiMessage);
			}

			logger.writeInfo(LOCATION, SIGNATURE, "-----> [ICO " + internalObjectId + "], [MSG KEY " + counter + "] MessageKey processing finished");
			counter++;
		}
		return firstXiMessages;
	}
	
	
	/**
	 * Get FIRST XI Message for single MessageKey
	 * @param key
	 */
	private XiMessage processMessageKeySingle(String key) throws ExtractorException {
		// Create a new MessageKey object
		MessageKey msgKey = new MessageKey(this, key);
			
		// Attach a reference to newly created MessageKey object to this ICO
		this.messageKeys.add(msgKey);
						
		XiMessage payloadXiMessage = msgKey.getBasicFirstInfo(key);
		return payloadXiMessage;
	}
	
}
