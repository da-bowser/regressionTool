package com.invixo.compare;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.XMLEvent;

import com.invixo.common.StateException;
import com.invixo.common.StateHandler;
import com.invixo.common.util.Logger;
import com.invixo.consistency.FileStructure;


public class IntegratedConfiguration {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = IntegratedConfiguration.class.getName();
	
	private Map<String, String> messageIdMap;
	private ArrayList<String> xpathExceptions = new ArrayList<String>();

	private String name;
	private String baseFilePath = null;		// Path to LAST payloads, baseline (often PRD)
	private String compareFilePath = null;	// Path to LAST payloads, compare (often TST)

	private int totalCompareDiffsFound = 0;
	private int totalCompareDiffsIgnored = 0;
	private int totalCompareDiffsUnhandled = 0;
	private int totalCompareSkipped = 0;
	private int totalCompareSuccess = 0;
	private int totalCompareProcessed = 0;
	private double totalCompareExecutionTime = 0;

	private List<Comparer> compareProcessedList = new ArrayList<Comparer>();
	private CompareException ce;


	/**
	 * Class constructor
	 * @param sourceIcoPath
	 * @param compareIcoPath
	 * @param icoName
	 * @throws CompareException 
	 */
	IntegratedConfiguration(String sourceIcoPath, String compareIcoPath, String icoName) {
		final String SIGNATURE = "IntegratedConfiguration(String, String, String";
		logger.writeDebug(LOCATION, SIGNATURE, "Initialize compare data for ICO compare");

		try {
			this.name 				= icoName;			// ICO name
			this.baseFilePath 		= sourceIcoPath;	// Path to LAST payloads: baseline (in many cases this is the extract from PRD)
			this.compareFilePath 	= compareIcoPath;	// Path to LAST payloads: compare (in many cases this is extract from TST)
					
			// Build message id map to match "Prod"(source) and "Test"(compare) messages
			this.messageIdMap = buildMessageIdMap(this.name);
			
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
	 * Build list of matching source and target message id's.
	 * List is created from Message Id mapping file.
	 * @param icoName				ICO name to be used for filtering the Message Id Mapping file
	 * @return						List of source and target message id's used when matching "LAST" files for compare
	 * @throws CompareException
	 */
	private static Map<String, String> buildMessageIdMap(String icoName) throws CompareException {
		String SIGNATURE = "buildMessageIdMap(String)";
		try {
			logger.writeDebug(LOCATION, SIGNATURE, "Building MAP of message ID's for source and compare files from: " + FileStructure.FILE_STATE_PATH);
			
			// Initialize state handler
			StateHandler.init(icoName);
			
			// Create map splitting on delimiter from map file <original extract id, inject message id>
	        Map<String, String> mapFromFile = StateHandler.getCompareMessageIdsFromIcoLines();
			
	        // Return map
	        return mapFromFile;
		} catch (StateException e) {
			String msg = "Error creating msgId map from file: " + StateHandler.getIcoPath() + "\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new CompareException(msg);
		}
	}

	
	/**
	 * Start processing ICO.
	 */
	void start() {
		String SIGNATURE = "start()";
		logger.writeDebug(LOCATION, SIGNATURE, "Processing ICO data of: \"" + this.name + "\"\nExpected compare count: " + this.messageIdMap.size());
		
		// Process each Message Id representing the baseline (often PRD)
		for (Entry<String, String> entry : this.messageIdMap.entrySet()) {
			final Path currentSourcePath = Paths.get(this.baseFilePath + entry.getKey() + FileStructure.PAYLOAD_FILE_EXTENSION);
			final Path comparePathMatch = Paths.get(this.compareFilePath + entry.getValue() + FileStructure.PAYLOAD_FILE_EXTENSION);

			Comparer comp = new Comparer(currentSourcePath, comparePathMatch, this.xpathExceptions);
			this.compareProcessedList.add(comp);
			
			comp.start();
			
			this.totalCompareDiffsFound += comp.getCompareDifferences().size();
			this.totalCompareDiffsIgnored += comp.getDiffsIgnoredByConfiguration().size();
			this.totalCompareDiffsUnhandled = this.totalCompareDiffsFound - this.totalCompareDiffsIgnored;
			
			this.totalCompareSkipped += comp.getCompareSkipped();
			this.totalCompareSuccess += comp.getCompareSuccess();
			this.totalCompareProcessed = this.totalCompareSkipped + this.totalCompareSuccess;
			this.totalCompareExecutionTime += comp.getExecutionTimeSeconds();
		}
		
		logger.writeDebug(LOCATION, SIGNATURE, "Processing ICO data of: \"" + this.name + "\" completed.\nActual: Success: " + this.totalCompareSuccess + " Skipped: " + this.totalCompareSkipped);
		logger.writeDebug(LOCATION, SIGNATURE, "Total execution time (seconds): " + this.totalCompareExecutionTime);
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
	
	
	public double getTotalCompareExecutionTime() {
		return totalCompareExecutionTime;
	}

}
