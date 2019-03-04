package com.invixo.common;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMultipart;

import com.invixo.common.util.HttpException;
import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.common.util.XiMessageUtil;
import com.invixo.consistency.FileStructure;
import com.invixo.extraction.ExtractorException;
import com.invixo.extraction.WebServiceUtil;

public class XiMessage {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = XiMessage.class.getName();	
	
	private String sapMessageKey = null;			// SAP Message Key used to get payload
	private String sapMessageId = null;				// SAP Message Id
	
	private Multipart xiMultipart = null;			// SAP XI MultiPart message
	private BodyPart xiHeader = null;				// SAP XI Header
	private BodyPart xiPayload = null;				// SAP XI Payload
	private int versionFirst = 0;					// By default, 0 = FIRST payload (before mapping)
	private int versionLast = -1;					// By default, -1 = LAST payload (after mapping)
	
	private STATUS payloadFoundStatus = STATUS.UNKNOWN;
	
	public enum STATUS {UNKNOWN, FOUND, NOT_FOUND}; 
	
	
	public XiMessage() {}
	
	
	public String getSapMessageKey() {
		return sapMessageKey;
	}

	
	public void setSapMessageKey(String sapMessageKey) {
		this.sapMessageKey = sapMessageKey;
		this.sapMessageId = Util.extractMessageIdFromKey(sapMessageKey);
	}

	
	public String getSapMessageId() {
		return this.sapMessageId;
	}

	
	public Multipart getXiMultipart() {
		return this.xiMultipart;
	}

	
	private void setXiMultipart(byte[] base64EncodedMultiPart) throws XiMessageException {
		final String SIGNATURE = "setXiMultipart(byte[])";
		try {
			// Decode
			byte[] decodedMessage = Base64.getMimeDecoder().decode(base64EncodedMultiPart);
			
			// Create multipart message from decoded base64
			MimeMultipart mmp = XiMessageUtil.createMultiPartMessage(decodedMessage);
		
			// Set MultiPart message
			this.xiMultipart = mmp;			
		} catch (MessagingException e) {
			String msg = "Error creating MultiPart message from decoded base64 message\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new XiMessageException(msg);
		}
	}

	
	public BodyPart getXiHeader() {
		return this.xiHeader;
	}

	
	private void setXiHeader(Multipart multiPartMessage) throws XiMessageException {
		final String SIGNATURE = "setXiHeader(Multipart)";
		try {
			BodyPart bp = XiMessageUtil.getHeaderFromMultiPartMessage(multiPartMessage);
			logger.writeDebug(LOCATION, SIGNATURE, "SAP XI Header fetched from multipart message");
			
			this.xiHeader = bp;			
		} catch (MessagingException e) {
			String msg = "Error extracting SAP XI Header from MultiPart message\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new XiMessageException(msg);
		}
	}

	
	public BodyPart getXiPayload() {
		return this.xiPayload;
	}

	
	private void setXiPayload(Multipart multiPartMessage) throws XiMessageException {
		final String SIGNATURE = "setXiPayload(Multipart)";
		try {
			BodyPart bp = XiMessageUtil.getPayloadFromMultiPartMessage(multiPartMessage);
			logger.writeDebug(LOCATION, SIGNATURE, "SAP XI payload fetched from multipart message");
			
			this.xiPayload = bp;			
		} catch (MessagingException e) {
			String msg = "Error extracting SAP XI Header from MultiPart message\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new XiMessageException(msg);
		}
	}

		
	public void setMultipartBase64Bytes(String multipartBase64Bytes) throws XiMessageException {
		this.setXiMultipart(multipartBase64Bytes.getBytes());
		
		// Set SAP XI Header
		this.setXiHeader(this.xiMultipart);
	
		// Set SAP XI Payload
		this.setXiPayload(this.xiMultipart);
	}
	
	
	public String getFileName() {
		String fileName = "";
		if (this.sapMessageKey != null) {
			fileName = this.sapMessageKey.replace("\\", "_");
		}
		return fileName + FileStructure.PAYLOAD_FILE_EXTENSION;
	}
	
	
	public void persistMessage(String path) throws XiMessageException {
		final String SIGNATURE = "persistMessage(String)";
		String targetPath = null;
		try {
			targetPath = path + this.getFileName();
			FileOutputStream fos = new FileOutputStream(targetPath);
			this.getXiMultipart().writeTo(fos);			
		} catch (IOException|MessagingException e) {
			String msg = "Error writing SAP XI MultiPart message to filesystem using path: " + targetPath + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new XiMessageException(msg);
		}
	}
	
	
	public void setPayloadFoundStatus(STATUS status) {
		this.payloadFoundStatus = status;
	}

	
	public STATUS getPayloadFoundStatus() {
		return this.payloadFoundStatus;
	}
	
	
	/**
	 * Extract payload from SAP PO system.
	 * @param isFirst
	 * @throws XiMessageException
	 */
	public void extractPayloadFromSystem(boolean isFirst) throws XiMessageException {
		final String SIGNATURE = "extractPayload(String, boolean)";
		try {	
			// Lookup SAP XI Message
			int version = isFirst ? this.versionFirst : this.versionLast;
			String base64EncodedMessage = WebServiceUtil.lookupSapXiMessage(this.getSapMessageKey(), version);
			
			// Check if payload was found
			if ("".equals(base64EncodedMessage)) {
				logger.writeDebug(LOCATION, SIGNATURE, "Web Service response contains no XI message.");
				setPayloadFoundStatus(XiMessage.STATUS.NOT_FOUND);
			} else {
				logger.writeDebug(LOCATION, SIGNATURE, "Web Service response contains XI message.");
				setPayloadFoundStatus(XiMessage.STATUS.FOUND);
				setMultipartBase64Bytes(base64EncodedMessage);
			}
		} catch (IOException e) {
			String msg = "Error reading bytes from WebService response.\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new XiMessageException(msg);
		} catch (ExtractorException e) {
			String msg = "Error handling XML creation (request) or extraction (response).\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new XiMessageException(msg);
		} catch (XiMessageException e) {
			String msg = "Error unwrapping multipart message and getting parts from it.\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new XiMessageException(msg);
		} catch (HttpException e) {
			String msg = "Error calling service when fetching payload.\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new XiMessageException(msg);
		}
	}

	
	public int getSequenceIdFromMessageKey() {
		String[] parts = this.sapMessageKey.split("\\\\");
		return Integer.parseInt(parts[4]);
	}
	
	
	public void clearPayload() {
		this.xiMultipart = null;
		this.xiHeader = null;
		this.xiPayload = null;
	}


	public int getVersionFirst() {
		return versionFirst;
	}


	public void setVersionFirst(int versionFirst) {
		this.versionFirst = versionFirst;
	}


	public int getVersionLast() {
		return versionLast;
	}


	public void setVersionLast(int versionLast) {
		this.versionLast = versionLast;
	}
	
}
