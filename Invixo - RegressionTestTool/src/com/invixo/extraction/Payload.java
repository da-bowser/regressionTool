package com.invixo.extraction;

import com.invixo.common.util.Util;
import com.invixo.consistency.FileStructure;

public class Payload {
	private String sapMessageKey = null;			// SAP Message Key used to get payload
	private String sapMessageId = null;				// SAP Message Id
	
	private String path = null;						// Path (no filename) to create target payload file
	private String fileName = null;					// File name (no path)
	private String payloadFoundStatus = "unknown";
	
	
	// Constructor
	public Payload() {}
	
	
	
	// Getters and Setters
	public String getSapMessageKey() {
		return sapMessageKey;
	}

	void setSapMessageKey(String sapMessageKey) {
		this.sapMessageKey = sapMessageKey;
		this.sapMessageId = Util.extractMessageIdFromKey(sapMessageKey);
	}

	public String getSapMessageId() {
		return sapMessageId;
	}

	public String getPath() {
		return path;
	}

	void setPath(String path) {
		this.path = path;
	}

	public String getFileName() {
		return fileName;
	}

	void setFileName(String fileName) {
		this.fileName = fileName + FileStructure.PAYLOAD_FILE_EXTENSION;
	}

	public String getPayloadFoundStatus() {
		return payloadFoundStatus;
	}

	void setPayloadFoundStatus(String payloadFoundStatus) {
		this.payloadFoundStatus = payloadFoundStatus;
	}
	
}
