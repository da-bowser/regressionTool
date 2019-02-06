package com.invixo.consistency;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.main.GlobalParameters;
import com.invixo.main.Main;


public class FileStructure {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = FileStructure.class.getName();
	
	// Base/root file location
	public static final String FILE_BASE_LOCATION					= Main.PARAM_VAL_BASE_DIR;
	
	// Extract: input
	private static final String DIR_EXTRACT							= FILE_BASE_LOCATION + "\\_Extract";
	public static final String DIR_EXTRACT_INPUT					= DIR_EXTRACT + "\\Input\\Integrated Configurations\\";
	
	// Extract: output
	public static final String DIR_EXTRACT_OUTPUT_PRE					= FILE_BASE_LOCATION + "\\_Extract\\Output\\";
	public static final String DIR_EXTRACT_OUTPUT_POST_FIRST_ENVLESS	= "\\Output\\Payloads\\First\\";
	public static final String DIR_EXTRACT_OUTPUT_POST_LAST_ENVLESS		= "\\Output\\Payloads\\Last\\";
	public static final String DIR_EXTRACT_OUTPUT_POST_DEV_FIRST		= "\\DEV" + DIR_EXTRACT_OUTPUT_POST_FIRST_ENVLESS;
	public static final String DIR_EXTRACT_OUTPUT_POST_DEV_LAST			= "\\DEV" + DIR_EXTRACT_OUTPUT_POST_LAST_ENVLESS;
	public static final String DIR_EXTRACT_OUTPUT_POST_TST_FIRST		= "\\TST" + DIR_EXTRACT_OUTPUT_POST_FIRST_ENVLESS;
	public static final String DIR_EXTRACT_OUTPUT_POST_TST_LAST			= "\\TST" + DIR_EXTRACT_OUTPUT_POST_LAST_ENVLESS;
	public static final String DIR_EXTRACT_OUTPUT_POST_PRD_FIRST		= "\\PRD" + DIR_EXTRACT_OUTPUT_POST_FIRST_ENVLESS;
	public static final String DIR_EXTRACT_OUTPUT_POST_PRD_LAST			= "\\PRD" + DIR_EXTRACT_OUTPUT_POST_LAST_ENVLESS;
	
	// Inject: mapping table
	public static final String DIR_INJECT							= FILE_BASE_LOCATION + "\\_Inject\\";
	
	// Various
	public static final String DIR_LOGS								= FILE_BASE_LOCATION + "\\Logs\\";
	public static final String DIR_REPORTS							= FILE_BASE_LOCATION + "\\Reports\\";
	public static final String DIR_CONFIG							= FILE_BASE_LOCATION + "\\Config\\";
	public static final String DIR_DEBUG							= FILE_BASE_LOCATION + "\\Debug\\";
	
	// Files
	public static final String FILE_CONFIG_SYSTEM_MAPPING			= DIR_CONFIG + "systemMapping.txt";
	public static final String FILE_CONFIG_COMPARE_EXEPTIONS		= DIR_CONFIG + "compareExceptions.xml";
	public static final String FILE_MSG_ID_MAPPING					= DIR_INJECT + Main.PARAM_VAL_SOURCE_ENV + "_to_" + Main.PARAM_VAL_TARGET_ENV + "_msgId_map.txt";
	
	static {
		final String SIGNATURE = "static";
		logger.writeDebug(LOCATION, SIGNATURE, "File containing Message ID Mapping initialized to: " + FILE_MSG_ID_MAPPING);
	}
	
	
	/**
	 * Start File Structure check.
	 */
	public static void startCheck() {
		String SIGNATURE = "startCheck()";
		logger.writeDebug(LOCATION, SIGNATURE, "Start file structure check");

		// Ensure project folder structure is present
		checkFolderStructure();
		
		// Ensure critical run files exists
		checkBaseFiles();
		
		// Clean-up old data from "Output"
		if (Main.PARAM_VAL_ALLOW_SAME_ENV) {
			logger.writeDebug(LOCATION, SIGNATURE, "Deletion of target data skipped (due to program parameter setting)");	
		} else {
			deleteOldRunData();
		}
		
		logger.writeDebug(LOCATION, SIGNATURE, "File structure check completed!");
	}


	/**
	 * Makes sure all old run data for target environment is deleted before a new run.
	 */
	private static void deleteOldRunData() {
		final String SIGNATURE = "deleteOldRunData()";
		try {       
			// Cleanup: delete all files contained in "Extract Output". Only done for sub-directories part of the specified target environment
			deletePayloadFiles(DIR_EXTRACT_OUTPUT_PRE, Main.PARAM_VAL_TARGET_ENV);
			logger.writeDebug(LOCATION, SIGNATURE, "Housekeeping: all old payload files deleted from root: " + DIR_EXTRACT_OUTPUT_PRE + " for environment: " + Main.PARAM_VAL_TARGET_ENV);
		} catch (Exception e) {
			String ex = "Housekeeping terminated with error! " + e;
			logger.writeError(LOCATION, SIGNATURE, ex);
			throw new RuntimeException(e);
		}            
	}


