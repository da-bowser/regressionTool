package com.invixo.consistency;

import java.io.File;

import com.invixo.common.util.PropertyAccessor;

public class FileStructure {
	public static final String FILE_BASE_LOCATION 										= PropertyAccessor.getProperty("BASE_DIRECTORY");
	private static final String DIR_BASE_REGRESSION										= FILE_BASE_LOCATION + "\\PROD Data";
	private static final String DIR_REGRESSION_INPUT									= DIR_BASE_REGRESSION + "\\Input";
	public static final String DIR_REGRESSION_INPUT_ICO									= DIR_REGRESSION_INPUT + "\\IntegratedConfigurations\\";
	public static final String DIR_REGRESSION_INPUT_INJECTION							= DIR_REGRESSION_INPUT + "\\Injection\\";
	public static final String 	DIR_REGRESSION_LOG										= DIR_BASE_REGRESSION + "\\Log\\";
	private static final String DIR_REGRESSION_OUTPUT									= DIR_BASE_REGRESSION + "\\Output";
	private static final String DIR_REGRESSION_OUTPUT_PAYLOADS 							= DIR_REGRESSION_OUTPUT + "\\Payloads";
	public static final String 	DIR_REGRESSION_OUTPUT_PAYLOADS_FIRST_MSG_VERSION 		= DIR_REGRESSION_OUTPUT_PAYLOADS + "\\First message version\\";
	public static final String 	DIR_REGRESSION_OUTPUT_PAYLOADS_LAST_MSG_VERSION 		= DIR_REGRESSION_OUTPUT_PAYLOADS + "\\Last message version\\";
	public static final String 	DIR_REGRESSION_OUTPUT_WS_RESPONSES 						= DIR_REGRESSION_OUTPUT + "\\WS Responses\\";
	public static final String 	DIR_REGRESSION_OUTPUT_WS_RESPONSES_FIRST_MSG_VERSION 	= DIR_REGRESSION_OUTPUT_WS_RESPONSES + "\\First message version\\";
	public static final String 	DIR_REGRESSION_OUTPUT_WS_RESPONSES_LAST_MSG_VERSION 	= DIR_REGRESSION_OUTPUT_WS_RESPONSES + "\\Last message version\\";
	

	public static void checkAll() {
		createDirIfNotExists(DIR_BASE_REGRESSION);
		createDirIfNotExists(DIR_REGRESSION_INPUT);
		createDirIfNotExists(DIR_REGRESSION_INPUT_ICO);
		createDirIfNotExists(DIR_REGRESSION_INPUT_INJECTION);
		createDirIfNotExists(DIR_REGRESSION_LOG);
		createDirIfNotExists(DIR_REGRESSION_OUTPUT);
		createDirIfNotExists(DIR_REGRESSION_OUTPUT_PAYLOADS);
		createDirIfNotExists(DIR_REGRESSION_OUTPUT_PAYLOADS_FIRST_MSG_VERSION);
		createDirIfNotExists(DIR_REGRESSION_OUTPUT_PAYLOADS_LAST_MSG_VERSION);
		createDirIfNotExists(DIR_REGRESSION_OUTPUT_WS_RESPONSES);
		createDirIfNotExists(DIR_REGRESSION_OUTPUT_WS_RESPONSES_FIRST_MSG_VERSION);
		createDirIfNotExists(DIR_REGRESSION_OUTPUT_WS_RESPONSES_LAST_MSG_VERSION);
	}
	
	public static void createDirIfNotExists(String directoryPath) {
		File directory = new File(directoryPath);
		
		System.out.println(directory);
	    if (! directory.exists()){
	    	directory.mkdir();
	    }
	}
}
