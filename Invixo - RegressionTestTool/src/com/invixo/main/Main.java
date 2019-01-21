package com.invixo.main;

import java.io.File;
import java.util.ArrayList;

import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.consistency.FileStructure;
import com.invixo.extraction.IntegratedConfiguration;
import com.invixo.extraction.reporting.ReportWriter;

public class Main {
	private static Logger logger 			= Logger.getInstance();
	private static final String LOCATION	= Main.class.getName();
	
	
	public static void main(String[] args) {
		// NB: this should be parameterized so it can be run from a console.
		
		// Test extraction (this should be checked for as a program parameter!!!!
		extract();
		
		// Test extraction (this should be checked for as a program parameter!!!!
//		inject();
	}

	
	/**
	 * Extract data from a productive or non-productive SAP PO system.
	 * This creates payload files (FIRST and/or LAST) on file system: 
 	 * NB: remember to set the proper properties in config file. Some should probably be parameterized in the class for safety and ease.
	 */
	public static void extract() {
		// Clean up file structure and ensure its consistency
		ensureFileStructureConsistency();
		
		// Start extracting
		ArrayList<IntegratedConfiguration> icoList = com.invixo.extraction.Orchestrator.start();
		
		// Write report
		ReportWriter report = new ReportWriter();
		report.interpretResult(icoList);
		report.create(icoList);
	}
	
		
	/**
	 * Inject new requests into a non-prod system
	 */
	public static void inject() {
		final String SIGNATURE = "inject()";
		
		// Get list of integrations to process.
		File[] files = Util.getListOfFilesInDirectory(FileStructure.DIR_REGRESSION_INPUT_ICO);
		logger.writeDebug(LOCATION, SIGNATURE, "Number of ICOs to be processed: " + files.length);
		
		// Inject all payload files related to ICO
		for (File file : files) {
			logger.writeDebug(LOCATION, SIGNATURE, "Start processing ICO: " + file);
			com.invixo.injection.Orchestrator.start();
		}
	}
	
	
	/**
	 * Start a file comparison
	 */
	public static void compare() {
		
	}
	
	
	/**
	 * Ensure the file structure is consistent for this program to run.
	 * This includes generating missing directories and file templates.
	 */
	private static void ensureFileStructureConsistency() {
		FileStructure.startCheck();
	}
	
}
