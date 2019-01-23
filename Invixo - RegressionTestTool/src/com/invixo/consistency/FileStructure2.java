package com.invixo.consistency;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.main.Main;


public class FileStructure2 {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = FileStructure2.class.getName();
	
	// Base/root file location
	public static final String FILE_BASE_LOCATION					= Main.PARAM_VAL_BASE_DIR;
	
	// Extract: input
	private static final String DIR_EXTRACT							= FILE_BASE_LOCATION + "\\_Exctract";
	public static final String DIR_EXTRACT_INPUT					= DIR_EXTRACT + "\\Input\\Integrated Configurations\\";
	
	// Extract: output
	public static final String DIR_EXTRACT_OUTPUT_PRE					= FILE_BASE_LOCATION + "\\_Extract\\Output\\";
	public static final String DIR_EXTRACT_OUTPUT_POST_FIRST_ENVLESS	= "\\Output\\Payloads\\First\\";
	public static final String DIR_EXTRACT_OUTPUT_POST_LAST_ENVLESS		= "\\Output\\Payloads\\Last\\";
	public static final String DIR_EXTRACT_OUTPUT_POST_DEV_FIRST		= "\\DEV" + DIR_EXTRACT_OUTPUT_POST_FIRST_ENVLESS;
	public static final String DIR_EXTRACT_OUTPUT_POST_DEV_LAST			= "\\DEV" + DIR_EXTRACT_OUTPUT_POST_LAST_ENVLESS;
	public static final String DIR_EXTRACT_OUTPUT_POST_TST_FIRST		= "\\TST" + DIR_EXTRACT_OUTPUT_POST_FIRST_ENVLESS;
	public static final String DIR_EXTRACT_OUTPUT_POST_TST_LAST			= "\\TST" + DIR_EXTRACT_OUTPUT_POST_LAST_ENVLESS;
	public static final String DIR_EXTRACT_OUTPUT_POST_PRD_FIRST		= "\\PRD" + DIR_EXTRACT_OUTPUT_POST_FIRST_ENVLESS;
	public static final String DIR_EXTRACT_OUTPUT_POST_PRD_LAST			= "\\PRD" + DIR_EXTRACT_OUTPUT_POST_LAST_ENVLESS;
	
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
	 * Makes sure all old run data for target environment is deleted before a new run.
	 */
	private static void deleteOldRunData() {
		final String SIGNATURE = "deleteOldRunData()";
		try {       
			// Cleanup: delete all files contained in "Extract Output". Only done for sub-directories part of the specified target environment
			deletePayloadFiles(DIR_EXTRACT_OUTPUT_PRE + Main.PARAM_VAL_TARGET_ENV);
			logger.writeDebug(LOCATION, SIGNATURE, "Housekeeping: all old payload files deleted from root: " + DIR_EXTRACT_OUTPUT_PRE + Main.PARAM_VAL_TARGET_ENV);
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
		createDirIfNotExists(DIR_EXTRACT_INPUT);
		createDirIfNotExists(DIR_EXTRACT_OUTPUT_PRE);
		createDirIfNotExists(DIR_LOGS);
		createDirIfNotExists(DIR_REPORTS);
		createDirIfNotExists(DIR_CONFIG);
		
		// Lastly generate dynamic output folders based on ICO request files
		List<Path> icoFiles = Util.generateListOfPaths(DIR_EXTRACT_INPUT, "FILE");
		for (Path path : icoFiles) {
			// Build path
			String icoDynamicPath = DIR_EXTRACT_OUTPUT_PRE + Util.getFileName(path.toAbsolutePath().toString(), false);
			
			// Create ICO directory
			createDirIfNotExists(icoDynamicPath);
			
			// Also create output folders for DEV, TST, PROD
			createDirIfNotExists(icoDynamicPath + DIR_EXTRACT_OUTPUT_POST_DEV_FIRST);
			createDirIfNotExists(icoDynamicPath + DIR_EXTRACT_OUTPUT_POST_DEV_LAST);
			createDirIfNotExists(icoDynamicPath + DIR_EXTRACT_OUTPUT_POST_TST_FIRST);
			createDirIfNotExists(icoDynamicPath + DIR_EXTRACT_OUTPUT_POST_TST_LAST);
			createDirIfNotExists(icoDynamicPath + DIR_EXTRACT_OUTPUT_POST_PRD_FIRST);
			createDirIfNotExists(icoDynamicPath + DIR_EXTRACT_OUTPUT_POST_PRD_LAST);
		}
	}


	public static void deletePayloadFiles(String rootDirectory) {
		// Create pathMatcher which will match all files and directories (in the world of this tool, only files) that
		// are located in FIRST or LAST directories.
		String pattern = "^(?=.*\\\\PRD\\\\.*\\\\.*\\\\).*$";
		PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("regex:" + pattern);
		
		// Find all matches to above regex starting from the specified DIR
		try (Stream<Path> paths = Files.find(Paths.get(rootDirectory), 100, (path, f)->pathMatcher.matches(path))) {
			// Delete all matches
			paths.forEach(path -> {
				try {
					Files.delete(path);
				} catch (IOException e) {
					throw new RuntimeException("*deletePayloadFiles* Error deleting file '" + path + "'\n" + e);
				}
			});
		} catch (IOException e) {
			throw new RuntimeException("*deletePayloadFiles* Error finding files." + e);
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
