package com.invixo.main;

import java.util.ArrayList;
import com.invixo.common.util.Logger;
import com.invixo.compare.Comparer;
import com.invixo.consistency.FileStructure;
import com.invixo.extraction.IntegratedConfiguration;
import com.invixo.extraction.reporting.ReportWriter;

public class Main {
	private static Logger logger 			= Logger.getInstance();
	private static final String LOCATION	= Main.class.getName();
	
	
	public static void main(String[] args) {
		for (String param : args) {
			System.out.println(param);
		}
		
		// NB: this should be parameterized so it can be run from a console.
		
		// Test extraction (this should be checked for as a program parameter!!!!
		extract();
		
		// Test inject (this should be checked for as a program parameter!!!!
//		inject();  
		
		// Test compare (this should be checked for as a program parameter!!!!
//		compare();
	}

	
	/**
	 * Extract data from a productive or non-productive SAP PO system.
	 * This creates payload files (FIRST and/or LAST) on file system: 
 	 * NB: remember to set the proper properties in config file. Some should probably be parameterized in the class for safety and ease.
	 */
	public static void extract() {
		final String SIGNATURE = "extract()";
		
		// Clean up file structure and ensure its consistency
		ensureFileStructureConsistency();
		
		// Start extracting
		ArrayList<IntegratedConfiguration> icoList = com.invixo.extraction.Orchestrator.start();
		
		// Write report
		ReportWriter report = new ReportWriter();
		report.interpretResult(icoList);
		String reportName = report.create(icoList);
		logger.writeDebug(LOCATION, SIGNATURE, "Report generated: " + reportName);
	}
	
		
	/**
	 * Inject new requests into a non-prod system
	 */
	public static void inject() {
		final String SIGNATURE = "inject()";
		// Start injecting
		ArrayList<com.invixo.injection.IntegratedConfiguration> icoList = com.invixo.injection.Orchestrator.start();
		
		// Write report
		com.invixo.injection.reporting.ReportWriter report = new com.invixo.injection.reporting.ReportWriter();
		report.interpretResult(icoList);
		String reportName = report.create(icoList);
		logger.writeDebug(LOCATION, SIGNATURE, "Report generated: " + reportName);
	}
	
	
	/**
	 * Start a file comparison
	 */
	public static void compare() {
		Comparer.startCompare();
	}
	
	
	/**
	 * Ensure the file structure is consistent for this program to run.
	 * This includes generating missing directories and file templates.
	 */
	private static void ensureFileStructureConsistency() {
		FileStructure.startCheck();
	}
	
}
