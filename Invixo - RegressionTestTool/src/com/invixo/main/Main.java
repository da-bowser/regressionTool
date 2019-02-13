package com.invixo.main;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.consistency.FileStructure;
import com.invixo.directory.api.Orchestrator;

public class Main {
	private static Logger logger = null;
	private static final String LOCATION = Main.class.getName();
	
	// Parameter: dictates which environment *all* ICO request files are based on. (used for translation/mapping of sender system)
	private static final String PARAM_KEY_ICO_REQUEST_FILES_ENV = "icoRequestFilesEnv";
	
	// Parameter: base directory for all reading and writing to/from file system
	private static final String PARAM_KEY_BASE_DIR 				= "baseDirectory";
	
	// Parameter: target environment to extract from, inject to or compare with
	private static final String PARAM_KEY_TARGET_ENV 			= "targetEnv";
	
	// Parameter: operation for program to perform (extract, inject, compare)
	private static final String PARAM_KEY_OPERATION 			= "operation";
	
	// Parameter: source environment to compare to targetEnvironment or in the case on injection, which environment to inject payload files from
	private static final String PARAM_KEY_SOURCE_ENV 			= "sourceEnv";
	
	// Parameter: location of a credentials file (expected to contain 2 lines. First line contains user name only, second line contains password
	private static final String PARAM_KEY_CREDENTIALS_FILE		= "credentialsFile";
	private static String PARAM_VAL_CREDENTIALS_FILE 			= null;
	
	// Parameter: SAP PO host name. Example: ipod.invixo.com
	private static final String PARAM_KEY_HTTP_HOST				= "httpHost";

	// Parameter: SAP PO host name. Example: 50000
	private static String PARAM_KEY_HTTP_PORT					= "httpPort";
	
	// Parameter: SAP XI sender adapter name
	private static final String PARAM_KEY_XI_SENDER_ADAPTER		= "xiSenderAdapter";

	// Parameter: SAP XI sender component containing the XI adapter
	private static final String PARAM_KEY_SENDER_COMPONENT		= "senderComponent";

	// Parameter: internal test parameter to skip deletion of target env payload files when source and target env are identical 
	private static final String PARAM_KEY_ALLOW_SAME_ENV		= "allowSameEnv";
	
	// Parameter: From time (for extraction)
	private static final String PARAM_KEY_FROM_TIME				= "fromTime";

	// Parameter: To time (for extraction)
	private static final String PARAM_KEY_TO_TIME				= "toTime";
	
	// Parameter: Extract mode (init or non-init)
	private static final String PARAM_KEY_EXTRACT_MODE_INIT		= "extractModeInit";
	
	
	public static void main(String[] args) {
		final String SIGNATURE = "main(String[])";
		long startTime = 0;
		try {
			// Get start time
			startTime = Util.getTime();
			
			// Set internal parameters based on program input arguments		
			setInternalParameters(args);
					
			// Validation of common parameters (relevant for all types of operations)
			validateGeneralParameters();
			
			// Init logger (done at this location since it requires both base location and operation when used in FILE mode)
			logger = Logger.getInstance();
			
			// Execute
			if (GlobalParameters.Operation.extract.toString().equals(GlobalParameters.PARAM_VAL_OPERATION)) {
				// Validate operation specific parameters
				validateExtractParameters();
				
				// Post parameter handling: get user/pass from credential file
				readAndSetCredentials(PARAM_VAL_CREDENTIALS_FILE);
				
				// Post parameter handling: build complete PO host and port
				GlobalParameters.SAP_PO_HTTP_HOST_AND_PORT = buildHttpHostPort();
				
				// Process
				extract();
			} else if (GlobalParameters.Operation.inject.toString().equals(GlobalParameters.PARAM_VAL_OPERATION)) {
				// Validate operation specific parameters
				validateInjectParameters();
				
				// Post parameter handling: get user/pass from credential file
				readAndSetCredentials(PARAM_VAL_CREDENTIALS_FILE);
				
				// Post parameter handling: build complete PO host and port
				GlobalParameters.SAP_PO_HTTP_HOST_AND_PORT = buildHttpHostPort();
				
				// Process
				inject(); 
			} else if (GlobalParameters.Operation.compare.toString().equals(GlobalParameters.PARAM_VAL_OPERATION)) {
				// Process				
				compare();
			} else {
				// Post parameter handling: get user/pass from credential file
				readAndSetCredentials(PARAM_VAL_CREDENTIALS_FILE);
				
				// Post parameter handling: build complete PO host and port
				GlobalParameters.SAP_PO_HTTP_HOST_AND_PORT = buildHttpHostPort();
				
				// Process
				createIcoOverview();
			}
		} catch (ValidationException e) {
			// TODO: Not valid input, inform end user in the nicest way possible
			e.printStackTrace(System.err);
		} finally {
			long endTime = Util.getTime();
			logger.writeDebug(LOCATION, SIGNATURE, "Program execution took (seconds): " + Util.measureTimeTaken(startTime, endTime));
		}
	}


