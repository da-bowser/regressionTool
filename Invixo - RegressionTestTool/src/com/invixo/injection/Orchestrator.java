package com.invixo.injection;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.invixo.common.util.InjectionException;
import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.consistency.FileStructure;

public class Orchestrator {
	private static Logger logger 						= Logger.getInstance();
	private static final String LOCATION 				= Orchestrator.class.getName();
	private static ArrayList<IntegratedConfiguration> icoList 	= new ArrayList<IntegratedConfiguration>();
	
	public static void main(String[] args) {
		start();
	}

	
	/**
	 * Main entry point for injecting all payload files related to any given ICO.
	 */
	public static ArrayList<IntegratedConfiguration> start() {
		final String SIGNATURE = "start()";
		try {
			logger.writeDebug(LOCATION, SIGNATURE, "Start processing all ICO's...");
			
			// Get list of all ICO request files to be processed
			File[] files = Util.getListOfFilesInDirectory(FileStructure.DIR_REGRESSION_INPUT_ICO);
			logger.writeDebug(LOCATION, SIGNATURE, "Number of ICO request files: " + files.length);
			
			// Process each ICO request file
			for (File file : files) {
				processSingleIco(file);
			}
			
			logger.writeDebug(LOCATION, SIGNATURE, "Finished processing all ICO's...");
			return icoList;
		} catch (InjectionException e) {
			String ex = "Processing terminated with error!";
			logger.writeError(LOCATION, SIGNATURE, ex);
			throw new RuntimeException(ex);
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
	
	
	private static void processSingleIco(File file) throws InjectionException {
		final String SIGNATURE = "processSingleIco(File)";
		IntegratedConfiguration ico = null;
		try {
			logger.writeDebug(LOCATION, SIGNATURE, "*********** Start processing ICO request file: " + file);
			
			// Process
			ico = new IntegratedConfiguration(file.getAbsolutePath());
			icoList.add(ico);
			ico.injectAllMessagesForSingleIco();

		} catch (InjectionException e) {
			if (ico != null) {
				ico.setEx(e);
			} else {
				// If ICO could not be instantiated then something is horrible wrong.
				String msg = "Fatal error! Could not instantiate ICO for file: " + file.getAbsolutePath() + "\n" + e;
				logger.writeError(LOCATION, SIGNATURE, msg);
				throw new RuntimeException(msg);
			}
		} finally {
			logger.writeDebug(LOCATION, SIGNATURE, "*********** Processing ICO finished");
		}
	}
	
}
