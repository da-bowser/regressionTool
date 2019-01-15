package com.invixo.injection;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamSource;

import com.invixo.common.util.Logger;
import com.invixo.common.util.PropertyAccessor;
import com.invixo.common.util.Util;
import com.invixo.common.util.XmlUtil;
import com.invixo.consistency.FileStructure;
import com.invixo.messageExtractor.blocks.BMultipartHandler;

public class RequestGenerator {
	private static Logger logger 						= Logger.getInstance();
	private static final String LOCATION 				= BMultipartHandler.class.getName();
	private static final String ENCODING 				= PropertyAccessor.getProperty("ENCODING");
	private static final String TARGET_DIR_INJECTION	= FileStructure.DIR_REGRESSION_INPUT_INJECTION;
	
	private static final String ELEMENT_ENDPOINT		= "{urn:com.invixo.regressionTool}endpoint";
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
	
	
	public static void main(String[] args) {
		String icoRequestFile = FileStructure.DIR_REGRESSION_INPUT_ICO + "NoScenario - Sys_QA3_011 oa_GoodsReceipt.xml";
		String payloadFile = FileStructure.FILE_BASE_LOCATION + "Test\\Extracts\\f7667537-157b-11e9-c0b1-000000554e16.xml";
		
		generateInjectionFile(icoRequestFile, payloadFile);
		System.out.println("Done");
	}
	
	
	/**
	 * 
	 * @param icoRequestfile
	 * @param payloadFile
	 */
	public static String generateInjectionFile(String icoRequestfile, String payloadFile) {
		String SIGNATURE = "generateSingleInjectionFile(String, String)";
		try {
			String newFile = null;
			
			// Create injection request object
			InjectionRequest ir = new InjectionRequest();
			
			// Extract relevant properties into POJO from request file
			extractInfoFromIcoRequest(ir, icoRequestfile);
			
			// Add payload to injection request. Payload is taken from an "instance" payload file (file extracted from the system)
			ir.setPayload(Util.readFile(payloadFile));
					
			// Generate injection xml
			newFile = generateInjectionXmlFile(ir);
			
			return newFile;	
		} catch (Exception e) {
			String msg = "Error generating injection file for SAP PO ICO request file " + icoRequestfile + " and payload file " + payloadFile + ".\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		}
	}

	
	/**
	 * Extract routing info from an Integrated Configuration request file used also when extracting data from SAP PO system.
	 * @param ir
	 * @param icoRequestfile
	 */
	private static void extractInfoFromIcoRequest(InjectionRequest ir, String icoRequestfile) {
		String SIGNATURE = "extractRoutingInfoFromIcoRequest(InjectionRequest, String)";
		try {
			// Read file
			byte[] fileContent = Util.readFile(icoRequestfile);
			
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
			    			ir.setSenderInterface(eventReader.peek().asCharacters().getData());	
			    		}
			    		
			    	// Sender interface namespace
			    	} else if (fetchData && ELEMENT_ITF_NS.equals(currentElementName)) {
			    		if (eventReader.peek().isCharacters()) {
			    			ir.setSenderNamespace(eventReader.peek().asCharacters().getData());	
			    		}
			    		
			    	// Sender party
			    	} else if (fetchData && ELEMENT_ITF_SPARTY.equals(currentElementName)) {
			    		if (eventReader.peek().isCharacters()) {
			    			ir.setSenderParty(eventReader.peek().asCharacters().getData());	
			    		}
			    		
			    	// Sender component
			    	} else if (fetchData && ELEMENT_ITF_SCOMPONENT.equals(currentElementName)) {
			    		if (eventReader.peek().isCharacters()) {
			    			ir.setSenderComponent(eventReader.peek().asCharacters().getData());	
			    		}
			    	
			    	// Receiver party
			    	} else if (fetchData && ELEMENT_ITF_RPARTY.equals(currentElementName)) {
			    		if (eventReader.peek().isCharacters()) {
			    			ir.setReceiverParty(eventReader.peek().asCharacters().getData());	
			    		}
			    		
			    	// Receiver component
			    	} else if (fetchData && ELEMENT_ITF_RCOMPONENT.equals(currentElementName)) {
			    		if (eventReader.peek().isCharacters()) {
			    			ir.setReceiverComponent(eventReader.peek().asCharacters().getData());	
			    		}
			    				    	
			    	// Quality of Service
			    	} else if (ELEMENT_QOS.equals(currentElementName)) {
			    		if (eventReader.peek().isCharacters()) {
			    			ir.setQualityOfService(eventReader.peek().asCharacters().getData());	
			    		}
			    	
			    	// Endpoint
			    	} else if (ELEMENT_ENDPOINT.equals(currentElementName)) {
			    		if (eventReader.peek().isCharacters()) {
			    			ir.setEndpoint(eventReader.peek().asCharacters().getData());
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
		} catch (Exception e) {
			String msg = "Error extracting routing info from ICO request file: " + icoRequestfile + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		} 
	}
	
	
	private static String generateInjectionXmlFile(InjectionRequest ir) {
		String SIGNATURE = "generateInjectionXmlFile(InjectionRequest, String)";
		
		try {
			XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
			XMLEventWriter xmlEventWriter = xMLOutputFactory.createXMLEventWriter(new FileOutputStream(TARGET_DIR_INJECTION + "injectionfile.xml"), ENCODING);
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
	        startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS_PREFIX, "Main");
	        xmlEventWriter.add(startElement);
	        attr = xmlEventFactory.createAttribute("versionMajor", "3");
	        xmlEventWriter.add(attr);
	        attr = xmlEventFactory.createAttribute("versionMinor", "1");
	        xmlEventWriter.add(attr);
	        attr = xmlEventFactory.createAttribute(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_NS, "mustUnderstand", "1");
	        xmlEventWriter.add(attr);
	        
			// Create element: Envelope | Header | Main | MessageClass
			startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS_PREFIX, "MessageClass");
			xmlEventWriter.add(startElement);
			value = xmlEventFactory.createCharacters("ApplicationMessage");
			xmlEventWriter.add(value);
			xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS_PREFIX, "MessageClass"));
			
			// Create element: Envelope | Header | Main | ProcessingMode
			startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS_PREFIX, "ProcessingMode");
			xmlEventWriter.add(startElement);
			value = xmlEventFactory.createCharacters("EO".equals(ir.getQualityOfService())?"asynchronous":"synchronous");
			xmlEventWriter.add(value);
			xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS_PREFIX, "ProcessingMode"));
	        
			// Create element: Envelope | Header | Main | MessageId
			startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS_PREFIX, "MessageId");
			xmlEventWriter.add(startElement);
			value = xmlEventFactory.createCharacters(UUID.randomUUID().toString());
			xmlEventWriter.add(value);
			xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS_PREFIX, "MessageId"));
			
			// Create element: Envelope | Header | Main | TimeSent
			startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS_PREFIX, "TimeSent");
			xmlEventWriter.add(startElement);
			value = xmlEventFactory.createCharacters(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date()));
			xmlEventWriter.add(value);
			xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS_PREFIX, "TimeSent"));
			
			// Create element: Envelope | Header | Main | Sender
			startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS_PREFIX, "Sender");
			xmlEventWriter.add(startElement);

			// Create element: Envelope | Header | Main | Sender | Party
			startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS_PREFIX, "Party");
			xmlEventWriter.add(startElement);
	        attr = xmlEventFactory.createAttribute("agency", "http://sap.com/xi/XI");
	        xmlEventWriter.add(attr);
	        attr = xmlEventFactory.createAttribute("scheme", "XIParty");
	        xmlEventWriter.add(attr);
			value = xmlEventFactory.createCharacters(ir.getSenderParty());
			xmlEventWriter.add(value);
			xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS_PREFIX, "Party"));
			
			// Create element: Envelope | Header | Main | Sender | Service
			startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS_PREFIX, "Service");
			xmlEventWriter.add(startElement);
			value = xmlEventFactory.createCharacters(ir.getSenderComponent());
			xmlEventWriter.add(value);
			xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS_PREFIX, "Service"));
			
			// Close tag: Envelope | Header | Main | Sender
			xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS_PREFIX, "Sender"));
			
			// Create element: Envelope | Header | Main | Interface
			startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS_PREFIX, "Interface");
			xmlEventWriter.add(startElement);
	        attr = xmlEventFactory.createAttribute("namespace", ir.getSenderNamespace());
	        xmlEventWriter.add(attr);
			value = xmlEventFactory.createCharacters(ir.getSenderInterface());
			xmlEventWriter.add(value);
			xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS_PREFIX, "Interface"));

	        // Close tag: Envelope | Header | Main
	        xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS_PREFIX, "Main"));
	        
			// Create element: Envelope | Header | Main | ReliableMessaging
			startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS_PREFIX, "ReliableMessaging");
			xmlEventWriter.add(startElement);
			attr = xmlEventFactory.createAttribute(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_NS, "mustUnderstand", "1");
	        xmlEventWriter.add(attr);
			
			// Create element: Envelope | Header | Main | ReliableMessaging | QualityOfService
			startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS_PREFIX, "QualityOfService");
			xmlEventWriter.add(startElement);
			value = xmlEventFactory.createCharacters("EO".equals(ir.getQualityOfService())?"ExactlyOnce":"BestEffort");		// EOIO emitted for now. Not sure of impact
			xmlEventWriter.add(value);
			xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS_PREFIX, "QualityOfService"));
			xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS_PREFIX, "ReliableMessaging"));

			// Close tag: Envelope | Header
	        xmlEventWriter.add(xmlEventFactory.createEndElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_NS, XmlUtil.SOAP_ENV_HEAD));
			
			// Create element: Envelope | Body
	        startElement = xmlEventFactory.createStartElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_NS, XmlUtil.SOAP_ENV_BODY);
	        xmlEventWriter.add(startElement);
	        
	        // Add payload to body
	        XMLInputFactory factory = XMLInputFactory.newInstance();
			StreamSource ss = new StreamSource(new ByteArrayInputStream(ir.getPayload()));
			XMLEventReader eventReader = factory.createXMLEventReader(ss);
			while (eventReader.hasNext()) {
			    XMLEvent event = eventReader.nextEvent();
			    if (event.getEventType()!= XMLEvent.START_DOCUMENT && event.getEventType() != XMLEvent.END_DOCUMENT) {
			    	xmlEventWriter.add(event);
			    }
			    eventReader.close();
			}

			// Close tags
	        xmlEventWriter.add(xmlEventFactory.createEndElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_NS, XmlUtil.SOAP_ENV_BODY));
	        xmlEventWriter.add(xmlEventFactory.createEndElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_NS, XmlUtil.SOAP_ENV_ROOT));

			// Finalize writing
	        xmlEventWriter.flush();
	        xmlEventWriter.close();
			
			logger.writeDebug(LOCATION, SIGNATURE, "Injection request created: " + TARGET_DIR_INJECTION + "injectionfile.xml");
			return TARGET_DIR_INJECTION + "injectionfile.xml";
		} catch (Exception e) {
			String msg = "Error creating injection file: " + TARGET_DIR_INJECTION + "injectionfile.xml";
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		}
	}
	
}
