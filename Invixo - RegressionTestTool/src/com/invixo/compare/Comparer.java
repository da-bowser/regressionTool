package com.invixo.compare;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.main.GlobalParameters;

public class Comparer {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = IntegratedConfiguration.class.getName();
	
	private Path sourceFile;
	private long sourceFileSize = 0;
	private Path compareFile;
	private long compareFileSize = 0;

	private int compareSuccessCount = 0; // Max 1, but it is easier to summarize in IntegratedConfiguration afterwards 
	private int compareSkippedCount = 0; // Max 1, but it is easier to summarize in IntegratedConfiguration afterwards
	private double executionTimeSeconds = 0;
	
	private Map<String, String> diffsIgnoredByConfiguration = new HashMap<String, String>();
	private ArrayList<Difference> compareDifferences = new ArrayList<Difference>();
	private ArrayList<String> icoXPathExceptions = new ArrayList<String>();
	private CompareException ce;
	
	public Comparer(Path sourceFile, Path compareFile, ArrayList<String> icoXPathExceptions) {
		this.sourceFile = sourceFile;
		this.compareFile = compareFile;
		this.icoXPathExceptions = icoXPathExceptions;
	}

	
	/**
	 * Start Compare
	 */
	void start() {
		String SIGNATURE = "start()";
		
		try {
			// Set file sizes for later reporting purposes
			setFileSizes(this.sourceFile, this.compareFile);
			
			// Prepare files for compare
			String sourceFileString = Util.inputstreamToString(new FileInputStream(this.sourceFile.toFile()), GlobalParameters.ENCODING);
			String compareFileString = Util.inputstreamToString(new FileInputStream(this.compareFile.toFile()), GlobalParameters.ENCODING);
						
			// Do compare
			Diff diff = compare(sourceFileString, compareFileString);
			
			// Add differences found for later reporting
			for (Difference d : diff.getDifferences()) {
				this.compareDifferences.add(d);
			}
			
			// Increment compare success for reporting purposes
			this.compareSuccessCount++;
		} catch (FileNotFoundException e) {
			// Increment compare skipped for reporting purposes
			this.compareSkippedCount++;
			String msg = "Problem during compare\n" + e.getMessage() + "\n" +
						 "No target msgId could be found in msgId mapping file to match the source.\n" +
						 "REASON: No \"First\" message could be retreived from the " + GlobalParameters.PARAM_VAL_SOURCE_ENV + " environment.\n" +
						 "As a result of this, a message is not injected to the " + GlobalParameters.PARAM_VAL_TARGET_ENV + " environment making a compare impossible.";

			logger.writeError(LOCATION, SIGNATURE, msg);
			
			this.ce = new CompareException(msg);
		}
	}


	private void setFileSizes(Path sourceFile, Path compareFile) {
		final String SIGNATURE = "setFileSizes(Path, Path)";
		
		try {
			this.sourceFileSize = Files.size(sourceFile);
			this.compareFileSize = Files.size(compareFile);
			
		} catch (IOException e) {
			// Not critical - just nice to know when reporting so no exception needs to be thrown
			logger.writeError(LOCATION, SIGNATURE, "Error during determination of file sizes" + "\n" + e.getMessage());
		}
	}


	private Diff compare(String sourceFileString, String compareFileString) {
		// Set start timer
		long startTime = Util.getTime();
		
		// Compare string representations of source and compare payloads
		Diff diff = DiffBuilder
				.compare(sourceFileString)
				.withTest(compareFileString)
				.withDifferenceEvaluator(new CustomDifferenceEvaluator(this.icoXPathExceptions, this))
				.ignoreWhitespace()
				.normalizeWhitespace()
				.build();
		
		// Set end timer
		long endTime = Util.getTime();
		
		// Calculate execution time
		this.executionTimeSeconds = Util.measureTimeTaken(startTime, endTime);
		
		return diff;
	}


	/*====================================================================================
	 *------------- Getters and Setters
	 *====================================================================================*/
	public CompareException getCompareException() {
		return this.ce;
	}
	
	public ArrayList<Difference> getCompareDifferences() {
		return this.compareDifferences;
	}
	
	public void addDiffIgnored(String diffFound, String ignoreXPath) {
		this.diffsIgnoredByConfiguration.put(diffFound, ignoreXPath);
	}
	
	public Map<String, String> getDiffsIgnoredByConfiguration() {
		return this.diffsIgnoredByConfiguration;
	}
	
	public int getCompareSuccess() {
		return this.compareSuccessCount;
	}
	
	public int getCompareSkipped() {
		return this.compareSkippedCount;
	}
	
	public Path getSourceFile() {
		return this.sourceFile;
	}
	
	public Path getCompareFile() {
		return this.compareFile;
	}
	
	public double getExecutionTimeSeconds() {
		return this.executionTimeSeconds;
	}
	
	
	public long getSourceFileSize() {
		return sourceFileSize;
	}


	public long getCompareFileSize() {
		return compareFileSize;
	}
}
