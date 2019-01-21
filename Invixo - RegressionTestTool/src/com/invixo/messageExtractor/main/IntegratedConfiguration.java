package com.invixo.messageExtractor.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.XMLEvent;

import com.invixo.common.util.ExtractorException;
import com.invixo.common.util.Logger;
import com.invixo.common.util.PropertyAccessor;
import com.invixo.common.util.Util;
import com.invixo.consistency.FileStructure;
import com.invixo.main.GlobalParameters;
import com.invixo.messageExtractor.webServices.WebServiceHandler;


@SuppressWarnings("unused")
public class IntegratedConfiguration {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = IntegratedConfiguration.class.getName();	

	// Indicators: should payloads (FIRST and/or LAST) be extracted or not
	public static final boolean EXTRACT_FIRST_PAYLOAD = Boolean.parseBoolean(PropertyAccessor.getProperty("EXTRACT_FIRST_PAYLOAD"));
	public static final boolean EXTRACT_LAST_PAYLOAD = Boolean.parseBoolean(PropertyAccessor.getProperty("EXTRACT_LAST_PAYLOAD"));
		
	private String name = null;				// Name of ICO
	private String fileName = null;			// Complete path to ICO request file
	private int maxMessagesToFetch = 0;		// Maximum messages to fetch via service GetMessageList
	private String qualityOfService = null;	// QoS
	
	private ArrayList<String> responseMessageKeys = new ArrayList<String>();	// MessageKey IDs returned by Web Service GetMessageList
	private ArrayList<MessageKey> messageKeys = new ArrayList<MessageKey>();	// List of MessageKeys created/processed
	
	private Exception ex = null;					// Error details

	
	// Overloaded constructor
	public IntegratedConfiguration(String icoFileName) throws ExtractorException {
		this.fileName = icoFileName;
		this.name = Util.getFileName(icoFileName, false);
		
		extractAdditionalInfoFromIco();
	}

	
	public String getName() {
		return this.name;
	}
	
	public String getFileName() {
		return this.fileName;
	}
	
	public int getMaxMessagesToFetch() {
		return this.maxMessagesToFetch;
	}
	
	public String getQualityOfService() {
		return this.qualityOfService;
	}

	public ArrayList<MessageKey> getMessageKeys() {
		return this.messageKeys;
	}
	
	public Exception getEx() {
		return this.ex;
	}
		
		
	private void extractAdditionalInfoFromIco() throws ExtractorException {
		String SIGNATURE = "extractAdditionalInfoFromIco()";
		try {
			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLEventReader eventReader = factory.createXMLEventReader(new FileInputStream(this.fileName), GlobalParameters.ENCODING);
			
			while (eventReader.hasNext()) {
			    XMLEvent event = eventReader.nextEvent();
			    
			    switch(event.getEventType()) {
			    case XMLStreamConstants.START_ELEMENT:
			    	String currentElementName = event.asStartElement().getName().getLocalPart();
			    	
			    	if ("qualityOfService".equals(currentElementName)) {
			    		this.qualityOfService = eventReader.peek().asCharacters().getData();
			    		
			    	} else if ("maxMessages".equals(currentElementName)) {
			    		this.maxMessagesToFetch = Integer.parseInt(eventReader.peek().asCharacters().getData());
			    	}
			    }
			}
		} catch (Exception e) {
			String msg = "Error extracting basic info from ICO request file: " + this.fileName + "\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			ExtractorException ex = new ExtractorException(msg);
			this.ex = ex;
			throw ex;
		} 
	}

	
	/**
	 * Process a single Integrated Configuration object.
	 * This also includes all MessageKeys related to this object.
	 * @param file
	 * @throws ExtractorException
	 */
	public void processSingleIco(String file) throws ExtractorException {
		String SIGNATURE = "processSingleIco(String)";

		// Read ICO file request
		byte[] requestBytes = Util.readFile(file);
		logger.writeDebug(LOCATION, SIGNATURE, "ICO request file read: " + file);
		
		// Call web service (GetMessageList)
		InputStream responseBytes = WebServiceHandler.callWebService(requestBytes);
		logger.writeDebug(LOCATION, SIGNATURE, "Web Service (GetMessageList) called");
		
		// Extract MessageKeys from web Service response
		this.responseMessageKeys = extractMessageKeysFromSingleResponseFile(responseBytes);
		logger.writeDebug(LOCATION, SIGNATURE, "Number of MessageKeys contained in Web Service response: " + messageKeys.size());
		
		// For each MessageKey fetch payloads (first and last)
		for (String key : this.responseMessageKeys) {
			// Process a single Message Key
			logger.writeDebug(LOCATION, SIGNATURE, "-----> MessageKey processing started for key: " + key);
			this.processSingleMessageKey(key);
			logger.writeDebug(LOCATION, SIGNATURE, "-----> MessageKey processing finished");
		}	
	}
	
	
	/**
	 * Processes a single MessageKey returned in Web Service response for service GetMessageList.
	 * This involves calling service GetMessageBytesJavaLangStringIntBoolean to fetch actual payload and storing 
	 * this on file system.
	 * This method can/will generate both FIRST and LAST payload if requested.
	 * @param key
	 * @throws Exception
	 */
	private void processSingleMessageKey(String key) throws ExtractorException {
		String SIGNATURE = "processSingleMessageKey(String, String)";
		
		// Create a new MessageKey object
		MessageKey msgKey = new MessageKey(this, key);
		
		// Attach a reference to newly created MessageKey object to this ICO
		this.messageKeys.add(msgKey);
		
		// Fetch payload: FIRST
		if (EXTRACT_FIRST_PAYLOAD) {
			msgKey.processMessageKey(key, true);
			logger.writeDebug(LOCATION, SIGNATURE, "MessageKey processing finished for FIRST payload");
		}
		
		// Fetch payload: LAST
		if (EXTRACT_LAST_PAYLOAD) {
			msgKey.processMessageKey(key, false);
			logger.writeDebug(LOCATION, SIGNATURE, "MessageKey processing finished for LAST payload");
		}
	}

	
	/**
	 * Get list of 'messageKey' contained in a single response file.
	 * @param file
	 * @return
	 */
	private static ArrayList<String> extractMessageKeysFromSingleResponseFile(InputStream responseBytes) {
		String SIGNATURE = "extractMessageKeysFromSingleResponseFile(InputStream)";
		ArrayList<String> objectKeys = new ArrayList<String>();
		
		try {
			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLEventReader eventReader = factory.createXMLEventReader(responseBytes);
			
			while (eventReader.hasNext()) {
			    XMLEvent event = eventReader.nextEvent();
			    
			    switch(event.getEventType()) {
			    case XMLStreamConstants.START_ELEMENT:
			    	String currentElementName = event.asStartElement().getName().getLocalPart();
			    	if ("messageKey".equals(currentElementName)) {
			    		objectKeys.add(eventReader.peek().asCharacters().getData());
			    	}
			    }
			}
			
			return objectKeys;
		} catch (Exception e) {
			String msg = "Error extracting MessageKeys.\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		} 
	}
	
}
