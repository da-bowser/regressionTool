package com.invixo.messageExtractor.main;

import java.io.File;
import java.util.ArrayList;

import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.consistency.FileStructure;

/**
 * This program uses SAP PO Message API.
 * It extracts message payloads from SAP PO database.
 *
 * Configuration of program is done in the 'apiConfig.properties' file part of the project.
 */
public class Orchestrator {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = Orchestrator.class.getName();	
	
	private static ArrayList<IntegratedConfiguration> icoExtractList = new ArrayList<IntegratedConfiguration>();
	

	
	public static void main(String[] args) {
		try {	
			// Test process for all files
			start();

		} catch (Exception e) {
			System.err.println("\nMessage caught in MAIN: \n" + e);
		}
	}
	

	/**
	 * This method extracts data from SAP PO based on request ICO files on file system.
	 */
	public static ArrayList<IntegratedConfiguration> start() {
		String SIGNATURE = "start()";
		try {
			// Get list of all request files to be processed
			File[] files = Util.getListOfFilesInDirectory(FileStructure.DIR_REGRESSION_INPUT_ICO);
			logger.writeDebug(LOCATION, SIGNATURE, "Number of ICO request files: " + files.length);
			
			// Process each ICO request file
			for (File file : files) {
				logger.writeDebug(LOCATION, SIGNATURE, "*********** Start processing ICO request file: " + file);
				
				// Prepare
				IntegratedConfiguration ico = new IntegratedConfiguration(file.getAbsolutePath());
				icoExtractList.add(ico);
				
				// Process
				ico.processSingleIco(file.getAbsolutePath());
				logger.writeDebug(LOCATION, SIGNATURE, "*********** Processing ICO finished successfully");
			}
			
			logger.writeDebug(LOCATION, SIGNATURE, "Processing all ICO's finished successfully!");
			return icoExtractList;
		} catch (Exception e) {
			String ex = "Processing terminated with error!";
			logger.writeError(LOCATION, SIGNATURE, ex);
			throw new RuntimeException(e);
		}
	}
	
}
