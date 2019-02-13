package com.invixo.directory.api;

import java.util.ArrayList;

public class IntegratedConfiguration {

	private String senderPartyId = "";
	private String senderComponentId = "";
	private String senderInterfaceName = "";
	private String senderInterfaceNamespace = "";
	private String qualityOfService = "";
	private ArrayList<Receiver> receiverList = new ArrayList<Receiver>(); 
	
	
	
	/*====================================================================================
	 *------------- Getters and Setters
	 *====================================================================================*/	
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
	
	
	public ArrayList<Receiver> getReceiverList() {
		return receiverList;
	}
	
	
	public void setReceiverList(ArrayList<Receiver> receiverList) {
		this.receiverList = receiverList;
	}

}
