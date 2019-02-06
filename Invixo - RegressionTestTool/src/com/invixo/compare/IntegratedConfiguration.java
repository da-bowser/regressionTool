package com.invixo.compare;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.XMLEvent;

import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.consistency.FileStructure;
import com.invixo.main.GlobalParameters;


public class IntegratedConfiguration {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = IntegratedConfiguration.class.getName();
	private List<Path> sourceFiles;
	private List<Path> compareFiles;
	private static Map<String, String> messageIdMap;
	private ArrayList<String> xpathExceptions = new ArrayList<String>();
	private String name;
	private int totalCompareDiffsFound = 0;
	private int totalCompareDiffsIgnored = 0;
	private int totalCompareDiffsUnhandled = 0;
	private int totalCompareSkipped = 0;
	private int totalCompareSuccess = 0;
	private int totalCompareProcessed = 0;
	private List<Comparer> compareProcessedList = new ArrayList<Comparer>();
	private CompareException ce;


	/**
	 * Class constructor
	 * @param sourceIcoPath
	 * @param compareIcoPath
	 * @param icoName
	 * @throws CompareException 
	 */
	public IntegratedConfiguration(String sourceIcoPath, String compareIcoPath, String icoName) {
		String SIGNATURE = "IntegratedConfiguration(String, String, String";
		logger.writeDebug(LOCATION, SIGNATURE, "Initialize compare data of ICO compare");

		try {
			// Set current ICO
			this.name = icoName;
			
			// Get files from source and compare directories
			sourceFiles = Util.generateListOfPaths(sourceIcoPath.toString(), "FILE");
			compareFiles = Util.generateListOfPaths(compareIcoPath.toString() , "FILE");
			
			// Build message id map to match "Prod"(source) and "Test"(compare) messages
			messageIdMap = buildMessageIdMap();
			
			// Build exception map to be used to exclude data elements in later compare
			xpathExceptions = extractIcoCompareExceptionsFromFile(FileStructure.FILE_CONFIG_COMPARE_EXEPTIONS, this.name);
		} catch (Exception e) {
			this.ce = new CompareException(e.getMessage());
		}
	}
	
	
	/**
	 * Extract configured XPath compare exceptions (SIMILAR) from file matching ICO.
	 * @param exceptionXPathConfigFilePath		Location of configuration file
	 * @param icoName							Name of relevant ICO
	 * @return									List of matching compare exceptions
	 * @throws CompareException
	 */
	private ArrayList<String> extractIcoCompareExceptionsFromFile(String exceptionXPathConfigFilePath, String icoName) throws CompareException {
		final String SIGNATURE = "extractIcoCompareExceptionsFromFile(String, String)";
		logger.writeDebug(LOCATION, SIGNATURE, "Building MAP of exceptions using data from: " + exceptionXPathConfigFilePath);
		
		ArrayList<String> icoExceptions = new ArrayList<String>();
		
		try {
			InputStream fileStream = new FileInputStream(exceptionXPathConfigFilePath);		
			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLEventReader eventReader = factory.createXMLEventReader(fileStream);
			boolean correctIcoFound = false;
			while (eventReader.hasNext()) {
			    XMLEvent event = eventReader.nextEvent();
			    
			    switch(event.getEventType()) {
			    case XMLStreamConstants.START_ELEMENT:
			    	String currentStartElementName = event.asStartElement().getName().getLocalPart();
			    	if ("Name".equals(currentStartElementName)) {
						if (icoName.equals(eventReader.peek().asCharacters().getData())) {
							// We are at the correct ICO element
							correctIcoFound = true;
						}
					}
			    	
			    	if ("XPath".equals(currentStartElementName) && correctIcoFound && eventReader.peek().isCharacters()) {
			    		String configuredExceptionXPath = eventReader.peek().asCharacters().getData();
			    		
			    		if (configuredExceptionXPath.length() > 0) {
				    		// Add exception data if we are at the right ICO and correct element
				    		icoExceptions.add(configuredExceptionXPath);
						}
			    	}
			    	break;
			    	
			    case XMLStreamConstants.END_ELEMENT:
			    	String currentEndElementName = event.asEndElement().getName().getLocalPart();
			    	if ("IntegratedConfiguration".equals(currentEndElementName)) {
			    		// We don't want to read any more ICO data
			    		correctIcoFound = false;
					}
			    	break;
			    }
			}
			
			// Return exceptions found
			return icoExceptions; 
			
		} catch (Exception e) {
			String msg = "Error extracting exceptions.\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new CompareException(msg);
		}
	}

	
	/**
	 * Build list of matching source and target message id's created during "Inject".
	 * @return						List of source and target message id's used when matching "LAST" files for compare
	 * @throws CompareException
	 */
	private static Map<String, String> buildMessageIdMap() throws CompareException {
		String SIGNATURE = "buildMessageIdMap(String)";
		
		try {
			logger.writeDebug(LOCATION, SIGNATURE, "Building MAP of message ID's for source and compare files from: " + FileStructure.FILE_MSG_ID_MAPPING);
			
			// Build path to mapping file generated during inject
			String mappingFilePath = FileStructure.FILE_MSG_ID_MAPPING;
			
			// Create map splitting on delimiter from map file
	        Map<String, String> mapFromFile = Util.getMessageIdsFromFile(mappingFilePath, GlobalParameters.FILE_DELIMITER, null, 1, 2);
			
	        // Return map
	        return mapFromFile;
	        
		} catch (IOException e) {
			String msg = "Error creating msgId map from file: " + FileStructure.FILE_MSG_ID_MAPPING + "\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new CompareException(msg);
		}
	}

	
	/**
	 * Start processing ICO.
	 */
	public void start() {
		String SIGNATURE = "start()";
		logger.writeDebug(LOCATION, SIGNATURE, "Processing ICO data of: \"" + this.name + "\"\nExpected compare count: " + this.sourceFiles.size());
		
		// Start looping over source files
		Path currentSourcePath;
		for (int i = 0; i < sourceFiles.size(); i++) {

			// Get matching compare file using message id map
			currentSourcePath = sourceFiles.get(i);

			// Prepare: Locate matching compare file based on source msgId
			Path comparePathMatch = getMatchingCompareFile(currentSourcePath, compareFiles, IntegratedConfiguration.messageIdMap);

			Comparer comp = new Comparer(currentSourcePath, comparePathMatch, this.xpathExceptions);
			this.compareProcessedList.add(comp);
			
			comp.start();
			
			this.totalCompareDiffsFound += comp.getCompareDifferences().size();
			this.totalCompareDiffsIgnored += comp.getDiffsIgnoredByConfiguration().size();
			this.totalCompareDiffsUnhandled = this.totalCompareDiffsFound - this.totalCompareDiffsIgnored;
			
			this.totalCompareSkipped += comp.getCompareSkipped();
			this.totalCompareSuccess += comp.getCompareSuccess();
			this.totalCompareProcessed = this.totalCompareSkipped + this.totalCompareSuccess;
		}
		
		logger.writeDebug(LOCATION, SIGNATURE, "Processing ICO data of: \"" + this.name + "\" completed.\nActual: Success: " + this.totalCompareSuccess + " Skipped: " + this.totalCompareSkipped);
		
	}
	
	
	/**
	 * Match source message id to get a target message id - used to locate compare "LAST" file in target environment.
	 * @param sourceFilePath		Location of source file
	 * @param compareFiles			List of compare files
	 * @param messageIdMap			Source <--> Target message id's	
	 * @return
	 */
	private static Path getMatchingCompareFile(Path sourceFilePath, List<Path> compareFiles, Map<String, String> messageIdMap) {
		String SIGNATURE = "getMatchingCompareFile(Path, List<Path>, Map<String, String>)";
		
		// Extract message id from filename 
		String sourceMsgId = Util.getFileName(sourceFilePath.getFileName().toString(), false);
		
		logger.writeDebug(LOCATION, SIGNATURE, "Prepare: Getting matching compare file for sourceId: " + sourceMsgId);

		// Get compare message id from map using source id
		String compareMsgId = messageIdMap.get(sourceMsgId);
		
		logger.writeDebug(LOCATION, SIGNATURE, "Prepare: match found, compare file msgId: " + compareMsgId);
		
			// Search for compare id in compare file list
			Path compareFileFound = null;
			for (int i = 0; i < compareFiles.size(); i++) {
				String currentFile = compareFiles.get(i).getFileName().toString();

				if (currentFile.toString().contains(compareMsgId)) {
					// Get current file if we have a match
					compareFileFound = compareFiles.get(i);

					// Stop searching
					break;
				}
			}

			// Handle situation where compare file is not found using mapping file
			if (compareFileFound == null) {
				compareFileFound = new File("File not found for msgId - " + compareMsgId).toPath();
			}
			
			// return compare file found
			return compareFileFound;
	}
	
	
	/*====================================================================================
	 *------------- Getters and Setters
	 *====================================================================================*/
	public String getName() {
		return name;
	}
	
	public ArrayList<String> getXpathExceptions() {
		return xpathExceptions;
	}
	
	public int getTotalCompareDiffsFound() {
		return totalCompareDiffsFound;
	}


	public int getTotalCompareDiffsIgnored() {
		return totalCompareDiffsIgnored;
	}


	public int getTotalCompareDiffsUnhandled() {
		return totalCompareDiffsUnhandled;
	}


	public int getTotalCompareSkipped() {
		return totalCompareSkipped;
	}


	public int getTotalCompareSuccess() {
		return totalCompareSuccess;
	}


	public int getTotalCompareProcessed() {
		return totalCompareProcessed;
	}
	
	public List<Comparer> getCompareProcessedList() {
		return compareProcessedList;
	}
	
	public CompareException getCompareException() {
		return ce;
	}
	
	public void setCompareException(CompareException ce) {
		this.ce = ce;
	}
}
