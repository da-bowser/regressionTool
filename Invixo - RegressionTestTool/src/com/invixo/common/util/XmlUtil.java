package com.invixo.common.util;

import java.io.StringWriter;
import java.util.Collection;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.invixo.main.GlobalParameters;

public class XmlUtil {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = XmlUtil.class.getName();	
	
	// Xml: soap envelope, general
	public static final String SOAP_ENV_NS 	 	= "http://schemas.xmlsoap.org/soap/envelope/";
	public static final String SOAP_ENV_PREFIX 	= "soapenv";
	public static final String SOAP_ENV_ROOT 	= "Envelope";
	public static final String SOAP_ENV_HEAD 	= "Header";
	public static final String SOAP_ENV_BODY 	= "Body";
	
	
	
	/**
	 * Create request message for GetMessagesWithSuccessors
	 * @param messageIds			List of Message IDs to get message details from. Map(key, value) = Map(original extract message id, inject message id)
	 * @return
	 */
	public static byte[] createGetMessagesWithSuccessorsRequest(Collection<String> messageIds) {
		final String SIGNATURE = "createGetMessagesWithSuccessorsRequest(Collection<String>)";
		try {
			final String XML_NS_URN_PREFIX	= "urn";
			final String XML_NS_URN_NS		= "urn:AdapterMessageMonitoringVi";
			final String XML_NS_LANG_PREFIX	= "lang";
			final String XML_NS_LANG_NS		= "java/lang";
			
			StringWriter stringWriter = new StringWriter();
			XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
			XMLStreamWriter xmlWriter = xMLOutputFactory.createXMLStreamWriter(stringWriter);

			// Add xml version and encoding to output
			xmlWriter.writeStartDocument(GlobalParameters.ENCODING, "1.0");

			// Create element: Envelope
			xmlWriter.writeStartElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_ROOT, XmlUtil.SOAP_ENV_NS);
			xmlWriter.writeNamespace(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_NS);
			xmlWriter.writeNamespace(XML_NS_URN_PREFIX, XML_NS_URN_NS);
			xmlWriter.writeNamespace(XML_NS_LANG_PREFIX, XML_NS_LANG_NS);

			// Create element: Envelope | Body
			xmlWriter.writeStartElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_BODY, XmlUtil.SOAP_ENV_NS);

			// Create element: Envelope | Body | getMessagesWithSuccessors
			xmlWriter.writeStartElement(XML_NS_URN_PREFIX, "getMessagesWithSuccessors", XML_NS_URN_NS);

			// Create element: Envelope | Body | getMessagesWithSuccessors | messageIds
			xmlWriter.writeStartElement(XML_NS_URN_PREFIX, "messageIds", XML_NS_URN_NS);

			// Add (inject) message id's to XML
	        for (String messageId : messageIds) {
				// Create element: Envelope | Body | getMessagesWithSuccessors | messageIds | String
				xmlWriter.writeStartElement(XML_NS_LANG_PREFIX, "String", XML_NS_LANG_NS);				
				xmlWriter.writeCharacters(messageId);
		        xmlWriter.writeEndElement();
	        }			
	        
	        // Close element: Envelope | Body | getMessagesWithSuccessors | messageIds
	        xmlWriter.writeEndElement();
	        
			// Create element: Envelope | Body | getMessagesWithSuccessors | archive
			xmlWriter.writeStartElement(XML_NS_URN_PREFIX, "archive", XML_NS_URN_NS);
			xmlWriter.writeCharacters("false");
			xmlWriter.writeEndElement();
			
			// Close tags
	        xmlWriter.writeEndElement(); // Envelope | Body | getMessagesWithSuccessors
			xmlWriter.writeEndElement(); // Envelope | Body
			xmlWriter.writeEndElement(); // Envelope

			// Finalize writing
			xmlWriter.flush();
			xmlWriter.close();
			stringWriter.flush();
			
