package com.invixo.extraction;

import java.io.IOException;
import java.util.ArrayList;

import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.common.Payload;
import com.invixo.common.PayloadException;
import com.invixo.common.StateHandler;
import com.invixo.common.util.HttpException;
import com.invixo.main.GlobalParameters;

public class MessageKey {
	/*====================================================================================
	 *------------- Class variables
	 *====================================================================================*/
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = MessageKey.class.getName();	


	
	/*====================================================================================
	 *------------- Instance variables
	 *====================================================================================*/
	private String sapMessageKey = null;			// SAP Message Key from Web Service response of GetMessageList
	private String sapMessageId = null;				// SAP Message Id 
	private IntegratedConfiguration ico	= null;		// Integrated Configuration
	private Payload payloadFirst = new Payload(); 	// FIRST payload
	private Payload payloadLast = new Payload();	// LAST payload
	private ArrayList<String> multiMapMessageKeys;	// List of Parent Message Keys in the case of Multimapping scenario
	private Exception ex = null;					// Error details
	
	
	
	/*====================================================================================
	 *------------- Constructors
	 *====================================================================================*/
	MessageKey(IntegratedConfiguration ico, String messageKey) {
		this.ico = ico;
		this.setSapMessageKey(messageKey);
		this.setSapMessageId(messageKey);
		
		if (this.ico.isUsingMultiMapping()) {
			this.multiMapMessageKeys = new ArrayList<String>();
		}
	}



	/*====================================================================================
	 *------------- Getters and Setters
	 *====================================================================================*/
	public String getSapMessageKey() {
		return sapMessageKey;
	}
	
	public void setSapMessageKey(String sapMessageKey) {
		this.sapMessageKey = sapMessageKey;
	}
	
	public String getSapMessageId() {
		return sapMessageId;
	}
	
	public void setSapMessageId(String sapMessageKey) {
		this.sapMessageId = Util.extractMessageIdFromKey(sapMessageKey);
	}

	public Exception getEx() {
		return ex;
	}
	
	public void setEx(Exception e) {
		this.ex = e;
	}
	
	public ArrayList<String> getMultiMapMessageKeys() {
		return multiMapMessageKeys;
	}
	
	public Payload getPayloadFirst() {
		return payloadFirst;
	}

	public Payload getPayloadLast() {
		return payloadLast;
	}

	
	
	/*====================================================================================
	 *------------- Instance methods
	 *====================================================================================*/
	
