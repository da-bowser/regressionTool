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
import com.invixo.compare.Comparer;
import com.invixo.consistency.FileStructure;
import com.invixo.consistency.FileStructure2;
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
	public static String SAP_PO_HTTP_HOST_AND_PORT				= null;
	
	// Parameter: SAP XI sender adapter name
	private static final String PARAM_KEY_XI_SENDER_ADAPTER		= "xiSenderAdapter";
	public static String PARAM_VAL_XI_SENDER_ADAPTER 			= null;

	// Parameter: SAP XI sender component containing the XI adapter
	private static final String PARAM_KEY_SENDER_COMPONENT		= "senderComponent";
	public static String PARAM_VAL_SENDER_COMPONENT 			= null;

	// Parameter: internal test parameter to skip deletion of target env payload files when source and target env are identical 
	private static final String PARAM_KEY_ALLOW_SAME_ENV		= "allowSameEnv";
	public static boolean PARAM_VAL_ALLOW_SAME_ENV 				= false;
	
	// Parameter: internal test parameter to store injection request messages on file system
	private static final String PARAM_KEY_STORE_INJECTION_REQ	= "debugStoreInjectionRequest";
	public static boolean PARAM_VAL_STORE_INJECTION_REQ 		= false;
	
	
	public static void main(String[] args) {
		try {
			// Set internal parameters based on program input arguments		
			setInternalParameters(args);
			
			// Validation of common parameters (relevant for all types of operations)
			validateGeneralParameters();
			
			// Execute
			if (Operation.extract.toString().equals(PARAM_VAL_OPERATION)) {
				// Validate operation specific parameters
				validateExtractParameters();
				
				// Post parameter handling: get user/pass from credential file
				readAndSetCredentials(PARAM_VAL_CREDENTIALS_FILE);
				
				// Post parameter handling: build complete PO host and port
				SAP_PO_HTTP_HOST_AND_PORT = "http://" + PARAM_VAL_HTTP_HOST + ":" + PARAM_VAL_HTTP_PORT + "/";
				
				// Process
				extract();
			} else if (Operation.inject.toString().equals(PARAM_VAL_OPERATION)) {
				// Validate operation specific parameters
				validateInjectParameters();
				
				// Post parameter handling: get user/pass from credential file
				readAndSetCredentials(PARAM_VAL_CREDENTIALS_FILE);
				
				// Post parameter handling: build complete PO host and port
				SAP_PO_HTTP_HOST_AND_PORT = "http://" + PARAM_VAL_HTTP_HOST + ":" + PARAM_VAL_HTTP_PORT + "/";
				
				// Process
				inject(); 
			} else {
				// Validate operation specific parameters
				validateCompareParameters();
				
				// Process				
				compare();
			}
		} catch (ValidationException e) {
			// TODO: Not valid input, inform end user in the nicest way possible
			e.printStackTrace(System.err);
		}
	}


	private static void validateGeneralParameters() throws ValidationException {
		StringWriter sw = new StringWriter();
		
		if (PARAM_VAL_ICO_REQUEST_FILES_ENV == null) {
			sw.write("Obligatory program parameter " + PARAM_KEY_ICO_REQUEST_FILES_ENV + " not set.\n");
		} else if (!environmentContains(PARAM_VAL_ICO_REQUEST_FILES_ENV)) {
			sw.write("Program parameter " + PARAM_KEY_ICO_REQUEST_FILES_ENV + " contains unsupported value. Value provided is: '" + PARAM_VAL_ICO_REQUEST_FILES_ENV + "'");			
		}

		if (PARAM_VAL_BASE_DIR == null) {
			sw.write("Obligatory program parameter " + PARAM_KEY_BASE_DIR + " not set.\n");
		}

		if (PARAM_VAL_TARGET_ENV == null) {
			sw.write("Obligatory program parameter " + PARAM_KEY_TARGET_ENV + " not set.\n");
		} else if (!environmentContains(PARAM_VAL_TARGET_ENV)) {
			sw.write("Program parameter " + PARAM_KEY_TARGET_ENV + " contains unsupported value. Value provided is: '" + PARAM_VAL_TARGET_ENV + "'");			
		}
		
		if (PARAM_VAL_OPERATION == null) {
			sw.write("Obligatory program parameter " + PARAM_KEY_OPERATION + " not set.\n");
		} else if (!operationContains(PARAM_VAL_OPERATION)) {
			sw.write("Program parameter " + PARAM_KEY_OPERATION + " contains unsupported value. Value provided is: '" + PARAM_VAL_OPERATION + "'");			
		}
		
		if (!sw.toString().equals("")) {
			throw new ValidationException(sw.toString());
		}
	}


	private static void validateExtractParameters() throws ValidationException {
		StringWriter sw = new StringWriter();
		
		if (PARAM_VAL_ICO_REQUEST_FILES_ENV == null) {
			sw.write("Obligatory program parameter " + PARAM_KEY_ICO_REQUEST_FILES_ENV + " not set.\n");
		} else if (!environmentContains(PARAM_VAL_ICO_REQUEST_FILES_ENV)) {
			sw.write("Program parameter " + PARAM_KEY_ICO_REQUEST_FILES_ENV + " contains unsupported value. Value provided is: '" + PARAM_VAL_ICO_REQUEST_FILES_ENV + "'");			
		}
		
		if (PARAM_VAL_CREDENTIALS_FILE == null) {
			sw.write("Obligatory program parameter " + PARAM_KEY_CREDENTIALS_FILE + " not set.\n");
		} else if (!Files.isRegularFile(Paths.get(PARAM_VAL_CREDENTIALS_FILE))) {
			sw.write("Program parameter " + PARAM_KEY_CREDENTIALS_FILE + " does not point to a file that exists. Value provided: " + PARAM_VAL_CREDENTIALS_FILE + ".\n");
		}
		
		if (PARAM_VAL_HTTP_HOST == null) {
			sw.write("Obligatory program parameter " + PARAM_KEY_HTTP_HOST + " not set.\n");
		} 
		
		if (PARAM_VAL_HTTP_PORT == null) {
			sw.write("Obligatory program parameter " + PARAM_KEY_HTTP_PORT + " not set.\n");
		} 
		
		if (!sw.toString().equals("")) {
			throw new ValidationException(sw.toString());
		}
	}


	private static void validateInjectParameters() throws ValidationException  {
		StringWriter sw = new StringWriter();
		
		// Inject uses the same parameters as Extract + some additions
		try {
			validateExtractParameters();
		} catch (ValidationException e) {
			sw.write(e.getMessage());
		}
		
		// Do not allow injecting to PRD
		if (Environment.PRD.toString().equals(PARAM_VAL_TARGET_ENV)) {
			sw.write("Program parameter " + PARAM_KEY_TARGET_ENV + " points to PRD. This is not supported\n");
		}	
		
		if (PARAM_VAL_SOURCE_ENV == null) {
			sw.write("Obligatory program parameter " + PARAM_KEY_SOURCE_ENV + " not set.\n");
		} else if (!environmentContains(PARAM_VAL_SOURCE_ENV)) {
			sw.write("Program parameter " + PARAM_KEY_SOURCE_ENV + " contains unsupported value. Value provided is: '" + PARAM_VAL_SOURCE_ENV + "'");			
		}		
		
		if (PARAM_VAL_XI_SENDER_ADAPTER == null) {
			sw.write("Obligatory program parameter " + PARAM_KEY_XI_SENDER_ADAPTER + " not set.\n");
		} 
		
		if (PARAM_VAL_SENDER_COMPONENT == null) {
			sw.write("Obligatory program parameter " + PARAM_KEY_SENDER_COMPONENT + " not set.\n");
		} 
		
		if (!sw.toString().equals("")) {
			throw new ValidationException(sw.toString());
		}
	}


	private static void validateCompareParameters() throws ValidationException  {
		StringWriter sw = new StringWriter();
		
		if (PARAM_VAL_SOURCE_ENV == null) {
			sw.write("Obligatory program parameter " + PARAM_KEY_SOURCE_ENV + " not set.\n");
		} else if (!environmentContains(PARAM_VAL_SOURCE_ENV)) {
			sw.write("Program parameter " + PARAM_KEY_SOURCE_ENV + " contains unsupported value. Value provided is: '" + PARAM_VAL_SOURCE_ENV + "'");			
		}	
		
		if (!sw.toString().equals("")) {
			throw new ValidationException(sw.toString());
		}
	}


	private static void setInternalParameters(String[] args) {
		for (String param : args) {	
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
			} else if(param.contains(PARAM_KEY_ALLOW_SAME_ENV)) {
				// Only ever set this parameter to true, if the source and target environments are actually identical
				if (PARAM_VAL_SOURCE_ENV.equals(PARAM_VAL_TARGET_ENV)) {
					PARAM_VAL_ALLOW_SAME_ENV = Boolean.parseBoolean(param.replace(PARAM_KEY_ALLOW_SAME_ENV + "=", ""));
				}
			} else if(param.contains(PARAM_KEY_STORE_INJECTION_REQ)) {
				PARAM_VAL_STORE_INJECTION_REQ = Boolean.parseBoolean(param.replace(PARAM_KEY_STORE_INJECTION_REQ + "=", ""));
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
			CREDENTIAL_USER = credentialLines.get(0);
			
			// line 2: password
			CREDENTIAL_PASS = credentialLines.get(1);
			
		} catch (IOException e) {
			String msg = "Error | Problem reading credential fil from :" + sourceDirectory + " " + e.getMessage();
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
		FileStructure2.startCheck();
		
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
		
		// Clean up file structure and ensure its consistency
		FileStructure2.startCheck();
		
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
		// Clean up file structure and ensure its consistency
		FileStructure2.startCheck();
		
		// Start comparing
		Comparer.startCompare();
	}
	
	
	/**
	 * Ensure the file structure is consistent for this program to run.
	 * This includes generating missing directories and file templates.
	 */
	private static void ensureFileStructureConsistency() {
		FileStructure.startCheck();
	}
	

	public static boolean operationContains(String value) {
	    for (Operation operation : Operation.values()) {
	        if (operation.name().equals(value)) {
	            return true;
	        }
	    }
	    return false;
	}

	
	public static boolean environmentContains(String value) {
	    for (Environment env : Environment.values()) {
	        if (env.name().equals(value)) {
	            return true;
	        }
	    }
	    return false;
	}
}
