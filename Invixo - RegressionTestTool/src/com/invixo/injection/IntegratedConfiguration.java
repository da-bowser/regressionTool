package com.invixo.injection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.http.client.methods.HttpPost;

import com.invixo.common.util.InjectionException;
import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.consistency.FileStructure;
import com.invixo.injection.webServices.WebServiceHandler;
import com.invixo.main.GlobalParameters;


public class IntegratedConfiguration {
	/*====================================================================================
	 *------------- Class variables
	 *====================================================================================*/
	private static Logger logger 			= Logger.getInstance();
	private static final String LOCATION 	= IntegratedConfiguration.class.getName();	
	private static final String MAP_FILE	= FileStructure.DIR_REGRESSION_OUTPUT_MAPPING + "Map_" + System.currentTimeMillis() + ".txt";
	
	
	/*====================================================================================
	 *------------- Instance variables
	 *====================================================================================*/
	private String name 							= null;		// Name of ICO
	private String fileName							= null;		// Complete path to ICO request file
	private String payloadDirectory					= null;		// Directory containing payload files to be injected
	private Exception ex 							= null;		// Error information in case of error
	private BufferedWriter mapWriter				= null; 	// Writer for creating MAPPING file between original SAP message ID and new SAP message ID
	private int payloadFilesProcessed				= 0;		// Number of payload files processed
	private ArrayList<InjectionRequest> injections 	= new ArrayList<InjectionRequest>();
	
	
	
	/*====================================================================================
	 *------------- Constructors
	 *====================================================================================*/
	public IntegratedConfiguration(String icoFileName) throws InjectionException {
		final String SIGNATURE = "IntegratedConfiguration(String)";
		this.fileName = icoFileName;
		this.name = Util.getFileName(icoFileName, false);
		this.payloadDirectory = FileStructure.DIR_REGRESSION_OUTPUT_PAYLOADS_FIRST_MSG_VERSION + this.name;
		logger.writeDebug(LOCATION, SIGNATURE, "Source directory containing FIRST messages: " + this.payloadDirectory);
	}
	
	
	
	/*====================================================================================
	 *------------- Getters and Setters
	 *====================================================================================*/
	public String getName() {
		return this.name;
	}
	public String getFileName() {
		return this.fileName;
	}	
	public Exception getEx() {
		return this.ex;
	}
		
	

	/*====================================================================================
	 *------------- Instance methods
	 *====================================================================================*/
	/**
	 * Inject FIRST payloads to SAP PO based on ICO request file
	 */
	public void injectAllMessagesForSingleIco() throws InjectionException {
		String SIGNATURE = "injectAllMessages()";
		try {
			// Get list of all request files related to ICO
			File[] files = Util.getListOfFilesInDirectory(this.payloadDirectory);
			logger.writeDebug(LOCATION, SIGNATURE, "Number of payload files to be processed: " + files.length);
			
			// Only create mapping file if there are files to inject and if it is not created already
			if (files.length > 0 && this.mapWriter == null) {
				this.mapWriter = Files.newBufferedWriter(Paths.get(MAP_FILE), Charset.forName(GlobalParameters.ENCODING));
				logger.writeDebug(LOCATION, SIGNATURE, "SAP message Id mapping file generated: " + MAP_FILE);
			}
			
			// Process each payload file
			for (File file : files) {
				logger.writeDebug(LOCATION, SIGNATURE, "Start processing payload file: " + file);
				injectMessage(file.getAbsolutePath());
				payloadFilesProcessed++;
			}

			// Log
			if (files.length > 0) { 
				logger.writeDebug(LOCATION, SIGNATURE, "Number of payload files processed: " + this.payloadFilesProcessed);				
			}
		} catch (Exception e) {
			String msg = "Error occurred during injection! Number of processed payload files: " + this.payloadFilesProcessed + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new InjectionException(msg);
		} finally {
			try {
				if (this.mapWriter != null) {
					this.mapWriter.flush();
					this.mapWriter.close();
				}
			} catch (Exception e) {
				// Too bad...
			}
		}
	}
	
	
	/**
	 * Create HTTP request message to be sent to SAP PO and inject it to the system.
	 * Routing info is extracted from the ICO Request file.
	 * Payload is taken from the referenced payload file.
	 * @param payloadFile
	 */
	private void injectMessage(String payloadFile) throws InjectionException {
		String SIGNATURE = "injectMessage(String)";
		try {
			// Create injection request object
			InjectionRequest ir = new InjectionRequest();
			
			// Extract relevant properties into POJO from request file
			RequestGeneratorUtil.extractInfoFromIcoRequest(ir, this.fileName);
			
			// Add payload to injection request. Payload is taken from an "instance" payload file (file extracted from the system)
			ir.setPayload(Util.readFile(payloadFile));

			// Generate SOAP XI Header
			String soapXiHeader = RequestGeneratorUtil.generateSoapXiHeaderPart(ir);
			
			// Build Request to be sent via Web Service call
			HttpPost webServiceRequest = WebServiceHandler.buildHttpPostRequest(soapXiHeader.getBytes(GlobalParameters.ENCODING), ir.getPayload()); 
			
			// Store request on file system (just for pleasant reference)
			String fileName = getTargetFileName(this.fileName, ir.getMessageId());
			webServiceRequest.getEntity().writeTo(new FileOutputStream(new File(fileName)));
			logger.writeError(LOCATION, SIGNATURE, "Request message to be sent to SAP PO is stored here: " + fileName);
	        
			// Call SAP PO Web Service (using XI protocol)
			WebServiceHandler.callWebService(webServiceRequest);
			
			// Write entry to mapping file
			addMappingEntryToFile(Util.getFileName(payloadFile, false), ir.getMessageId());
		} catch (IOException e) {
			String msg = "Error injecting new request to SAP PO for ICO file " + this.fileName + " and payload file " + payloadFile + ".\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg + e);
			throw new InjectionException(msg);
		}
	}
	
	
	private void addMappingEntryToFile(String sourceMsgId, String targetMsgId) throws IOException {
		final String SIGNATURE = "addMappingEntryToFile(String, String)";
		final String separator = "|";
		
		// Create mapping line
		String mapEntry = sourceMsgId + separator + targetMsgId + "\n";
		
		// Write line to map
		this.mapWriter.write(mapEntry);
		logger.writeDebug(LOCATION, SIGNATURE, "Map file update with new entry: " + mapEntry);
	}
	
	
	private static String getTargetFileName(String icoRequestfile, String messageId) {
		String scenarioName = Util.getFileName(icoRequestfile, false);
		String targetFile = FileStructure.DIR_REGRESSION_INPUT_INJECTION + scenarioName + " -- " +  messageId + ".txt";
		return targetFile;
	}
	
}
