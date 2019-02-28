package com.invixo.extraction;

import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.common.Payload;
import com.invixo.common.PayloadException;
import com.invixo.common.util.HttpException;
import com.invixo.main.GlobalParameters;

public class MessageKey {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = MessageKey.class.getName();	

	private String sapMessageKey = null;			// SAP Message Key from Web Service response of GetMessageList
	private String sapMessageId = null;				// SAP Message Id 
	private IntegratedConfiguration ico	= null;		// Integrated Configuration
	private Payload payloadFirst = new Payload(); 	// FIRST payload
	private Payload payloadLast = new Payload();	// LAST payload
	private Exception ex = null;					// Error details

		
	MessageKey(IntegratedConfiguration ico, String messageKey) {
		this.ico = ico;
		this.setSapMessageKey(messageKey);
		this.setSapMessageId(messageKey);
	}

	
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
		
	
	public Payload getPayloadFirst() {
		return payloadFirst;
	}

	
	public Payload getPayloadLast() {
		return payloadLast;
	}
	
	
	/**
	 * Extract original FIRST message from PO of a MultiMapping interface (1:n multiplicity).
	 * NB:	for a multimapping scenario GetMessageList always returns LAST message keys. This is why these 
	 * 		require translation into a FIRST message key.
	 * @param messageId
	 * @return
	 * @throws ExtractorException
	 * @throws PayloadException
	 */
	private Payload processMessageKeyMultiMapping(String messageId) throws ExtractorException, PayloadException {
		final String SIGNATURE = "processMessageKeyMultiMapping(String)";
		try {
			logger.writeDebug(LOCATION, SIGNATURE, "MessageKey [FIRST] MultiMapping processing start");
			
			String parentId = messageId;
			if (Boolean.parseBoolean(GlobalParameters.PARAM_VAL_EXTRACT_MODE_INIT)) {
				// Lookup parent (FIRST) Message Id
				parentId = WebServiceUtil.lookupPredecessorMessageId(messageId, this.ico.getName());
			}

			// Lookup parent (FIRST) Message Key
			String messageKey = WebServiceUtil.lookupMessageKey(parentId, this.ico.getName());
			
			// Many of the Message IDs returned by GetMessageList response may have the same parent.
			// We are only interested in extracting data for the parent once.
			Payload payload = null;
			if (ico.getMultiMapFirstMsgKeys().contains(messageKey)) {
				// MessageKey already processed and FIRST message details already found.
				// Do nothing
				logger.writeDebug(LOCATION, SIGNATURE, "Skip looking up FIRST msg, since previously found for current message key: " + messageKey);
			} else {
				// Fetch FIRST payload using the original FIRST messageKey
				payload = new Payload();
				payload.setSapMessageKey(messageKey);

				// Add current, processed MessageKey to complete list of unique, previously found, FIRST payloads
				ico.getMultiMapFirstMsgKeys().add(messageKey);
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
	 * Get basic FIRST message info (Message Key and Message Id)
	 * @param key
	 * @return
	 * @throws PayloadException
	 * @throws ExtractorException 
	 */
	Payload getBasicFirstInfo(String key) throws ExtractorException {
		final String SIGNATURE = "getBasicFirstInfo(String)";
		try {
			Payload payload = null;
			// Process according to multiplicity
			if (this.ico.isUsingMultiMapping()) {
				// Fetch payload: FIRST for multimapping interface (1:n multiplicity)
				payload = this.processMessageKeyMultiMapping(Util.extractMessageIdFromKey(key));
			} else {
				// Fetch payload: FIRST for non-multimapping interface (1:1 multiplicity)	
				payload = new Payload();
				payload.setSapMessageKey(key);
			}
			
			return payload;
		} catch (PayloadException e) {
			this.setEx(e);
			String msg = "Error finding basic FIRST info for key: " + key + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);
		}
	}

}