package com.invixo.injection;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamSource;

import com.invixo.common.util.InjectionException;
import com.invixo.common.util.InjectionPayloadException;
import com.invixo.common.util.Logger;
import com.invixo.common.util.PropertyAccessor;
import com.invixo.common.util.Util;
import com.invixo.common.util.XmlUtil;
import com.invixo.consistency.FileStructure;

public class RequestGeneratorUtil {
	private static Logger logger 						= Logger.getInstance();
	private static final String LOCATION 				= RequestGeneratorUtil.class.getName();
	private static final String ELEMENT_INTERFACE		= "{urn:com.sap.aii.mdt.server.adapterframework.ws}senderInterface";
	private static final String ELEMENT_QOS				= "{urn:com.sap.aii.mdt.server.adapterframework.ws}qualityOfService";
	private static final String ELEMENT_ITF_NAME		= "{urn:com.sap.aii.mdt.api.data}name";
	private static final String ELEMENT_ITF_NS			= "{urn:com.sap.aii.mdt.api.data}namespace";
	private static final String ELEMENT_ITF_SPARTY		= "{urn:com.sap.aii.mdt.api.data}senderParty";
	private static final String ELEMENT_ITF_SCOMPONENT	= "{urn:com.sap.aii.mdt.api.data}senderComponent";
	private static final String ELEMENT_ITF_RPARTY		= "{urn:com.sap.aii.mdt.api.data}receiverParty";
	private static final String ELEMENT_ITF_RCOMPONENT	= "{urn:com.sap.aii.mdt.api.data}receiverComponent";
	private static final String TARGET_SAP_NS			= "http://sap.com/xi/XI/Message/30";
	private static final String TARGET_SAP_NS_PREFIX	= "sap";
	
