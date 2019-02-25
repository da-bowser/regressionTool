package com.invixo.directory.api;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;

import com.invixo.common.util.HttpException;
import com.invixo.common.util.HttpHandler;
import com.invixo.common.util.Logger;
import com.invixo.common.util.PropertyAccessor;
import com.invixo.common.util.Util;
import com.invixo.common.util.XmlUtil;
import com.invixo.consistency.FileStructure;
import com.invixo.main.GlobalParameters;

public class Orchestrator {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = Orchestrator.class.getName();	
	private static final String XML_PREFIX 	= "inv";
	private static final String XML_NS 		= "urn:invixo.com.directory.api";
	private static final String ICO_OVERVIEW_FILE = FileStructure.ICO_OVERVIEW_FILE;
	private static final String ENDPOINT = GlobalParameters.SAP_PO_HTTP_HOST_AND_PORT + PropertyAccessor.getProperty("SERVICE_PATH_DIR_API");
	
	private static String repositorySimpleQueryTemplate = "rep/read/ext?method=PLAIN&TYPE=MAPPING&KEY=###MAPPING_NAME####%7C###MAPPING_NAMESPACE###&VC=SWC&SWCGUID=###MAPPING_SWCGUID###&SP=-1&UC=false&release=7.0";
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
			byte[] responseIcoQueryBytes = HttpHandler.post(ENDPOINT, GlobalParameters.CONTENT_TYPE_TEXT_XML, requestIcoQueryBytes);
		
			// Extract all ICOs from query response
			Orchestrator.icoReadRequestList = extractIcoDataFromQueryResponse(new ByteArrayInputStream(responseIcoQueryBytes));
			
			// Create read request to get additional information about ICO (Receiver, QoS, etc)
			byte[] requestIcoReadBytes = createIntegratedConfigurationReadRequest(icoReadRequestList);
						
			// Call web service
			byte[] responseIcoReadBytes = HttpHandler.post(ENDPOINT, GlobalParameters.CONTENT_TYPE_TEXT_XML, requestIcoReadBytes);
			
			// Read relevant sender and receiver information from read responses
			Orchestrator.icoList = extractIcoInformationFromReadResponse(new ByteArrayInputStream(responseIcoReadBytes));
			
			// Determine if any ico receiver interfaces uses multimapping
			getIcoMultiplicityInfoFromRepository(Orchestrator.icoList);
			