	private static String buildHttpHostPort() {
		return "http://" + GlobalParameters.PARAM_VAL_HTTP_HOST + ":" + GlobalParameters.PARAM_VAL_HTTP_PORT + "/";
	}


	private static void validateGeneralParameters() throws ValidationException {
		StringWriter sw = new StringWriter();
		
		if (GlobalParameters.PARAM_VAL_SOURCE_ENV == null) {
			sw.write("Obligatory program parameter " + PARAM_KEY_SOURCE_ENV + " not set.\n");
		} else if (!environmentContains(GlobalParameters.PARAM_VAL_SOURCE_ENV)) {
			sw.write("Program parameter " + PARAM_KEY_SOURCE_ENV + " contains unsupported value. Value provided is: '" + GlobalParameters.PARAM_VAL_SOURCE_ENV + "'");			
		}	
		
		if (GlobalParameters.PARAM_VAL_ICO_REQUEST_FILES_ENV == null) {
			sw.write("Obligatory program parameter " + PARAM_KEY_ICO_REQUEST_FILES_ENV + " not set.\n");
		} else if (!environmentContains(GlobalParameters.PARAM_VAL_ICO_REQUEST_FILES_ENV)) {
			sw.write("Program parameter " + PARAM_KEY_ICO_REQUEST_FILES_ENV + " contains unsupported value. Value provided is: '" + GlobalParameters.PARAM_VAL_ICO_REQUEST_FILES_ENV + "'");			
		}

		if (GlobalParameters.PARAM_VAL_BASE_DIR == null) {
			sw.write("Obligatory program parameter " + PARAM_KEY_BASE_DIR + " not set.\n");
		}

		if (GlobalParameters.PARAM_VAL_TARGET_ENV == null) {
			sw.write("Obligatory program parameter " + PARAM_KEY_TARGET_ENV + " not set.\n");
		} else if (!environmentContains(GlobalParameters.PARAM_VAL_TARGET_ENV)) {
			sw.write("Program parameter " + PARAM_KEY_TARGET_ENV + " contains unsupported value. Value provided is: '" + GlobalParameters.PARAM_VAL_TARGET_ENV + "'");			
		}
		
		if (GlobalParameters.PARAM_VAL_OPERATION == null) {
			sw.write("Obligatory program parameter " + PARAM_KEY_OPERATION + " not set.\n");
		} else if (!operationContains(GlobalParameters.PARAM_VAL_OPERATION)) {
			sw.write("Program parameter " + PARAM_KEY_OPERATION + " contains unsupported value. Value provided is: '" + GlobalParameters.PARAM_VAL_OPERATION + "'");			
		}
		
		if (!sw.toString().equals("")) {
			throw new ValidationException(sw.toString());
		}
	}


	private static void validateExtractParameters() throws ValidationException {
		StringWriter sw = new StringWriter();

		// Extract and inject shares some common parameters
		try {
			validateCommonExtractAndInjectParameters();
		} catch (ValidationException e) {
			sw.write(e.getMessage());
		}
		
		if (GlobalParameters.PARAM_VAL_EXTRACT_MODE_INIT == null) {
			sw.write("Obligatory program parameter " + PARAM_KEY_EXTRACT_MODE_INIT + " not set.\n");
		} 
		
		if (!sw.toString().equals("")) {
			throw new ValidationException(sw.toString());
		}
	}

	
	private static void validateCommonExtractAndInjectParameters() throws ValidationException {
		StringWriter sw = new StringWriter();
		
		if (GlobalParameters.PARAM_VAL_ICO_REQUEST_FILES_ENV == null) {
			sw.write("Obligatory program parameter " + PARAM_KEY_ICO_REQUEST_FILES_ENV + " not set.\n");
		} else if (!environmentContains(GlobalParameters.PARAM_VAL_ICO_REQUEST_FILES_ENV)) {
			sw.write("Program parameter " + PARAM_KEY_ICO_REQUEST_FILES_ENV + " contains unsupported value. Value provided is: '" + GlobalParameters.PARAM_VAL_ICO_REQUEST_FILES_ENV + "'");			
		}
		
		if (PARAM_VAL_CREDENTIALS_FILE == null) {
			sw.write("Obligatory program parameter " + PARAM_KEY_CREDENTIALS_FILE + " not set.\n");
		} else if (!Files.isRegularFile(Paths.get(PARAM_VAL_CREDENTIALS_FILE))) {
			sw.write("Program parameter " + PARAM_KEY_CREDENTIALS_FILE + " does not point to a file that exists. Value provided: " + PARAM_VAL_CREDENTIALS_FILE + ".\n");
		}
		
		if (GlobalParameters.PARAM_VAL_HTTP_HOST == null) {
			sw.write("Obligatory program parameter " + PARAM_KEY_HTTP_HOST + " not set.\n");
		} 
		
		if (GlobalParameters.PARAM_VAL_HTTP_PORT == null) {
			sw.write("Obligatory program parameter " + PARAM_KEY_HTTP_PORT + " not set.\n");
		} 
		
		if (!sw.toString().equals("")) {
			throw new ValidationException(sw.toString());
		}
	}
	

