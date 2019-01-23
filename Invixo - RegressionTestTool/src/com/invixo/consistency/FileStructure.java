package com.invixo.consistency;

import java.io.File;

import com.invixo.common.util.Logger;
import com.invixo.common.util.PropertyAccessor;
import com.invixo.common.util.Util;


public class FileStructure {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = FileStructure.class.getName();
	public static final String FILE_BASE_LOCATION									= PropertyAccessor.getProperty("BASE_DIRECTORY");
	private static final String DIR_BASE_REGRESSION                                 = FILE_BASE_LOCATION + PropertyAccessor.getProperty("TARGET_ENVIRONMENT");
	private static final String DIR_REGRESSION_COMPARE								= FILE_BASE_LOCATION + "Compare";
	public static final String DIR_REGRESSION_COMPARE_RESULTS						= DIR_REGRESSION_COMPARE + "\\Results\\";
	public static final String DIR_REGRESSION_COMPARE_EXEPTIONS						= DIR_REGRESSION_COMPARE + "\\Exceptions\\";
	private static final String DIR_REGRESSION_INPUT                                = DIR_BASE_REGRESSION + "\\Input";
	public static final String DIR_REGRESSION_INPUT_ICO                             = DIR_REGRESSION_INPUT + "\\IntegratedConfigurations\\";
	public static final String DIR_REGRESSION_INPUT_INJECTION                       = DIR_REGRESSION_INPUT + "\\Injection\\";
	public static final String DIR_REGRESSION_LOG                                   = DIR_BASE_REGRESSION + "\\Log\\";
	public static final String DIR_REGRESSION_REPORTS                              	= DIR_BASE_REGRESSION + "\\Reports";
	private static final String DIR_REGRESSION_OUTPUT                               = DIR_BASE_REGRESSION + "\\Output";
	private static final String DIR_REGRESSION_OUTPUT_PAYLOADS                      = DIR_REGRESSION_OUTPUT + "\\Payloads";
	public static final String DIR_REGRESSION_OUTPUT_PAYLOADS_FIRST_MSG_VERSION     = DIR_REGRESSION_OUTPUT_PAYLOADS + "\\First message version\\";
	public static final String DIR_REGRESSION_OUTPUT_PAYLOADS_LAST_MSG_VERSION      = DIR_REGRESSION_OUTPUT_PAYLOADS + "\\Last message version\\";
	public static final String DIR_REGRESSION_OUTPUT_MAPPING						= DIR_REGRESSION_OUTPUT + "\\Mapping\\";

	// Test folders
	private static final String DIR_BASE_REGRESSION_TEST							= FILE_BASE_LOCATION + "TEST Data";
	public static final String DIR_REGRESSION_COMPARE_PAYLOAD_LAST_MSG_VERSION		= DIR_BASE_REGRESSION_TEST + "\\Output\\Payloads\\Last message version\\";

	
	/**
	 * Start File Structure check.
	 */
	public static void startCheck() {
		String SIGNATURE = "startCheck()";
		logger.writeDebug(LOCATION, SIGNATURE, "Start file structure check");

		// Clean-up old data from "Output"
		deleteOldRunData();

		// Ensure project folder structure is present
		checkFolderStructure();

		logger.writeDebug(LOCATION, SIGNATURE, "File structure check completed!");
	}


	/**
	 * Makes sure all old run data is deleted before a new run.
	 */
	private static void deleteOldRunData() {
		String SIGNATURE = "deleteOldRunData()";

		try {       
			// Cleanup: delete all files and directories contained in "Output"
			Util.deleteFilesAndSubDirectories(FileStructure.DIR_REGRESSION_OUTPUT);
			logger.writeDebug(LOCATION, SIGNATURE, "Housekeeping: all old output files deleted from directory: " + FileStructure.DIR_REGRESSION_OUTPUT);
		} catch (Exception e) {
			String ex = "Housekeeping terminated with error!";
			logger.writeError(LOCATION, SIGNATURE, ex);
			throw new RuntimeException(e);
		}            
	}


	/**
	 * Ensure project folder structure is healthy.
	 */
	private static void checkFolderStructure() {
		createDirIfNotExists(DIR_BASE_REGRESSION);
		createDirIfNotExists(DIR_REGRESSION_COMPARE);
		createDirIfNotExists(DIR_REGRESSION_COMPARE_RESULTS);
		createDirIfNotExists(DIR_REGRESSION_COMPARE_EXEPTIONS);
		createDirIfNotExists(DIR_REGRESSION_INPUT);
		createDirIfNotExists(DIR_REGRESSION_INPUT_ICO);
		createDirIfNotExists(DIR_REGRESSION_INPUT_INJECTION);
		createDirIfNotExists(DIR_REGRESSION_LOG);
		createDirIfNotExists(DIR_REGRESSION_REPORTS);
		createDirIfNotExists(DIR_REGRESSION_OUTPUT);
		createDirIfNotExists(DIR_REGRESSION_OUTPUT_PAYLOADS);
		createDirIfNotExists(DIR_REGRESSION_OUTPUT_PAYLOADS_FIRST_MSG_VERSION);
		createDirIfNotExists(DIR_REGRESSION_OUTPUT_PAYLOADS_LAST_MSG_VERSION);
		createDirIfNotExists(DIR_REGRESSION_OUTPUT_MAPPING);
	}


	/**
	 * Create a directory if it is missing.
	 * @param directoryPath
	 */
	public static void createDirIfNotExists(String directoryPath) {
		File directory = new File(directoryPath);

		if (! directory.exists()){
			directory.mkdir();
		}
	}
}