	/**
	 * Ensure project folder structure is healthy.
	 */
	private static void checkFolderStructure() {
		createDirIfNotExists(FILE_BASE_LOCATION);
		createDirIfNotExists(DIR_EXTRACT);
		createDirIfNotExists(DIR_EXTRACT_INPUT);
		createDirIfNotExists(DIR_EXTRACT_OUTPUT_PRE);
		createDirIfNotExists(DIR_INJECT);
		createDirIfNotExists(DIR_LOGS);
		createDirIfNotExists(DIR_REPORTS);
		createDirIfNotExists(DIR_CONFIG);
		createDirIfNotExists(DIR_DEBUG);
		
		// Generate dynamic output folders based on ICO request files
		List<Path> icoFiles = Util.generateListOfPaths(DIR_EXTRACT_INPUT, "FILE");
		for (Path path : icoFiles) {
			// Build path
			String icoDynamicPath = DIR_EXTRACT_OUTPUT_PRE + Util.getFileName(path.toAbsolutePath().toString(), false);
			
			// Create ICO directory
			createDirIfNotExists(icoDynamicPath);
			
			// Create output folders for DEV, TST, PROD
			createDirIfNotExists(icoDynamicPath + DIR_EXTRACT_OUTPUT_POST_DEV_FIRST);
			createDirIfNotExists(icoDynamicPath + DIR_EXTRACT_OUTPUT_POST_DEV_LAST);
			createDirIfNotExists(icoDynamicPath + DIR_EXTRACT_OUTPUT_POST_TST_FIRST);
			createDirIfNotExists(icoDynamicPath + DIR_EXTRACT_OUTPUT_POST_TST_LAST);
			createDirIfNotExists(icoDynamicPath + DIR_EXTRACT_OUTPUT_POST_PRD_FIRST);
			createDirIfNotExists(icoDynamicPath + DIR_EXTRACT_OUTPUT_POST_PRD_LAST);
		}
	}

	
	public static void checkBaseFiles() {
		String SIGNATURE = "checkBaseFiles()";
		
		File systemMappingFile = new File(FILE_CONFIG_SYSTEM_MAPPING);
		//File compareExceptionsFile = new File(FILE_CONFIG_COPMARE_EXEPTIONS);
		
		// Make sure system mapping file exists
		if (systemMappingFile.exists()) {
			logger.writeDebug(LOCATION, SIGNATURE, FILE_CONFIG_COMPARE_EXEPTIONS + " exists!");
		} else {
			logger.writeDebug(LOCATION, SIGNATURE, "System critical file: " + FILE_CONFIG_COMPARE_EXEPTIONS + " is missing and will be created!");
			Util.writeFileToFileSystem(FILE_CONFIG_SYSTEM_MAPPING, "".getBytes());
		}
		
		// Always create the ICO exception file with current ICO request files when a new run i started / overwrite if exists
		if (Main.PARAM_VAL_OPERATION.equals(Main.Operation.extract.toString())) {
			logger.writeDebug(LOCATION, SIGNATURE, Main.PARAM_VAL_OPERATION + " scenario found, create a new " + FILE_CONFIG_SYSTEM_MAPPING + " to make sure all ICO's are represented for later compare run");
			generateInitialIcoExeptionContent();
		}
	}

	
	private static void generateInitialIcoExeptionContent() {
		final String	XML_PREFIX = "inv";
		final String	XML_NS = "urn:invixo.com.consistency";
		List<Path> icoFiles = Util.generateListOfPaths(DIR_EXTRACT_INPUT, "FILE");

		try {
			XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
			XMLStreamWriter xmlWriter = xMLOutputFactory.createXMLStreamWriter(new FileOutputStream(FILE_CONFIG_COMPARE_EXEPTIONS), GlobalParameters.ENCODING);

			// Add xml version and encoding to output
			xmlWriter.writeStartDocument(GlobalParameters.ENCODING, "1.0");

			// Create element: Configuration
			xmlWriter.writeStartElement(XML_PREFIX, "Configuration", XML_NS);
			xmlWriter.writeNamespace(XML_PREFIX, XML_NS);
			
			// Loop ICO's found
			for (Path path : icoFiles) {
				// Get name of current ICO
				String icoName = Util.getFileName(path.toAbsolutePath().toString(), false);
				
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
	

	public static void deletePayloadFiles(String rootDirectory, String environment) {
		// Create pathMatcher which will match all files and directories (in the world of this tool, only files) that
		// are located in FIRST or LAST directories for the specified environment.
		String pattern = "^(?=.*\\\\" + environment + "\\\\.*\\\\.*\\\\.*\\\\).*$";
		PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("regex:" + pattern);
		
		// Find all matches to above regex starting from the specified DIR
		try (Stream<Path> paths = Files.find(Paths.get(rootDirectory), 100, (path, f)->pathMatcher.matches(path))) {
			// Delete all matches
			paths.forEach(path -> {
				try {
					Files.delete(path);
				} catch (IOException e) {
					throw new RuntimeException("*deletePayloadFiles* Error deleting file '" + path + "'\n" + e);
				}
			});
		} catch (IOException e) {
			throw new RuntimeException("*deletePayloadFiles* Error finding files." + e);
		}
	}
	
	
	/**
	 * Create directories part of a directory path, if they are missing
	 * @param directoryPath
	 */
	public static void createDirIfNotExists(String directorypath) {
		try {
			Files.createDirectories(Paths.get(directorypath));			
		} catch (IOException e) {
			throw new RuntimeException("*createDirIfNotExists* Error creating directories for path: " + directorypath);
		}
	}
}