	private static void validateInjectParameters() throws ValidationException  {
		StringWriter sw = new StringWriter();
		
		// Extract and inject shares some common parameters
		try {
			validateCommonExtractAndInjectParameters();
		} catch (ValidationException e) {
			sw.write(e.getMessage());
		}
		
		// Do not allow injecting to PRD
		if (GlobalParameters.Environment.PRD.toString().equals(GlobalParameters.PARAM_VAL_TARGET_ENV)) {
			sw.write("Program parameter " + PARAM_KEY_TARGET_ENV + " points to PRD. This is not supported\n");
		}	
		
		if (GlobalParameters.PARAM_VAL_XI_SENDER_ADAPTER == null) {
			sw.write("Obligatory program parameter " + PARAM_KEY_XI_SENDER_ADAPTER + " not set.\n");
		} 
		
		if (GlobalParameters.PARAM_VAL_SENDER_COMPONENT == null) {
			sw.write("Obligatory program parameter " + PARAM_KEY_SENDER_COMPONENT + " not set.\n");
		} 
		
		if (!sw.toString().equals("")) {
			throw new ValidationException(sw.toString());
		}
	}


	private static void setInternalParameters(String[] args) {
		for (String param : args) {	
			if(param.contains(PARAM_KEY_ICO_REQUEST_FILES_ENV)) {
				GlobalParameters.PARAM_VAL_ICO_REQUEST_FILES_ENV = param.replace(PARAM_KEY_ICO_REQUEST_FILES_ENV + "=", "");
			} else if(param.contains(PARAM_KEY_BASE_DIR)) {
				GlobalParameters.PARAM_VAL_BASE_DIR = param.replace(PARAM_KEY_BASE_DIR + "=", "");
			} else if(param.contains(PARAM_KEY_TARGET_ENV)) {
				GlobalParameters.PARAM_VAL_TARGET_ENV = param.replace(PARAM_KEY_TARGET_ENV + "=", "");
			} else if(param.contains(PARAM_KEY_OPERATION)) {
				GlobalParameters.PARAM_VAL_OPERATION = param.replace(PARAM_KEY_OPERATION + "=", "");
			} else if(param.contains(PARAM_KEY_SOURCE_ENV)) {
				GlobalParameters.PARAM_VAL_SOURCE_ENV = param.replace(PARAM_KEY_SOURCE_ENV + "=", "");
			} else if(param.contains(PARAM_KEY_CREDENTIALS_FILE)) {
				PARAM_VAL_CREDENTIALS_FILE = param.replace(PARAM_KEY_CREDENTIALS_FILE + "=", "");
			} else if(param.contains(PARAM_KEY_HTTP_HOST)) {
				GlobalParameters.PARAM_VAL_HTTP_HOST = param.replace(PARAM_KEY_HTTP_HOST + "=", "");
			} else if(param.contains(PARAM_KEY_HTTP_PORT)) {
				GlobalParameters.PARAM_VAL_HTTP_PORT = param.replace(PARAM_KEY_HTTP_PORT + "=", "");
			} else if(param.contains(PARAM_KEY_XI_SENDER_ADAPTER)) {
				GlobalParameters.PARAM_VAL_XI_SENDER_ADAPTER = param.replace(PARAM_KEY_XI_SENDER_ADAPTER + "=", "");
			} else if(param.contains(PARAM_KEY_SENDER_COMPONENT)) {
				GlobalParameters.PARAM_VAL_SENDER_COMPONENT = param.replace(PARAM_KEY_SENDER_COMPONENT + "=", "");
			} else if(param.contains(PARAM_KEY_ALLOW_SAME_ENV)) {
				// Only ever set this parameter to true, if the source and target environments are actually identical
				if (GlobalParameters.PARAM_VAL_SOURCE_ENV.equals(GlobalParameters.PARAM_VAL_TARGET_ENV)) {
					GlobalParameters.PARAM_VAL_ALLOW_SAME_ENV = Boolean.parseBoolean(param.replace(PARAM_KEY_ALLOW_SAME_ENV + "=", ""));
				}
			} else if(param.contains(PARAM_KEY_FROM_TIME)) {
				GlobalParameters.PARAM_VAL_FROM_TIME = param.replace(PARAM_KEY_FROM_TIME + "=", "");
			} else if(param.contains(PARAM_KEY_TO_TIME)) {
				GlobalParameters.PARAM_VAL_TO_TIME = param.replace(PARAM_KEY_TO_TIME + "=", "");
			} else if(param.contains(PARAM_KEY_EXTRACT_MODE_INIT)) {
				GlobalParameters.PARAM_VAL_EXTRACT_MODE_INIT = param.replace(PARAM_KEY_EXTRACT_MODE_INIT + "=", "");
			}
		}
	}

	
	private static void readAndSetCredentials(String sourceDirectory) throws ValidationException {
		try {
			// Get credential file
			List<Path> credentialsFile = Util.generateListOfPaths(sourceDirectory, "FILE");
			
			// We expect only credentials file
			List<String> credentialLines = Files.lines(credentialsFile.get(0)).collect(Collectors.toList());
			
			// Line 1: user name
			GlobalParameters.CREDENTIAL_USER = credentialLines.get(0);
			
			// line 2: password
			GlobalParameters.CREDENTIAL_PASS = credentialLines.get(1);
		} catch (IOException e) {
			String msg = "Error | Problem reading credentials file from :" + sourceDirectory + " " + e.getMessage();
			throw new ValidationException(msg);
		}
	}


