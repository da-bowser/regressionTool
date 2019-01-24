package com.invixo.compare;

import java.nio.file.Path;
import java.util.List;
import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.consistency.FileStructure;
import com.invixo.consistency.FileStructure2;
import com.invixo.main.Main;

public class Comparer {

	private static Logger logger 						= Logger.getInstance();
	private static final String LOCATION 				= Comparer.class.getName();

	public static void startCompare() {
		String SIGNATURE = "startCompare()";
		
		logger.writeDebug(LOCATION, SIGNATURE, "Start compare");
		
		// Get number of ICO's to handle
		List<Path> sourceIcoFiles = Util.generateListOfPaths(FileStructure2.DIR_EXTRACT_INPUT, "FILE");
//		logger.writeDebug(LOCATION, SIGNATURE, "Load compare files for comparison from: " + FileStructure.DIR_REGRESSION_COMPARE_PAYLOAD_LAST_MSG_VERSION);
//		logger.writeDebug(LOCATION, SIGNATURE, "Load source files for comparison from: " + FileStructure.DIR_REGRESSION_OUTPUT_PAYLOADS_LAST_MSG_VERSION);
//		
//		// Load ICO's to compare
//		List<Path> sourceLibs = Util.generateListOfPaths(FileStructure.DIR_REGRESSION_OUTPUT_PAYLOADS_LAST_MSG_VERSION, "DIRECTORY");
//		List<Path> compareLibs = Util.generateListOfPaths(FileStructure.DIR_REGRESSION_COMPARE_PAYLOAD_LAST_MSG_VERSION, "DIRECTORY");
//		
//		// Start processing files for compare
		processCompareLibs(SIGNATURE, sourceIcoFiles);
//		
		logger.writeDebug(LOCATION, SIGNATURE, "Compare completed," + " results can be found here: " + FileStructure.DIR_REGRESSION_COMPARE_RESULTS);


	}

	
	private static void processCompareLibs(String SIGNATURE, List<Path> sourceIcoFiles) {
		logger.writeDebug(LOCATION, SIGNATURE, "ICO's found and ready to process: " + sourceIcoFiles.size());

		// Process found ICO's
		for (int i = 0; i < sourceIcoFiles.size(); i++) {
			Path currentSourcePath = sourceIcoFiles.get(i);
			String sourceIcoComparePath = buildEnvironmentComparePath(currentSourcePath, Main.PARAM_VAL_SOURCE_ENV);
			System.out.println("Source: " + sourceIcoComparePath);
			String targetIcoComparePath = buildEnvironmentComparePath(currentSourcePath, Main.PARAM_VAL_TARGET_ENV);
			System.out.println("Target: " + targetIcoComparePath);
			// Create instance of CompareHandler containing all relevant data for a given ICO compare
			//CompareHandler ch = new CompareHandler(currentSourcePath, currentComparePath);
			//ch.start();
		}
	}


	private static String buildEnvironmentComparePath(Path currentSourcePath, String environment) {
		String comparePath;
		comparePath = FileStructure2.DIR_EXTRACT_OUTPUT_PRE + Util.getFileName(currentSourcePath.toString(), false) + "\\" + environment + FileStructure2.DIR_EXTRACT_OUTPUT_POST_LAST_ENVLESS;
		return comparePath;
		
	}


}