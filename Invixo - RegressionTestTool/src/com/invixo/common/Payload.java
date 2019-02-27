package com.invixo.common;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMultipart;

import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.common.util.XiMessageUtil;
import com.invixo.consistency.FileStructure;

public class Payload {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = Payload.class.getName();	
	
	private String sapMessageKey = null;			// SAP Message Key used to get payload
	private String sapMessageId = null;				// SAP Message Id
	
	private Multipart xiMultipart = null;			// SAP XI MultiPart message
	private BodyPart xiHeader = null;				// SAP XI Header
	private BodyPart xiPayload = null;				// SAP XI Payload
	
	private STATUS payloadFoundStatus = STATUS.UNKNOWN;
	
	public enum STATUS {UNKNOWN, FOUND, NOT_FOUND}; 
	
	
	public Payload() {}
	
	
	public String getSapMessageKey() {
		return sapMessageKey;
	}

	
	public void setSapMessageKey(String sapMessageKey) {
		this.sapMessageKey = sapMessageKey;
		this.sapMessageId = Util.extractMessageIdFromKey(sapMessageKey);
	}

	
	public String getSapMessageId() {
		return sapMessageId;
	}

	
	public Multipart getXiMultipart() {
		return xiMultipart;
	}

	
	private void setXiMultipart(byte[] base64EncodedMultiPart) throws PayloadException {
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
			throw new PayloadException(msg);
		}
	}

	
	public BodyPart getXiHeader() {
		return xiHeader;
	}

	
	private void setXiHeader(Multipart multiPartMessage) throws PayloadException {
		final String SIGNATURE = "setXiHeader(Multipart)";
		try {
			BodyPart bp = XiMessageUtil.getHeaderFromMultiPartMessage(multiPartMessage);
			logger.writeDebug(LOCATION, SIGNATURE, "SAP XI Header fetched from multipart message");
			
			this.xiHeader = bp;			
		} catch (MessagingException e) {
			String msg = "Error extracting SAP XI Header from MultiPart message\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new PayloadException(msg);
		}
	}

	
	public BodyPart getXiPayload() {
		return xiPayload;
	}

	
	private void setXiPayload(Multipart multiPartMessage) throws PayloadException {
		final String SIGNATURE = "setXiPayload(Multipart)";
		try {
			BodyPart bp = XiMessageUtil.getPayloadFromMultiPartMessage(multiPartMessage);
			logger.writeDebug(LOCATION, SIGNATURE, "SAP XI payload fetched from multipart message");
			
			this.xiPayload = bp;			
		} catch (MessagingException e) {
			String msg = "Error extrac SAP XI Header from MultiPart message\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new PayloadException(msg);
		}
	}

		
	public void setMultipartBase64Bytes(String multipartBase64Bytes) throws PayloadException {
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
	
	
	public void persistMessage(String path) throws PayloadException {
		final String SIGNATURE = "persistMessage(String)";
		String targetPath = null;
		try {
			targetPath = path + this.getFileName();
			FileOutputStream fos = new FileOutputStream(targetPath);
			this.getXiMultipart().writeTo(fos);			
		} catch (IOException|MessagingException e) {
			String msg = "Error writing SAP XI MultiPart message to filesystem using path: " + targetPath + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new PayloadException(msg);
		}
	}
	
	
	public void setPayloadFoundStatus(STATUS status) {
		this.payloadFoundStatus = status;
	}

	
	public STATUS getPayloadFoundStatus() {
		return this.payloadFoundStatus;
	}
	
}
