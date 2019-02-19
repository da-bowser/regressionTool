package com.invixo.injection;

import java.io.IOException;
import java.util.ArrayList;

import com.invixo.common.GeneralException;
import com.invixo.common.IcoOverviewInstance;
import com.invixo.common.util.Logger;


/**
 * This Class uses SAP PO Message API.
 * It injects message payloads to SAP PO.
 */
public class Orchestrator {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = Orchestrator.class.getName();
	private static ArrayList<IntegratedConfiguration> icoList = new ArrayList<IntegratedConfiguration>();
	private static int numberOfIcosToBeProcessed = 0;
	
	
	/**
	 * Main entry point for injecting all payload files related to all Integrated Configurations
	 */
	public static ArrayList<IntegratedConfiguration> start(ArrayList<IcoOverviewInstance> icoOverviewList) {
		final String SIGNATURE = "start(ArrayList<IcoOverviewInstance>)";
		try {
			logger.writeInfo(LOCATION, SIGNATURE, "Start processing all ICO's...");
			
			// Set total number of ICOs to be processed (for logging purposes)
			numberOfIcosToBeProcessed = icoOverviewList.size();
			
			// Process each ICO request file
			int counter = 0;
			for (IcoOverviewInstance ico : icoOverviewList) {
				counter++;
				processSingleIco(ico, counter);
			}
			
			logger.writeInfo(LOCATION, SIGNATURE, "Finished processing all ICO's...");
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
	
	
	private static void processSingleIco(IcoOverviewInstance icoOverviewInstance, int counter) {
		final String SIGNATURE = "processSingleIco(IcoOverviewInstance, int)";
		IntegratedConfiguration ico = null;
		try {
			logger.writeInfo(LOCATION, SIGNATURE, "*********** (" + counter + " / " + numberOfIcosToBeProcessed + ") Start processing ICO: " + icoOverviewInstance.getName());
			
			// Prepare
			ico = new IntegratedConfiguration(icoOverviewInstance);
			icoList.add(ico);
			
			// Process
			ico.startInjection();
		} catch (GeneralException e) {
			String ex = "Error instantiating Integrated Configuration object! " + e;
			logger.writeError(LOCATION, SIGNATURE, ex);
			throw new RuntimeException(e);
		} finally {
			logger.writeInfo(LOCATION, SIGNATURE, "*********** Processing ICO finished");
		}
	}
	
}
