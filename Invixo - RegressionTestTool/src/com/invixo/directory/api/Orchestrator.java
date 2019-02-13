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
	
	/**
	 * Start processing
	 * @return icoOverviewFilePath
	 */
	public static String start() {
		final String SIGNATURE = "start()";
		String icoOverviewFilePath = "";
		
		try {
			// Create initial ICO query request - get all ICO's in source PO system
			byte[] requestIcoQueryBytes = createIntegratedConfigurationQueryRequest();
			
			// Call web service
			ByteArrayInputStream responseIcoQueryBytes = WebServiceHandler.callWebService(requestIcoQueryBytes);
		
			// Extract all ICOs from query response
			Orchestrator.icoReadRequestList = extractIcoDataFromQueryResponse(responseIcoQueryBytes);
			
			// Create read request to get additional information about ICO (Receiver, QoS, etc)
			byte[] requestIcoReadBytes = createIntegratedConfigurationReadRequest(icoReadRequestList);
						
			// Call web service
			ByteArrayInputStream responseIcoReadBytes = WebServiceHandler.callWebService(requestIcoReadBytes);
			
			// Read relevant sender and receiver information from read respones
			Orchestrator.icoList = extractIcoInformationFromReadResponse(responseIcoReadBytes);
			
			// Create complete ICO overview file
			icoOverviewFilePath = createCompleteIcoOverviewFile(Orchestrator.icoList);
			
		} catch (DirectoryApiException e) {
			String msg = "Error during web service call " + "\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		} catch (FileNotFoundException | XMLStreamException e) {
			String msg = "Error during creation of overview file " + "\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		}

		// Return file path
		return icoOverviewFilePath;
	}


	/**
	 * Create overview file.
	 * @param icoList		Integrated configuration data found during processing
	 * @return
	 * @throws FileNotFoundException
	 * @throws XMLStreamException
	 */
	public static String createCompleteIcoOverviewFile(ArrayList<IntegratedConfiguration> icoList) throws FileNotFoundException, XMLStreamException {
		final String SIGNATURE = "createCompleteIcoOverviewFile(ArrayList<IntegratedConfiguration>)";
		String icoOverviewFilePath = ICO_OVERVIEW_FILE;
		
		logger.writeDebug(LOCATION, SIGNATURE, "Ico overview file create: start");
		XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
		XMLStreamWriter xmlWriter = xMLOutputFactory.createXMLStreamWriter(new FileOutputStream(icoOverviewFilePath), GlobalParameters.ENCODING);

		// Add xml version and encoding to output
		xmlWriter.writeStartDocument(GlobalParameters.ENCODING, "1.0");

		// Create element: IntegratedConfigurationList
		xmlWriter.writeStartElement(XML_PREFIX, "IntegratedConfigurationList", XML_NS);
		xmlWriter.writeNamespace(XML_PREFIX, XML_NS);
		
		for (IntegratedConfiguration ico : icoList) {
			// Get all receivers found for ico
			ArrayList<Receiver> receiverList = ico.getReceiverList();
			
			for (Receiver r : receiverList) {
				// get all receiver interfaces found for receiver
				ArrayList<ReceiverInterfaceRule> receiverInterfaceRules = r.getReceiverInterfaceRules();
				
				for (ReceiverInterfaceRule rir : receiverInterfaceRules) {
					// Create element: IntegratedConfigurationList | IntegratedConfiguration
					xmlWriter.writeStartElement(XML_PREFIX, "IntegratedConfiguration", XML_NS);			
					
					// Create element: IntegratedConfigurationList | IntegratedConfiguration | Active
					xmlWriter.writeStartElement(XML_PREFIX, "Active", XML_NS);
					xmlWriter.writeCharacters("false");
					xmlWriter.writeEndElement(); // Close element: IntegratedConfigurationList | IntegratedConfiguration | Active
					
					// Create element: IntegratedConfigurationList | IntegratedConfiguration | Name
					xmlWriter.writeStartElement(XML_PREFIX, "Name", XML_NS);
					xmlWriter.writeCharacters(ico.getSenderComponentId() + "_" + ico.getSenderInterfaceName() + "_to_" + r.getComponentId() + "_" + rir.getInterfaceName());
					xmlWriter.writeEndElement(); // Close element: IntegratedConfigurationList | IntegratedConfiguration | Name
					
					// Create element: IntegratedConfigurationList | IntegratedConfiguration | QualityOfService
					xmlWriter.writeStartElement(XML_PREFIX, "QualityOfService", XML_NS);
					xmlWriter.writeCharacters(ico.getQualityOfService());
					xmlWriter.writeEndElement(); // Close element: IntegratedConfigurationList | IntegratedConfiguration | QualityOfService
					
					// Create element: IntegratedConfigurationList | IntegratedConfiguration | FromTime
					xmlWriter.writeStartElement(XML_PREFIX, "FromTime", XML_NS);
					xmlWriter.writeEndElement(); // Close element: IntegratedConfigurationList | IntegratedConfiguration | FromTime
					
					// Create element: IntegratedConfigurationList | IntegratedConfiguration | ToTime
					xmlWriter.writeStartElement(XML_PREFIX, "ToTime", XML_NS);
					xmlWriter.writeEndElement(); // Close element: IntegratedConfigurationList | IntegratedConfiguration | ToTime
					
					// Create element: IntegratedConfigurationList | IntegratedConfiguration | MaxMessages
					xmlWriter.writeStartElement(XML_PREFIX, "MaxMessages", XML_NS);
					xmlWriter.writeCharacters("10");
					xmlWriter.writeEndElement(); // Close element: IntegratedConfigurationList | IntegratedConfiguration | MaxMessages
					
					// Create element: IntegratedConfigurationList | IntegratedConfiguration | Sender
					xmlWriter.writeStartElement(XML_PREFIX, "Sender", XML_NS);	
					addSenderInformation(xmlWriter, ico);
					xmlWriter.writeEndElement();// Close element: IntegratedConfigurationList | IntegratedConfiguration | Sender
					
					// Create element: IntegratedConfigurationList | IntegratedConfiguration | Receiver
					xmlWriter.writeStartElement(XML_PREFIX, "Receiver", XML_NS);	
					addReciverInformation(xmlWriter, r, rir);					
					xmlWriter.writeEndElement();// Close element: IntegratedConfigurationList | IntegratedConfiguration | Receiver
				
					xmlWriter.writeEndElement(); // Close element: IntegratedConfigurationList | IntegratedConfiguration
				}
			}
		}
		
		xmlWriter.writeEndElement(); // Close element: IntegratedConfigurationList

		// Finalize writing
		xmlWriter.flush();
		xmlWriter.close();
		
		logger.writeDebug(LOCATION, SIGNATURE, "Ico overview file create: end");
		
		// Return file path
		return icoOverviewFilePath;
	}

	
	/**
	 * Add "Sender" information to ico overview.
	 * @param xmlWriter
	 * @param ico
	 * @throws XMLStreamException
	 */
	private static void addSenderInformation(XMLStreamWriter xmlWriter, IntegratedConfiguration ico) throws XMLStreamException {
		// Create element: ... | Sender | Party
		xmlWriter.writeStartElement(XML_PREFIX, "Party", XML_NS);
		xmlWriter.writeCharacters(ico.getSenderPartyId());
		xmlWriter.writeEndElement(); // Close element: ... | Sender | Party 
		
		// Create element: ... | Sender | Component
		xmlWriter.writeStartElement(XML_PREFIX, "Component", XML_NS);
		xmlWriter.writeCharacters(ico.getSenderComponentId());
		xmlWriter.writeEndElement(); // Close element: ... | Sender | Component 
		
		// Create element: ... | Sender | Interface
		xmlWriter.writeStartElement(XML_PREFIX, "Interface", XML_NS);
		xmlWriter.writeCharacters(ico.getSenderInterfaceName());
		xmlWriter.writeEndElement(); // Close element: ... | Sender | Interface 
		
		// Create element: ... | Sender | Namespace
		xmlWriter.writeStartElement(XML_PREFIX, "Namespace", XML_NS);
		xmlWriter.writeCharacters(ico.getSenderInterfaceNamespace());
		xmlWriter.writeEndElement(); // Close element: ... | Sender | Namespace 
		
	}

	
	/**
	 * Add "Receiver" information to ico overview.
	 * @param xmlWriter
	 * @param r
	 * @param rir
	 * @throws XMLStreamException
	 */
	private static void addReciverInformation(XMLStreamWriter xmlWriter, Receiver r, ReceiverInterfaceRule rir) throws XMLStreamException {
		// Create element: ... | Receiver | Party
		xmlWriter.writeStartElement(XML_PREFIX, "Party", XML_NS);
		xmlWriter.writeCharacters(r.getPartyId());
		xmlWriter.writeEndElement(); // Close element: ... | Receiver | Party 
		
		// Create element: ... | Receiver | Component
		xmlWriter.writeStartElement(XML_PREFIX, "Component", XML_NS);
		xmlWriter.writeCharacters(r.getComponentId());
		xmlWriter.writeEndElement(); // Close element: ... | Receiver | Component 
		
		// Create element: ... | Receiver | Interface
		xmlWriter.writeStartElement(XML_PREFIX, "Interface", XML_NS);
		xmlWriter.writeCharacters(rir.getInterfaceName());
		xmlWriter.writeEndElement(); // Close element: ... | Receiver | Interface 
		
		// Create element: ... | Receiver | Namespace
		xmlWriter.writeStartElement(XML_PREFIX, "Namespace", XML_NS);
		xmlWriter.writeCharacters(rir.getInterfaceNamespace());
		xmlWriter.writeEndElement(); // Close element: ... | Receiver | Namespace 
	}


	/**
	 * Extract all ico data (sender, receiver, qos, etc..) from ico "IntegratedConfigurationReadRequest" response.
	 * @param responseBytes
	 * @return
	 * @throws DirectoryApiException
	 */
	public static ArrayList<IntegratedConfiguration> extractIcoInformationFromReadResponse(InputStream responseBytes) throws DirectoryApiException {
		final String SIGNATURE = "extractIcoReceiverInfo(InputStream, IntegratedConfiguration)";
		logger.writeDebug(LOCATION, SIGNATURE, "Extract ico info from read response: start");
		try {
	        
			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLEventReader eventReader = factory.createXMLEventReader(responseBytes);
			ArrayList<IntegratedConfiguration> icoList = new ArrayList<IntegratedConfiguration>();
			IntegratedConfiguration ico = null;
			Receiver r = null;
			ReceiverInterfaceRule rir = null;
			boolean receiverFound = false;
			boolean receiverInterfaceRuleFound = false;
			
			while (eventReader.hasNext()) {
				XMLEvent event = eventReader.nextEvent();

				switch (event.getEventType()) {
				case XMLStreamConstants.START_ELEMENT:
					String currentElementName = event.asStartElement().getName().getLocalPart();

					/**
					 *  Sender information
					 */
					if ("IntegratedConfigurationID".equals(currentElementName)) {
						ico = new IntegratedConfiguration();
					} else if ("SenderPartyID".equals(currentElementName) && eventReader.peek().isCharacters()) {
						ico.setSenderPartyId(eventReader.peek().asCharacters().getData());
					} else if ("SenderComponentID".equals(currentElementName)) {
						ico.setSenderComponentId(eventReader.peek().asCharacters().getData());
					} else if ("InterfaceName".equals(currentElementName)) {
						ico.setSenderInterfaceName(eventReader.peek().asCharacters().getData());
					} else if ("InterfaceNamespace".equals(currentElementName)) {
						ico.setSenderInterfaceNamespace(eventReader.peek().asCharacters().getData());
					} else if ("QualityOfService".equals(currentElementName)) {
						ico.setQualityOfService(eventReader.peek().asCharacters().getData());
					}

					/**
					 * Receiver information
					 */
					else if ("ReceiverInterfaces".equals(currentElementName)) { 
						// New receiver is found
						r = new Receiver();
						receiverFound = true;
					} else if ("PartyID".equals(currentElementName) && eventReader.peek().isCharacters() && receiverFound) {
						r.setPartyId(eventReader.peek().asCharacters().getData());
					} else if ("ComponentID".equals(currentElementName) && receiverFound) {
						r.setComponentId(eventReader.peek().asCharacters().getData());
					}
					
					/**
					 * Receiver interface rule information
					 */
					else if ("ReceiverInterfaceRule".equals(currentElementName) && receiverFound) { 
						// New receiver interface rule found
						rir = new ReceiverInterfaceRule();
						receiverInterfaceRuleFound = true;
					} else if ("Operation".equals(currentElementName) && eventReader.peek().isCharacters() && receiverInterfaceRuleFound) { 
						rir.setInterfaceOperation(eventReader.peek().asCharacters().getData());
					} else if ("Name".equals(currentElementName) &&  receiverInterfaceRuleFound) { 
						rir.setInterfaceName(eventReader.peek().asCharacters().getData());
					} else if ("Namespace".equals(currentElementName) && receiverInterfaceRuleFound) { 
						rir.setInterfaceNamespace(eventReader.peek().asCharacters().getData());
					}
					
					break;

				case XMLStreamConstants.END_ELEMENT:
					String currentEndElementName = event.asEndElement().getName().getLocalPart();

					if ("IntegratedConfiguration".equals(currentEndElementName)) {
						icoList.add(ico);
					} else if ("ReceiverInterfaces".equals(currentEndElementName)) {
						receiverFound = false;
						ico.getReceiverList().add(r);
					} else if ("ReceiverInterfaceRule".equals(currentEndElementName)) {
						receiverInterfaceRuleFound = false;
						r.getReceiverInterfaceRules().add(rir);
					}
					break;
				}
			}
			
			logger.writeDebug(LOCATION, SIGNATURE, "Extract done, ico's found in read response: " + icoList.size());
			
			return icoList;
		} catch (XMLStreamException e) {
			String msg = "Error extracting message info from Web Service response.\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new DirectoryApiException(msg);
		}
		
	}

	
	/**
	 * Create "IntegratedConfigurationReadRequest" to get all ico information from source system
	 * @param icoRequestList	List of ico's found in query response
	 * @return
	 */
	private static byte[] createIntegratedConfigurationReadRequest(ArrayList<IntegratedConfigurationReadRequest> icoRequestList) {
		final String SIGNATURE = "createIntegratedConfigurationReadRequest(IntegratedConfiguration)";
		logger.writeDebug(LOCATION, SIGNATURE, "Create ico read request: start");
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
				
				xmlWriter.writeEndElement(); // Envelope | Body | IntegratedConfigurationReadRequest | IntegratedConfigurationID
			}
			
			xmlWriter.writeEndElement(); // Envelope | Body | IntegratedConfigurationReadRequest
			xmlWriter.writeEndElement(); // Envelope | Body
			xmlWriter.writeEndElement(); // Envelope

			// Finalize writing
			xmlWriter.flush();
			xmlWriter.close();
			stringWriter.flush();
			
			// Write IntegratedConfigurationReadRequest request to file system if debug for this is enabled (property)
			if (GlobalParameters.DEBUG) {
				String file = FileStructure.getDebugFileName("createIntegratedConfigurationReadRequest", true, "IntegratedConfigurationReadRequest", "xml");
				Util.writeFileToFileSystem(file, stringWriter.toString().getBytes());
				logger.writeDebug(LOCATION, SIGNATURE, "<debug enabled> IntegratedConfigurationReadRequest request message is stored here: " + file);
			}
			
			logger.writeDebug(LOCATION, SIGNATURE, "Create ico read request: done");
			
			// Return read request
			return stringWriter.toString().getBytes();
			
		} catch (XMLStreamException e) {
			String msg = "Error creating SOAP request for IntegratedConfigurationReadRequest" + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		}
	}

	
	/**
	 * Extract data from query response. This will be the basis for getting all ico data later during "read"
	 * @param responseBytes		"IntegratedConfigurationQueryRequest" response 
	 * @return
	 * @throws DirectoryApiException
	 */
	public static ArrayList<IntegratedConfigurationReadRequest> extractIcoDataFromQueryResponse(InputStream responseBytes) throws DirectoryApiException {
		final String SIGNATURE = "extractMessageInfo(InputStream)";
		logger.writeDebug(LOCATION, SIGNATURE, "Extracting data from ico query response");
		try {

			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLEventReader eventReader = factory.createXMLEventReader(responseBytes);
			ArrayList<IntegratedConfigurationReadRequest> icoRequestList = new ArrayList<IntegratedConfigurationReadRequest>();
			
			IntegratedConfigurationReadRequest icoRequest = null;
			
			// Build a list of read requests based on query response
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
			
			logger.writeDebug(LOCATION, SIGNATURE, "Extract done, ico's found in query response: " + icoRequestList.size());
			
			// Return list of read requests found
			return icoRequestList;
			
		} catch (XMLStreamException e) {
			String msg = "Error extracting message info from Web Service response.\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new DirectoryApiException(msg);
		}
	}

	
	/**
	 * Create "empty" request message for IntegratedConfigurationQueryRequest to include all ICO's in system in the response
	 * @return IntegratedConfigurationQueryRequest
	 */
	public static byte[] createIntegratedConfigurationQueryRequest() {
		final String SIGNATURE = "createIntegratedConfigurationQueryRequest()";
		
		logger.writeDebug(LOCATION, SIGNATURE, "Create query request: start");
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

			xmlWriter.writeEndElement(); // Envelope | Body | IntegratedConfigurationQueryRequest
			xmlWriter.writeEndElement(); // Envelope | Body
			xmlWriter.writeEndElement(); // Envelope

			// Finalize writing
			xmlWriter.flush();
			xmlWriter.close();
			stringWriter.flush();
			
			logger.writeDebug(LOCATION, SIGNATURE, "Create query request: done");
			
			// Write IntegratedConfigurationQueryRequest request to file system if debug for this is enabled (property)
			if (GlobalParameters.DEBUG) {
				String file = FileStructure.getDebugFileName("createIntegratedConfigurationQueryRequest", true, "IntegratedConfigurationQueryRequest", "xml");
				Util.writeFileToFileSystem(file, stringWriter.toString().getBytes());
				logger.writeDebug(LOCATION, SIGNATURE, "<debug enabled> IntegratedConfigurationQueryRequest request message is stored here: " + file);
			}
			
			return stringWriter.toString().getBytes();
		} catch (XMLStreamException e) {
			String msg = "Error creating SOAP request for GetMessagesWithSuccessors. " + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		}
	}
}
