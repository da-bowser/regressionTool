package com.invixo.extraction;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Stream;

import com.invixo.common.GeneralException;
import com.invixo.common.IcoOverviewInstance;
import com.invixo.common.IntegratedConfigurationMain;
import com.invixo.common.Payload;
import com.invixo.common.PayloadException;
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
				Payload firstPayload = currentPayloadsLink.getFirstPayload();
				ArrayList<Payload> lastPayloads = currentPayloadsLink.getLastPayloadList();

				int total = lastPayloads.size()-1;
				for (int i=total; i>=0; i--) {
					Payload currentLast = lastPayloads.get(i);
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
		ArrayList<Payload> firstPayloads = collectBasicFirstInfoForAllKeys(firstMessageKeys, currentIcoCount);
		int firstPaylodsTotal = firstPayloads.size();
		logger.writeInfo(LOCATION, SIGNATURE, "FIRST: basic info collected. Number of FIRST payloads: " + firstPaylodsTotal);
		
		// Add basic LAST info
		int firstCounter = 0;
		ArrayList<Payloads> payloadsLinkList = new ArrayList<Payloads>();
		for (Payload firstPayload : firstPayloads) {
			firstCounter++;
			logger.writeInfo(LOCATION, SIGNATURE, "Find basic LAST info for FIRST entry ["+ firstCounter + "/" + firstPaylodsTotal + "]. Referenced FIRST key: " + firstPayload.getSapMessageKey());
			
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
				Payload lastPayload = new Payload();
				lastPayload.setSapMessageKey(messageKey);

				// Add current LAST payload to current FIRST payload
				currentPayloadsLink.getLastPayloadList().add(lastPayload);
			}
			payloadsLinkList.add(currentPayloadsLink);
			logger.writeInfo(LOCATION, SIGNATURE, "Number of LAST keys found: " + currentPayloadsLink.getLastPayloadList().size());
			logger.writeInfo(LOCATION, SIGNATURE, "Finished finding basic info for LAST keys for FIRST key: " + firstPayload.getSapMessageKey());
		}
		 
		// Persist payload (FIRST and/or LAST)
		storePayloads(payloadsLinkList, isInit);
		
		return payloadsLinkList;
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
			Payload firstPayload = currentPayloadsLink.getFirstPayload();
			ArrayList<Payload> lastPayloads = currentPayloadsLink.getLastPayloadList();

			for (int i=0; i < lastPayloads.size(); i++) {
				Payload currentLast = lastPayloads.get(i);
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
			Payload firstPayload = currentPayloadsLink.getFirstPayload();
			ArrayList<Payload> lastPayloads = currentPayloadsLink.getLastPayloadList();
			logger.writeDebug(LOCATION, SIGNATURE, "Number of LAST payloads to lookup and persist for current FIRST payload: " + lastPayloads.size());
			
			// Persist FIRST
			if (Boolean.parseBoolean(GlobalParameters.PARAM_VAL_EXTRACT_MODE_INIT)) {
				persist(firstPayload, true);				
			}

			// Persist LAST
			for (Payload lastPaylad : lastPayloads) {
				persist(lastPaylad, false);
			}
		}
		logger.writeDebug(LOCATION, SIGNATURE, "Finished persisting payloads to file system");
	}
	

	private void persist(Payload payload, boolean isFirst)  throws ExtractorException {
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
		} catch (PayloadException e) {
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
	private ArrayList<Payload> collectBasicFirstInfoForAllKeys(HashSet<String> messageKeys, int internalObjectId) throws ExtractorException {
		final String SIGNATURE = "collectBasicFirstInfoForAllKeys(HashSet<String>, int)";
		
		// For each MessageKey fetch payloads (first and/or last)
		ArrayList<Payload> firstPayloads = new ArrayList<Payload>();
		int counter = 1;
		for (String key : messageKeys) {
			// Process a single Message Key
			logger.writeInfo(LOCATION, SIGNATURE, "-----> [ICO " + internalObjectId + "], [MSG KEY " + counter + "] MessageKey processing started for key: " + key);
			Payload firstPayload = this.processMessageKeySingle(key);
			
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
	private Payload processMessageKeySingle(String key) throws ExtractorException {
		// Create a new MessageKey object
		MessageKey msgKey = new MessageKey(this, key);
			
		// Attach a reference to newly created MessageKey object to this ICO
		this.messageKeys.add(msgKey);
						
		Payload payloadFirst = msgKey.getBasicFirstInfo(key);
		return payloadFirst;
	}
	
}
