package com.invixo.common;


public class IcoOverviewInstance {
	private String name = null;
	private boolean active = false;
	private String qualityOfService = null;
	private String fromTime = null;
	private String toTime = null;
	private int maxMessages = 0;

	private boolean isUsingMultiMapping = false;
	
	private String senderParty = null;
	private String senderComponent = null;
	private String senderInterface = null;
	private String senderNamespace = null;

	private String receiverParty = null;
	private String receiverComponent = null;
	private String receiverInterface = null;
	private String receiverNamespace = null;
	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean isActive() {
		return active;
	}
	public void setActive(boolean active) {
		this.active = active;
	}
	public String getQualityOfService() {
		return qualityOfService;
	}
	public void setQualityOfService(String qualityOfService) {
		this.qualityOfService = qualityOfService;
	}
	public String getFromTime() {
		return fromTime;
	}
	public void setFromTime(String fromTime) {
		this.fromTime = fromTime;
	}
	public String getToTime() {
		return toTime;
	}
	public void setToTime(String toTime) {
		this.toTime = toTime;
	}
	public int getMaxMessages() {
		return maxMessages;
	}
	public void setMaxMessages(int maxMessages) {
		this.maxMessages = maxMessages;
	}
	public boolean isUsingMultiMapping() {
		return isUsingMultiMapping;
	}
	public void setUsingMultiMapping(boolean isUsingMultiMapping) {
		this.isUsingMultiMapping = isUsingMultiMapping;
	}
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
	public String getReceiverInterface() {
		return receiverInterface;
	}
	public void setReceiverInterface(String receiverInterface) {
		this.receiverInterface = receiverInterface;
	}
	public String getReceiverNamespace() {
		return receiverNamespace;
	}
	public void setReceiverNamespace(String receiverNamespace) {
		this.receiverNamespace = receiverNamespace;
	}
}
