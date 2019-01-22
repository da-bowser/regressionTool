package com.invixo.injection;

import java.util.UUID;

public class InjectionRequest {

	private String messageId = UUID.randomUUID().toString();
	private String sourcePayloadFile = null;
	private String injectionRequestFile = null;
	private InjectionPayloadException error = null;
	
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
	public InjectionPayloadException getError() {
		return error;
	}
	public void setError(InjectionPayloadException error) {
		this.error = error;
	}
	
}
