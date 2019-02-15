package com.invixo.directory.api;

public class ReceiverInterfaceRule {
	private String interfaceOperation = "";
	private String interfaceName = "";
	private String interfaceNamespace = "";	
	private String interfaceMappingName = "";
	private String interfaceMappingNamespace = "";
	private String interfaceMultiplicity = "";
	private String interfaceMappingSoftwareComponentVersionId = "";
	private RepositorySimpleQueryException ex = null;
	
	
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


	public String getInterfaceMappingName() {
		return interfaceMappingName;
	}


	public void setInterfaceMappingName(String interfaceMappingName) {
		this.interfaceMappingName = interfaceMappingName;
	}


	public String getInterfaceMappingNamespace() {
		return interfaceMappingNamespace;
	}


	public void setInterfaceMappingNamespace(String interfaceMappingNamespace) {
		this.interfaceMappingNamespace = interfaceMappingNamespace;
	}


	public String getInterfaceMappingSoftwareComponentVersionId() {
		return interfaceMappingSoftwareComponentVersionId;
	}


	public void setInterfaceMappingSoftwareComponentVersionId(String interfaceMappingSoftwareComponentVersionId) {
		this.interfaceMappingSoftwareComponentVersionId = interfaceMappingSoftwareComponentVersionId;
	}


	public String getInterfaceMultiplicity() {
		return interfaceMultiplicity;
	}


	public void setInterfaceMultiplicity(String interfaceMultiplicity) {
		this.interfaceMultiplicity = interfaceMultiplicity;
	}


	public RepositorySimpleQueryException getEx() {
		return ex;
	}


	public void setEx(RepositorySimpleQueryException ex) {
		this.ex = ex;
	}
	
}
