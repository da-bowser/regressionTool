package com.invixo.directory.api;

public class ReceiverInterfaceRule {
	private String interfaceOperation;
	private String interfaceName;
	private String interfaceNamespace;	
	
	
	/*====================================================================================
	 *------------- Getters and Setters
	 *====================================================================================*/
	public String getInterfaceOperation() {
		return interfaceOperation;
	}
	
	
	public void setInterfaceOperation(String operation) {
		this.interfaceOperation = operation;
	}
	
	
	public String getInterfaceName() {
		return interfaceName;
	}
	
	
	public void setInterfaceName(String interfaceName) {
		this.interfaceName = interfaceName;
	}
	
	
	public String getInterfaceNamespace() {
		return interfaceNamespace;
	}
	
	
	public void setInterfaceNamespace(String interfaceNamespace) {
		this.interfaceNamespace = interfaceNamespace;
	}
	
}
