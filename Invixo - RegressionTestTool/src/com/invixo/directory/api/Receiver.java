package com.invixo.directory.api;

import java.util.ArrayList;

public class Receiver {
	private String partyId = "";
	private String componentId = "";
	private ArrayList<ReceiverInterfaceRule> receiverInterfaceRules = new ArrayList<ReceiverInterfaceRule>();
	
	
	
	
	/*====================================================================================
	 *------------- Getters and Setters
	 *====================================================================================*/
	public String getPartyId() {
		return partyId;
	}
	
	
	public void setPartyId(String partyId) {
		this.partyId = partyId;
	}
	
	
	public String getComponentId() {
		return componentId;
	}
	
	
	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}


	public ArrayList<ReceiverInterfaceRule> getReceiverInterfaceRules() {
		return receiverInterfaceRules;
	}


	public void setReceiverInterfaceRules(ArrayList<ReceiverInterfaceRule> receiverInterfaceRules) {
		this.receiverInterfaceRules = receiverInterfaceRules;
	}
}
