package com.invixo.extraction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamSource;

import com.invixo.common.GeneralException;
import com.invixo.common.IntegratedConfigurationMain;
import com.invixo.common.util.Logger;
import com.invixo.common.util.PropertyAccessor;
import com.invixo.common.util.Util;
import com.invixo.extraction.webServices.WebServiceHandler;


public class IntegratedConfiguration extends IntegratedConfigurationMain {
	/*====================================================================================
	 *------------- Class variables
	 *====================================================================================*/
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = IntegratedConfiguration.class.getName();	

	// Indicators: should payloads (FIRST and/or LAST) be extracted or not
	public static final boolean EXTRACT_FIRST_PAYLOAD = Boolean.parseBoolean(PropertyAccessor.getProperty("EXTRACT_FIRST_PAYLOAD"));
	public static final boolean EXTRACT_LAST_PAYLOAD = Boolean.parseBoolean(PropertyAccessor.getProperty("EXTRACT_LAST_PAYLOAD"));
		
	
	
	/*====================================================================================
	 *------------- Instance variables
	 *====================================================================================*/
	private ArrayList<String> responseMessageKeys = new ArrayList<String>();	// MessageKey IDs returned by Web Service GetMessageList
	private ArrayList<MessageKey> messageKeys = new ArrayList<MessageKey>();	// List of MessageKeys created/processed

	
	
	/*====================================================================================
	 *------------- Constructors
	 *====================================================================================*/
	public IntegratedConfiguration(String icoFileName) throws GeneralException {
		super(icoFileName);
	}

	
	
	/*====================================================================================
	 *------------- Getters and Setters
	 *====================================================================================*/
	public ArrayList<MessageKey> getMessageKeys() {
		return this.messageKeys;
	}

		
	
	/*====================================================================================
	 *------------- Instance methods
	 *====================================================================================*/
	/**
	 * Process a single Integrated Configuration object.
	 * This also includes all MessageKeys related to this object.
	 * @param file
	 */
	public void processSingleIco(String file) {
		final String SIGNATURE = "processSingleIco(String)";
		try {
			logger.writeDebug(LOCATION, SIGNATURE, "*********** Start processing ICO request file: " + file);
			
			// Extract data from ICO request file
			extractInfoFromIcoRequest();
			
			// Check extracted info
			checkDataExtract();
			
			// Read ICO file request
			byte[] requestBytes = Util.readFile(file);
			logger.writeDebug(LOCATION, SIGNATURE, "ICO request file read: " + file);
			
			// Modify ICO request
			byte[] modifiedRequestBytes = modifyIcoRequestFile(requestBytes, this);
			logger.writeDebug(LOCATION, SIGNATURE, "ICO request modified. ");
			
			// Call web service (GetMessageList)
			InputStream responseBytes = WebServiceHandler.callWebService(modifiedRequestBytes);
			logger.writeDebug(LOCATION, SIGNATURE, "Web Service (GetMessageList) called");
			
			// Extract MessageKeys from web Service response
			this.responseMessageKeys = extractMessageKeysFromSingleResponseFile(responseBytes);
			logger.writeDebug(LOCATION, SIGNATURE, "Number of MessageKeys contained in Web Service response: " + this.responseMessageKeys.size());
			
			// For each MessageKey fetch payloads (first and last)
			int counter = 1;
			for (String key : this.responseMessageKeys) {
				// Process a single Message Key
				logger.writeDebug(LOCATION, SIGNATURE, "-----> (" + counter + ") MessageKey processing started for key: " + key);
				this.processSingleMessageKey(key);
				logger.writeDebug(LOCATION, SIGNATURE, "-----> (" + counter + ") MessageKey processing finished");
				counter++;
			}	
		} catch (GeneralException|ExtractorException e) {
			this.ex = e;
		} finally {
			logger.writeDebug(LOCATION, SIGNATURE, "*********** Finished processing ICO request file");
		}
	}
	
	
	/**
	 * Processes a single MessageKey returned in Web Service response for service GetMessageList.
	 * This involves calling service GetMessageBytesJavaLangStringIntBoolean to fetch actual payload and storing 
	 * this on file system.
	 * This method can/will generate both FIRST and LAST payload if requested.
	 * @param key
	 * @throws ExtractorException
	 */
	private void processSingleMessageKey(String key) throws ExtractorException {
		final String SIGNATURE = "processSingleMessageKey(String)";
		MessageKey msgKey = null;
		try {
			// Create a new MessageKey object
			msgKey = new MessageKey(this, key);
			
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
		} catch (ExtractorException e) {
			if (msgKey != null) {
				msgKey.setEx(e);
			}
		}
	}

	
	
