package com.invixo.compare;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.consistency.FileStructure;
import com.invixo.compare.IntegratedConfiguration;
import com.invixo.main.GlobalParameters;

public class Orchestrator {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = Orchestrator.class.getName();
	private static ArrayList<IntegratedConfiguration> icoList = new ArrayList<IntegratedConfiguration>();
	private static int icoProcessSuccess = 0;
	private static int icoProccesError = 0;	
	private static double totalExecutionTime = 0;
	
	public static ArrayList<IntegratedConfiguration> start() {
		final String SIGNATURE = "start()";
		logger.writeDebug(LOCATION, SIGNATURE, "Start compare");
		
		// Get list of ICO's to handle
		List<Path> sourceIcoFiles = Util.generateListOfPaths(FileStructure.DIR_EXTRACT_INPUT, "FILE");

		// Start processing files for compare
		icoList = processCompareLibs(sourceIcoFiles);
		
		logger.writeDebug(LOCATION, SIGNATURE, "Compare completed");
		
		return icoList;
	}

	
	private static ArrayList<IntegratedConfiguration> processCompareLibs(List<Path> sourceIcoFiles) {
		final String SIGNATURE = "processCompareLibs(List)";
		logger.writeDebug(LOCATION, SIGNATURE, "ICO's found and ready to process: " + sourceIcoFiles.size());

		// Process found ICO's
		for (int i = 0; i < sourceIcoFiles.size(); i++) {
			logger.writeDebug(LOCATION, SIGNATURE, "[ICO: " + (i+1) + " ] processing");
			Path currentSourcePath = sourceIcoFiles.get(i);
			String icoName = Util.getFileName(currentSourcePath.toString(), false);
			String sourceIcoComparePath = buildEnvironmentComparePath(currentSourcePath, GlobalParameters.PARAM_VAL_SOURCE_ENV, icoName);
			String targetIcoComparePath = buildEnvironmentComparePath(currentSourcePath, GlobalParameters.PARAM_VAL_TARGET_ENV, icoName);

			// Create instance of CompareHandler containing all relevant data for a given ICO compare
			IntegratedConfiguration ico = new IntegratedConfiguration(sourceIcoComparePath, targetIcoComparePath, icoName);
			
			// Add ico to list for later reporting
			icoList.add(ico);
			
			if (ico.getCompareException() == null) {
				// Start processing ico
				ico.start();
				
				// Increment ico compare count
				Orchestrator.icoProcessSuccess++;
				
				// Calculate total execution time
				Orchestrator.totalExecutionTime += ico.getTotalCompareExecutionTime();
			} else {
				// Increment counter for compares in error
				Orchestrator.icoProccesError++;
				logger.writeError(LOCATION, SIGNATURE, "Error during compare: " + ico.getCompareException().getMessage());
			}
		}
		logger.writeDebug(LOCATION, SIGNATURE, "ICO processing done. Success: " + icoProcessSuccess + " Skipped: " + icoProccesError);
		return icoList;
	}

	
	private static String buildEnvironmentComparePath(Path currentSourcePath, String environment, String icoName) {
		String comparePath;
		comparePath = FileStructure.DIR_EXTRACT_OUTPUT_PRE + icoName + "\\" + environment + FileStructure.DIR_EXTRACT_OUTPUT_POST_LAST_ENVLESS;
		return comparePath;
	}
	
	
	
	
	/*====================================================================================
	 *------------- Getters and Setters
	 *====================================================================================*/
	public static int getIcoProcessSuccess() {
		return icoProcessSuccess;
	}


	public static int getIcoProccesError() {
		return icoProccesError;
	}
	

	public static double getTotalExecutionTime() {
		return totalExecutionTime;
	}

}