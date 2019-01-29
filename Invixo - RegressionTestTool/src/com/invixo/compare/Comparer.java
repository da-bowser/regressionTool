package com.invixo.compare;

import java.io.FileInputStream;
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
	private Path compareFile;
	
	private int compareSuccessCount = 0;
	private int compareSkippedCount = 0;
	
	private Map<String, String> diffsIgnoredByConfiguration = new HashMap<String, String>();
	private ArrayList<Difference> compareDifferences = new ArrayList<Difference>();
	private ArrayList<String> icoXPathExceptions = new ArrayList<String>();
	private CompareException ce;
	
	public Comparer(Path sourceFile, Path compareFile, ArrayList<String> icoXPathExceptions) {
		this.sourceFile = sourceFile;
		this.compareFile = compareFile;
		this.icoXPathExceptions = icoXPathExceptions;
	}

	public CompareException getCompareException() {
		return this.ce;
	}
	
	public void addDiffComparison(Difference d) {
		this.compareDifferences.add(d);
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
	
	void start() throws CompareException {
		String SIGNATURE = "start(Path, Path)";
		
		try {
			// Prepare files for compare
			String sourceFileString = Util.inputstreamToString(new FileInputStream(this.sourceFile.toFile()), GlobalParameters.ENCODING);
			String compareFileString = Util.inputstreamToString(new FileInputStream(this.compareFile.toFile()), GlobalParameters.ENCODING);
			
			// Compare string representations of source and compare payloads
			Diff diff = DiffBuilder
					.compare(sourceFileString)
					.withTest(compareFileString)
					.withDifferenceEvaluator(new CustomDifferenceEvaluator(this.icoXPathExceptions, this))
					.ignoreWhitespace()
					.normalizeWhitespace()
					.build();
			
			// Calculate result
			for (Difference d : diff.getDifferences()) {
				this.addDiffComparison(d);
			}
			
			// Increment compare success for reporting purposes
			this.compareSuccessCount++;
			
		} catch (Exception e) {
			// Increment compare skipped for reporting purposes
			this.compareSkippedCount++;
			String msg = "Problem during compare\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			
			this.ce = new CompareException(msg);
		}
	}
}
