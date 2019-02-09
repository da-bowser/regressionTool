package com.invixo.injection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.http.client.methods.HttpPost;

import com.invixo.common.GeneralException;
import com.invixo.common.IntegratedConfigurationMain;
import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.consistency.FileStructure;
import com.invixo.injection.webServices.WebServiceHandler;
import com.invixo.main.GlobalParameters;


public class IntegratedConfiguration extends IntegratedConfigurationMain  {
	/*====================================================================================
	 *------------- Class variables
	 *====================================================================================*/
	private static Logger logger 						= Logger.getInstance();
	private static final String LOCATION 				= IntegratedConfiguration.class.getName();	
	public static BufferedWriter mapWriter				= null; 	// Writer for creating MAPPING file between original SAP message ID and new SAP message ID
	private int filesToBeProcessedTotal					= 0;
	
	
	
	/*====================================================================================
	 *------------- Instance variables
	 *====================================================================================*/
	private String sourcePayloadDirectory	= null;		// Directory containing payload files to be injected
	private ArrayList<InjectionRequest> injections 	= new ArrayList<InjectionRequest>();

	
	
	/*====================================================================================
	 *------------- Constructors
	 *====================================================================================*/
	public IntegratedConfiguration(String icoFileName) throws GeneralException {
		super(icoFileName);
		initialize();
	}
	
	
	public IntegratedConfiguration(String icoFileName, String mapfilePath, String sourceEnv, String targetEnv) throws GeneralException {
		super(icoFileName, mapfilePath, sourceEnv, targetEnv);
		initialize();
	}
	
	
	
	/*====================================================================================
	 *------------- Getters and Setters
	 *====================================================================================*/
	public ArrayList<InjectionRequest> getInjections() {
		return this.injections;
	}	
	
	

	/*====================================================================================
	 *------------- Instance methods
	 *====================================================================================*/
	/**
	 * Inject FIRST payloads to SAP PO based on single ICO request file
	 */
	public void startInjection() {
		final String SIGNATURE = "startInjection()";
		InjectionRequest ir = null;
		try {
			// Get list of all request/payload files related to ICO
			File[] files = Util.getListOfFilesInDirectory(this.sourcePayloadDirectory);
			this.filesToBeProcessedTotal = files.length;
			logger.writeDebug(LOCATION, SIGNATURE, "Number of payload files to be processed: " + this.filesToBeProcessedTotal);
			
			// Prepare
			if (files.length > 0) {
				// Only create mapping file if there are files to inject and if it is not created already
				initMappingTableWriter();
			}

			// Process each payload file
			for (File file : files) {
				ir = new InjectionRequest();
				injections.add(ir);
				
				// NB: message injection for single payloads terminates on first error!
				injectMessage(file.getAbsolutePath(), ir);
			}
		} catch (InjectionException e) {
			this.setEx(e);
		} catch (InjectionPayloadException e) {
			if (ir != null ) {
				ir.setError(e);
			}
		} finally {
			try {
				// Logging
				String msg = "Number of processed payload files: " + this.injections.size();
				logger.writeDebug(LOCATION, SIGNATURE, msg);
				
				// Close resources
				if (mapWriter != null) {
					mapWriter.flush();
				}
			} catch (Exception e) {
				// Too bad...
			}
		}
	}


