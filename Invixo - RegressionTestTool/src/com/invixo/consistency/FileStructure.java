package com.invixo.consistency;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.invixo.common.IcoOverviewInstance;
import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.main.GlobalParameters;

public class FileStructure {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = FileStructure.class.getName();
	
	// Base/root file location
	private static final String FILE_BASE_LOCATION					= GlobalParameters.PARAM_VAL_BASE_DIR;
	
	// Extract: input
	private static final String DIR_EXTRACT							= FILE_BASE_LOCATION + "\\_Extract";
	
	// Extract: output
	public static final String DIR_EXTRACT_OUTPUT_PRE					= FILE_BASE_LOCATION + "\\_Extract\\Output\\";
	public static final String DIR_EXTRACT_OUTPUT_POST_FIRST_ENVLESS	= "\\Output\\Payloads\\First\\";
	public static final String DIR_EXTRACT_OUTPUT_POST_LAST_ENVLESS		= "\\Output\\Payloads\\Last\\";
	private static final String DIR_EXTRACT_OUTPUT_POST_DEV_FIRST		= "\\DEV" + DIR_EXTRACT_OUTPUT_POST_FIRST_ENVLESS;
	private static final String DIR_EXTRACT_OUTPUT_POST_DEV_LAST		= "\\DEV" + DIR_EXTRACT_OUTPUT_POST_LAST_ENVLESS;
	private static final String DIR_EXTRACT_OUTPUT_POST_TST_FIRST		= "\\TST" + DIR_EXTRACT_OUTPUT_POST_FIRST_ENVLESS;
	private static final String DIR_EXTRACT_OUTPUT_POST_TST_LAST		= "\\TST" + DIR_EXTRACT_OUTPUT_POST_LAST_ENVLESS;
	private static final String DIR_EXTRACT_OUTPUT_POST_PRD_FIRST		= "\\PRD" + DIR_EXTRACT_OUTPUT_POST_FIRST_ENVLESS;
	private static final String DIR_EXTRACT_OUTPUT_POST_PRD_LAST		= "\\PRD" + DIR_EXTRACT_OUTPUT_POST_LAST_ENVLESS;
	
	// Inject: mapping table
	private static final String DIR_INJECT							= FILE_BASE_LOCATION + "\\_Inject\\";
	
	// Various
	private static final String DIR_LOGS							= FILE_BASE_LOCATION + "\\Logs\\";		// Manually set in Logger also.
	private static final String DIR_DEBUG							= FILE_BASE_LOCATION + "\\Debug\\";
	public static final String DIR_REPORTS							= FILE_BASE_LOCATION + "\\Reports\\";
	public static final String DIR_CONFIG							= FILE_BASE_LOCATION + "\\Config\\";
	
	// Files
	public static final String FILE_CONFIG_SYSTEM_MAPPING			= DIR_CONFIG + "systemMapping.txt";
	public static final String FILE_CONFIG_COMPARE_EXEPTIONS		= DIR_CONFIG + "compareExceptions.xml";
	public static final String FILE_MSG_ID_MAPPING					= DIR_INJECT + GlobalParameters.PARAM_VAL_SOURCE_ENV + "_to_" + GlobalParameters.PARAM_VAL_TARGET_ENV + "_msgId_map.txt";
	public static final String FILE_STATE							= DIR_INJECT + GlobalParameters.PARAM_VAL_SOURCE_ENV + "_to_" + GlobalParameters.PARAM_VAL_TARGET_ENV + "_msgId_mapNEW.txt";
	public static final String FILE_STATE_PATH						= DIR_INJECT + GlobalParameters.PARAM_VAL_SOURCE_ENV + "_to_" + GlobalParameters.PARAM_VAL_TARGET_ENV + "\\";
	public static final String PAYLOAD_FILE_EXTENSION 				= ".multipart";	
	public static final String ICO_OVERVIEW_FILE 					= DIR_CONFIG + GlobalParameters.PARAM_VAL_SOURCE_ENV + "_IntegratedConfigurationsOverview.xml";
	
	static {
		final String SIGNATURE = "static";
		logger.writeDebug(LOCATION, SIGNATURE, "File containing Message ID Mapping initialized to: " + FILE_MSG_ID_MAPPING);
	}
	
	
	/**
	 * Start File Structure check.
	 */
	public static void startCheck(ArrayList<IcoOverviewInstance> icoList) {
		String SIGNATURE = "startCheck(ArrayList<IcoOverviewInstance>)";
		logger.writeDebug(LOCATION, SIGNATURE, "Start file structure check");

		// Ensure project folder structure is present
		checkFolderStructure(icoList);
		
		// Ensure critical run files exists
		checkBaseFiles(icoList);
		
		logger.writeDebug(LOCATION, SIGNATURE, "File structure check completed!");
	}


