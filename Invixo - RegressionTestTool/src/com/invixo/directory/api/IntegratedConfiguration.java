package com.invixo.directory.api;

public class IntegratedConfiguration {

	private String senderPartyId = "";
	private String senderComponentId = "";
	private String senderInterfaceName = "";
	private String senderInterfaceNamespace = "";
	private String operation = "";
	private String receiverPartyId = "";
	private String receiverComponentId = "";
	private String receiverInterfaceName = "";
	private String receiverInterfaceNamespace = "";
	private String qualityOfService = "";
	
	
	public String getOperation() {
		return operation;
	}
	public void setOperation(String operation) {
		this.operation = operation;
	}
	public String getReceiverPartyId() {
		return receiverPartyId;
	}
	public void setReceiverPartyId(String receiverPartyId) {
		this.receiverPartyId = receiverPartyId;
	}
	public String getReceiverComponentId() {
		return receiverComponentId;
	}
	public void setReceiverComponentId(String receiverComponent) {
		this.receiverComponentId = receiverComponent;
	}
	public String getReceiverInterfaceName() {
		return receiverInterfaceName;
	}
	public void setReceiverInterfaceName(String receiverInterfaceName) {
		this.receiverInterfaceName = receiverInterfaceName;
	}
	public String getReceiverInterfaceNamespace() {
		return receiverInterfaceNamespace;
	}
	public void setReceiverInterfaceNamespace(String receiverInterfaceNamespace) {
		this.receiverInterfaceNamespace = receiverInterfaceNamespace;
	}
	public String getQualityOfService() {
		return qualityOfService;
	}
	public void setQualityOfService(String qualityOfService) {
		this.qualityOfService = qualityOfService;
	}

	public String getSenderPartyId() {
		return senderPartyId;
	}
	public void setSenderPartyId(String senderPartyId) {
		this.senderPartyId = senderPartyId;
	}
	public String getSenderComponentId() {
		return senderComponentId;
	}
	public void setSenderComponentId(String senderComponentId) {
		this.senderComponentId = senderComponentId;
	}
	public String getSenderInterfaceName() {
		return senderInterfaceName;
	}
	public void setSenderInterfaceName(String senderInterfaceName) {
		this.senderInterfaceName = senderInterfaceName;
	}
	public String getSenderInterfaceNamespace() {
		return senderInterfaceNamespace;
	}
	public void setSenderInterfaceNamespace(String senderInterfaceNamespace) {
		this.senderInterfaceNamespace = senderInterfaceNamespace;
	}


}
