package com.invixo.injection;

import java.util.UUID;

public class InjectionRequest {
	private String messageId = UUID.randomUUID().toString();
	private String sourcePayloadFile = null;					// Source (extracted) payload 
	private String injectionRequestFile = null;					// Injection request (SAP XI multipart message)
	private Exception error = null;
	
	public String getMessageId() {
		return messageId;
	}
	public String getSourcePayloadFile() {
		return sourcePayloadFile;
	}
	public void setSourcePayloadFile(String payloadFile) {
		this.sourcePayloadFile = payloadFile;
	}
	public String getInjectionRequestFile() {
		return injectionRequestFile;
	}
	public void setInjectionRequestFile(String injectionRequestFile) {
		this.injectionRequestFile = injectionRequestFile;
	}
	public Exception getError() {
		return error;
	}
	public void setError(Exception error) {
		this.error = error;
	}
	
}
