package com.invixo.main;

import java.util.ArrayList;

import com.invixo.common.GeneralException;
import com.invixo.common.util.Logger;
import com.invixo.compare.Comparer;
import com.invixo.consistency.FileStructure;
import com.invixo.extraction.IntegratedConfiguration;
import com.invixo.extraction.reporting.ReportWriter;

public class Main {
	
	private static Logger logger 			= Logger.getInstance();
	private static final String LOCATION	= Main.class.getName();
	
	enum Environment { DEV, TST, PRD };
	enum Operation { extract, inject, compare };
	
	// Parameter: dictates which environment *all* ICO request files are based on. (used for translation/mapping of sender system)
	private static final String PARAM_KEY_ICO_REQUEST_FILES_ENV = "icoRequestFilesEnv";
	public static String PARAM_VAL_ICO_REQUEST_FILES_ENV 		= null;
	
	// Parameter: base directory for all reading and writing to/from file system
	private static final String PARAM_KEY_BASE_DIR 				= "baseDirectory";
	public static String PARAM_VAL_BASE_DIR 					= null;
	
	// Parameter: target environment to extract from, inject to or compare with
	private static final String PARAM_KEY_TARGET_ENV 			= "targetEnv";
	public static String PARAM_VAL_TARGET_ENV 					= null;
	
	// Parameter: operation for program to perform (extract, inject, compare)
	private static final String PARAM_KEY_OPERATION 			= "operation";
	public static String PARAM_VAL_OPERATION 					= null;
	
	// Parameter: source environment to compare to targetEnvironment or in the case on injection, which environment to inject payload files from
	private static final String PARAM_KEY_SOURCE_ENV 			= "sourceEnv";
	public static String PARAM_VAL_SOURCE_ENV 					= null;
	
	// Parameter: location of a credentials file (expected to contain 2 lines. First line contains user name only, second line contains password
	private static final String PARAM_KEY_CREDENTIALS_FILE		= "credentialsFile";
	private static String PARAM_VAL_CREDENTIALS_FILE 			= null;
	
	// Data read from credentials file (line 1 and 2)
	public static String CREDENTIAL_USER						= null;
	public static String CREDENTIAL_PASS 						= null;
	
	// Parameter: SAP PO host name. Example: ipod.invixo.com
	private static final String PARAM_KEY_HTTP_HOST				= "httpHost";
	public static String PARAM_VAL_HTTP_HOST					= null;

	// Parameter: SAP PO host name. Example: 50000
	private static String PARAM_KEY_HTTP_PORT					= "httpPort";
	public static String PARAM_VAL_HTTP_PORT 					= null;
	
	// SAP PO URL PREFIX/START. Example result: http://ipod.invixo.com:50000/
	public static final String SAP_PO_HTTP_HOST_AND_PORT		= null;
	
	// Parameter: SAP XI sender adapter name
	private static final String PARAM_KEY_XI_SENDER_ADAPTER		= "xiSenderAdapter";
	public static String PARAM_VAL_XI_SENDER_ADAPTER 			= null;

