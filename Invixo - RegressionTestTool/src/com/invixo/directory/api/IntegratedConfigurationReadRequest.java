package com.invixo.directory.api;

class IntegratedConfigurationReadRequest {
	private String senderPartyId = "";
	private String senderComponentId = "";
	private String senderInterfaceName = "";
	private String senderInterfaceNamespace = "";
	private String receiverPartyId = "";
	private String receiverComponentId = "";
	
	
	
	
	/*====================================================================================
	 *------------- Getters and Setters
	 *====================================================================================*/
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
	
	
	public String getReceiverPartyId() {
		return receiverPartyId;
	}
	public void setReceiverPartyId(String receiverPartyId) {
		this.receiverPartyId = receiverPartyId;
	}
	
	
	public String getReceiverComponentId() {
		return receiverComponentId;
	}
	
	
	public void setReceiverComponentId(String receiverComponentId) {
		this.receiverComponentId = receiverComponentId;
	}
}