			return stringWriter.toString().getBytes();
		} catch (XMLStreamException e) {
			String msg = "Error creating SOAP request for GetMessagesWithSuccessors. " + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		}
	}


	/**
	 * Create request message for GetMessagesByIDsRequest
	 * @param messageIdMap			List of Message IDs to get message details from.
	 * @return
	 */
	public static byte[] createGetMessagesByIDsRequest(String messageId) {
		final String SIGNATURE = "createGetMessagesByIDsRequest(String)";
		try {
			final String XML_NS_URN_PREFIX	= "urn";
			final String XML_NS_URN_NS		= "urn:AdapterMessageMonitoringVi";
			final String XML_NS_LANG_PREFIX	= "lang";
			final String XML_NS_LANG_NS		= "java/lang";
			
			StringWriter stringWriter = new StringWriter();
			XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
			XMLStreamWriter xmlWriter = xMLOutputFactory.createXMLStreamWriter(stringWriter);

			// Add xml version and encoding to output
			xmlWriter.writeStartDocument(GlobalParameters.ENCODING, "1.0");

			// Create element: Envelope
			xmlWriter.writeStartElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_ROOT, XmlUtil.SOAP_ENV_NS);
			xmlWriter.writeNamespace(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_NS);
			xmlWriter.writeNamespace(XML_NS_URN_PREFIX, XML_NS_URN_NS);
			xmlWriter.writeNamespace(XML_NS_LANG_PREFIX, XML_NS_LANG_NS);

			// Create element: Envelope | Body
			xmlWriter.writeStartElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_BODY, XmlUtil.SOAP_ENV_NS);

			// Create element: Envelope | Body | getMessagesByIDs
			xmlWriter.writeStartElement(XML_NS_URN_PREFIX, "getMessagesByIDs", XML_NS_URN_NS);

			// Create element: Envelope | Body | getMessagesByIDs | messageIds
			xmlWriter.writeStartElement(XML_NS_URN_PREFIX, "messageIds", XML_NS_URN_NS);

			// Create element: Envelope | Body | getMessagesByIDs | messageIds | String
			xmlWriter.writeStartElement(XML_NS_LANG_PREFIX, "String", XML_NS_LANG_NS);				
			xmlWriter.writeCharacters(messageId);
	        xmlWriter.writeEndElement();			
	        
	        xmlWriter.writeEndElement(); // Close element: Envelope | Body | getMessagesByIDs | messageIds
	        
			// Create element: Envelope | Body | getMessagesByIDs | referenceIds
	        xmlWriter.writeStartElement(XML_NS_URN_PREFIX, "referenceIds", XML_NS_URN_NS);
	        xmlWriter.writeEndElement(); // Close element:  Envelope | Body | getMessagesByIDs | referenceIds
	        
			// Create element: Envelope | Body | getMessagesByIDs | correlationIds
	        xmlWriter.writeStartElement(XML_NS_URN_PREFIX, "correlationIds", XML_NS_URN_NS);
	        xmlWriter.writeEndElement(); // Close element:  Envelope | Body | getMessagesByIDs | correlationIds
	        
			// Create element: Envelope | Body | getMessagesByIDs | archive
			xmlWriter.writeStartElement(XML_NS_URN_PREFIX, "archive", XML_NS_URN_NS);
			xmlWriter.writeCharacters("false");
			xmlWriter.writeEndElement();
			
			// Close tags
	        xmlWriter.writeEndElement(); // Envelope | Body | getMessagesByIDs
			xmlWriter.writeEndElement(); // Envelope | Body
			xmlWriter.writeEndElement(); // Envelope

			// Finalize writing
			xmlWriter.flush();
			xmlWriter.close();
			stringWriter.flush();
			
			return stringWriter.toString().getBytes();
		} catch (XMLStreamException e) {
			String msg = "Error creating SOAP request for getMessagesByIDs. " + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		}
	}
		
}