	// Parameter: SAP XI sender component containing the XI adapter
	private static final String PARAM_KEY_SENDER_COMPONENT		= "senderComponent";
	public static String PARAM_VAL_SENDER_COMPONENT 			= null;

	
	public static void main(String[] args) {
		for (String param : args) {	
			// Set input parameters			
			if(param.contains(PARAM_KEY_ICO_REQUEST_FILES_ENV)) {
				PARAM_VAL_ICO_REQUEST_FILES_ENV = param.replace(PARAM_KEY_ICO_REQUEST_FILES_ENV + "=", "");
			} else if(param.contains(PARAM_KEY_BASE_DIR)) {
				PARAM_VAL_BASE_DIR = param.replace(PARAM_KEY_BASE_DIR + "=", "");
			} else if(param.contains(PARAM_KEY_TARGET_ENV)) {
				PARAM_VAL_TARGET_ENV = param.replace(PARAM_KEY_TARGET_ENV + "=", "");
			} else if(param.contains(PARAM_KEY_OPERATION)) {
				PARAM_VAL_OPERATION = param.replace(PARAM_KEY_OPERATION + "=", "");
			} else if(param.contains(PARAM_KEY_SOURCE_ENV)) {
				PARAM_VAL_SOURCE_ENV = param.replace(PARAM_KEY_SOURCE_ENV + "=", "");
			} else if(param.contains(PARAM_KEY_CREDENTIALS_FILE)) {
				PARAM_VAL_CREDENTIALS_FILE = param.replace(PARAM_KEY_CREDENTIALS_FILE + "=", "");
			} else if(param.contains(PARAM_KEY_HTTP_HOST)) {
				PARAM_VAL_HTTP_HOST = param.replace(PARAM_KEY_HTTP_HOST + "=", "");
			} else if(param.contains(PARAM_KEY_HTTP_PORT)) {
				PARAM_VAL_HTTP_PORT = param.replace(PARAM_KEY_HTTP_PORT + "=", "");
			} else if(param.contains(PARAM_KEY_XI_SENDER_ADAPTER)) {
				PARAM_VAL_XI_SENDER_ADAPTER = param.replace(PARAM_KEY_XI_SENDER_ADAPTER + "=", "");
			} else if(param.contains(PARAM_KEY_SENDER_COMPONENT)) {
				PARAM_VAL_SENDER_COMPONENT = param.replace(PARAM_KEY_SENDER_COMPONENT + "=", "");
			}
		}
		
		try {
			// Validate input parameters
			validateParameters(PARAM_VAL_OPERATION);
			
			// Execute
			if (Operation.extract.toString().equals(PARAM_VAL_OPERATION)) {
				extract();
			} else if (Operation.inject.toString().equals(PARAM_VAL_OPERATION)) {
				inject(); 
			} else {
				compare();
			}
		} catch (GeneralException e) {
			// TODO: Not valid input, inform end user in the nicest way possible
		}
		
	}

	
	private static void validateParameters(String operation) throws GeneralException {
		// TODO: Validate
	}


	/**
	 * Extract data from a productive or non-productive SAP PO system.
	 * This creates payload files (FIRST and/or LAST) on file system: 
 	 * NB: remember to set the proper properties in config file. Some should probably be parameterized in the class for safety and ease.
	 */
	public static void extract() {
		final String SIGNATURE = "extract()";
		
		// Clean up file structure and ensure its consistency
		ensureFileStructureConsistency();
		
		// Start extracting
		ArrayList<IntegratedConfiguration> icoList = com.invixo.extraction.Orchestrator.start();
		
		// Write report
		ReportWriter report = new ReportWriter();
		report.interpretResult(icoList);
		String reportName = report.create(icoList);
		logger.writeDebug(LOCATION, SIGNATURE, "Report generated: " + reportName);
	}
	
		
	/**
	 * Inject new requests into a non-prod system
	 */
	public static void inject() {
		final String SIGNATURE = "inject()";
		// Start injecting
		ArrayList<com.invixo.injection.IntegratedConfiguration> icoList = com.invixo.injection.Orchestrator.start();
		
		// Write report
		com.invixo.injection.reporting.ReportWriter report = new com.invixo.injection.reporting.ReportWriter();
		report.interpretResult(icoList);
		String reportName = report.create(icoList);
		logger.writeDebug(LOCATION, SIGNATURE, "Report generated: " + reportName);
	}
	
	
	/**
	 * Start a file comparison
	 */
	public static void compare() {
		Comparer.startCompare();
	}
	
	
	/**
	 * Ensure the file structure is consistent for this program to run.
	 * This includes generating missing directories and file templates.
	 */
	private static void ensureFileStructureConsistency() {
		FileStructure.startCheck();
	}
	
}