	/**
	 * Initialize writer enabling writing to mapping file for source/target SAP message IDs
	 * @throws InjectionException
	 */
	private static void initMappingTableWriter() throws InjectionException {
		final String SIGNATURE = "initMappingTableWriter()";
		try {
			if (mapWriter == null) {
				mapWriter = Files.newBufferedWriter(Paths.get(FileStructure.FILE_MSG_ID_MAPPING), Charset.forName(GlobalParameters.ENCODING));
				logger.writeDebug(LOCATION, SIGNATURE, "SAP message Id mapping file initialized: " + FileStructure.FILE_MSG_ID_MAPPING);
			}
		} catch (IOException e) {
			String msg = "Error initializing SAP MessageId mapping file "+ FileStructure.FILE_MSG_ID_MAPPING + ".\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new InjectionException(msg);
		}
	}
	
	
	/**
	 * Main entry point for processing/injecting a single payload file to SAP PO.
	 * Create HTTP request message to be sent to SAP PO and inject it to the system.
	 * Routing info is extracted from the ICO Request file.
	 * Payload is taken from the referenced payload file.
	 * @param payloadFile
	 * @param ir
	 * @throws InjectionPayloadException
	 */
	private void injectMessage(String payloadFile, InjectionRequest ir) throws InjectionPayloadException {
		final String SIGNATURE = "injectMessage(String, InjectionRequest)";
		try {
			logger.writeDebug(LOCATION, SIGNATURE, "---- (File " + this.injections.size() + " / " + this.filesToBeProcessedTotal + ") Payload processing BEGIN: " + payloadFile);
			ir.setSourcePayloadFile(payloadFile);

			// Add payload to injection request. Payload is taken from an "instance" payload file (a file extracted previously)
			byte[] payload = Util.readFile(payloadFile);
			logger.writeDebug(LOCATION, SIGNATURE, "Payload size (MB): " + Util.convertBytesToMegaBytes(payload.length));
			
			// Generate SOAP XI Header
			String soapXiHeader = RequestGeneratorUtil.generateSoapXiHeaderPart(this, ir);
			
			// Build Request to be sent via Web Service call
			HttpPost webServiceRequest = WebServiceHandler.buildHttpPostRequest(soapXiHeader.getBytes(GlobalParameters.ENCODING), payload); 
			
			// Store request on file system (only relevant for debugging purposes)
			if (GlobalParameters.DEBUG) {
				ir.setInjectionRequestFile(FileStructure.getDebugFileName("InjectionMultipart", true, this.getName() + "_" + ir.getMessageId(), "txt"));
				webServiceRequest.getEntity().writeTo(new FileOutputStream(new File(ir.getInjectionRequestFile())));
				logger.writeDebug(LOCATION, SIGNATURE, "<debug enabled> Request message to be sent to SAP PO is stored here: " + ir.getInjectionRequestFile());
			}
			
			// Call SAP PO Web Service (using XI protocol)
			WebServiceHandler.callWebService(webServiceRequest);
			
			// Write entry to mapping file
			addMappingEntryToFile(Util.getFileName(payloadFile, false), ir.getMessageId(), this.getName());
		} catch (IOException e) {
			String msg = "Error injecting new request to SAP PO for ICO file " + super.fileName + " and payload file " + payloadFile + ".\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new InjectionPayloadException(msg);
		} finally {
			logger.writeDebug(LOCATION, SIGNATURE, "---- Payload processing END");
		}
	}
	
	
	/**
	 * Write new SAP Message ID (source --> target -->) mapping to mapping file 
	 * @param sourceMsgId
	 * @param targetMsgId
	 * @param icoName
	 * @throws IOException
	 */
	private void addMappingEntryToFile(String sourceMsgId, String targetMsgId, String icoName) throws IOException {
		final String SIGNATURE = "addMappingEntryToFile(String, String, String)";
		final String separator = GlobalParameters.FILE_DELIMITER;
		
		// Create mapping line
		String mapEntry = System.currentTimeMillis() + separator + sourceMsgId + separator + targetMsgId + separator + icoName + "\n";
		
		// Write line to map
		mapWriter.write(mapEntry);
		
		logger.writeDebug(LOCATION, SIGNATURE, "Map file update with new entry: " + mapEntry);
	}
		
	
	/**
	 * Implementation specific object initialization
	 */
	protected void initialize() throws GeneralException {
		final String SIGNATURE = "initialize()";

		// Set directory for Payloads (FIRST)
		this.sourcePayloadDirectory = FileStructure.DIR_EXTRACT_OUTPUT_PRE + super.name + "\\" + GlobalParameters.PARAM_VAL_SOURCE_ENV + FileStructure.DIR_EXTRACT_OUTPUT_POST_FIRST_ENVLESS;
		logger.writeDebug(LOCATION, SIGNATURE, "Source directory containing FIRST messages: " + this.sourcePayloadDirectory);
	}
	
}
