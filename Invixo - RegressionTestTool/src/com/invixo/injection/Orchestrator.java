package com.invixo.injection;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.invixo.common.GeneralException;
import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.consistency.FileStructure;

/**
 * This Class uses SAP PO Message API.
 * It injects message payloads to SAP PO.
 */
public class Orchestrator {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = Orchestrator.class.getName();
	private static ArrayList<IntegratedConfiguration> icoList = new ArrayList<IntegratedConfiguration>();
	
	
	public static void main(String[] args) {
		start();
	}

	
	/**
	 * Main entry point for injecting all payload files related to all Integrated Configurations
	 */
	public static ArrayList<IntegratedConfiguration> start() {
		try {
			final String SIGNATURE = "start()";
			logger.writeDebug(LOCATION, SIGNATURE, "Start processing all ICO's...");
			
			// Get list of all ICO request files to be processed
			File[] files = Util.getListOfFilesInDirectory(FileStructure.DIR_EXTRACT_INPUT);
			logger.writeDebug(LOCATION, SIGNATURE, "Number of ICO request files to be processed: " + files.length);
			
			// Process each ICO request file
			for (File file : files) {
				processSingleIco(file);
			}
			
			logger.writeDebug(LOCATION, SIGNATURE, "Finished processing all ICO's...");
			return icoList;
		} finally {
			try {
				// Close resources
				if (IntegratedConfiguration.mapWriter != null) {
					IntegratedConfiguration.mapWriter.flush();
					IntegratedConfiguration.mapWriter.close();
				}
			} catch (IOException e) {
				// Too bad...
			}	
		}
	}
	
	
	private static void processSingleIco(File file) {
		final String SIGNATURE = "processSingleIco(File)";
		IntegratedConfiguration ico = null;
		try {
			logger.writeDebug(LOCATION, SIGNATURE, "*********** Start processing ICO request file: " + file);
			
			// Prepare
			ico = new IntegratedConfiguration(file.getAbsolutePath());
			icoList.add(ico);
			
			// Process
			ico.startInjection();
		} catch (GeneralException e) {
			String ex = "Error instantiating Integrated Configuration object! " + e;
			logger.writeError(LOCATION, SIGNATURE, ex);
			throw new RuntimeException(e);
		} finally {
			logger.writeDebug(LOCATION, SIGNATURE, "*********** Processing ICO finished");
		}
	}
	
}
