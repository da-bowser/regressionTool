package com.invixo.consistency;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.invixo.common.util.Logger;
import com.invixo.common.util.PropertyAccessor;
import com.invixo.common.util.Util;


public class FileStructure2 {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = FileStructure2.class.getName();
	
	// Base/root file location
	public static final String FILE_BASE_LOCATION									= PropertyAccessor.getProperty("BASE_DIRECTORY");
	
	// Extract: input
	public static final String DIR_EXTRACT_INPUT									= FILE_BASE_LOCATION + "\\_Extract\\Input\\Integrated Configurations\\";

	// Extract: output
	public static final String DIR_EXTRACT_OUTPUT_PRE								= FILE_BASE_LOCATION + "\\_Extract\\Output\\";
	public static final String DIR_EXTRACT_OUTPUT_POST_FIRST_ENVLESS				= "\\Output\\Payloads\\First\\";
	public static final String DIR_EXTRACT_OUTPUT_POST_LAST_ENVLESS					= "\\Output\\Payloads\\Last\\";
	public static final String DIR_EXTRACT_OUTPUT_POST_DEV_FIRST					= "\\DEV" + DIR_EXTRACT_OUTPUT_POST_FIRST_ENVLESS;
	public static final String DIR_EXTRACT_OUTPUT_POST_DEV_LAST						= "\\DEV" + DIR_EXTRACT_OUTPUT_POST_LAST_ENVLESS;
	public static final String DIR_EXTRACT_OUTPUT_POST_TST_FIRST					= "\\TST" + DIR_EXTRACT_OUTPUT_POST_FIRST_ENVLESS;
	public static final String DIR_EXTRACT_OUTPUT_POST_TST_LAST						= "\\TST" + DIR_EXTRACT_OUTPUT_POST_LAST_ENVLESS;
	public static final String DIR_EXTRACT_OUTPUT_POST_PRD_FIRST					= "\\PRD" + DIR_EXTRACT_OUTPUT_POST_FIRST_ENVLESS;
	public static final String DIR_EXTRACT_OUTPUT_POST_PRD_LAST						= "\\prD" + DIR_EXTRACT_OUTPUT_POST_LAST_ENVLESS;
	
	// Inject: mapping table
	public static final String DIR_INJECT_OUTPUT									= FILE_BASE_LOCATION + "\\_Inject\\Output\\";
	
	// Various
	public static final String DIR_LOGS												= FILE_BASE_LOCATION + "\\Logs\\";
	public static final String DIR_REPORTS											= FILE_BASE_LOCATION + "\\Reports\\";
	public static final String DIR_CONFIG											= FILE_BASE_LOCATION + "\\Config\\";

	
	public static void main(String[] args) throws Exception  {
	}
	
	
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
		final String SIGNATURE = "deleteOldRunData()";
		try {       
			// Cleanup: delete all files and directories contained in "Extract Output"
			Util.deleteFilesAndSubDirectories(DIR_EXTRACT_OUTPUT_PRE);
			logger.writeDebug(LOCATION, SIGNATURE, "Housekeeping: all old output files and sub-directories deleted from root: " + DIR_EXTRACT_OUTPUT_PRE);
		} catch (Exception e) {
			String ex = "Housekeeping terminated with error! " + e;
			logger.writeError(LOCATION, SIGNATURE, ex);
			throw new RuntimeException(e);
		}            
	}


	/**
	 * Ensure project folder structure is healthy.
	 */
	private static void checkFolderStructure() {
		createDirIfNotExists(FILE_BASE_LOCATION);
		createDirIfNotExists(DIR_EXTRACT_INPUT);
		createDirIfNotExists(DIR_EXTRACT_OUTPUT_PRE);
		createDirIfNotExists(DIR_INJECT_OUTPUT);
		createDirIfNotExists(DIR_LOGS);
		createDirIfNotExists(DIR_REPORTS);
		createDirIfNotExists(DIR_CONFIG);
	}


	/**
	 * Create directories part of a directory path, if they are missing
	 * @param directoryPath
	 */
	public static void createDirIfNotExists(String directorypath) {
		try {
			Files.createDirectories(Paths.get(directorypath));			
		} catch (IOException e) {
			throw new RuntimeException("*createDirIfNotExists* Error creating directories for path: " + directorypath);
		}
	}
}
