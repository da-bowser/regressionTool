package com.invixo.compare;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.consistency.FileStructure;


public class CompareHandler {
	
	private static Logger logger 					= Logger.getInstance();
	private static final String LOCATION 			= CompareHandler.class.getName();
	private List<Path> sourceFiles;
	private List<Path> compareFiles;
	private static Map<String, String> messageIdMap; // Static only loaded once!
	private	List<String> compareExceptions;
	private String sourceIcoName;

	
	/**
	 * Class constructor
	 * @param sourceIcoPath
	 * @param compareIcoPath
	 */
	public CompareHandler(Path sourceIcoPath, Path compareIcoPath) {
		String SIGNATURE = "CompareHandler - *Class Constructor*";
		logger.writeDebug(LOCATION, SIGNATURE, "Initialize compare data of ICO compare");
		
		// Set current ICO
		sourceIcoName = sourceIcoPath.getFileName().toString();
		
		// Get files from source and compare directories
		sourceFiles = Util.generateListOfPaths(sourceIcoPath.toString(), "FILE");
		compareFiles = Util.generateListOfPaths(compareIcoPath.toString(), "FILE");
		
		// Build message id map to match "Prod"(source) and "Test"(compare) messages
		messageIdMap = buildMessageIdMap();
		
		// Build exception map to be used to exclude data elements in later compare
		compareExceptions = buildCompareExceptionMap(FileStructure.DIR_REGRESSION_COMPARE_EXEPTIONS + sourceIcoName + "\\");
		
	}

	
	private static List<String> buildCompareExceptionMap(String icoExceptionFilePath) {
		String SIGNATURE = "buildCompareExceptionMap";
		logger.writeDebug(LOCATION, SIGNATURE, "Building MAP of exceptions using data from: " + icoExceptionFilePath);
		
		// Make sure the ICO has an exception folder if compare exclusions are needed in future compare runs
		FileStructure.createDirIfNotExists(icoExceptionFilePath);
		
		// Get exceptions found for current ICO
		List<Path> compareExceptionList;
		compareExceptionList = Util.generateListOfPaths(icoExceptionFilePath, "FILE");
		
		// Get all exceptions listed in files found
		List<String> compareExceptions = new ArrayList<String>();
		for (Path path : compareExceptionList) {
			try {
				// Appends to compareExceptions list for each file found - currently only one exception file is expected, but who knows!
				Files.lines(path).collect(Collectors.toCollection(() -> compareExceptions));
				
			} catch (IOException e) {
				String msg = "ERROR | Reading exceptions from: " + icoExceptionFilePath + e.getMessage();
				throw new RuntimeException(msg);
			}
		}
		
		// Return exception map
		return compareExceptions;
	}

	
	private static Map<String, String> buildMessageIdMap() {
		String SIGNATURE = "buildMessageIdMap";
		
		logger.writeDebug(LOCATION, SIGNATURE, "Building MAP of message ID's for source and compare files from: " + FileStructure.DIR_REGRESSION_OUTPUT_MAPPING);
		List<Path> mapList;
		try {
			mapList = Util.generateListOfPaths(FileStructure.DIR_REGRESSION_OUTPUT_MAPPING, "FILE");
			
			// Get first entry in mapList as there is always only one mapping file!
			Path mapFilePath = mapList.get(0);
			
			// Create map splitting on delimiter | from map file
	        Map<String, String> mapFromFile = Util.createMapFromPath(mapFilePath, "\\|", 0, 1);
			
	        // Return map
	        return mapFromFile;
	        
		} catch (IOException e) {
			String msg = "ERROR | Can't read msgId map from: " + FileStructure.DIR_REGRESSION_OUTPUT_MAPPING + e.getMessage();
			throw new RuntimeException(msg);
		}
	}

	
	public void start() {
		String SIGNATURE = "start()";
		logger.writeDebug(LOCATION, SIGNATURE, "Processing ICO data of: \"" + this.sourceIcoName + "\" start");
		
		// Prepare ICO compare files
		this.prepareFilesAndCompare();
		
		logger.writeDebug(LOCATION, SIGNATURE, "Processing ICO data of: \"" + this.sourceIcoName + "\" completed!");
	}

	
	private void prepareFilesAndCompare() {
		String SIGNATURE = "prepareFilesAndCompare()";
		logger.writeDebug(LOCATION, SIGNATURE, "Start prepare and compare. Source files: " + this.sourceFiles.size()  + " Target files: " + this.compareFiles.size());

		// Start looping over source files
		int fileCompareCount = 0;
		Path currentSourcePath;
		for (int i = 0; i < sourceFiles.size(); i++) {

			// Get matching compare file using message id map
			currentSourcePath = sourceFiles.get(i); 

			// Locate matching compare file based on source msgId
			Path comparePathMatch = getMatchingCompareFile(currentSourcePath, compareFiles, CompareHandler.messageIdMap);

			// Do compare
			compareFiles(currentSourcePath, comparePathMatch);

			// Increment compare count
			fileCompareCount++;
		}

		logger.writeDebug(LOCATION, SIGNATURE, "Prepare and compare done. Files compared: " + fileCompareCount); 
		
	}
	
	
	private static Path getMatchingCompareFile(Path sourceFilePath, List<Path> compareFiles, Map<String, String> map) {
		String SIGNATURE = "getMatchingCompareFile()";
		
		// Extract message id from filename 
		String sourceMsgId = Util.getFileName(sourceFilePath.getFileName().toString(), false);
		
		logger.writeDebug(LOCATION, SIGNATURE, "Getting matching compare file for sourceId: " + sourceMsgId);

		// Get compare message id from map using source id
		String compareMsgId = map.get(sourceMsgId);
		
		logger.writeDebug(LOCATION, SIGNATURE, "Match found, compare file msgId: " + compareMsgId);
		
		try {
			// Search for compare id in compare file list
			Path compareFileFound = sourceFilePath;
			for (int i = 0; i < compareFiles.size(); i++) {
				String currentFile = compareFiles.get(i).getFileName().toString();

				if (currentFile.toString().contains(compareMsgId)) {
					// Get current file if we have a match
					compareFileFound = compareFiles.get(i);

					// Stop searching
					break;
				}

			}		
			// return compare file found
			return compareFileFound;
			
		} catch (NullPointerException e) {
			String msg = "ERROR | No matching message id found for: " + sourceMsgId + " " + e.getMessage();
			throw new RuntimeException(msg);
		}
	}
	