	/**
	 * Main entry point
	 * Extract FIRST and/or LAST payload.
	 * @param messageKey
	 * @throws ExtractorException
	 */
	void extractAllPayloads(String messageKey) throws ExtractorException {
		// Extract FIRST payload
		if (Boolean.parseBoolean(GlobalParameters.PARAM_VAL_EXTRACT_MODE_INIT)) {
			this.payloadFirst = this.extractFirstPayload(messageKey);
		}
			
		// Extract LAST payload
		this.payloadLast = this.extractLastPayload(messageKey);
	}
	
	
	void storeState(Payload first, Payload last) throws ExtractorException {
		final String SIGNATURE = "storeState(Payload, Payload)";
		try {
			// Persist message: FIRST
			first.persistMessage(this.ico.getFilePathFirstPayloads());
			
			// Persist message: LAST
			last.persistMessage(this.ico.getFilePathLastPayloads());
			
			// Build and add new State entry line
			String newEntry = StateHandler.createExtractEntry(this.ico.getName(), first, last);
			StateHandler.addEntryToInternalList(newEntry);
		} catch (PayloadException e) {
			String msg = "Error persisting payload for MessageKey!\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			ExtractorException ex = new ExtractorException(msg);
			this.ex = ex;
			throw ex;
		}
	}
	
	
	/**
	 * Extract FIRST or LAST payload for ICOs with a mapping multiplicitiy of 1:1.
	 * Call Web Service for fetching SAP PO message data (SOAP envelope). 
	 * A normal web service response will contain an XML payload containing base64 encoded SAP XI multipart message.
	 * This method is responsible for creating a Payload object.
	 * @param messageKey
	 * @param isFirst
	 * @return
	 * @throws ExtractorException			Other errors during extraction
	 * @throws HttpException				Web Service call failed
	 * @throws PayloadException				Error setting state on Payload
	 */
	private Payload extractPayload(String messageKey, boolean isFirst) throws ExtractorException, HttpException, PayloadException {
		final String SIGNATURE = "extractPayload(String, boolean)";
		try {
			logger.writeDebug(LOCATION, SIGNATURE, "MessageKey [" + (isFirst?"FIRST":"LAST") + "] processing started...");
			
			// Lookup SAP XI Message
			String base64EncodedMessage = WebServiceUtil.lookupSapXiMessage(messageKey, isFirst);

			// Create Payload object
			Payload payload = new Payload();
			payload.setSapMessageKey(messageKey);
			
			// Check if payload was found
			if ("".equals(base64EncodedMessage)) {
				logger.writeDebug(LOCATION, SIGNATURE, "Web Service response contains no XI message.");
				payload.setPayloadFoundStatus(Payload.STATUS.NOT_FOUND);
			} else {
				logger.writeDebug(LOCATION, SIGNATURE, "Web Service response contains XI message.");
				payload.setPayloadFoundStatus(Payload.STATUS.FOUND);
				payload.setMultipartBase64Bytes(base64EncodedMessage);
			}
						
			return payload;
		} catch (IOException e) {
			String msg = "Error reading all bytes from generated web service request\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			ExtractorException ex = new ExtractorException(msg);
			this.ex = ex;
			throw ex;
		} finally {
			logger.writeDebug(LOCATION, SIGNATURE, "MessageKey [" + (isFirst?"FIRST":"LAST") + "] processing finished...");
		}
	}

	
	/**
	 * Extract original FIRST message from PO of a MultiMapping interface (1:n multiplicity).
	 * @param payload
	 * @return
	 * @throws ExtractorException
	 * @throws PayloadException
	 */
	Payload processMessageKeyMultiMapping(Payload payload) throws ExtractorException, PayloadException {
		final String SIGNATURE = "processMessageKeyMultiMapping(Payload)";
		try {
			logger.writeDebug(LOCATION, SIGNATURE, "MessageKey [FIRST] MultiMapping processing start");
			
			// Lookup parent Message Id
			String parentId = WebServiceUtil.lookupParentMessageId(payload.getSapMessageId(), this.ico.getName());
			
			// Lookup Message Key
			String messageKey = WebServiceUtil.lookupMessageKey(parentId, this.ico.getName());
			
			// Prevent processing an storing the same messageKey several times and ensure payloadFilesCreated consistency
			if (ico.getMultiMapMessageKeys().contains(messageKey)) {
				// MessageKey already processed and FIRST message is found
				// Do nothing
			} else {
				// Fetch FIRST payload using the original FIRST messageKey
				payload =  this.extractPayload(messageKey, true);
			}
			
			// Return;
			return payload;
		} catch (HttpException e) {
			String msg = "Error during web service call\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			ExtractorException ex = new ExtractorException(msg);
			this.ex = ex;
			throw ex;
		} finally {		
			logger.writeDebug(LOCATION, SIGNATURE, "MessageKey [FIRST] MultiMapping processing finished...");
		}
	}


	/**
	 * Extract FIRST payload.
	 * @param key
	 * @return
	 * @throws PayloadException
	 * @throws ExtractorException 
	 */
	private Payload extractFirstPayload(String key) throws ExtractorException {
		final String SIGNATURE = "extractFirstPayload(String)";
		try {
			Payload payload = new Payload();
			payload.setSapMessageKey(key);
			
			// Process according to multiplicity
			if (this.ico.isUsingMultiMapping()) {
				// Fetch payload: FIRST for multimapping interface (1:n multiplicity)
				payload = this.processMessageKeyMultiMapping(payload);
				
				// Save key to make sure it is only used once, as one FIRST messageKey can create multiple LAST
				this.getMultiMapMessageKeys().add(payload.getSapMessageId());
			} else {
				// Fetch payload: FIRST for non-multimapping interface (1:1 multiplicity)	
				payload = this.extractPayload(key, true);
			}
			
			return payload;
		} catch (PayloadException|ExtractorException|HttpException e) {
			this.setEx(e);
			String msg = "Error processing FIRST key: " + key + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);
		}
	}

	
	/**
	 * Extract LAST payload.
	 * @param key
	 * @returns
	 * @throws ExtractorException 
	 */
	private Payload extractLastPayload(String key) throws ExtractorException {
		final String SIGNATURE = "extractLastPayload(String)";
		try {
			// Fetch payload: LAST
			Payload payload = extractPayload(key, false);
			return payload;
		} catch (PayloadException|ExtractorException|HttpException e) {
			this.setEx(e);
			String msg = "Error processing LAST key: " + key + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);
		}
	}

}