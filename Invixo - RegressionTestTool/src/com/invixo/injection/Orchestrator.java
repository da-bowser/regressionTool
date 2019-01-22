package com.invixo.injection;

import java.io.File;

import com.invixo.common.util.InjectionException;
import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.consistency.FileStructure;

public class Orchestrator {
	private static Logger logger 			= Logger.getInstance();
	private static final String LOCATION 	= Orchestrator.class.getName();
	
	
	public static void main(String[] args) {
		start();
	}

	
	/**
	 * Main entry point for injecting all payload files related to any given ICO.
	 */
	public static void start() {
		String SIGNATURE = "start()";
		try {
			// Get list of all ICO request files to be processed
			File[] files = Util.getListOfFilesInDirectory(FileStructure.DIR_REGRESSION_INPUT_ICO);
			logger.writeDebug(LOCATION, SIGNATURE, "Number of ICO request files: " + files.length);
			
			// Process each ICO request file
			for (File file : files) {
				logger.writeDebug(LOCATION, SIGNATURE, "*********** Start processing ICO request file: " + file);
				
				// Process
				IntegratedConfiguration ico = new IntegratedConfiguration(file.getAbsolutePath());
				ico.injectAllMessagesForSingleIco();

				logger.writeDebug(LOCATION, SIGNATURE, "*********** Processing ICO finished successfully");
			}
			
			logger.writeDebug(LOCATION, SIGNATURE, "Processing all ICO's finished successfully!");
		} catch (InjectionException e) {
			String ex = "Processing terminated with error!";
			logger.writeError(LOCATION, SIGNATURE, ex);
			throw new RuntimeException(ex);
		}
	}
	
}
