package com.invixo.injection;

import java.util.UUID;

public class InjectionRequest {
	private String messageId = UUID.randomUUID().toString();
	private String sourceMultiPartFile = null;					// Source: FIRST XI Message extracted during INIT 
	private String injectionRequestFile = null;					// Injection request (SAP XI multipart message)
	private Exception error = null;
	
	public String getMessageId() {
		return messageId;
	}
	public String getSourceMultiPartFile() {
		return sourceMultiPartFile;
	}
	public void setSourceMultiPartFile(String messageFile) {
		this.sourceMultiPartFile = messageFile;
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
