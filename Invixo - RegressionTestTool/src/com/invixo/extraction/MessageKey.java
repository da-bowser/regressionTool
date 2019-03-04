package com.invixo.extraction;

import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.common.XiMessage;
import com.invixo.common.XiMessageException;
import com.invixo.common.util.HttpException;
import com.invixo.main.GlobalParameters;

class MessageKey {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = MessageKey.class.getName();	

	private String sapMessageKey = null;				// SAP Message Key from Web Service response of GetMessageList
	private String sapMessageId = null;					// SAP Message Id 
	private IntegratedConfiguration ico	= null;			// Integrated Configuration
	private XiMessage xiMessageFirst = new XiMessage(); // FIRST payload
	private XiMessage xiMessageLast = new XiMessage();	// LAST payload
	private Exception ex = null;						// Error details

		
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
		
	
	public XiMessage getXiMessageFirst() {
		return xiMessageFirst;
	}

	
	public XiMessage getXiMessageLast() {
		return xiMessageLast;
	}
	
	
	/**
	 * Determine original FIRST message from PO of a MultiMapping interface (1:n multiplicity).
	 * NB:	for a multimapping scenario GetMessageList always returns LAST message keys. This is why these 
	 * 		require translation into a FIRST message key.
	 * @param messageId
	 * @return
	 * @throws ExtractorException
	 * @throws XiMessageException
	 */
	private XiMessage processMessageKeyMultiMapping(String messageId) throws ExtractorException, XiMessageException {
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
			XiMessage payload = null;
			if (ico.getMultiMapFirstMsgKeys().contains(messageKey)) {
				// MessageKey already processed and FIRST message details already found.
				// Do nothing
				logger.writeDebug(LOCATION, SIGNATURE, "Skip looking up FIRST msg, since previously found for current message key: " + messageKey);
			} else {
				// Fetch FIRST XI Message using the original FIRST messageKey
				payload = new XiMessage();
				payload.setSapMessageKey(messageKey);

				// Add current, processed MessageKey to complete list of unique, previously found, FIRST XI Messages
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
	 * @throws XiMessageException
	 * @throws ExtractorException 
	 */
	XiMessage getBasicFirstInfo(String key) throws ExtractorException {
		final String SIGNATURE = "getBasicFirstInfo(String)";
		try {
			XiMessage xiMessage = null;
			// Process according to multiplicity
			if (this.ico.isUsingMultiMapping()) {
				// Fetch XI Message: FIRST for multimapping interface (1:n multiplicity)
				xiMessage = this.processMessageKeyMultiMapping(Util.extractMessageIdFromKey(key));
			} else {
				// Fetch XI Message: FIRST for non-multimapping interface (1:1 multiplicity)	
				xiMessage = new XiMessage();
				xiMessage.setSapMessageKey(key);
			}
			
			return xiMessage;
		} catch (XiMessageException e) {
			String msg = "Error finding basic FIRST info for key: " + key + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);
		}
	}

}