package com.invixo.injection;

import java.io.File;
import org.apache.http.client.methods.HttpPost;

import com.invixo.common.util.Logger;
import com.invixo.common.util.PropertyAccessor;
import com.invixo.common.util.Util;
import com.invixo.consistency.FileStructure;

public class Injector {
	private static Logger logger 			= Logger.getInstance();
	private static final String LOCATION 	= Injector.class.getName();
	private static final String ENCODING 	= PropertyAccessor.getProperty("ENCODING");
	private String configurationFile 		= null;		// A request configuration file (foundation file for MessageList web service calls)
	private int payloadFilesProcessed		= 0;
	
	
	public static void main(String[] args) {
		String icoRequestFile 	= "c:\\Users\\dhek\\Desktop\\Test\\NoScenario - Invixo - DEK.xml";
//		String payloadFile 		= "c:\\Users\\dhek\\Desktop\\Test\\NoScenario - Invixo - DEK - PAYLOAD.xml";
		Injector injector = new Injector(icoRequestFile);
		
		// Test name generation for target file
//		String targetFileName = getTargetFileName(icoRequestFile, "7447b182-1968-11e9-c902-000000554e16");
//		System.out.println("Target file name: " + targetFileName);
		
		// Test injection of FIRST payload files
		injector.injectAllMessages();
		System.out.println("TEST: all messages should be injected!");
		
		// Test injection of single message
//		injector.injectMessage(payloadFile);
	}

	
	// Overloaded constructor
	public Injector(String file) {
		this.configurationFile = file;
	}
	
	
	public void injectAllMessages() {
		String SIGNATURE = "injectAllMessages()";
		try {
			// Determine input directory containing FIRST payloads relevant for current ICO
			String scenarioName = new File(this.configurationFile).getName().replaceFirst(".xml", "");
			String directory = FileStructure.DIR_REGRESSION_OUTPUT_PAYLOADS_FIRST_MSG_VERSION + scenarioName;
			logger.writeDebug(LOCATION, SIGNATURE, "Source directory containing FIRST messages: " + directory);
			
			// Get list of all request files related to ICO
			File[] files = Util.getListOfFilesInDirectory(directory);
			logger.writeDebug(LOCATION, SIGNATURE, "Number of request files to be processed: " + files.length);
			
			// Process each request file
			for (File file : files) {
				logger.writeDebug(LOCATION, SIGNATURE, "Start processing file: " + file);
				injectMessage(file.getAbsolutePath());
				payloadFilesProcessed++;
			}
			
			// Log
			logger.writeDebug(LOCATION, SIGNATURE, "Number of request files processed: " + this.payloadFilesProcessed);
		} catch (Exception e) {
			
		}
	}
	
	
	/**
	 * Create HTTP request message to be sent to SAP PO and inject it to the system.
	 * Routing info is extracted from the ICO Request file.
	 * Payload is taken from the referenced payload file.
	 * @param payloadFile
	 */
	private void injectMessage(String payloadFile) {
		String SIGNATURE = "injectMessage(String)";
		try {
			// Create injection request object
			InjectionRequest ir = new InjectionRequest();
			
			// Extract relevant properties into POJO from request file
			RequestGeneratorUtil.extractInfoFromIcoRequest(ir, this.configurationFile);
			
			// Add payload to injection request. Payload is taken from an "instance" payload file (file extracted from the system)
			ir.setPayload(Util.readFile(payloadFile));

			// Generate SOAP XI Header
			String soapXiHeader = RequestGeneratorUtil.generateSoapXiHeaderPart(ir);
			
			// Build Request to be sent via Web Service call
			HttpPost webServiceRequest = WebServiceHandler.buildHttpPostRequest(soapXiHeader.getBytes(ENCODING), ir.getPayload()); 
			
			// Store request on file system (just for pleasant reference)
			String fileName = getTargetFileName(this.configurationFile, ir.getMessageId());
			Util.writeFileToFileSystem(fileName, webServiceRequest.getEntity().getContent().readAllBytes());
			logger.writeError(LOCATION, SIGNATURE, "Request message to be sent to SAP PO is stored here: " + fileName);
	        
			// Call SAP PO Web Service (using XI protocol)
			WebServiceHandler.callWebService(webServiceRequest);
		} catch (Exception e) {
			String msg = "Error injecting new request to SAP PO for ICO file " + this.configurationFile + " and payload file " + payloadFile + ".\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		}
	}
	
	
	private static String getTargetFileName(String icoRequestfile, String messageId) {
		File file = new File(icoRequestfile);
		String scenarioName = file.getName().replaceFirst(".xml", "");
		String targetFile = FileStructure.DIR_REGRESSION_INPUT_INJECTION + scenarioName + " -- " +  messageId + ".txt";
		return targetFile;
	}
	
}