	private static final String MAP_FILE				= FileStructure.FILE_BASE_LOCATION + "\\systemMapping.txt";
	private static final String SOURCE_ENV 				= PropertyAccessor.getProperty("SOURCE_ENVIRONMENT");
	private static final String TARGET_ENV 				= PropertyAccessor.getProperty("TARGET_ENVIRONMENT");
	private static HashMap<String, String> SYSTEM_MAP	= initializeSystemMap();
	
		
	/**
	 * Extract routing info from an Integrated Configuration request file used also when extracting data from SAP PO system.
	 * @param ico
	 */
	static void extractInfoFromIcoRequest(IntegratedConfiguration ico) throws InjectionException {
		final String SIGNATURE = "extractInfoFromIcoRequest(IntegratedConfiguration)";
		try {
			// Read file
			byte[] fileContent = Util.readFile(ico.getFileName());
			
			// Read XML file and extract data
			XMLInputFactory factory = XMLInputFactory.newInstance();
			StreamSource ss = new StreamSource(new ByteArrayInputStream(fileContent));
			XMLEventReader eventReader = factory.createXMLEventReader(ss);
			
			boolean fetchData = false;
			while (eventReader.hasNext()) {
			    XMLEvent event = eventReader.nextEvent();
			    
			    switch(event.getEventType()) {
			    case XMLStreamConstants.START_ELEMENT:
			    	String currentElementName = event.asStartElement().getName().toString();

			    	// interface start
			    	if (ELEMENT_INTERFACE.equals(currentElementName)) {
			    		fetchData = true;
			    		
			    	// Sender interface name
			    	} else if (fetchData && ELEMENT_ITF_NAME.equals(currentElementName)) {
			    		if (eventReader.peek().isCharacters()) {
			    			ico.setSenderInterface(eventReader.peek().asCharacters().getData());	
			    		}
			    		
			    	// Sender interface namespace
			    	} else if (fetchData && ELEMENT_ITF_NS.equals(currentElementName)) {
			    		if (eventReader.peek().isCharacters()) {
			    			ico.setSenderNamespace(eventReader.peek().asCharacters().getData());	
			    		}
			    		
			    	// Sender party
			    	} else if (fetchData && ELEMENT_ITF_SPARTY.equals(currentElementName)) {
			    		if (eventReader.peek().isCharacters()) {
			    			ico.setSenderParty(eventReader.peek().asCharacters().getData());	
			    		}
			    		
			    	// Sender component
			    	} else if (fetchData && ELEMENT_ITF_SCOMPONENT.equals(currentElementName)) {
			    		if (eventReader.peek().isCharacters()) {
			    			String sender = eventReader.peek().asCharacters().getData();
			    			ico.setSenderComponent(SYSTEM_MAP.get(sender));
			    			
			    			// Check
			    			if (ico.getSenderComponent() == null) {
			    				String ex = "System Mapping: missing entry for source system " + sender;
			    				logger.writeError(LOCATION, SIGNATURE, ex);
			    				throw new InjectionException(ex);
			    			}
			    		}
			    	
			    	// Receiver party
			    	} else if (fetchData && ELEMENT_ITF_RPARTY.equals(currentElementName)) {
			    		if (eventReader.peek().isCharacters()) {
			    			ico.setReceiverParty(eventReader.peek().asCharacters().getData());	
			    		}
			    		
			    	// Receiver component
			    	} else if (fetchData && ELEMENT_ITF_RCOMPONENT.equals(currentElementName)) {
			    		if (eventReader.peek().isCharacters()) {
			    			ico.setReceiverComponent(eventReader.peek().asCharacters().getData());	
			    		}
			    				    	
			    	// Quality of Service
			    	} else if (ELEMENT_QOS.equals(currentElementName)) {
			    		if (eventReader.peek().isCharacters()) {
			    			ico.setQualityOfService(eventReader.peek().asCharacters().getData());	
			    		}
			    	}
			    	break;
			    case XMLStreamConstants.END_ELEMENT:
			    	if (ELEMENT_INTERFACE.equals(event.asEndElement().getName().toString())) {
			    		fetchData = false;
			    	}
			    	break;
			    }
			}
		} catch (XMLStreamException e) {
			String msg = "Error extracting routing info from ICO request file: " + ico.getFileName() + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new InjectionException(msg);
		} 
	}
	
	
	static String generateSoapXiHeaderPart(IntegratedConfiguration ico, InjectionRequest ir) throws InjectionPayloadException {
		final String SIGNATURE = "generateSoapXiHeaderPart(IntegratedConfiguration, InjectionRequest)";
		try {
			StringWriter stringWriter = new StringWriter();
			XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
			XMLEventWriter xmlEventWriter = xMLOutputFactory.createXMLEventWriter(stringWriter);
			XMLEventFactory xmlEventFactory = XMLEventFactory.newFactory();
			
			// Common
			Attribute attr;
			Characters value;
			Namespace ns;
			StartElement startElement;
			
			// Add xml version and encoding to output
			xmlEventWriter.add(xmlEventFactory.createStartDocument());
			
			// Create element: Envelope
			startElement = xmlEventFactory.createStartElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_NS, XmlUtil.SOAP_ENV_ROOT);
	        xmlEventWriter.add(startElement);
	        ns = xmlEventFactory.createNamespace(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_NS);
	        xmlEventWriter.add(ns);
	        ns = xmlEventFactory.createNamespace(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS);
	        xmlEventWriter.add(ns);

			// Create element: Envelope | Header
	        startElement = xmlEventFactory.createStartElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_NS, XmlUtil.SOAP_ENV_HEAD);
	        xmlEventWriter.add(startElement);

