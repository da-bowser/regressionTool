package com.invixo.extraction;

import com.invixo.common.util.Logger;
import com.invixo.common.util.PropertyAccessor;
import com.invixo.main.GlobalParameters;

public class Payload {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = Payload.class.getName();	
	public static final String PAYLOAD_FOUND = "Found";
	public static final String PAYLOAD_NOT_FOUND = "Not found";
	static final String ENDPOINT = GlobalParameters.SAP_PO_HTTP_HOST_AND_PORT + PropertyAccessor.getProperty("SERVICE_PATH_EXTRACT");
	
		
	private String sapMessageKey = null;			// SAP Message Key used to get payload
	private String sapMessageId = null;				// SAP Message Id
	private String xiMessageInResponse = "unknown";	// Indicates if a payload/message was returned by SAP PO or not

	// Target file
	private String targetPath = null;				// Path (no filename) to create target payload file
	private String fileName = null;					// File name
	
	
	
	// Constructor
	public Payload(String sapMessageKey, String sapMessageId, boolean isFirst, String targetPath, String fileName) {
		super();
		this.setSapMessageKey(sapMessageKey);
		this.setSapMessageId(sapMessageId);
		this.setTargetPath(targetPath);
		this.setFileName(fileName); 
	}
	
	
	
	// Getters and Setters
	public String getSapMessageKey() {
		return sapMessageKey;
	}

	public void setSapMessageKey(String sapMessageKey) {
		this.sapMessageKey = sapMessageKey;
	}

	public String getSapMessageId() {
		return sapMessageId;
	}

	public void setSapMessageId(String sapMessageId) {
		this.sapMessageId = sapMessageId;
	}

	public String getXiMessageInResponse() {
		return xiMessageInResponse;
	}

	public void setXiMessageInResponse(String xiMessageInResponse) {
		this.xiMessageInResponse = xiMessageInResponse;
	}

	public String getTargetPath() {
		return targetPath;
	}

	public void setTargetPath(String targetPath) {
		this.targetPath = targetPath;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	
}
