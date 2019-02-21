package com.invixo.common;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.Attribute;
import javax.xml.transform.stream.StreamSource;

import com.invixo.common.util.Logger;


public class IcoOverviewDeserializer {
	private static Logger logger 			= Logger.getInstance();
	private static final String LOCATION 	= IcoOverviewDeserializer.class.getName();	
	
	
	/**
	 * Deserialize ICO overview XML file into Java object.
	 * Only active ICOs are collected.
	 * @param icoOverviewFile			ICO Overview inputstream
	 * @return
	 */
	public static ArrayList<IcoOverviewInstance> deserialize(InputStream icoOverviewFile) {
		final String SIGNATURE = "deserialize(InputStream)";
		try {
			// Prepare
			XMLInputFactory factory = XMLInputFactory.newInstance();
			StreamSource ss = new StreamSource(icoOverviewFile);
			XMLEventReader eventReader = factory.createXMLEventReader(ss);
			
			// Parse XML file and extract data
			boolean fetchSenderData = false;
			boolean fetchReceiverData = false;
			boolean isActive = false;
			ArrayList<IcoOverviewInstance> icoExtracts = new ArrayList<IcoOverviewInstance>();
		    IcoOverviewInstance currentExtract = null;;
		    
			while (eventReader.hasNext()) {
			    XMLEvent event = eventReader.nextEvent();
			    
			    switch(event.getEventType()) {
			    case XMLStreamConstants.START_ELEMENT:
			    	String currentStartElementName = event.asStartElement().getName().getLocalPart().toString();

					// IntegratedConfiguration (root)
					if ("IntegratedConfiguration".equals(currentStartElementName)) {
						currentExtract = new IcoOverviewInstance();

					// Name
			    	} else if ("Name".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
						currentExtract.setName(eventReader.peek().asCharacters().getData());	
						
					// Active
			    	} else if ("Active".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
			    		isActive = Boolean.parseBoolean(eventReader.peek().asCharacters().getData());
						currentExtract.setActive(isActive);	

					// Quality of Service
					} else if ("QualityOfService".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
						currentExtract.setQualityOfService(eventReader.peek().asCharacters().getData());	
						
					// Max message count
					} else if ("MaxMessages".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
						currentExtract.setMaxMessages(Integer.parseInt(eventReader.peek().asCharacters().getData()));	
			    	
			    	// Time, From
					} else if ("FromTime".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
						currentExtract.setFromTime(eventReader.peek().asCharacters().getData());	
			    		
			    	// Time, To
					} else if ("ToTime".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
						currentExtract.setToTime(eventReader.peek().asCharacters().getData());	
						
				    // Container for Sender info
				    } else if ("Sender".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
				    	fetchSenderData = true;	

				    // Sender Party
			    	} else if (fetchSenderData && "Party".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
			    		currentExtract.setSenderParty(eventReader.peek().asCharacters().getData());
				    	
				    // Sender Component
			    	} else if (fetchSenderData && "Component".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
			    		currentExtract.setSenderComponent(eventReader.peek().asCharacters().getData());
			    		
			    	// Sender Interface
			    	} else if (fetchSenderData && "Interface".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
			    		currentExtract.setSenderInterface(eventReader.peek().asCharacters().getData());
			    		
			    	// Sender Interface Namespace
			    	} else if (fetchSenderData && "Namespace".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
			    		currentExtract.setSenderNamespace(eventReader.peek().asCharacters().getData());	
			    	
			    	// Container for Receiver info
			    	} else if ("Receiver".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
					    fetchReceiverData = true;

					// Receiver Party
			    	} else if (fetchReceiverData && "Party".equals(currentStartElementName) && eventReader.peek().isCharacters()) {		    		
				    	currentExtract.setReceiverParty(eventReader.peek().asCharacters().getData());

			    	// Receiver Component
			    	} else if (fetchReceiverData && "Component".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
			    		currentExtract.setReceiverComponent(eventReader.peek().asCharacters().getData());

			    	// Receiver Interface
			    	} else if (fetchReceiverData && "Interface".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
			    		currentExtract.setReceiverInterface(eventReader.peek().asCharacters().getData());
			    		
			    		// Get attribute values
			    		Iterator<Attribute> iterator = event.asStartElement().getAttributes();
			            while (iterator.hasNext())
			            {
			                Attribute attribute = iterator.next();
			                String name = attribute.getName().toString();
			                if ("MultiMapping".equals(name)) {
			                	currentExtract.setUsingMultiMapping(Boolean.parseBoolean(attribute.getValue()));
			                	break;
			                }
			            }
			    		
			    	// Receiver Interface Namespace
			    	} else if (fetchReceiverData && "Namespace".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
			    		currentExtract.setReceiverNamespace(eventReader.peek().asCharacters().getData());	
			    	}
			    	break;
			    				    	
			    case XMLStreamConstants.END_ELEMENT:
			    	String currentEndElementName = event.asEndElement().getName().getLocalPart().toString();
			    	
			    	if ("Sender".equals(currentEndElementName)) {
			    		fetchSenderData = false;
			    		
			    	} else if ("Receiver".equals(currentEndElementName)) {
			    		fetchReceiverData = false;
			    	
			    	} else if ("IntegratedConfiguration".equals(currentEndElementName)) {
			    		if (isActive && (currentExtract.isUsingMultiMapping() && currentExtract.getQualityOfService().equals("EO"))) {
			    			logger.writeInfo(LOCATION, SIGNATURE, "Combination of multimapping and QoS EO is not supported!" + "\nICO skipped: " + currentExtract.getName());
						} else if (isActive) {
			    			// ICO is active/enabled for processing, so add it
				    		icoExtracts.add(currentExtract);
				    		isActive = false;
			    		}
			    	}
			    	break;
			    }
			}
			
			return icoExtracts;
		} catch (XMLStreamException e) {
			String msg = "Error deserializing ICO overview XML file into list of java objects\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		} 
	}
	
}