			// Create complete ICO overview file
			icoOverviewFilePath = createCompleteIcoOverviewFile(Orchestrator.icoList);
			
		} catch (HttpException e) {
			String msg = "Error during web service call " + "\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		} catch (DirectoryApiException | FileNotFoundException | XMLStreamException e) {
			String msg = "Error during creation of overview file " + "\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		}

		// Return file path
		return icoOverviewFilePath;
	}


	/**
	 * Calls PO repository simple query to extract receiver interface mapping multiplicity 1:1/1:n.
	 * @param icoList			List of ICO's to process
	 * @throws DirectoryApiException
	 */
	private static void getIcoMultiplicityInfoFromRepository(ArrayList<IntegratedConfiguration> icoList) throws DirectoryApiException {
		final String SIGNATURE = "getIcoMultiplicityInfoFromRepository(ArrayList<IntegratedConfiguration>)";
		// For every receiver interface rule found on a receiver in an ico
		for (IntegratedConfiguration ico : icoList) {
			
			// An ICO can have multiple receivers
			for (Receiver r : ico.getReceiverList()) {
			
				// A Receiver can have multiple receiver interfaces and mappings
				for (ReceiverInterfaceRule rir : r.getReceiverInterfaceRules()) {
					if ("".equals(rir.getInterfaceMappingName())) {
						// Default to single mapping interface if no mapping is found
						rir.setInterfaceMultiplicity("1:1");
					} else {
						// Create a simple query string for every unique receiver interface rule found
						String repositorySimpeQuery = createRepositorySimpleQuery(repositorySimpleQueryTemplate, rir);
						
						try {
							// Do a http get using the created url
							byte[] responseBytes = HttpHandler.get(repositorySimpeQuery);
							
							// Store response on file system (only relevant for debugging purposes)
							if (GlobalParameters.DEBUG) {
								String fileName = FileStructure.getDebugFileName("getIcoMultiplicityInfoFromRepository", false, rir.getInterfaceName() + "_" + rir.getInterfaceMappingName(), ".xml");
								
								Util.writeFileToFileSystem(fileName, responseBytes);
								logger.writeDebug(LOCATION, SIGNATURE, "<debug enabled> Repository simple query response message is stored here: " + fileName);
							}
							
							// Extract interface multiplicity from response
							extractInterfaceMultiplicityFromResponse(new ByteArrayInputStream(responseBytes), rir);
							
						} catch (HttpException e) {
							// Shorten error and throw a new simple query exeption
							String msg = "Error while trying to get multiplicity info for mapping: " + 
										 rir.getInterfaceMappingName() + 
										 "\nValue of \"@MultiMapping\" will be set to \"false\"." + 
										 "\nTo view the error in its entirety, please enable FILE logging and review.";
							
							logger.writeError(LOCATION, SIGNATURE, msg);
							 
							// Default to 1:1
							rir.setInterfaceMultiplicity("1:1");
							
							// Set exception
							rir.setEx(new RepositorySimpleQueryException(msg));
						}
					}
				}
			}
		}
	}


	/**
	 * Extract interface mapping information regarding multiplicity (multimapping use) from simple query response.
	 * @param responseBytes		Simple query response
	 * @param rir				ReceiverInterfaceRule object
	 * @throws DirectoryApiException
	 */
	public static void extractInterfaceMultiplicityFromResponse(InputStream responseBytes, ReceiverInterfaceRule rir) throws DirectoryApiException {
		final String SIGNATURE = "extractInterfaceMultiplicityFromResponse(InputStream, ReceiverInterfaceRule)";
		
		try {
	        
			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLEventReader eventReader = factory.createXMLEventReader(responseBytes);
			
			while (eventReader.hasNext()) {
				XMLEvent event = eventReader.nextEvent();

				switch (event.getEventType()) {
				case XMLStreamConstants.START_ELEMENT:
					String currentElementName = event.asStartElement().getName().getLocalPart();
					
					if ("Multiplicity".equals(currentElementName)) {
						rir.setInterfaceMultiplicity(eventReader.peek().asCharacters().getData());
					}
					break;
				}
			}
			
			logger.writeDebug(LOCATION, SIGNATURE, "Interface multiplicity found for " + rir.getInterfaceMappingName() + ": " + rir.getInterfaceMultiplicity());
		} catch (XMLStreamException e) {
			String msg = "Error extracting interface multiplicity from repository simple query response.\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new DirectoryApiException(msg);
		}
	}


	/**
	 * Create a unique url to get multiplicity data for a given ReceiverInterfaceRule object.
	 * @param rsqTemplate		Url template with dummy values
	 * @param rir				Real interface values to substitute dummy values in template
	 * @return
	 */
	private static String createRepositorySimpleQuery(String rsqTemplate, ReceiverInterfaceRule rir) {
		// Add host and port, plus template to url
		String url = GlobalParameters.SAP_PO_HTTP_HOST_AND_PORT + rsqTemplate;
		
		// Replace actual info with dummy attributes in template string
		url = url.replace("###MAPPING_NAME####", rir.getInterfaceMappingName());
		url = url.replace("###MAPPING_NAMESPACE###", URLEncoder.encode(rir.getInterfaceMappingNamespace(), StandardCharsets.UTF_8));
		url = url.replace("###MAPPING_SWCGUID###", rir.getInterfaceMappingSoftwareComponentVersionId());
		
		// Return complete url
		return url;
	}


	/**
	 * Create overview file.
	 * @param icoList		Integrated configuration data found during processing
	 * @return
	 * @throws FileNotFoundException
	 * @throws XMLStreamException
	 */
	static String createCompleteIcoOverviewFile(ArrayList<IntegratedConfiguration> icoList) throws FileNotFoundException, XMLStreamException {
		final String SIGNATURE = "createCompleteIcoOverviewFile(ArrayList<IntegratedConfiguration>)";
		String icoOverviewFilePath = ICO_OVERVIEW_FILE;
		
		logger.writeDebug(LOCATION, SIGNATURE, "Ico overview file create: start");
		XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
		XMLStreamWriter xmlWriter = xMLOutputFactory.createXMLStreamWriter(new FileOutputStream(icoOverviewFilePath), GlobalParameters.ENCODING);
		int interfaceCount = 0;
		
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
					// New receiver interface is being processed, increment counter
					interfaceCount++;
					
					// Create element: IntegratedConfigurationList | IntegratedConfiguration
					xmlWriter.writeStartElement(XML_PREFIX, "IntegratedConfiguration", XML_NS);			
					
					// Create element: IntegratedConfigurationList | IntegratedConfiguration | Active
					xmlWriter.writeStartElement(XML_PREFIX, "Active", XML_NS);
					xmlWriter.writeCharacters("false");
					xmlWriter.writeEndElement(); // Close element: IntegratedConfigurationList | IntegratedConfiguration | Active
					
					// Create element: IntegratedConfigurationList | IntegratedConfiguration | Name
					xmlWriter.writeStartElement(XML_PREFIX, "Name", XML_NS);
					xmlWriter.writeCharacters(createCombinedName(ico, r, rir));
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
		
		logger.writeDebug(LOCATION, SIGNATURE, "Ico overview file create done. Total interfaces extracted: " + interfaceCount);
		
		// Return file path
		return icoOverviewFilePath;
	}

	
	/**
	 * Combines ico data to create a unique name for a given ico.
	 * @param ico		Integrated Configuration object
	 * @param r			Receiver object
	 * @param rir		ReceiverInterfaceRule object
	 * @return			Combined name
	 */
	private static String createCombinedName(IntegratedConfiguration ico, Receiver r, ReceiverInterfaceRule rir) {
		String senderPartyId = ico.getSenderPartyId();
		String virtualReceiverPartyId = ico.getVirtualReceiverPartyId();
		String virtualReceiverComponentId = ico.getVirtualReceiverComponentId();
		String receiverPartyId = r.getPartyId();
		
		// Largely these info has a "" value, but in the rare case they don't - add a pre- or postfix to the name
		senderPartyId = addPostOrPrefixHandler("postfix" ,senderPartyId, "_");
		receiverPartyId = addPostOrPrefixHandler("postfix", receiverPartyId, "_");
		virtualReceiverPartyId = addPostOrPrefixHandler("prefix", virtualReceiverPartyId, "_virt_");
		virtualReceiverComponentId = addPostOrPrefixHandler("prefix", virtualReceiverComponentId, "_virt_");
		
		// Create combined ico name
		String icoName = senderPartyId + ico.getSenderComponentId() + "-" + ico.getSenderInterfaceName() + virtualReceiverPartyId + virtualReceiverComponentId + "_to_" + receiverPartyId + r.getComponentId() + "-" + rir.getInterfaceName();
		
		// Return name
		return icoName;
	}

	
	/**
	 * Add pre- or postfix to a value to make it fit into ICO combined name.
	 * @param type		Prefix/postfix
	 * @param input		Text input to process
	 * @param text		Pre- or postfix text to use
	 * @return			Result
	 */
	private static String addPostOrPrefixHandler(String type, String input, String text) {
		if (input.equals("")) { 
			// No value, no pre- or postfix needed
			
		} else if(type.equals("postfix")) {
			// Add text as a postfix
			input = input + text;
			
		} else {
			// Add text as prefix
			input = text + input;
		}
		
		// Return name
		return input;
	}


	/**
	 * Add "Sender" information to ICO overview.
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
		
		String virtualReceiverPartyId = ico.getVirtualReceiverPartyId();
		String virtualReceiverComponentId = ico.getVirtualReceiverComponentId();
		
		// Create element: ... | Sender | SenderUsesVirtualReceiver
		xmlWriter.writeStartElement(XML_PREFIX, "VirtualReceiver", XML_NS);
		if (!virtualReceiverPartyId.equals("") || !virtualReceiverComponentId.equals("")) {			
			xmlWriter.writeAttribute("Used", "true");
			
			// Create element: ... | Sender | SenderUsesVirtualReceiver | Party
			xmlWriter.writeStartElement(XML_PREFIX, "Party", XML_NS);
			xmlWriter.writeCharacters(ico.getVirtualReceiverPartyId());
			xmlWriter.writeEndElement(); // Close element: ... | Sender | SenderUsesVirtualReceiver | Party 
			
			// Create element: ... | Sender | SenderUsesVirtualReceiver | Component
			xmlWriter.writeStartElement(XML_PREFIX, "Component", XML_NS);
			xmlWriter.writeCharacters(ico.getVirtualReceiverComponentId());	
			xmlWriter.writeEndElement(); // Close element: ... | Sender | SenderUsesVirtualReceiver | Component 
		} else {
			xmlWriter.writeAttribute("Used", "false");
		}
		
		xmlWriter.writeEndElement(); // Close element: ... | Sender | SenderUsesVirtualReceiver
	}

	
	/**
	 * Add "Receiver" information to ICO overview.
	 * @param xmlWriter
	 * @param r
	 * @param rir
	 * @throws XMLStreamException
	 */
	private static void addReciverInformation(XMLStreamWriter xmlWriter, Receiver r, ReceiverInterfaceRule rir) throws XMLStreamException {
		// Create element: ... | Receiver | Error
		xmlWriter.writeStartElement(XML_PREFIX, "Error", XML_NS);
		 
		if (rir.getEx() != null) {
			xmlWriter.writeCharacters(rir.getEx().getMessage());
		}
		
		xmlWriter.writeEndElement(); // Close element: ... | Receiver | Error
		
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
		xmlWriter.writeAttribute("MultiMapping", rir.getInterfaceMultiplicity().equals("1:n") ? "true" : "false");
		xmlWriter.writeCharacters(rir.getInterfaceName());
		xmlWriter.writeEndElement(); // Close element: ... | Receiver | Interface 
		
		// Create element: ... | Receiver | Namespace
		xmlWriter.writeStartElement(XML_PREFIX, "Namespace", XML_NS);
		xmlWriter.writeCharacters(rir.getInterfaceNamespace());
		xmlWriter.writeEndElement(); // Close element: ... | Receiver | Namespace 
	}


	/**
	 * Extract all ICO data (sender, receiver, qos, etc..) from ico "IntegratedConfigurationReadRequest" response.
	 * @param responseBytes
	 * @return
	 * @throws DirectoryApiException
	 */
	public static ArrayList<IntegratedConfiguration> extractIcoInformationFromReadResponse(InputStream responseBytes) throws DirectoryApiException {
		final String SIGNATURE = "extractIcoInformationFromReadResponse(InputStream)";
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
			boolean receiverInterfaceRuleMappingFound = false;
			
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
					} else if ("ReceiverPartyID".equals(currentElementName) && eventReader.peek().isCharacters()) {
						ico.setVirtualReceiverPartyId(eventReader.peek().asCharacters().getData());
					} else if ("ReceiverComponentID".equals(currentElementName) && eventReader.peek().isCharacters()) {
						ico.setVirtualReceiverComponentId(eventReader.peek().asCharacters().getData());
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
					 * Receiver interface rule mapping information
					 */
					else if ("Mapping".equals(currentElementName) && receiverInterfaceRuleFound) {
						receiverInterfaceRuleMappingFound = true;
					} else if ("Name".equals(currentElementName) &&  receiverInterfaceRuleMappingFound) {
						rir.setInterfaceMappingName(eventReader.peek().asCharacters().getData());
					} else if ("Namespace".equals(currentElementName) &&  receiverInterfaceRuleMappingFound) {
						rir.setInterfaceMappingNamespace(eventReader.peek().asCharacters().getData());
					} else if ("SoftwareComponentVersionID".equals(currentElementName) &&  receiverInterfaceRuleMappingFound) {
						// Remove hyphen in SWCV id
						rir.setInterfaceMappingSoftwareComponentVersionId(eventReader.peek().asCharacters().getData().replace("-", ""));
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
					} else if ("Mapping".equals(currentEndElementName)) {
						receiverInterfaceRuleMappingFound = false;
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
	static byte[] createIntegratedConfigurationReadRequest(ArrayList<IntegratedConfigurationReadRequest> icoRequestList) {
		final String SIGNATURE = "createIntegratedConfigurationReadRequest(ArrayList<IntegratedConfigurationReadRequest>)";
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

				// Create element: Envelope | Body | IntegratedConfigurationReadRequest | IntegratedConfigurationID | ReceiverPartyID
				xmlWriter.writeStartElement("ReceiverPartyID");
				xmlWriter.writeCharacters(icoRequest.getReceiverPartyId());
				// Close element: Envelope | Body | IntegratedConfigurationReadRequest | IntegratedConfigurationID | ReceiverPartyID
				xmlWriter.writeEndElement();
				
				// Create element: Envelope | Body | IntegratedConfigurationReadRequest | IntegratedConfigurationID | ReceiverComponentID
				xmlWriter.writeStartElement("ReceiverComponentID");
				xmlWriter.writeCharacters(icoRequest.getReceiverComponentId());
				// Close element: Envelope | Body | IntegratedConfigurationReadRequest | IntegratedConfigurationID | ReceiverComponentID
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
	static ArrayList<IntegratedConfigurationReadRequest> extractIcoDataFromQueryResponse(InputStream responseBytes) throws DirectoryApiException {
		final String SIGNATURE = "extractIcoDataFromQueryResponse(InputStream)";
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
					} else if ("ReceiverPartyID".equals(currentElementName) && eventReader.peek().isCharacters()) {
						icoRequest.setReceiverPartyId(eventReader.peek().asCharacters().getData());
					} else if ("ReceiverComponentID".equals(currentElementName) && eventReader.peek().isCharacters()) {
						icoRequest.setReceiverComponentId(eventReader.peek().asCharacters().getData());
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
	private static byte[] createIntegratedConfigurationQueryRequest() {
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
