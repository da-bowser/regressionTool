package com.invixo.injection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.xml.stream.XMLStreamException;

import org.apache.http.client.methods.HttpPost;

import com.invixo.common.GeneralException;
import com.invixo.common.IcoOverviewInstance;
import com.invixo.common.IntegratedConfigurationMain;
import com.invixo.common.StateException;
import com.invixo.common.StateHandler;
import com.invixo.common.util.Logger;
import com.invixo.common.util.PropertyAccessor;
import com.invixo.common.util.Util;
import com.invixo.common.util.XiHeader;
import com.invixo.common.util.XiMessageUtil;
import com.invixo.common.util.HttpException;
import com.invixo.common.util.HttpHandler;
import com.invixo.consistency.FileStructure;
import com.invixo.main.GlobalParameters;


public class IntegratedConfiguration extends IntegratedConfigurationMain  {
	/*====================================================================================
	 *------------- Class variables
	 *====================================================================================*/
	private static Logger logger 						= Logger.getInstance();
	private static final String LOCATION 				= IntegratedConfiguration.class.getName();
	
	private static final String SERVICE_HOST_PORT 		= GlobalParameters.SAP_PO_HTTP_HOST_AND_PORT;
	private static final String SERVICE_PATH_INJECT 	= PropertyAccessor.getProperty("SERVICE_PATH_INJECT") + GlobalParameters.PARAM_VAL_SENDER_COMPONENT + ":" + GlobalParameters.PARAM_VAL_XI_SENDER_ADAPTER;
	private static final String ENDPOINT 				= SERVICE_HOST_PORT + SERVICE_PATH_INJECT;
	
	private int filesToBeProcessedTotal					= 0;

	
	
	/*====================================================================================
	 *------------- Instance variables
	 *====================================================================================*/
	private String sourcePayloadDirectory	= null;		// Directory containing payload files to be injected
	private ArrayList<InjectionRequest> injections 	= new ArrayList<InjectionRequest>();

	
	
