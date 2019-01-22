package com.invixo.compare;

import java.nio.file.Path;
import java.util.List;
import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.consistency.FileStructure;

public class Comparer {

	private static Logger logger 						= Logger.getInstance();
	private static final String LOCATION 				= Comparer.class.getName();

	public static void startCompare() {
		String SIGNATURE = "startCompare()";
	
		logger.writeDebug(LOCATION, SIGNATURE, "Start compare");
		logger.writeDebug(LOCATION, SIGNATURE, "Load compare files for comparison from: " + FileStructure.DIR_REGRESSION_COMPARE_PAYLOAD_LAST_MSG_VERSION);
		logger.writeDebug(LOCATION, SIGNATURE, "Load source files for comparison from: " + FileStructure.DIR_REGRESSION_OUTPUT_PAYLOADS_LAST_MSG_VERSION);
		
		// Load ICO's to compare
		List<Path> sourceLibs = Util.generateListPath(FileStructure.DIR_REGRESSION_OUTPUT_PAYLOADS_LAST_MSG_VERSION, "DIRECTORY");
		List<Path> compareLibs = Util.generateListPath(FileStructure.DIR_REGRESSION_COMPARE_PAYLOAD_LAST_MSG_VERSION, "DIRECTORY");
		
		// Start processing files for compare
		processCompareLibs(SIGNATURE, sourceLibs, compareLibs);
		
		logger.writeDebug(LOCATION, SIGNATURE, "Compare completed," + " results can be found here: " + FileStructure.DIR_REGRESSION_COMPARE_RESULTS);


	}

	
	private static void processCompareLibs(String SIGNATURE, List<Path> sourceLibs, List<Path> compareLibs) {
		logger.writeDebug(LOCATION, SIGNATURE, "ICO's found and ready to process: " + sourceLibs.size());

		// Process found ICO's
		for (int i = 0; i < sourceLibs.size(); i++) {
			Path currentSourcePath = sourceLibs.get(i);
			Path currentComparePath = compareLibs.get(i);

			// Create instance of CompareHandler containing all relevant data for a given ICO compare
			CompareHandler ch = new CompareHandler(currentSourcePath, currentComparePath);
			ch.start();
		}
	}


}
