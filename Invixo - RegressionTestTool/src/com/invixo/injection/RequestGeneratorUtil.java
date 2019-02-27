package com.invixo.injection;

import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;

import com.invixo.common.util.Logger;
import com.invixo.common.util.XiHeader;
import com.invixo.common.util.XmlUtil;

public class RequestGeneratorUtil {
	private static Logger logger 						= Logger.getInstance();
	private static final String LOCATION 				= RequestGeneratorUtil.class.getName();
	private static final String TARGET_SAP_NS			= "http://sap.com/xi/XI/Message/30";
	private static final String TARGET_SAP_NS_PREFIX	= "sap";
	
	
	public static String generateSoapXiHeaderPart(IntegratedConfiguration ico, String messageId, XiHeader firstXiHeader) throws InjectionPayloadException {
		final String SIGNATURE = "generateSoapXiHeaderPart(IntegratedConfiguration, String)";
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
			value = xmlEventFactory.createCharacters("BestEffort".equals(firstXiHeader.getQualityOfService())?"synchronous":"asynchronous");
			xmlEventWriter.add(value);
			xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "ProcessingMode"));
	        
			// Create element: Envelope | Header | Main | MessageId
			startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "MessageId");
			xmlEventWriter.add(startElement);
			value = xmlEventFactory.createCharacters(messageId);
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
			
			if ("ExactlyOnce".equals(firstXiHeader.getQualityOfService())) {
				value = xmlEventFactory.createCharacters("ExactlyOnce");
			} else if ("BestEffort".equals(firstXiHeader.getQualityOfService())) {
				value = xmlEventFactory.createCharacters("BestEffort");
			} else {
				value = xmlEventFactory.createCharacters("ExactlyOnceInOrder");
			}
			xmlEventWriter.add(value);
			xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "QualityOfService"));

			// Create element: Envelope | Header | Main | ReliableMessaging | QueueId
	        if ("ExactlyOnceInOrder".equals(firstXiHeader.getQualityOfService())) {
				startElement = xmlEventFactory.createStartElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "QueueId");
				xmlEventWriter.add(startElement);
				value = xmlEventFactory.createCharacters(ico.getQueueId());
				xmlEventWriter.add(value);
				xmlEventWriter.add(xmlEventFactory.createEndElement(TARGET_SAP_NS_PREFIX, TARGET_SAP_NS, "QueueId"));
	        }

	        // Close tag: Envelope | Header | Main | ReliableMessaging
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
	
}