	        // Create element: Envelope | Header | Main
	        startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "Main");
	        xmlEventWriter.add(startElement);
	        attr = xmlEventFactory.createAttribute("versionMajor", "3");
	        xmlEventWriter.add(attr);
	        attr = xmlEventFactory.createAttribute("versionMinor", "1");
	        xmlEventWriter.add(attr);
	        attr = xmlEventFactory.createAttribute(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_NS, "mustUnderstand", "1");
	        xmlEventWriter.add(attr);
	        
			// Create element: Envelope | Header | Main | MessageClass
			startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "MessageClass");
			xmlEventWriter.add(startElement);
			value = xmlEventFactory.createCharacters("ApplicationMessage");
			xmlEventWriter.add(value);
			xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "MessageClass"));
			
			// Create element: Envelope | Header | Main | ProcessingMode
			startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "ProcessingMode");
			xmlEventWriter.add(startElement);
			value = xmlEventFactory.createCharacters("EO".equals(ico.getQualityOfService())?"asynchronous":"synchronous");
			xmlEventWriter.add(value);
			xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "ProcessingMode"));
	        
			// Create element: Envelope | Header | Main | MessageId
			startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "MessageId");
			xmlEventWriter.add(startElement);
			value = xmlEventFactory.createCharacters(ir.getMessageId());
			xmlEventWriter.add(value);
			xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "MessageId"));
			
			// Create element: Envelope | Header | Main | TimeSent
			startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "TimeSent");
			xmlEventWriter.add(startElement);
			value = xmlEventFactory.createCharacters(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date()));
			xmlEventWriter.add(value);
			xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "TimeSent"));
			
			// Create element: Envelope | Header | Main | Sender
			startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "Sender");
			xmlEventWriter.add(startElement);

			// Create element: Envelope | Header | Main | Sender | Party
			startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "Party");
			xmlEventWriter.add(startElement);
	        attr = xmlEventFactory.createAttribute("agency", "http://sap.com/xi/XI");
	        xmlEventWriter.add(attr);
	        attr = xmlEventFactory.createAttribute("scheme", "XIParty");
	        xmlEventWriter.add(attr);
			value = xmlEventFactory.createCharacters(ico.getSenderParty());
			xmlEventWriter.add(value);
			xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "Party"));
			
			// Create element: Envelope | Header | Main | Sender | Service
			startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "Service");
			xmlEventWriter.add(startElement);
			value = xmlEventFactory.createCharacters(ico.getSenderComponent());
			xmlEventWriter.add(value);
			xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "Service"));
			
			// Close tag: Envelope | Header | Main | Sender
			xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "Sender"));
			
			// Create element: Envelope | Header | Main | Interface
			startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "Interface");
			xmlEventWriter.add(startElement);
	        attr = xmlEventFactory.createAttribute("namespace", ico.getSenderNamespace());
	        xmlEventWriter.add(attr);
			value = xmlEventFactory.createCharacters(ico.getSenderInterface());
			xmlEventWriter.add(value);
			xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "Interface"));

	        // Close tag: Envelope | Header | Main
	        xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "Main"));
	        
			// Create element: Envelope | Header | Main | ReliableMessaging
			startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "ReliableMessaging");
			xmlEventWriter.add(startElement);
			attr = xmlEventFactory.createAttribute(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_NS, "mustUnderstand", "1");
	        xmlEventWriter.add(attr);
			
			// Create element: Envelope | Header | Main | ReliableMessaging | QualityOfService
			startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "QualityOfService");
			xmlEventWriter.add(startElement);
			value = xmlEventFactory.createCharacters("EO".equals(ico.getQualityOfService())?"ExactlyOnce":"BestEffort");		// EOIO emitted for now. Not sure of impact
			xmlEventWriter.add(value);
			xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "QualityOfService"));
			xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "ReliableMessaging"));

			// Close tag: Envelope | Header
	        xmlEventWriter.add(xmlEventFactory.createEndElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_NS, XmlUtil.SOAP_ENV_HEAD));
			
			// Create element: Envelope | Body
	        startElement = xmlEventFactory.createStartElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_NS, XmlUtil.SOAP_ENV_BODY);
	        xmlEventWriter.add(startElement);
	        
	        // Create element: Envelope | Body | Manifest
	        startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "Manifest");
	        xmlEventWriter.add(startElement);
	        ns = xmlEventFactory.createNamespace("xlink", "http://www.w3.org/1999/xlink");
	        xmlEventWriter.add(ns);
	        ns = xmlEventFactory.createNamespace("sap", "http://sap.com/xi/XI/Message/30");
	        xmlEventWriter.add(ns);

	        // Create element: Envelope | Body | Manifest | Payload
	        startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "Payload");
	        xmlEventWriter.add(startElement);
	        attr = xmlEventFactory.createAttribute("xlink", "http://www.w3.org/1999/xlink", "type", "simple");
	        xmlEventWriter.add(attr);
	        attr = xmlEventFactory.createAttribute("xlink", "http://www.w3.org/1999/xlink", "href", "cid:INJECTION_PAYLOAD");
	        xmlEventWriter.add(attr);
	        
	        // Create element: Envelope | Body | Manifest | Name
	        startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "Name");
	        xmlEventWriter.add(startElement);
			value = xmlEventFactory.createCharacters("MainDocument");
			xmlEventWriter.add(value);
	        xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "Name"));
	        
	        // Create element: Envelope | Body | Manifest | Name
	        startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "Description");
	        xmlEventWriter.add(startElement);
			value = xmlEventFactory.createCharacters("Main XML document");
			xmlEventWriter.add(value);
	        xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "Description"));
	        
	        // Create element: Envelope | Body | Manifest | Type
	        startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "Type");
	        xmlEventWriter.add(startElement);
			value = xmlEventFactory.createCharacters("Application");
			xmlEventWriter.add(value);
	        xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "Type"));
	        
			// Close tags
	        xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "Manifest"));
	        xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "Payload"));
	        xmlEventWriter.add(xmlEventFactory.createEndElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_NS, XmlUtil.SOAP_ENV_BODY));
	        xmlEventWriter.add(xmlEventFactory.createEndElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_NS, XmlUtil.SOAP_ENV_ROOT));

			// Finalize writing
	        xmlEventWriter.flush();
	        xmlEventWriter.close();
			
			logger.writeDebug(LOCATION, SIGNATURE, "SOAP XI Header generated.");
			stringWriter.flush();
			return stringWriter.toString();
		} catch (XMLStreamException e) {
			String msg = "Error generating SOAP XI Header. " + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new InjectionPayloadException(msg);
		}
	}
		
	
	private static HashMap<String, String> initializeSystemMap() {
		final String SIGNATURE = "initializeSystemMap()";
		try {
			// Determine source index (how the request ICO's are created)
			int sourceIndex = -1;
			if ("DEV".equals(SOURCE_ENV)) {
				sourceIndex = 0;
			} else if ("TST".equals(SOURCE_ENV)) {
				sourceIndex = 1;
			} else {
				sourceIndex = 2;
			}
			
			// Determine target index (which target system to map to when injecting)
			int targetIndex = -1;
			if ("DEV".equals(TARGET_ENV)) {
				targetIndex = 0;
			} else if ("TST".equals(TARGET_ENV)) {
				targetIndex = 1;
			} else {
				targetIndex = 2;
			}
			
			// Populate map
	 		SYSTEM_MAP = new HashMap<String, String>();
	 		String line;
	 		FileReader fileReader = new FileReader(MAP_FILE);
	 		try (BufferedReader bufferedReader = new BufferedReader(fileReader)) {
	 			while((line = bufferedReader.readLine()) != null) {
	 				String[] str = line.split("\\|");
	 				SYSTEM_MAP.put(str[sourceIndex], str[targetIndex]);
	 			}			   
	 		}

		    // Return initialized map
		    logger.writeDebug(LOCATION, SIGNATURE, "System mapping initialized. Source ENV '" + SOURCE_ENV + "'. Target ENV '" + TARGET_ENV + "'. Number of entries: " + SYSTEM_MAP.size());
		    return SYSTEM_MAP;			
		} catch (IOException e) {
			String msg = "Error generating system mapping\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		}
	}
	
}
