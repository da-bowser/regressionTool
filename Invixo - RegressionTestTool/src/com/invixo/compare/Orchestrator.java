package com.invixo.compare;

import java.nio.file.Path;
import java.util.List;
import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.consistency.FileStructure;
import com.invixo.main.Main;

public class Orchestrator {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = Orchestrator.class.getName();

	
	public static void start() {
		String SIGNATURE = "start()";
		logger.writeDebug(LOCATION, SIGNATURE, "Start compare");
		
		// Get number of ICO's to handle
		List<Path> sourceIcoFiles = Util.generateListOfPaths(FileStructure.DIR_EXTRACT_INPUT, "FILE");

		// Start processing files for compare
		processCompareLibs(SIGNATURE, sourceIcoFiles);
		
		logger.writeDebug(LOCATION, SIGNATURE, "Compare completed," + " results can be found here: " + FileStructure.DIR_REPORTS);
	}

	
	private static void processCompareLibs(String SIGNATURE, List<Path> sourceIcoFiles) {
		logger.writeDebug(LOCATION, SIGNATURE, "ICO's found and ready to process: " + sourceIcoFiles.size());

		// Process found ICO's
		for (int i = 0; i < sourceIcoFiles.size(); i++) {
			Path currentSourcePath = sourceIcoFiles.get(i);
			String icoName = Util.getFileName(currentSourcePath.toString(), false);
			String sourceIcoComparePath = buildEnvironmentComparePath(currentSourcePath, Main.PARAM_VAL_SOURCE_ENV, icoName);
			String targetIcoComparePath = buildEnvironmentComparePath(currentSourcePath, Main.PARAM_VAL_TARGET_ENV, icoName);

			// Create instance of CompareHandler containing all relevant data for a given ICO compare
			IntegratedConfiguration ch = new IntegratedConfiguration(sourceIcoComparePath, targetIcoComparePath, icoName);
			ch.start();
		}
	}


	private static String buildEnvironmentComparePath(Path currentSourcePath, String environment, String icoName) {
		String comparePath;
		comparePath = FileStructure.DIR_EXTRACT_OUTPUT_PRE + icoName + "\\" + environment + FileStructure.DIR_EXTRACT_OUTPUT_POST_LAST_ENVLESS;
		return comparePath;
	}

}