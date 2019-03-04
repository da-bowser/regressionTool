package com.invixo.extraction;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
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
	private ArrayList<Payloads> payloadsLinkList = new ArrayList<Payloads>();
	


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
	
	public ArrayList<Payloads> getPayloadsLinkList() {
		return payloadsLinkList;
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
			this.payloadsLinkList = commonGround(injectIds, false, this.internalObjectId);
			
			// Handle STATE file
			logger.writeDebug(LOCATION, SIGNATURE, "Start building internal STATE list (template replacement)");
			for (Payloads currentPayloadsLink : payloadsLinkList) {
				XiMessage firstPayload = currentPayloadsLink.getFirstPayload();
				ArrayList<XiMessage> lastPayloads = currentPayloadsLink.getLastPayloadList();

				int total = lastPayloads.size()-1;
				for (int i=total; i>=0; i--) {
					XiMessage currentLast = lastPayloads.get(i);
					StateHandler.nonInitReplaceTemplates(firstPayload, currentLast, (total - i)+"");
				}
			}
			logger.writeDebug(LOCATION, SIGNATURE, "Finished building internal STATE list (template replacement)");
		} catch (IllegalStateException|StateException e) {
			String msg = "Error reading Message Id Map file: " + StateHandler.getIcoPath() + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);			
		}
	}
	
	
	private ArrayList<Payloads> commonGround(HashSet<String> firstMessageKeys, boolean isInit, int currentIcoCount) throws ExtractorException, HttpException {
		final String SIGNATURE = "commonGround(HashSet<String>, boolean, int)";
		
		// Collect basic FIRST info
		ArrayList<XiMessage> firstPayloads = collectBasicFirstInfoForAllKeys(firstMessageKeys, currentIcoCount);
		int firstPaylodsTotal = firstPayloads.size();
		logger.writeInfo(LOCATION, SIGNATURE, "FIRST: basic info collected. Number of FIRST payloads: " + firstPaylodsTotal);
		
		// Test stuff
//		ArrayList<Payloads> payloadsLinkList = lastHandlingOriginal(firstPayloads);
		ArrayList<Payloads> payloadsLinkList = lastPlayground(firstPayloads);
		 
		// Persist payload (FIRST and/or LAST)
		storePayloads(payloadsLinkList, isInit);
		
		return payloadsLinkList;
	}
	
	
	private ArrayList<Payloads> lastHandlingOriginal(ArrayList<XiMessage> firstPayloads) throws HttpException, ExtractorException {
		final String SIGNATURE = "lastHandlingOriginal(ArrayList<Payload>)";
		
		// Add basic LAST info
		int firstCounter = 0;
		ArrayList<Payloads> payloadsLinkList = new ArrayList<Payloads>();
		for (XiMessage firstPayload : firstPayloads) {
			firstCounter++;
			logger.writeInfo(LOCATION, SIGNATURE, "Find basic LAST info for FIRST entry ["+ firstCounter + "/" + firstPayloads.size() + "]. Referenced FIRST key: " + firstPayload.getSapMessageKey());
			
			// Add current FIRST to combined list of FIRST and related LAST messages
			Payloads currentPayloadsLink = new Payloads();
			currentPayloadsLink.setFirstPayload(firstPayload);
			
			// Get successors (children) of current FIRST message
			byte[] successorResponse = WebServiceUtil.lookupSuccessors(firstPayload.getSapMessageId(), this.getName());
			ArrayList<String> successorsList = WebServiceUtil.extractSuccessors(successorResponse, this.getReceiverInterface());
			logger.writeInfo(LOCATION, SIGNATURE, "Number of successors found: " + successorsList.size());
			
			// Add successors to current FIRST
			for (String messageKey : successorsList) {
				// Create LAST basic info
				XiMessage lastPayload = new XiMessage();
				lastPayload.setSapMessageKey(messageKey);

				// Add current LAST payload to current FIRST payload
				currentPayloadsLink.getLastPayloadList().add(lastPayload);
			}
			payloadsLinkList.add(currentPayloadsLink);
			logger.writeInfo(LOCATION, SIGNATURE, "Number of LAST keys found: " + currentPayloadsLink.getLastPayloadList().size());
			logger.writeInfo(LOCATION, SIGNATURE, "Finished finding basic info for LAST keys for FIRST key: " + firstPayload.getSapMessageKey());
		}
		return payloadsLinkList;
	}
		
	
	private ArrayList<Payloads> lastPlayground(ArrayList<XiMessage> firstPayloads) throws HttpException, ExtractorException {
		final String SIGNATURE = "lastPlayground(ArrayList<Payload>)";
		int payloadsTotal = firstPayloads.size();
		int batchesTotal = getBatchCount(payloadsTotal, BATCH_SIZE);
		logger.writeDebug(LOCATION, SIGNATURE, "Total number of FIRST to be processed: " + payloadsTotal);
		logger.writeDebug(LOCATION, SIGNATURE, "Number of batches required: " + batchesTotal);
		
		ArrayList<Payloads> payloadsLinkListTotal = new ArrayList<Payloads>();
		ArrayList<XiMessage> currentBatch = new ArrayList<XiMessage>();
		int counter = 0;
		
		for (XiMessage currentFirst : firstPayloads) {
        	// Add current entry to batch
        	currentBatch.add(currentFirst);
			
			// Process batches if max batch size is reached
        	if (currentBatch.size() >= BATCH_SIZE) {
        		counter++;
        		logger.writeDebug(LOCATION, SIGNATURE, "Process batch [" + counter + "/" + batchesTotal + "]...");
        		payloadsLinkListTotal.addAll(lastPlaygroundBatch(currentBatch));
        		currentBatch.clear();
        	}
		}
		
		// Process remaining/leftover maps
    	logger.writeDebug(LOCATION, SIGNATURE, "Process batch [" + batchesTotal + "/" + batchesTotal + "] (leftovers)");
    	if (currentBatch.size() > 0) {
    		payloadsLinkListTotal.addAll(lastPlaygroundBatch(currentBatch));        		
    	}
    	
    	return payloadsLinkListTotal;
	}
	
	
	public static int getBatchCount(double maxMessages, double batchSize) {
		double batches = Math.ceil( maxMessages / batchSize );
		return (int) batches;	
	}
	
	
	private ArrayList<Payloads> lastPlaygroundBatch(ArrayList<XiMessage> firstPayloads) throws HttpException, ExtractorException {
		final String SIGNATURE = "lastPlaygroundBatch(ArrayList<Payload>)";
		logger.writeInfo(LOCATION, SIGNATURE, "Batch of FIRST to be processed. Batch size: " + firstPayloads.size());
		
		// Get successors (children) of current FIRST message
		byte[] successorResponse = WebServiceUtil.lookupSuccessorsBatch(getMessageIdsFromList(firstPayloads), this.getName());
		HashMap<String, String> rawResponseMap = WebServiceUtil.extractSuccessorsBatch(successorResponse, this.getSenderInterface(), this.getReceiverInterface());
		
		// Divide raw response info into LAST messages referring to proper FIRST message 
		ArrayList<Payloads> payloadsLinkList = new ArrayList<Payloads>();
		for (XiMessage firstPayload : firstPayloads) {
			Payloads currentPayloads = getLastMessagesForFirstEntry(rawResponseMap, firstPayload);
			payloadsLinkList.add(currentPayloads);
		}

		return payloadsLinkList;
	}
		
	
	/**
	 * Get LAST payloads for FIRST payload based on extracted data WS response of service GetMessagesWithSuccessors 
	 * @param rawResponseMap
	 * @param firstPayload
	 * @return
	 */
	static Payloads getLastMessagesForFirstEntry(HashMap<String, String> rawResponseMap, XiMessage firstPayload) {
		String currentFirstMsgId = firstPayload.getSapMessageId(); 
		
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
				XiMessage lastPayload = new XiMessage();
				lastPayload.setSapMessageKey(parentEntry.getValue());
				currentLastList.add(lastPayload);
				System.out.println("Case 1: " + firstPayload.getSapMessageKey());
				// FIRST message: correct version used to fetch payloads
				switchPayloadVersions(firstPayload);
			} else {
				// More than 1 match (all matches are LAST messages)
				System.out.println("Case 2: " + firstPayload.getSapMessageKey());
				for (String currentMessageKey : matchList) {
					XiMessage lastPayload = new XiMessage();
					lastPayload.setSapMessageKey(currentMessageKey);
					currentLastList.add(lastPayload);
				}
			}
		} else {
			// More than 1 parent message. All of these is a LAST message
			System.out.println("Case 3: " + firstPayload.getSapMessageKey());
			for (Entry<String, String> parentEntry : parentMap.entrySet()) {
				XiMessage lastPayload = new XiMessage();
				lastPayload.setSapMessageKey(parentEntry.getValue());
				currentLastList.add(lastPayload);
				
				// FIRST message: correct version used to fetch payloads
				switchPayloadVersions(firstPayload);
			}
		}

		// Create and add coupling between first and last payloads
		Payloads currentPayloads = new Payloads();
		currentPayloads.setFirstPayload(firstPayload);
		currentPayloads.setLastPayloadList(currentLastList);
		return currentPayloads;
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
		this.payloadsLinkList = commonGround(responseMessageKeys, true,  this.internalObjectId);
		
		// Handle STATE file
		logger.writeDebug(LOCATION, SIGNATURE, "Start building internal STATE list (template replacement)");
		for (Payloads currentPayloadsLink : payloadsLinkList) {
			XiMessage firstPayload = currentPayloadsLink.getFirstPayload();
			ArrayList<XiMessage> lastPayloads = currentPayloadsLink.getLastPayloadList();

			for (int i=0; i < lastPayloads.size(); i++) {
				XiMessage currentLast = lastPayloads.get(i);
				String currentIcoLine = StateHandler.createExtractEntry(this.getName(), firstPayload, currentLast, i+"");
				StateHandler.addEntryToInternalList(currentIcoLine);
			}
		}
		logger.writeDebug(LOCATION, SIGNATURE, "Finished building internal STATE list (template replacement)");
	}
	
	
	private void storePayloads(ArrayList<Payloads> payloadsLinkList, boolean isInit) throws ExtractorException {
		final String SIGNATURE = "storePayloads(ArrayList<Payloads>, boolean)";
		logger.writeDebug(LOCATION, SIGNATURE, "Start persisting payloads to file system");
		logger.writeDebug(LOCATION, SIGNATURE, "Number of FIRST payloads to lookup and persist: " + payloadsLinkList.size());
		
		for (Payloads currentPayloadsLink : payloadsLinkList) {
			XiMessage firstPayload = currentPayloadsLink.getFirstPayload();
			ArrayList<XiMessage> lastPayloads = currentPayloadsLink.getLastPayloadList();
			logger.writeDebug(LOCATION, SIGNATURE, "Number of LAST payloads to lookup and persist for current FIRST payload: " + lastPayloads.size());
			
			// Persist FIRST
			if (Boolean.parseBoolean(GlobalParameters.PARAM_VAL_EXTRACT_MODE_INIT)) {
				persist(firstPayload, true);				
			}

			// Persist LAST
			for (XiMessage lastPaylad : lastPayloads) {
				persist(lastPaylad, false);
			}
		}
		logger.writeDebug(LOCATION, SIGNATURE, "Finished persisting payloads to file system");
	}
	

	private void persist(XiMessage payload, boolean isFirst)  throws ExtractorException {
		final String SIGNATURE = "persist(Payload, boolean)";
		try {
			// Lookup Payload in SAP system (sets internal variables)
			payload.extractPayloadFromSystem(isFirst);
			
			// Persist on file system
			if (isFirst) {
				payload.persistMessage(this.getFilePathFirstPayloads());	
			} else {
				payload.persistMessage(this.getFilePathLastPayloads());
			}
			
			// Clear payload so we do not carry around large objects
			payload.clearPayload();
		} catch (XiMessageException e) {
			String msg = "Error during persist of payload: " + payload.getSapMessageKey() + ". " + e;
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
		
		// For each MessageKey fetch payloads (first and/or last)
		ArrayList<XiMessage> firstPayloads = new ArrayList<XiMessage>();
		int counter = 1;
		for (String key : messageKeys) {
			// Process a single Message Key
			logger.writeInfo(LOCATION, SIGNATURE, "-----> [ICO " + internalObjectId + "], [MSG KEY " + counter + "] MessageKey processing started for key: " + key);
			XiMessage firstPayload = this.processMessageKeySingle(key);
			
			// Only add FIRST payload if it was new (this related to MultiMapping handling
			// where many LAST keys (which is what is contained in @messageKeys for a multimap) can have the same parent (FIRST).
			// This is why in a multimap scenario the FIRST payload can be 'null'.
			if (firstPayload == null) {
				logger.writeDebug(LOCATION, SIGNATURE, "MultiMap scenario: payload was null (already processed by a previous MessageKey");
			} else {
				firstPayloads.add(firstPayload);
			}

			logger.writeInfo(LOCATION, SIGNATURE, "-----> [ICO " + internalObjectId + "], [MSG KEY " + counter + "] MessageKey processing finished");
			counter++;
		}
		return firstPayloads;
	}
	
	
	/**
	 * Get FIRST payload for single MessageKey
	 * @param key
	 */
	private XiMessage processMessageKeySingle(String key) throws ExtractorException {
		// Create a new MessageKey object
		MessageKey msgKey = new MessageKey(this, key);
			
		// Attach a reference to newly created MessageKey object to this ICO
		this.messageKeys.add(msgKey);
						
		XiMessage payloadFirst = msgKey.getBasicFirstInfo(key);
		return payloadFirst;
	}
	
}
