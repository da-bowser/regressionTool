package com.invixo.main;

import com.invixo.consistency.FileStructure;
import com.invixo.messageExtractor.main.Orchestrator;

public class Main {

	public static void main(String[] args) {
		ensureFileStructureConsistency();
		extract();
	}

	
	/**
	 * Extract data from a productive or non-productive SAP PO system.
	 * This creates 2 files on file system: 
	 * 		1) Raw multipart message extracted from Web Service GetMessageBytesJavaLangStringIntBoolean
	 * 		2) SAP PO payload extracted from multipart message
	 * 
 	 * NB: remember to set the proper properties in config file. Some should probably be parameterized in the class for safety and ease.
	 */
	public static void extract() {
		Orchestrator.startAll();
	}
	
	
	
	/**
	 * Generate injection ready SOAP envelopes (with SAP PO header and proper payload from an extract) and place on file system
	 */
	public static void generateInjectionFiles() {
		
	}
	
	
	/**
	 * Inject new requests into a non-prod system
	 */
	public static void inject() {
		
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
	public static void ensureFileStructureConsistency() {
		FileStructure.startCheck();
	}
	
}
