package com.invixo.directory.api;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;

import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.common.util.XmlUtil;
import com.invixo.consistency.FileStructure;
import com.invixo.directory.api.webServices.WebServiceHandler;
import com.invixo.main.GlobalParameters;

public class Orchestrator {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = IntegratedConfiguration.class.getName();	
	
	private static final String XML_PREFIX 	= "inv";
	private static final String XML_NS 		= "urn:invixo.com.directory.api";
	private static final String ICO_OVERVIEW_FILE = FileStructure.DIR_CONFIG + "IntegratedConfigurationsOverview.xml";
	private static ArrayList<IntegratedConfiguration> icoList = new ArrayList<IntegratedConfiguration>();
	private static ArrayList<IntegratedConfigurationReadRequest> icoReadRequestList = new ArrayList<IntegratedConfigurationReadRequest>();
	
	
	public static String start() {
		
			
		try {
			
			// Create initial ICO query request - get all ICO's in source PO system
			byte[] requestIcoQueryBytes = createIntegratedConfigurationQueryRequest();
			
			// Call web service
			ByteArrayInputStream responseIcoQueryBytes = WebServiceHandler.callWebService(requestIcoQueryBytes);
		
			// Extract all ICOs in response
			Orchestrator.icoReadRequestList = extractIcoDataFromResponse(responseIcoQueryBytes);
			
			// Create read request to get additional information about ICO (Receiver, QoS, etc)
			byte[] requestIcoReadBytes = createIntegratedConfigurationReadRequest(icoReadRequestList);
			
			//TODO: Debug
			//Util.writeFileToFileSystem(FileStructure.DIR_DEBUG + "\\resonseIcoRead", requestIcoReadBytes);
			
			// Call web service
			ByteArrayInputStream responseIcoReadBytes = WebServiceHandler.callWebService(requestIcoReadBytes);
			
			// POJO: IntegratedConfigurationResponse
			extractIcoInformationFromReadResponse(responseIcoReadBytes);
			
			// Create complete ICO overview file
			createCompleteIcoOverviewFile(Orchestrator.icoList);
			
		} catch (DirectoryApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XMLStreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return ICO_OVERVIEW_FILE;
	}


	private static void createCompleteIcoOverviewFile(ArrayList<IntegratedConfiguration> icoList) throws FileNotFoundException, XMLStreamException {
		XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
		XMLStreamWriter xmlWriter = xMLOutputFactory.createXMLStreamWriter(new FileOutputStream(ICO_OVERVIEW_FILE), GlobalParameters.ENCODING);

		// Add xml version and encoding to output
		xmlWriter.writeStartDocument(GlobalParameters.ENCODING, "1.0");

		// Create element: IntegratedConfigurationList
		xmlWriter.writeStartElement(XML_PREFIX, "IntegratedConfigurationList", XML_NS);
		xmlWriter.writeNamespace(XML_PREFIX, XML_NS);
		
		for (IntegratedConfiguration ico : icoList) {
			xmlWriter.writeStartElement(XML_PREFIX, "IntegratedConfiguration", XML_NS);			
			
			xmlWriter.writeStartElement(XML_PREFIX, "Active", XML_NS);
			xmlWriter.writeCharacters("false");
			xmlWriter.writeEndElement();
			
			xmlWriter.writeStartElement(XML_PREFIX, "Name", XML_NS);
			xmlWriter.writeCharacters(ico.getSenderComponentId() + "_" + ico.getSenderInterfaceName() + "_to_" + ico.getReceiverComponentId() + "_" + ico.getReceiverInterfaceName());
			xmlWriter.writeEndElement();
			
			xmlWriter.writeStartElement(XML_PREFIX, "QualityOfService", XML_NS);
			xmlWriter.writeCharacters(ico.getQualityOfService());
			xmlWriter.writeEndElement();
			
			xmlWriter.writeStartElement(XML_PREFIX, "Sender", XML_NS);		
			
			xmlWriter.writeStartElement(XML_PREFIX, "Party", XML_NS);
			xmlWriter.writeCharacters(ico.getSenderPartyId());
			xmlWriter.writeEndElement();
			
			xmlWriter.writeStartElement(XML_PREFIX, "Component", XML_NS);
			xmlWriter.writeCharacters(ico.getSenderComponentId());
			xmlWriter.writeEndElement();
			
			xmlWriter.writeStartElement(XML_PREFIX, "Interface", XML_NS);
			xmlWriter.writeCharacters(ico.getSenderInterfaceName());
			xmlWriter.writeEndElement();
			
			xmlWriter.writeStartElement(XML_PREFIX, "Namespace", XML_NS);
			xmlWriter.writeCharacters(ico.getSenderInterfaceNamespace());
			xmlWriter.writeEndElement();
			
			xmlWriter.writeEndElement();
			
			xmlWriter.writeStartElement(XML_PREFIX, "Receiver", XML_NS);		
			
			xmlWriter.writeStartElement(XML_PREFIX, "Party", XML_NS);
			xmlWriter.writeCharacters(ico.getReceiverPartyId());
			xmlWriter.writeEndElement();
			
			xmlWriter.writeStartElement(XML_PREFIX, "Component", XML_NS);
			xmlWriter.writeCharacters(ico.getReceiverComponentId());
			xmlWriter.writeEndElement();
			
			xmlWriter.writeStartElement(XML_PREFIX, "Interface", XML_NS);
			xmlWriter.writeCharacters(ico.getReceiverInterfaceName());
			xmlWriter.writeEndElement();
			
			xmlWriter.writeStartElement(XML_PREFIX, "Namespace", XML_NS);
			xmlWriter.writeCharacters(ico.getReceiverInterfaceNamespace());
			xmlWriter.writeEndElement();
			
			xmlWriter.writeEndElement();
			
			xmlWriter.writeEndElement();
		}
		
		xmlWriter.writeEndElement();

		// Finalize writing
		xmlWriter.flush();
		xmlWriter.close();
	}


	private static ArrayList<IntegratedConfiguration> extractIcoInformationFromReadResponse(ByteArrayInputStream responseBytes) throws DirectoryApiException {
		final String SIGNATURE = "extractIcoReceiverInfo(InputStream, IntegratedConfiguration)";
		try {
	        
			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLEventReader eventReader = factory.createXMLEventReader(responseBytes);
			ArrayList<IntegratedConfiguration> icoList = new ArrayList<IntegratedConfiguration>();
			IntegratedConfiguration ico = null;
			
			while (eventReader.hasNext()) {
				XMLEvent event = eventReader.nextEvent();

				switch (event.getEventType()) {
				case XMLStreamConstants.START_ELEMENT:
					String currentElementName = event.asStartElement().getName().getLocalPart();

					
					break;
				
				case XMLStreamConstants.END_ELEMENT:
					String currentEndElementName = event.asEndElement().getName().getLocalPart();
					
					
					break;
				}
			}
			
			return icoList;
		} catch (XMLStreamException e) {
			String msg = "Error extracting message info from Web Service response.\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new DirectoryApiException(msg);
		}
		
	}

	private static byte[] createIntegratedConfigurationReadRequest(ArrayList<IntegratedConfigurationReadRequest> icoRequestList) {
		final String SIGNATURE = "createIntegratedConfigurationReadRequest(IntegratedConfiguration)";
		try {
			final String XML_NS_BAS_PREFIX	= "bas";
			final String XML_NS_BAS_NS		= "http://sap.com/xi/BASIS";
			
			StringWriter stringWriter = new StringWriter();
			XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
			XMLStreamWriter xmlWriter = xMLOutputFactory.createXMLStreamWriter(stringWriter);

			// Add xml version and encoding to output
			xmlWriter.writeStartDocument(GlobalParameters.ENCODING, "1.0");

			// Create element: Envelope
			xmlWriter.writeStartElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_ROOT, XmlUtil.SOAP_ENV_NS);
			xmlWriter.writeNamespace(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_NS);
			xmlWriter.writeNamespace(XML_NS_BAS_PREFIX, XML_NS_BAS_NS);


			// Create element: Envelope | Body
			xmlWriter.writeStartElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_BODY, XmlUtil.SOAP_ENV_NS);

			// Create element: Envelope | Body | IntegratedConfigurationReadRequest
			xmlWriter.writeStartElement(XML_NS_BAS_PREFIX, "IntegratedConfigurationReadRequest", XML_NS_BAS_NS);

			for (IntegratedConfigurationReadRequest icoRequest : icoRequestList) {
				// Create element: Envelope | Body | IntegratedConfigurationReadRequest | IntegratedConfigurationID
				xmlWriter.writeStartElement("IntegratedConfigurationID");
				
				// Create element: Envelope | Body | IntegratedConfigurationReadRequest | IntegratedConfigurationID | SenderPartyID
				xmlWriter.writeStartElement("SenderPartyID");
				xmlWriter.writeCharacters(icoRequest.getSenderPartyId());
				// Close element: Envelope | Body | IntegratedConfigurationReadRequest | IntegratedConfigurationID | SenderPartyID
				xmlWriter.writeEndElement();
				
				// Create element: Envelope | Body | IntegratedConfigurationReadRequest | IntegratedConfigurationID | SenderComponentID
				xmlWriter.writeStartElement("SenderComponentID");
				xmlWriter.writeCharacters(icoRequest.getSenderComponentId());
				// Close element: Envelope | Body | IntegratedConfigurationReadRequest | IntegratedConfigurationID | SenderComponentID
				xmlWriter.writeEndElement();
				
				// Create element: Envelope | Body | IntegratedConfigurationReadRequest | IntegratedConfigurationID | InterfaceName
				xmlWriter.writeStartElement("InterfaceName");
				xmlWriter.writeCharacters(icoRequest.getSenderInterfaceName());
				// Close element: Envelope | Body | IntegratedConfigurationReadRequest | IntegratedConfigurationID | InterfaceName
				xmlWriter.writeEndElement();
				
				// Create element: Envelope | Body | IntegratedConfigurationReadRequest | IntegratedConfigurationID | InterfaceNamespace
				xmlWriter.writeStartElement("InterfaceNamespace");
				xmlWriter.writeCharacters(icoRequest.getSenderInterfaceNamespace());
				// Close element: Envelope | Body | IntegratedConfigurationReadRequest | IntegratedConfigurationID | InterfaceNamespace
				xmlWriter.writeEndElement();
				
				
				// Close tags
				xmlWriter.writeEndElement(); // Envelope | Body | IntegratedConfigurationReadRequest | IntegratedConfigurationID
			
			}
			
			xmlWriter.writeEndElement(); // Envelope | Body | IntegratedConfigurationReadRequest
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

	
	private static ArrayList<IntegratedConfigurationReadRequest> extractIcoDataFromResponse(InputStream responseBytes) throws DirectoryApiException {
		final String SIGNATURE = "extractMessageInfo(InputStream)";
		try {

			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLEventReader eventReader = factory.createXMLEventReader(responseBytes);
			ArrayList<IntegratedConfigurationReadRequest> icoRequestList = new ArrayList<IntegratedConfigurationReadRequest>();
			
			IntegratedConfigurationReadRequest icoRequest = null;
			
			while (eventReader.hasNext()) {
				XMLEvent event = eventReader.nextEvent();

				switch (event.getEventType()) {
				case XMLStreamConstants.START_ELEMENT:
					String currentElementName = event.asStartElement().getName().getLocalPart();

					if ("IntegratedConfigurationID".equals(currentElementName)){
						icoRequest = new IntegratedConfigurationReadRequest();
					} else if ("SenderPartyID".equals(currentElementName) && eventReader.peek().isCharacters()) {
						icoRequest.setSenderPartyId(eventReader.peek().asCharacters().getData());
					} else if ("SenderComponentID".equals(currentElementName)) {
						icoRequest.setSenderComponentId(eventReader.peek().asCharacters().getData());
					} else if ("InterfaceName".equals(currentElementName)) {
						icoRequest.setSenderInterfaceName(eventReader.peek().asCharacters().getData());
					} else if ("InterfaceNamespace".equals(currentElementName)) {
						icoRequest.setSenderInterfaceNamespace(eventReader.peek().asCharacters().getData());
					}
					break;
					
				case XMLStreamConstants.END_ELEMENT:
					String currentEndElementName = event.asEndElement().getName().getLocalPart();
					
					if ("IntegratedConfigurationID".equals(currentEndElementName)) {
						icoRequestList.add(icoRequest);
					}
					break;
				}
			}
			
			return icoRequestList;
			
		} catch (XMLStreamException e) {
			String msg = "Error extracting message info from Web Service response.\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new DirectoryApiException(msg);
		}
	}

	
	/**
	 * Create request message for IntegratedConfigurationQueryRequest
	 * @param ico
	 * @param messageIdMap
	 * @return
	 */
	public static byte[] createIntegratedConfigurationQueryRequest() {
		final String SIGNATURE = "createIntegratedConfigurationQueryRequest()";
		try {
			final String XML_NS_BAS_PREFIX	= "bas";
			final String XML_NS_BAS_NS		= "http://sap.com/xi/BASIS";
			
			StringWriter stringWriter = new StringWriter();
			XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
			XMLStreamWriter xmlWriter = xMLOutputFactory.createXMLStreamWriter(stringWriter);

			// Add xml version and encoding to output
			xmlWriter.writeStartDocument(GlobalParameters.ENCODING, "1.0");

			// Create element: Envelope
			xmlWriter.writeStartElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_ROOT, XmlUtil.SOAP_ENV_NS);
			xmlWriter.writeNamespace(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_NS);
			xmlWriter.writeNamespace(XML_NS_BAS_PREFIX, XML_NS_BAS_NS);


			// Create element: Envelope | Body
			xmlWriter.writeStartElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_BODY, XmlUtil.SOAP_ENV_NS);

			// Create element: Envelope | Body | IntegratedConfigurationQueryRequest
			xmlWriter.writeStartElement(XML_NS_BAS_PREFIX, "IntegratedConfigurationQueryRequest", XML_NS_BAS_NS);

			
			// Close tags
	        xmlWriter.writeEndElement(); // Envelope | Body | IntegratedConfigurationQueryRequest
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
}