	/**
	 * Extract data from a productive or non-productive SAP PO system.
	 * This creates payload files (FIRST and/or LAST) on file system: 
 	 * NB: remember to set the proper properties in config file. Some should probably be parameterized in the class for safety and ease.
	 */
	public static void extract() {
		final String SIGNATURE = "extract()";
		
		// Clean up file structure and ensure its consistency
		FileStructure.startCheck();
		
		// Start extracting
		ArrayList<com.invixo.extraction.IntegratedConfiguration> icoList = com.invixo.extraction.Orchestrator.start();
		
		// Write report
		com.invixo.extraction.reporting.ReportWriter report = new com.invixo.extraction.reporting.ReportWriter(icoList);
		String reportName = report.create(icoList);
		logger.writeDebug(LOCATION, SIGNATURE, "Report generated: " + reportName);
	}
	
		
	/**
	 * Inject new requests into a non-prod system
	 */
	public static void inject() {
		final String SIGNATURE = "inject()";
		
		// Clean up file structure and ensure its consistency
		FileStructure.startCheck();
		
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
		final String SIGNATURE = "compare()";
		
		// Start comparing
		ArrayList<com.invixo.compare.IntegratedConfiguration> icoList = com.invixo.compare.Orchestrator.start();
		
		// Write report
		com.invixo.compare.reporting.ReportWriter report = new com.invixo.compare.reporting.ReportWriter(icoList);
		report.create(icoList);
		String reportName = report.create(icoList);
		logger.writeDebug(LOCATION, SIGNATURE, "Report generated: " + reportName);
	}
	
	
	public static void createIcoOverview() {
		final String SIGNATURE = "createIcoOverview";
		
		String fileName = Orchestrator.start();
		logger.writeDebug(LOCATION, SIGNATURE, "Ico overview generated: " + fileName);
	}

	
	public static boolean operationContains(String value) {
	    for (GlobalParameters.Operation operation : GlobalParameters.Operation.values()) {
	        if (operation.name().equals(value)) {
	            return true;
	        }
	    }
	    return false;
	}

	
	public static boolean environmentContains(String value) {
	    for (GlobalParameters.Environment env : GlobalParameters.Environment.values()) {
	        if (env.name().equals(value)) {
	            return true;
	        }
	    }
	    return false;
	}
}