	private void compareFiles(Path sourcePath, Path comparePath) {
		String SIGNATURE = "compareFiles()";
		logger.writeDebug(LOCATION, SIGNATURE, "Start comparring: " + sourcePath + " and " + comparePath);
		
		// TODO: how do we check the mime-type of the message, xml, text, etc - for now we assume payloads are always xml.
		//if (sourcePath.getFileName().toString().contains(".xml")) {
		this.doXmlCompare(sourcePath, comparePath);
		//} else {
			//this.doTextCompare(sourcePath, comparePath);
		//}
		
		logger.writeDebug(LOCATION, SIGNATURE, "Compare done!");
	}
	
	
	private void doXmlCompare(Path sourcePath, Path comparePath) {
		String sourceFileString = null;
		String compareFileString = null;

		try {
			// Prepare files for compare
			sourceFileString = Util.inputstreamToString(new FileInputStream(sourcePath.toFile()), "UTF-8");
			compareFileString = Util.inputstreamToString(new FileInputStream(comparePath.toFile()), "UTF-8");

			// Compare string representations of source and compare payloads
			Diff xmlDiff = DiffBuilder
					.compare(sourceFileString)
					.withTest(compareFileString)
					.withDifferenceEvaluator(new CustomDifferenceEvaluator(this.compareExceptions))
					.ignoreWhitespace()
					.normalizeWhitespace()
					.build();
			
			// Handle compare result
			handleCompareResult(xmlDiff, sourcePath.getFileName().toString(), comparePath.getFileName().toString());

		} catch (FileNotFoundException e) {
			String msg = "ERROR | Problem converting source and/or compare payloads to string: " + e.getMessage();
			throw new RuntimeException(msg);
		}
	}
	
	private void handleCompareResult(Diff xmlDiff, String sourceFileName, String compareFileName) {
		String SIGNATURE = "handleCompareResult()";

		String result =	"Result:\n--------------------------------------------------\n";
		
		Iterable<Difference> diffs = xmlDiff.getDifferences();
		int diffErrors = 0;
		for (Difference d : diffs) {
			result += d.getComparison() + "\n";
			diffErrors++;
		}		
		
		logger.writeDebug(LOCATION, SIGNATURE, "Differences found during compare: " + diffErrors);

		// Write result to file system
		writeCompareResultToFile(sourceFileName, compareFileName, result, diffErrors);
	}


	private void writeCompareResultToFile(String sourceFileName, String compareFileName, String result,
			int diffErrors) {
		// Make sure we have a results+ICO directory to write results
		String targetResultDir = FileStructure.DIR_REGRESSION_COMPARE_RESULTS + this.sourceIcoName;
		FileStructure.createDirIfNotExists(targetResultDir);

		// Build final result path
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss").format(new Date());
		String resultFilePath = targetResultDir + "\\" + timeStamp + "_Errors_" + diffErrors + "_" + sourceFileName + " vs. " + compareFileName + ".txt";

		// Write to file system
		Util.writeFileToFileSystem(resultFilePath , result.getBytes());
	}

	
}