	/**
	 * Ensure project folder structure is healthy.
	 */
	private static void checkFolderStructure(ArrayList<IcoOverviewInstance> icoList) {
		Util.createDirIfNotExists(FILE_BASE_LOCATION);
		Util.createDirIfNotExists(DIR_EXTRACT);
		Util.createDirIfNotExists(DIR_EXTRACT_OUTPUT_PRE);
		Util.createDirIfNotExists(DIR_INJECT);
		Util.createDirIfNotExists(DIR_LOGS);
		Util.createDirIfNotExists(DIR_REPORTS);
		Util.createDirIfNotExists(DIR_CONFIG);
		Util.createDirIfNotExists(DIR_DEBUG);
		
		// Generate dynamic output folders based on ICO request files
		for (IcoOverviewInstance ico : icoList) {
			// Build path
			String icoDynamicPath = DIR_EXTRACT_OUTPUT_PRE + ico.getName();
			
			// Create output folders for DEV, TST, PRD
			Util.createDirIfNotExists(icoDynamicPath + DIR_EXTRACT_OUTPUT_POST_DEV_FIRST);
			Util.createDirIfNotExists(icoDynamicPath + DIR_EXTRACT_OUTPUT_POST_DEV_LAST);
			Util.createDirIfNotExists(icoDynamicPath + DIR_EXTRACT_OUTPUT_POST_TST_FIRST);
			Util.createDirIfNotExists(icoDynamicPath + DIR_EXTRACT_OUTPUT_POST_TST_LAST);
			Util.createDirIfNotExists(icoDynamicPath + DIR_EXTRACT_OUTPUT_POST_PRD_FIRST);
			Util.createDirIfNotExists(icoDynamicPath + DIR_EXTRACT_OUTPUT_POST_PRD_LAST);
		}
	}

	
	private static void checkBaseFiles(ArrayList<IcoOverviewInstance> icoList) {
		String SIGNATURE = "checkBaseFiles(ArrayList<IcoOverviewInstance>)";
		File systemMappingFile = new File(FILE_CONFIG_SYSTEM_MAPPING);

		// Make sure system mapping file exists
		if (systemMappingFile.exists()) {
			logger.writeDebug(LOCATION, SIGNATURE, FILE_CONFIG_COMPARE_EXEPTIONS + " exists!");
		} else {
			logger.writeDebug(LOCATION, SIGNATURE, "System critical file: " + FILE_CONFIG_COMPARE_EXEPTIONS + " is missing and will be created!");
			Util.writeFileToFileSystem(FILE_CONFIG_SYSTEM_MAPPING, "".getBytes());
		}
		
		// Always create the ICO exception file with current ICO request files when a new run i started / overwrite if exists
		if (GlobalParameters.PARAM_VAL_OPERATION.equals(GlobalParameters.Operation.extract.toString())) {
			logger.writeDebug(LOCATION, SIGNATURE, GlobalParameters.PARAM_VAL_OPERATION + " scenario found, create a new " + FILE_CONFIG_SYSTEM_MAPPING + " to make sure all ICO's are represented for later compare run");
			generateInitialIcoExeptionContent(icoList);
		}
	}

	
	private static void generateInitialIcoExeptionContent(ArrayList<IcoOverviewInstance> icoList) {
		final String	XML_PREFIX = "inv";
		final String	XML_NS = "urn:invixo.com.consistency";
		try {
			XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
			XMLStreamWriter xmlWriter = xMLOutputFactory.createXMLStreamWriter(new FileOutputStream(FILE_CONFIG_COMPARE_EXEPTIONS), GlobalParameters.ENCODING);

			// Add xml version and encoding to output
			xmlWriter.writeStartDocument(GlobalParameters.ENCODING, "1.0");

			// Create element: Configuration
			xmlWriter.writeStartElement(XML_PREFIX, "Configuration", XML_NS);
			xmlWriter.writeNamespace(XML_PREFIX, XML_NS);
			
			// Loop ICO's found
			for (IcoOverviewInstance ico : icoList) {
				// Get name of current ICO
				String icoName = ico.getName();
				
				// Create element: Configuration | IntegratedConfiguration
				xmlWriter.writeStartElement(XML_PREFIX, "IntegratedConfiguration", XML_NS);
				
				// Create element: Configuration | IntegratedConfiguration | Name
				xmlWriter.writeStartElement(XML_PREFIX, "Name", XML_NS);
				xmlWriter.writeCharacters(icoName);
				// Close element: Configuration | IntegratedConfiguration | Name
				xmlWriter.writeEndElement();
				
				// Create element: Configuration | IntegratedConfiguration | Exceptions
				xmlWriter.writeStartElement(XML_PREFIX, "Exceptions", XML_NS);
				
				// Create element: Configuration | IntegratedConfiguration | Exceptions | XPath
				xmlWriter.writeStartElement(XML_PREFIX, "XPath", XML_NS);
				// Close element: Configuration | IntegratedConfiguration | Exceptions | XPath
				xmlWriter.writeEndElement();
				
				// Close element: Configuration | IntegratedConfiguration | Exceptions
				xmlWriter.writeEndElement();
				
				// Close element: Configuration | IntegratedConfiguration
				xmlWriter.writeEndElement();
			}
			
			// Close element: IntegratedConfigurations
			xmlWriter.writeEndElement();
			
			// Finalize writing
			xmlWriter.flush();
			xmlWriter.close();
		} catch (XMLStreamException | FileNotFoundException e) {
			throw new RuntimeException("Error generating compareExceptions.xml file! " + e);
		}
	}
	
	
	/**
	 * Generate file name for a file only used at debugging time (this is web service requests and responses).
	 * @param webServiceName
	 * @param isRequest
	 * @param identifier
	 * @param extension
	 * @return
	 */
	public static String getDebugFileName(String webServiceName, boolean isRequest, String identifier, String extension) {
		String fileName = FileStructure.DIR_DEBUG 
						+ webServiceName 
						+ "_" 
						+ (isRequest?"req":"resp") 
						+ "_" 
						+ identifier 
						+ "_" 
						+ System.currentTimeMillis() 
						+ "." 
						+ extension;
		return fileName;
	}
}
