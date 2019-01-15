package com.invixo.messageExtractor.blocks;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.XMLEvent;

import com.invixo.messageExtractor.httpHandlers.HGetMessageList;
import com.invixo.messageExtractor.util.Logger;


public class BGetMessageList {
	private static final String LOCATION 		= BGetMessageList.class.getName();
	private static Logger logger 				= Logger.getInstance();

	
	public static void main(String[] args) {
		try {
			// Test extraction of MessageKeys from Web Service resonse
			InputStream responseBytes = HGetMessageList.invoke("GetMessageListRequest.xml");
			
			ArrayList<String> messageKeys = extractMessageKeysFromSingleResponseFile(responseBytes);
			for (String key : messageKeys) {
				System.out.println("Message Key: " + key);
			}			
		} catch (Exception e) {
			System.err.println("Test failed!\n" + e);
		}
	}
	
	
	/**
	 * Process all requests files.
	 * This means making the HTTP call and storing the response on file system.
	 */
	public static void processRequestFiles() throws Exception {
		String SIGNATURE = "processRequestFiles()";
		
		// Get list of all request files to be processed
		File folder = new File(HGetMessageList.DIR_REQUEST);
		File[] files = folder.listFiles();
		logger.writeDebug(LOCATION, SIGNATURE, "GetMessageList: number of request files: " + files.length);
		
		// Process each request file
		for (File file : files) {
			// Call web service and store result
			HGetMessageList.invoke(file.getName());
		}
	}
	
	
	/**
	 * Get list of 'messageKey' contained in a single response file.
	 * @param file
	 * @return
	 */
	public static ArrayList<String> extractMessageKeysFromSingleResponseFile(InputStream responseBytes) {
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
