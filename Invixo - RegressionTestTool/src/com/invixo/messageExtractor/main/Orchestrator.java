package com.invixo.messageExtractor.main;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

import com.invixo.common.util.Util;
import com.invixo.messageExtractor.blocks.BGetMessageBytesJavaLangStringIntBoolean;
import com.invixo.messageExtractor.blocks.BGetMessageList;
import com.invixo.messageExtractor.httpHandlers.HGetMessageBytesJavaLangStringIntBoolean;
import com.invixo.messageExtractor.httpHandlers.HGetMessageList;
import com.invixo.messageExtractor.util.Logger;
import com.invixo.messageExtractor.util.PropertyAccessor;


/**
 * This program uses SAP PO Message API.
 * It extracts message payloads from SAP PO database.
 *
 * Configuration of program is done in the 'apiConfig.properties' file part of the project.
 */
public class Orchestrator {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = Orchestrator.class.getName();
	public static final String ENCODING = PropertyAccessor.getProperty("ENCODING");
	public static final boolean EXTRACT_FIRST_PAYLOAD = Boolean.parseBoolean(PropertyAccessor.getProperty("EXTRACT_FIRST_PAYLOAD"));
	public static final boolean EXTRACT_LAST_PAYLOAD = Boolean.parseBoolean(PropertyAccessor.getProperty("EXTRACT_LAST_PAYLOAD"));
	
	
	public static void main(String[] args) {
		try {	
			// Test process for all files
			startAll();	
			
			// Test process for a single file
//			startSingle("GetMessageListRequest.xml");			
		} catch (Exception e) {
			System.err.println("\nMessage caught in MAIN: \n" + e);
		}
	}
	

	public static void startAll() {
		String SIGNATURE = "startAll()";
		
		try {
			// Cleanup: delete all files contained in directory for Web Service responses for method GetMessageBytesJavaLangStringIntBoolean
			Util.deleteFilesInDirectory(HGetMessageBytesJavaLangStringIntBoolean.DIR_RESPONSE);
			logger.writeDebug(LOCATION, SIGNATURE, "Housekeeping: all response files deleted from directory: " + HGetMessageBytesJavaLangStringIntBoolean.DIR_RESPONSE);
			
			// Get list of all request files to be processed
			File[] files = Util.getListOfFilesInDirectory(HGetMessageList.DIR_REQUEST);
			logger.writeDebug(LOCATION, SIGNATURE, "--- Number of request files: " + files.length);
			
			// Process each request file
			for (File file : files) {
				logger.writeDebug(LOCATION, SIGNATURE, "Start processing file: " + file);
				startSingle(file.getName());
			}
			
			logger.writeDebug(LOCATION, SIGNATURE, "Processing finished successfully! ");
		} catch (Exception e) {
			String ex = "Processing terminated with error!";
			logger.writeError(LOCATION, SIGNATURE, ex);
			throw new RuntimeException(e);
		}
	}
	
	
	private static  void startSingle(String file) throws Exception {
		String SIGNATURE = "startSingle(String)";
		
		// GetMessageList: call web service
		InputStream responseBytes = HGetMessageList.invoke(file);
		
		// Extract MessageKeys from web service response
		ArrayList<String> messageKeys = BGetMessageList.extractMessageKeysFromSingleResponseFile(responseBytes);
		logger.writeDebug(LOCATION, SIGNATURE, "Number of MessageKeys contained in Web Service response: " + messageKeys.size());
		
		// For each MessageKey fetch payloads (first and last)
		for (String key : messageKeys) {
			processSingleMessageKey(key);
		}
	}
	
	
	/**
	 * Processes a single MessageKey returned in Web Service response for service GetMessageList.
	 * This includes storing the raw Web Service response from service GetMessageBytesJavaLangStringIntBoolean on file system
	 * and also storing the extracted SAP PO payload contained within this response (also as a file on file system).
	 * @param key
	 * @throws Exception
	 */
	private static void processSingleMessageKey(String key) throws Exception {
		String SIGNATURE = "processSingleMessageKey(String)";
		logger.writeDebug(LOCATION, SIGNATURE, "--> Processing message key: " + key);
		logger.writeDebug(LOCATION, SIGNATURE, "--> Payload extraction enabled for FIRST payload: " + EXTRACT_FIRST_PAYLOAD);
		logger.writeDebug(LOCATION, SIGNATURE, "--> Payload extraction enabled for LAST payload: " + EXTRACT_LAST_PAYLOAD);

		// Fetch payload: FIRST
		if (EXTRACT_FIRST_PAYLOAD) {
			BGetMessageBytesJavaLangStringIntBoolean.processSingleMessageKey(key, true);
		}
		
		// Fetch payload: LAST
		if (EXTRACT_LAST_PAYLOAD) {
			 BGetMessageBytesJavaLangStringIntBoolean.processSingleMessageKey(key, false);			
		}
	}

}