	/*====================================================================================
	 *------------- Constructors
	 *====================================================================================*/
	IntegratedConfiguration(IcoOverviewInstance icoInstance) throws GeneralException {
		super(icoInstance);
		initialize();
	}
	
	
	public IntegratedConfiguration(IcoOverviewInstance icoInstance, String mapfilePath, String sourceEnv, String targetEnv) throws GeneralException {
		super(icoInstance, mapfilePath, sourceEnv, targetEnv);
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
	 * Inject FIRST payloads to SAP PO based on single ICO request
	 */
	void startInjection() {
		final String SIGNATURE = "startInjection()";
		InjectionRequest ir = null;
		try {
			// State Handling: prepare
			StateHandler.init(GlobalParameters.Operation.valueOf(GlobalParameters.PARAM_VAL_OPERATION), this.getName());
			
			// Get unique FIRST file names
			HashSet<String> uniqueFirstMessages = StateHandler.getUniqueFirstFileNames();
			
			// Get list of FIRST lines
			this.filesToBeProcessedTotal = uniqueFirstMessages.size();
			logger.writeInfo(LOCATION, SIGNATURE, "Number of FIRST messages to be processed: " + this.filesToBeProcessedTotal);

			// Process each FIRST SAP XI Message file
			for (String firstMessage : uniqueFirstMessages) {
				ir = new InjectionRequest();
				injections.add(ir);
				
				// NB: message injection for single messages terminates on first error!
				injectMessage(this.getFilePathFirstPayloads() + firstMessage, ir);
			}
			
			// State Handling: persist
			StateHandler.replaceInjectTemplateWithId();
			StateHandler.storeIcoState();
		} catch (InjectionPayloadException|HttpException|StateException e) {
			if (ir != null) {
				ir.setError(e);
			}
			
			// Set error on entire ICO
			this.setEx(new GeneralException("Error occurred while injecting a payload file. Entire ICO set in error state"));
		} finally {
			try {
				// Logging
				String msg = "Number of processed FIRST messages: " + this.injections.size();
				logger.writeInfo(LOCATION, SIGNATURE, msg);
			} catch (Exception e) {
				// Too bad...
			} finally {
				this.endTime = Util.getTime();
			}
		}
	}
	
	
	/**
	 * Main entry point for processing/injecting a single payload file to SAP PO.
	 * Create HTTP request message to be sent to SAP PO and inject it to the system.
	 * Payload is taken from the referenced SAP XI Message file. 
	 * @param sapXiMessage
	 * @param ir
	 * @throws InjectionPayloadException
	 * @throws HttpException
	 */
	private void injectMessage(String sapXiMessage, InjectionRequest ir) throws InjectionPayloadException, HttpException {
		final String SIGNATURE = "injectMessage(String, InjectionRequest)";
		try {
			logger.writeInfo(LOCATION, SIGNATURE, "---- (File " + this.injections.size() + " / " + this.filesToBeProcessedTotal + ") Message processing BEGIN: " + sapXiMessage);
			ir.setSourceMultiPartFile(sapXiMessage);

			// Get FIRST multipart SAP XI Message
			Multipart firstMultipart = XiMessageUtil.createMultiPartMessage(Util.readFile(sapXiMessage));
			
			// Get payload to be injected from FIRST multipart
			byte[] payload = XiMessageUtil.getPayloadBytesFromMultiPartMessage(firstMultipart);
			logger.writeInfo(LOCATION, SIGNATURE, "Payload size (MB): " + Util.convertBytesToMegaBytes(payload.length));
			
			// Generate SOAP XI Header
			String soapXiHeader = RequestGeneratorUtil.generateSoapXiHeaderPart(this, ir.getMessageId());
			
			// Build Request to be sent via Web Service call
			HttpPost webServiceRequest = HttpHandler.buildMultipartHttpPostRequest(ENDPOINT, soapXiHeader.getBytes(GlobalParameters.ENCODING), payload); 
			
			// Store request on file system (only relevant for debugging purposes)
			if (GlobalParameters.DEBUG) {
				ir.setInjectionRequestFile(FileStructure.getDebugFileName("InjectionMultipart", true, this.getName() + "_" + ir.getMessageId(), "txt"));
				webServiceRequest.getEntity().writeTo(new FileOutputStream(new File(ir.getInjectionRequestFile())));
				logger.writeDebug(LOCATION, SIGNATURE, "<debug enabled> Request message to be sent to SAP PO is stored here: " + ir.getInjectionRequestFile());
			}
			
			// Call SAP PO Web Service (using XI protocol)
			HttpHandler.post(webServiceRequest);
			
			// Get XI Header from FIRST multipart
			XiHeader firstXiHeader = XiMessageUtil.deserializeXiHeader(firstMultipart);
			
			// Add new entry to internal list of lines to be updated after injection
			StateHandler.addInjectEntry(firstXiHeader.getMessageId(), ir.getMessageId());
		} catch (IOException | MessagingException e) {
			String msg = "Error injecting new request to SAP PO for ICO " + super.getName() + " with source message file " + sapXiMessage + ".\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new InjectionPayloadException(msg);
		} catch (XMLStreamException e) {
				String msg = "Error deserializing XI Header contained in FIRST multipart: " + sapXiMessage + ".\n" + e.getMessage();
				logger.writeError(LOCATION, SIGNATURE, msg);
				throw new InjectionPayloadException(msg);
		} finally {
			logger.writeInfo(LOCATION, SIGNATURE, "---- Message processing END");
		}
	}
		
	
	/**
	 * Implementation specific object initialization
	 */
	private void initialize() throws GeneralException {
		final String SIGNATURE = "initialize()";

		// Set directory for Payloads (FIRST)
		this.sourcePayloadDirectory = FileStructure.DIR_EXTRACT_OUTPUT_PRE + super.getName() + "\\" + GlobalParameters.PARAM_VAL_SOURCE_ENV + FileStructure.DIR_EXTRACT_OUTPUT_POST_FIRST_ENVLESS;
		logger.writeDebug(LOCATION, SIGNATURE, "Source directory containing FIRST messages: " + this.sourcePayloadDirectory);
	}
	
}
