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
		String payloadFile 		= "c:\\Users\\dhek\\Desktop\\Test\\NoScenario - Invixo - DEK - PAYLOAD.xml";
		
		// Test name generation for target file
//		String targetFileName = getTargetFileName(icoRequestFile, "7447b182-1968-11e9-c902-000000554e16");
//		System.out.println("Target file name: " + targetFileName);
		
		// Test injection of single message
		injectMessage(icoRequestFile, payloadFile);
	}

	
	// Overloaded constructor
	public Injector(String file) {
		this.configurationFile = file;
	}
	
	
	/**
	 * 
	 * @param icoRequestfile
	 * @param payloadFile
	 */
	public static void injectMessage(String icoRequestfile, String payloadFile) {
		String SIGNATURE = "injectMessage(String, String)";
		try {
			// Create injection request object
			InjectionRequest ir = new InjectionRequest();
			
			// Extract relevant properties into POJO from request file
			RequestGeneratorUtil.extractInfoFromIcoRequest(ir, icoRequestfile);
			
			// Add payload to injection request. Payload is taken from an "instance" payload file (file extracted from the system)
			ir.setPayload(Util.readFile(payloadFile));

			// Generate SOAP XI Header
			String soapXiHeader = RequestGeneratorUtil.generateSoapXiHeaderPart(ir);
			
			// Build Request to be sent via Web Service call
			HttpPost webServiceRequest = WebServiceHandler.buildHttpPostRequest(soapXiHeader.getBytes(ENCODING), ir.getPayload()); 
			
			// Store request on file system (just for pleasant reference)
			String fileName = getTargetFileName(icoRequestfile, ir.getMessageId());
			Util.writeFileToFileSystem(fileName, webServiceRequest.getEntity().getContent().readAllBytes());
			logger.writeError(LOCATION, SIGNATURE, "Request message to be sent to SAP PO is stored here: " + fileName);
	        
			// Call SAP PO Web Service (using XI protocol)
			WebServiceHandler.callWebService(webServiceRequest);
		} catch (Exception e) {
			String msg = "Error injecting new request to SAP PO for ICO file " + icoRequestfile + " and payload file " + payloadFile + ".\n" + e.getMessage();
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
