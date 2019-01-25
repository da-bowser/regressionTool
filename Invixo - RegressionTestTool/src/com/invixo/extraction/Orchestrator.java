package com.invixo.extraction;

import java.io.File;
import java.util.ArrayList;

import com.invixo.common.GeneralException;
import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.consistency.FileStructure;

/**
 * This program uses SAP PO Message API.
 * It extracts message payloads from SAP PO database.
 *
 * Configuration of program is done in the 'messageExtractor.properties' file part of the project.
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
		final String SIGNATURE = "start()";

		// Get list of all request files to be processed
		File[] files = Util.getListOfFilesInDirectory(FileStructure.DIR_EXTRACT_INPUT);
		logger.writeDebug(LOCATION, SIGNATURE, "Number of ICO request files: " + files.length);
			
		// Process each ICO request file
		for (File file : files) {
			processSingleIco(file);
		}

		logger.writeDebug(LOCATION, SIGNATURE, "Finished processing all ICO's");
		return icoExtractList;
	}
	
	
	private static void processSingleIco(File file) {
		final String SIGNATURE = "processSingleIco(File)";
		try {
			// Prepare
			IntegratedConfiguration ico = new IntegratedConfiguration(file.getAbsolutePath());
			icoExtractList.add(ico);

			// Process
			ico.processSingleIco(file.getAbsolutePath());
		} catch (GeneralException e) {
			String ex = "Error instantiating Integrated Configuration object! " + e;
			logger.writeError(LOCATION, SIGNATURE, ex);
			throw new RuntimeException(e);
		}
	}
	
}
