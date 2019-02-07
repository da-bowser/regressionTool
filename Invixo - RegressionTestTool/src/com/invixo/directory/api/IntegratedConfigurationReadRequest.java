package com.invixo.directory.api;

public class IntegratedConfigurationReadRequest {
	private String senderPartyId = "";
	private String senderComponentId = "";
	private String senderInterfaceName = "";
	private String senderInterfaceNamespace = "";
	
	
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
