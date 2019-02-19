package com.invixo.compare;

import java.util.ArrayList;

import com.invixo.common.IcoOverviewInstance;
import com.invixo.common.util.Logger;
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
	
	public static ArrayList<IntegratedConfiguration> start(ArrayList<IcoOverviewInstance> icoOverviewList) {
		final String SIGNATURE = "start(ArrayList<IcoOverviewInstance>)";
		logger.writeDebug(LOCATION, SIGNATURE, "Start compare");
		
		// Start processing files for compare
		icoList = processCompareLibs(icoOverviewList);
		
		logger.writeDebug(LOCATION, SIGNATURE, "Compare completed");
		
		return icoList;
	}

	
	private static ArrayList<IntegratedConfiguration> processCompareLibs(ArrayList<IcoOverviewInstance> icoOverviewList) {
		final String SIGNATURE = "processCompareLibs(ArrayList<IcoOverviewInstance>)";
		logger.writeDebug(LOCATION, SIGNATURE, "ICO's found and ready to process: " + icoOverviewList.size());

		// Process found ICO's
		for (int i = 0; i < icoOverviewList.size(); i++) {
			logger.writeDebug(LOCATION, SIGNATURE, "[ICO: " + (i+1) + " ] processing");
			String currentIcoName = icoOverviewList.get(i).getName();
			String sourceIcoComparePath = buildEnvironmentComparePath(GlobalParameters.PARAM_VAL_SOURCE_ENV, currentIcoName);
			String targetIcoComparePath = buildEnvironmentComparePath(GlobalParameters.PARAM_VAL_TARGET_ENV, currentIcoName);

			// Create instance of CompareHandler containing all relevant data for a given ICO compare
			IntegratedConfiguration ico = new IntegratedConfiguration(sourceIcoComparePath, targetIcoComparePath, currentIcoName);
			
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

	
	private static String buildEnvironmentComparePath(String environment, String icoName) {
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