package com.invixo.injection;

import com.invixo.common.util.PropertyAccessor;

public class InjectionRequest {

	private static final String SAP_HOST_PORT = PropertyAccessor.getProperty("SERVICE_HOST_PORT");
	private String senderParty = null;
	private String senderComponent = null;
	private String senderInterface = null;
	private String senderNamespace = null;
	private String receiverParty = null;
	private String receiverComponent = null;
	private String qualityOfService = null;
	private String endpoint = null;
	private byte[] payload = null;
	
	public String getSenderParty() {
		return senderParty;
	}
	public void setSenderParty(String senderParty) {
		this.senderParty = senderParty;
	}
	public String getSenderComponent() {
		return senderComponent;
	}
	public void setSenderComponent(String senderComponent) {
		this.senderComponent = senderComponent;
	}
	public String getSenderInterface() {
		return senderInterface;
	}
	public void setSenderInterface(String senderInterface) {
		this.senderInterface = senderInterface;
	}
	public String getSenderNamespace() {
		return senderNamespace;
	}
	public void setSenderNamespace(String senderNamespace) {
		this.senderNamespace = senderNamespace;
	}
	public String getReceiverParty() {
		return receiverParty;
	}
	public void setReceiverParty(String receiverParty) {
		this.receiverParty = receiverParty;
	}
	public String getReceiverComponent() {
		return receiverComponent;
	}
	public void setReceiverComponent(String receiverComponent) {
		this.receiverComponent = receiverComponent;
	}
	public String getQualityOfService() {
		return qualityOfService;
	}
	public void setQualityOfService(String qualityOfService) {
		this.qualityOfService = qualityOfService;
	}
	public String getEndpoint() {
		return endpoint;
	}
	public void setEndpoint(String endpoint) {
		this.endpoint = SAP_HOST_PORT + endpoint;
	}
	public byte[] getPayload() {
		return payload;
	}
	public void setPayload(byte[] payload) {
		this.payload = payload;
	}
}
