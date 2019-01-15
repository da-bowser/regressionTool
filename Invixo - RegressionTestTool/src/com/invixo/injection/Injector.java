package com.invixo.injection;

import com.invixo.common.util.Logger;

public class Injector {
	private static Logger logger 			= Logger.getInstance();
	private static final String LOCATION 	= Injector.class.getName();
	private String configurationFile 		= null;		// A request configuration file (foundation file for MessageList web service calls)
	private int payloadFilesProcessed		= 0;
	
	
	
	public static void main(String[] args) {
	// Tis
	}

	
	// Overloaded constructor
	Injector(String file) {
		this.configurationFile = file;
	}
	
	

	
	/**
	 * Inject a request SOAP message into SAP PO 
	 */
	private void injectSingleFile(String payloadFile) {
		String SIGNATURE = "injectSingleFile(String)";
		
		// Create a new SOAP request file ready to be injected into SAP PO
		String injectionFile = RequestGenerator.generateInjectionFile(configurationFile, payloadFile);
		logger.writeError(LOCATION, SIGNATURE, "Injection file created: " + injectionFile);
	
		// 
	}
	
}
