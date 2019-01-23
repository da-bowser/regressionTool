package com.invixo.consistency;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.invixo.common.util.Logger;
import com.invixo.common.util.PropertyAccessor;
import com.invixo.common.util.Util;
import com.invixo.main.Main;


public class FileStructure2 {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = FileStructure2.class.getName();
	
	// Base/root file location
	public static final String FILE_BASE_LOCATION					= Main.BASE_DIR;
	
	// Extract: input
	private static final String DIR_EXTRACT							= FILE_BASE_LOCATION + "\\_Exctract";
	public static final String DIR_EXTRACT_INPUT_ICA				= DIR_EXTRACT + "\\Input\\Integrated Configurations\\";
	
	// Extract: output
	public static final String DIR_EXTRACT_OUTPUT					= DIR_EXTRACT + "\\Output\\";
	public static final String DIR_EXTRACT_OUTPUT_ICO_PRD_FIRST		= "\\PRD\\FIRST\\";
	public static final String DIR_EXTRACT_OUTPUT_ICO_PRD_LAST		= "\\PRD\\LAST\\";
	public static final String DIR_EXTRACT_OUTPUT_ICO_TST_FIRST		= "\\TST\\FIRST\\";
	public static final String DIR_EXTRACT_OUTPUT_ICO_TST_LAST		= "\\TST\\LAST\\";
	public static final String DIR_EXTRACT_OUTPUT_ICO_DEV_FIRST		= "\\DEV\\FIRST\\";
	public static final String DIR_EXTRACT_OUTPUT_ICO_DEV_LAST		= "\\DEV\\LAST\\";

	// Inject: mapping table
	public static final String DIR_INJECT							= FILE_BASE_LOCATION + "\\_Inject\\";
	
	// Various
	public static final String DIR_LOGS								= FILE_BASE_LOCATION + "\\Logs\\";
	public static final String DIR_REPORTS							= FILE_BASE_LOCATION + "\\Reports\\";
	public static final String DIR_CONFIG							= FILE_BASE_LOCATION + "\\Config\\";

	
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
			Util.deleteFilesAndSubDirectories(DIR_EXTRACT_OUTPUT);
			logger.writeDebug(LOCATION, SIGNATURE, "Housekeeping: all old output files and sub-directories deleted from root: " + DIR_EXTRACT_OUTPUT);
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
		createDirIfNotExists(DIR_EXTRACT);
		createDirIfNotExists(DIR_EXTRACT_INPUT_ICA);
		createDirIfNotExists(DIR_EXTRACT_OUTPUT);
		createDirIfNotExists(DIR_LOGS);
		createDirIfNotExists(DIR_REPORTS);
		createDirIfNotExists(DIR_CONFIG);
		
		// Lastly generate dynamic output folders based on ICO request files
		List<Path> icoFiles = Util.generateListOfPaths(DIR_EXTRACT_INPUT_ICA, "FILE");
		for (Path path : icoFiles) {
			// Build path
			String icoDynamicPath = DIR_EXTRACT_OUTPUT + Util.getFileName(path.toAbsolutePath().toString(), false);
			
			// Create ICO directory
			createDirIfNotExists(icoDynamicPath);
			
			// Also create output folders for DEV, TST, PROD
			createDirIfNotExists(icoDynamicPath + DIR_EXTRACT_OUTPUT_ICO_PRD_FIRST);
			createDirIfNotExists(icoDynamicPath + DIR_EXTRACT_OUTPUT_ICO_PRD_LAST);
			createDirIfNotExists(icoDynamicPath + DIR_EXTRACT_OUTPUT_ICO_TST_FIRST);
			createDirIfNotExists(icoDynamicPath + DIR_EXTRACT_OUTPUT_ICO_TST_LAST);
			createDirIfNotExists(icoDynamicPath + DIR_EXTRACT_OUTPUT_ICO_DEV_FIRST);
			createDirIfNotExists(icoDynamicPath + DIR_EXTRACT_OUTPUT_ICO_DEV_LAST);
		}
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
