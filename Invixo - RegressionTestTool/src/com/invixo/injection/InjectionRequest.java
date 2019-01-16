package com.invixo.injection;

import java.util.UUID;

public class InjectionRequest {
	private String senderParty = null;
	private String senderComponent = null;
	private String senderInterface = null;
	private String senderNamespace = null;
	private String receiverParty = null;
	private String receiverComponent = null;
	private String qualityOfService = null;
	private String messageId = UUID.randomUUID().toString();
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
	public String getMessageId() {
		return messageId;
	}
	public byte[] getPayload() {
		return payload;
	}
	public void setPayload(byte[] payload) {
		this.payload = payload;
	}
}