	/*====================================================================================
	 *------------- Class methods
	 *====================================================================================*/
	/**
	 * Get list of 'messageKey' contained in a single response file.
	 * @param file
	 * @return
	 * @throws ExtractorException
	 */
	private static ArrayList<String> extractMessageKeysFromSingleResponseFile(InputStream responseBytes) throws ExtractorException {
		final String SIGNATURE = "extractMessageKeysFromSingleResponseFile(InputStream)";
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
		} catch (XMLStreamException e) {
			String msg = "Error extracting MessageKeys.\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);
		} 
	}
	
	
	/**
	 * Parse ICO request xml and create a modified copy of it with updated properties.
	 * @param requestBytes
	 * @param ico
	 * @throws ExtractorException
	 */
	private static byte[] modifyIcoRequestFile(byte[] requestBytes, IntegratedConfiguration ico) throws ExtractorException {
		final String SIGNATURE = "modifyIcoRequestFile(byte[], IntegratedConfiguration)";
		try {
			String modificationEnabledForElement = null;
			
			// Create XML Writer (creating a modified copy of the request ICO file)
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
			XMLEventWriter xmlEventWriter = xMLOutputFactory.createXMLEventWriter(baos, "UTF-8");
			XMLEventFactory xmlEventFactory = XMLEventFactory.newFactory();
			
			// Create XML Reader (reading request ICO file)
	        XMLInputFactory factory = XMLInputFactory.newInstance();
			StreamSource ss = new StreamSource(new ByteArrayInputStream(requestBytes));
			XMLEventReader eventReader = factory.createXMLEventReader(ss);
			
			// Read and modify ICO request
			while (eventReader.hasNext()) {
			    XMLEvent event = eventReader.nextEvent();
			    
			    if (event.getEventType() == XMLEvent.START_ELEMENT) {
			    	String currentName = event.asStartElement().getName().toString();
			    	System.out.println(currentName);
			    	
			    	
			    	// Enable modification for certain elements
			    	if (		"{urn:com.sap.aii.mdt.api.data}senderComponent".equals(currentName) 
			    			|| 	"{urn:AdapterMessageMonitoringVi}maxMessages".equals(currentName)
			    			|| 	"{urn:com.sap.aii.mdt.server.adapterframework.ws}receiverName".equals(currentName)
			    			|| 	"{urn:com.sap.aii.mdt.server.adapterframework.ws}senderName".equals(currentName)) {
			    		modificationEnabledForElement = currentName;
			    	}
			    	
			    	// Always add event
			    	xmlEventWriter.add(event);
			    	
			    	// Modify value for certain elements
			    }  else if ((modificationEnabledForElement != null) && (event.getEventType() == XMLEvent.CHARACTERS)) {
			    	switch (modificationEnabledForElement) {
			    	case "{urn:com.sap.aii.mdt.api.data}senderComponent" : 
			    		xmlEventWriter.add(xmlEventFactory.createCharacters(ico.getSenderComponent()));
			    		modificationEnabledForElement = null;
			    		break;
			    	case "{urn:com.sap.aii.mdt.server.adapterframework.ws}receiverName" : 
			    		xmlEventWriter.add(xmlEventFactory.createCharacters(ico.getReceiverComponent()));
			    		modificationEnabledForElement = null;
			    		break;
			    	case "{urn:com.sap.aii.mdt.server.adapterframework.ws}senderName" : 
			    		xmlEventWriter.add(xmlEventFactory.createCharacters(ico.getSenderComponent()));
			    		modificationEnabledForElement = null;
			    		break;
			    	case "{urn:AdapterMessageMonitoringVi}maxMessages" : 
			    		xmlEventWriter.add(xmlEventFactory.createCharacters("" + ico.getMaxMessages()));
			    		modificationEnabledForElement = null;
			    		break;
			    	}
		    	} else {
		    		xmlEventWriter.add(event);
		    	}
			}
			
			baos.flush();
			xmlEventWriter.flush();
			xmlEventWriter.close();
			
			return baos.toByteArray();	
		} catch (IOException|XMLStreamException e) {
			String msg = "Error modifying ICO request.\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);
		}
	}

}
