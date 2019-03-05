package com.invixo.extraction;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamSource;

import com.invixo.common.util.HttpException;
import com.invixo.common.util.HttpHandler;
import com.invixo.common.util.Logger;
import com.invixo.common.util.PropertyAccessor;
import com.invixo.common.util.Util;
import com.invixo.common.util.XmlUtil;
import com.invixo.consistency.FileStructure;
import com.invixo.main.GlobalParameters;

public class WebServiceUtil {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = WebServiceUtil.class.getName();
	private static final String ENDPOINT = GlobalParameters.SAP_PO_HTTP_HOST_AND_PORT + PropertyAccessor.getProperty("SERVICE_PATH_EXTRACT");
	
	
	/**
	 * Call service: GetMessageBytesJavaLangStringIntBoolean
	 * @param messageKey
	 * @param version
	 * @return
	 * @throws ExtractorException
	 * @throws HttpException
	 * @throws IOException
	 */
	public static String lookupSapXiMessage(String messageKey, int version) throws ExtractorException, HttpException, IOException {
		final String SIGNATURE = "lookupSapXiMessage(String, int)";
		
		// Build request payload (service: getMessageBytesJavaLangStringIntBoolean)
		InputStream wsRequest = createRequestGetMessageBytesJavaLangStringIntBoolean(messageKey, version);
		logger.writeDebug(LOCATION, SIGNATURE, "Web Service request payload created for Message Key " + messageKey + " with version " + version);
		
		// Call Web Service fetching the SAP XI Message (XI header and payload)
		byte[] wsResponse = HttpHandler.post(ENDPOINT, GlobalParameters.CONTENT_TYPE_TEXT_XML, wsRequest.readAllBytes());
		logger.writeDebug(LOCATION, SIGNATURE, "Web Service called");

		// Extract base64 encoded message from Web Service response
		String base64EncodedMessage = extractEncodedPayload(wsResponse);
		return base64EncodedMessage;
	}
	
	
	/**
	 * Call service: GetMessagesByIDs
	 * @param messageId
	 * @param icoName
	 * @return
	 * @throws HttpException
	 * @throws ExtractorException
	 */
	static String lookupMessageKey(String messageId, String icoName) throws HttpException, ExtractorException {
		final String SIGNATURE = "lookupMessageKey(String, String)";
		
		// Create "GetMessagesByIDs" request
		byte[] getMessageByIdsRequestBytes = createRequestGetMessagesByIDs(messageId);
		
		// Write request to file system if debug for this is enabled (property)
		if (GlobalParameters.DEBUG) {
			String file = FileStructure.getDebugFileName("GetMessagesByIDs", true, icoName, "xml");
			Util.writeFileToFileSystem(file, getMessageByIdsRequestBytes);
			logger.writeDebug(LOCATION, SIGNATURE, "<debug enabled> MultiMapping scenario: GetMessagesByIDs request message to be sent to SAP PO is stored here: " + file);
		}
		
		// Call web service (GetMessagesByIDs)
		byte[] getMessageByIdsResponseBytes = HttpHandler.post(ENDPOINT, GlobalParameters.CONTENT_TYPE_TEXT_XML, getMessageByIdsRequestBytes);
		logger.writeDebug(LOCATION, SIGNATURE, "Web Service (GetMessagesByIDs) called");
		
		// Extract messageKey from response
		String messageKey = extractMessageKeyFromResponse(getMessageByIdsResponseBytes);
		return messageKey;
	}

	
	/**
	 * Call service: GetPredecessorMessageId
	 * @param messageId
	 * @param icoName
	 * @return
	 * @throws HttpException
	 * @throws ExtractorException
	 */
	static String lookupPredecessorMessageId(String messageId, String icoName) throws HttpException, ExtractorException {
		final String SIGNATURE = "lookupPredecessorMessageId(String, String)";
		
		// Create "GetPredecessorMessageId" request
		byte[] getMessagesWithPredecessorsRequestBytes = createRequestGetPredecessorMessageId(messageId);
		logger.writeDebug(LOCATION, SIGNATURE, "GetPredecessorMessageId request created");
		
		// Write request to file system if debug for this is enabled (property)
		if (GlobalParameters.DEBUG) {
			String file = FileStructure.getDebugFileName("GetPredecessorMessageId", true, icoName, "xml");
			Util.writeFileToFileSystem(file, getMessagesWithPredecessorsRequestBytes);
			logger.writeDebug(LOCATION, SIGNATURE, "<debug enabled> MultiMapping scenario: GetPredecessorMessageId request message to be sent to SAP PO is stored here: " + file);
		}
					
		// Call web service (GetPredecessorMessageId)
		byte[] getPredecessorMessageIdResponseBytes = HttpHandler.post(ENDPOINT, GlobalParameters.CONTENT_TYPE_TEXT_XML, getMessagesWithPredecessorsRequestBytes);
		logger.writeDebug(LOCATION, SIGNATURE, "Web Service (GetPredecessorMessageId) called");
		
		// Extract parentId from response
		String parentId = extractPredecessorIdFromResponse(getPredecessorMessageIdResponseBytes);
		return parentId;
	}

	
	static byte[] lookupSuccessorsBatch(ArrayList<String> messageIdList, String icoName) throws HttpException {
		final String SIGNATURE = "lookupSuccessorsBatch(ArrayList<String>, String)";
		
		// Create request for GetMessagesWithSuccessors
		byte[] requestBytes = createRequestGetMessagesWithSuccessors(messageIdList);
		logger.writeDebug(LOCATION, SIGNATURE, "GetMessagesWithSuccessors request created");
		
		// Write request to file system if debug for this is enabled (property)
		if (GlobalParameters.DEBUG) {
			String file = FileStructure.getDebugFileName("GetMessagesWithSuccessors", true, icoName, "xml");
			Util.writeFileToFileSystem(file, requestBytes);
			logger.writeDebug(LOCATION, SIGNATURE, "<debug enabled> GetMessagesWithSuccessors request message to be sent to SAP PO is stored here: " + file);
		}
					
		// Call web service (GetMessagesWithSuccessors)
		byte[] responseBytes = HttpHandler.post(ENDPOINT, GlobalParameters.CONTENT_TYPE_TEXT_XML, requestBytes);
		logger.writeDebug(LOCATION, SIGNATURE, "Web Service (GetMessagesWithSuccessors) called");	
		
		return responseBytes;
	}
		
	
	/**
	 * Call service: GetMessageList
	 * @param ico
	 * @return
	 * @throws ExtractorException
	 * @throws HttpException
	 */
	static MessageInfo lookupMessages(IntegratedConfiguration ico) throws ExtractorException, HttpException {
		final String SIGNATURE = "lookupMessages(IntegratedConfiguration)";
		// Create request for GetMessageList
		byte[] requestBytes = createRequestGetMessageList(ico);
		logger.writeDebug(LOCATION, SIGNATURE, "GetMessageList request created");
		
		// Write request to file system if debug for this is enabled (property)
		if (GlobalParameters.DEBUG) {
			String file = FileStructure.getDebugFileName("GetMessageList", true, ico.getName(), "xml");
			Util.writeFileToFileSystem(file, requestBytes);
			logger.writeDebug(LOCATION, SIGNATURE, "<debug enabled> GetMessageList request message to be sent to SAP PO is stored here: " + file);
		}
					
		// Call web service (GetMessageList)
		byte[] responseBytes = HttpHandler.post(ENDPOINT, GlobalParameters.CONTENT_TYPE_TEXT_XML, requestBytes);
		logger.writeDebug(LOCATION, SIGNATURE, "Web Service (GetMessageList) called");
			
		// Extract MessageKeys from web Service response
		MessageInfo msgInfo = extractMessageInfo(responseBytes, ico.getReceiverInterface());
		return msgInfo;
	}
	
	
	/**
	 * Extract Predecessor from GetPredecessorMessageId response.
	 * @param responseBytes
	 * @return
	 * @throws ExtractorException
	 */
	private static String extractPredecessorIdFromResponse(byte[] responseBytes) throws ExtractorException {
		final String SIGNATURE = "extractPredecessorIdFromResponse(byte[])";
		try {
	        String predecessorId = "";
	        
			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLEventReader eventReader = factory.createXMLEventReader(new ByteArrayInputStream(responseBytes));

			while (eventReader.hasNext()) {
				XMLEvent event = eventReader.nextEvent();

				switch (event.getEventType()) {
				case XMLStreamConstants.START_ELEMENT:
					String currentElementName = event.asStartElement().getName().getLocalPart();

					if ("Response".equals(currentElementName)) {
						predecessorId = eventReader.peek().asCharacters().getData();
					}
					break;
				}
			}
			
			// Return parentId found in response
			return predecessorId;
		} catch (XMLStreamException e) {
			String msg = "Error extracting parentIds from 'GetMessagesWithSuccessors' Web Service response.\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);
		}
	}
	
	
	
	private static String extractEncodedPayload(byte[] fileContent) throws ExtractorException {
		final String SIGNATURE = "extractEncodedPayload(byte[])";
		boolean fetchData = false;
		try {
			String response = "";
			XMLInputFactory factory = XMLInputFactory.newInstance();
			StreamSource ss = new StreamSource(new ByteArrayInputStream(fileContent));
			XMLEventReader eventReader = factory.createXMLEventReader(ss);
			
			while (eventReader.hasNext()) {
			    XMLEvent event = eventReader.nextEvent();
			    
			    switch(event.getEventType()) {
			    case XMLStreamConstants.START_ELEMENT:
			    	String currentElementName = event.asStartElement().getName().getLocalPart();
			    	if ("Response".equals(currentElementName)) {
			    		fetchData = true;
			    	}
			    	break;
			    case XMLStreamConstants.CHARACTERS:
			    	if (event.isCharacters() && fetchData) {		    	
				    	response += event.asCharacters().getData();
			    	}
			    	break;
			    case XMLStreamConstants.END_ELEMENT:
			    	if (fetchData) {
				    	fetchData = false;
			    	}
			    }
			}
			return response;
		} catch (XMLStreamException e) {
			String msg = "Error extracting encoded (base64) payload from response.\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);
		} 
	}
	
	
	/**
	 * Create a new XML Request based on the specified parameters matching Web Service method GetMessageBytesJavaLangStringIntBoolean. 
	 * @param messageKey
	 * @param version
	 * @return
	 * @throws ExtractorException
	 */
	static ByteArrayInputStream createRequestGetMessageBytesJavaLangStringIntBoolean(String messageKey, int version) throws ExtractorException {
		final String SIGNATURE = "createRequestGetMessageBytesJavaLangStringIntBoolean(String, int)";
		try {
			StringWriter stringWriter = new StringWriter();
			XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
			XMLStreamWriter xmlWriter = xMLOutputFactory.createXMLStreamWriter(stringWriter);

			// Add xml version and encoding to output
			xmlWriter.writeStartDocument(GlobalParameters.ENCODING, "1.0");

			// Create SOAP Envelope start element
			xmlWriter.writeStartElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_ROOT, XmlUtil.SOAP_ENV_NS);
			
			// Add namespaces to start element
			xmlWriter.writeNamespace(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_NS);
			xmlWriter.writeNamespace("urn", "urn:AdapterMessageMonitoringVi");

			// Add SOAP Body start element
			xmlWriter.writeStartElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_BODY, XmlUtil.SOAP_ENV_NS);

			xmlWriter.writeStartElement("urn", "getMessageBytesJavaLangStringIntBoolean", "urn:AdapterMessageMonitoringVi");

			// Create element: messageKey
			xmlWriter.writeStartElement("urn", "messageKey", "urn:AdapterMessageMonitoringVi");
			xmlWriter.writeCharacters(messageKey);
			xmlWriter.writeEndElement();

			// Create element: version
			xmlWriter.writeStartElement("urn", "version", "urn:AdapterMessageMonitoringVi");
			xmlWriter.writeCharacters("" + version);
			xmlWriter.writeEndElement();

			// Create element: archive
			xmlWriter.writeStartElement("urn", "archive", "urn:AdapterMessageMonitoringVi");
			xmlWriter.writeCharacters("false");
			xmlWriter.writeEndElement();

			// Close tags
			xmlWriter.writeEndElement(); // getMessageBytesJavaLangStringIntBoolean
			xmlWriter.writeEndElement(); // SOAP_ENV_BODY
			xmlWriter.writeEndElement(); // SOAP_ENV_ROOT

			// Finalize writing
			xmlWriter.flush();
			xmlWriter.close();
			
			// Write to inputstream
			ByteArrayInputStream bais = new ByteArrayInputStream(stringWriter.toString().getBytes());

			return bais;
		} catch (XMLStreamException e) {
			String msg = "Error creating request payload for messageKey: " + messageKey + " with version " + version;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);
		}
	}
	

	/**
	 * Extract messageKey from GetMessagesByIDs response.
	 * @param responseBytes
	 * @return
	 * @throws ExtractorException
	 */
	static String extractMessageKeyFromResponse(byte[] responseBytes) throws ExtractorException {
		final String SIGNATURE = "extractMessageKeyFromResponse(byte[])";
		try {
	        String messageKey = "";
	        
			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLEventReader eventReader = factory.createXMLEventReader(new ByteArrayInputStream(responseBytes));

			while (eventReader.hasNext()) {
				XMLEvent event = eventReader.nextEvent();

				switch (event.getEventType()) {
				case XMLStreamConstants.START_ELEMENT:
					String currentElementName = event.asStartElement().getName().getLocalPart();

					if ("messageKey".equals(currentElementName)) {
						messageKey = eventReader.peek().asCharacters().getData();
					}
					break;
				}
			}
			
			return messageKey;
		} catch (XMLStreamException e) {
			String msg = "Error extracting messageKey from 'GetMessagesByIDs' Web Service response.\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);
		}
	}	

	
	/**
	 * Extract message info from Web Service response (extraction is generic and used across multiple services responses)
	 * @param responseBytes					XML to extract data from 
	 * @param receiverInterfaceName			Only extract message info from integrated configurations having this receiver interface name
	 * @return
	 * @throws ExtractorException
	 */
	static MessageInfo extractMessageInfo(byte[] responseBytes, String receiverInterfaceName) throws ExtractorException {
		final String SIGNATURE = "extractMessageInfo(byte[], String)";
		try {
	        MessageInfo msgInfo = new MessageInfo();
	        String messageId = null;
	        String parentId = null;
	        String messageKey = null;
	        boolean receiverInterfaceElementFound = false;
	        boolean matchingReceiverInterfaceNameFound = false;
	        
			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLEventReader eventReader = factory.createXMLEventReader(new ByteArrayInputStream(responseBytes));

			while (eventReader.hasNext()) {
				XMLEvent event = eventReader.nextEvent();

				switch (event.getEventType()) {
				case XMLStreamConstants.START_ELEMENT:
					String currentElementName = event.asStartElement().getName().getLocalPart();

					if ("parentID".equals(currentElementName)) {
						parentId = eventReader.peek().asCharacters().getData();
						
					} else if ("messageID".equals(currentElementName)) {
						messageId = eventReader.peek().asCharacters().getData();
						
					} else if ("messageKey".equals(currentElementName)) {
						messageKey = eventReader.peek().asCharacters().getData();
												
			    	} else if ("receiverInterface".equals(currentElementName)) {
			    		// We found the correct element
			    		receiverInterfaceElementFound = true;
			    		
			    	} else if("name".equals(currentElementName) && eventReader.peek().isCharacters() && receiverInterfaceElementFound) {
			    		String name = eventReader.peek().asCharacters().getData();

			    		// REASON: In case of message split we get all interfaces in the response payload
			    		// we only want the ones matching the receiverInterfaceName of the current ICO being processed
			    		if (name.equals(receiverInterfaceName) && receiverInterfaceElementFound) {
			    			// We found a match we want to add to our "splitMessageIds" map
			    			matchingReceiverInterfaceNameFound = true;
			    			
			    			// We are no longer interested in more data before next iteration
							receiverInterfaceElementFound = false;
						}
			    	}
					break;
					
				case XMLStreamConstants.END_ELEMENT:
					String currentEndElementName = event.asEndElement().getName().getLocalPart();
					
					if ("AdapterFrameworkData".equals(currentEndElementName) && matchingReceiverInterfaceNameFound) {
						if (parentId != null) {
							msgInfo.getSplitMessageIds().put(parentId, messageId);	
						}
						msgInfo.getObjectKeys().add(messageKey);
						matchingReceiverInterfaceNameFound = false;
						parentId = null;
					}
					break;
				}
			}
			
			return msgInfo;
		} catch (XMLStreamException e) {
			String msg = "Error extracting message info from Web Service response.\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);
		}
	}
		
	
	/**
	 * Extract successors message keys from Web Service response.
	 * @param responseBytes					XML to extract data from 
	 * @param senderInterface
	 * @param receiverInterface
	 * @return								Map of <MsgKey, Parent MsgId>
	 * @throws ExtractorException
	 */
	static HashMap<String, String> extractSuccessorsBatch(byte[] responseBytes, String senderInterface, String receiverInterface) throws ExtractorException {
		final String SIGNATURE = "extractSuccessorsBatch(byte[], String, String)";
		try {
			HashMap<String, String> successors = new HashMap<String, String>();
	        String parentId = null;
	        String messageKey = null;
	        boolean hasRoot = false;
	        boolean receiverInterfaceElementFound = false;
	        boolean matchingReceiverInterfaceNameFound = false;
	        String currentSenderInterface = null;
	        
			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLEventReader eventReader = factory.createXMLEventReader(new ByteArrayInputStream(responseBytes));

			while (eventReader.hasNext()) {
				XMLEvent event = eventReader.nextEvent();

				switch (event.getEventType()) {
				case XMLStreamConstants.START_ELEMENT:
					String currentElementName = event.asStartElement().getName().getLocalPart();

					if ("AdapterFrameworkData".equals(currentElementName)) {
				        messageKey = null;
				        parentId = null;
				        matchingReceiverInterfaceNameFound = false;
				        receiverInterfaceElementFound = false;
				        hasRoot = false;

					} else if ("messageKey".equals(currentElementName)) {
						messageKey = eventReader.peek().asCharacters().getData();
						
					} else if ("parentID".equals(currentElementName)) {
						parentId = eventReader.peek().asCharacters().getData();

					} else if ("rootID".equals(currentElementName)) {
						hasRoot = true;

			    	} else if ("receiverInterface".equals(currentElementName)) {
			    		// We found the correct element
			    		receiverInterfaceElementFound = true;
			    		
			    	} else if("name".equals(currentElementName) && eventReader.peek().isCharacters() && receiverInterfaceElementFound) {
			    		String name = eventReader.peek().asCharacters().getData();
	
			    		// REASON: In case of message split we get all interfaces in the response payload
			    		// we only want the ones matching the Outbound or Inbound interfaces of the current ICO being processed.
			    		// Both outbound and inbound is required to track a FIRST message id to its related LAST messages which
			    		// is needed in a batch situation where multiple FIRST message ids are sent in the request.
			    		if ((name.equals(receiverInterface) || name.equals(senderInterface)) && receiverInterfaceElementFound) {
			    			// We found a match we want to add to our "splitMessageIds" map
			    			matchingReceiverInterfaceNameFound = true;
			    			
			    			// We are no longer interested in more data before next iteration
							receiverInterfaceElementFound = false;
							
							currentSenderInterface = eventReader.peek().asCharacters().getData();
						}
			    	}
					break;
					
				case XMLStreamConstants.END_ELEMENT:
					String currentEndElementName = event.asEndElement().getName().getLocalPart();
					
					if ("AdapterFrameworkData".equals(currentEndElementName) && matchingReceiverInterfaceNameFound) {
						
						if (hasRoot && currentSenderInterface.equals(senderInterface)) {
							// do nothing
						} else {
							successors.put(messageKey, parentId);	
						}
					}
					break;
				}
			}
			
			return successors;
		} catch (XMLStreamException e) {
			String msg = "Error extracting successors from Web Service response.\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);
		}
	}
	
	
	/**
	 * Create request message for GetMessageList
	 * @param ico
	 * @return
	 */
	static byte[] createRequestGetMessageList(IntegratedConfiguration ico) {
		final String SIGNATURE = "createGetMessageListRequest(IntegratedConfiguration)";
		try {
			final String XML_NS_URN_PREFIX	= "urn";
			final String XML_NS_URN_NS		= "urn:AdapterMessageMonitoringVi";
			final String XML_NS_URN1_PREFIX	= "urn1";
			final String XML_NS_URN1_NS		= "urn:com.sap.aii.mdt.server.adapterframework.ws";
			final String XML_NS_URN2_PREFIX	= "urn2";
			final String XML_NS_URN2_NS		= "urn:com.sap.aii.mdt.api.data";
			
			StringWriter stringWriter = new StringWriter();
			XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
			XMLStreamWriter xmlWriter = xMLOutputFactory.createXMLStreamWriter(stringWriter);

			// Add xml version and encoding to output
			xmlWriter.writeStartDocument(GlobalParameters.ENCODING, "1.0");

			// Create element: Envelope
			xmlWriter.writeStartElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_ROOT, XmlUtil.SOAP_ENV_NS);
			xmlWriter.writeNamespace(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_NS);
			xmlWriter.writeNamespace(XML_NS_URN_PREFIX, XML_NS_URN_NS);
			xmlWriter.writeNamespace(XML_NS_URN1_PREFIX, XML_NS_URN1_NS);
			xmlWriter.writeNamespace(XML_NS_URN2_PREFIX, XML_NS_URN2_NS);
			xmlWriter.writeNamespace("lang", "java/lang");

			// Create element: Envelope | Body
			xmlWriter.writeStartElement(XmlUtil.SOAP_ENV_PREFIX, XmlUtil.SOAP_ENV_BODY, XmlUtil.SOAP_ENV_NS);

			// Create element: Envelope | Body | getMessageList
			xmlWriter.writeStartElement(XML_NS_URN_PREFIX, "getMessageList", XML_NS_URN_NS);

			// Create element: Envelope | Body | getMessageList | filter
			xmlWriter.writeStartElement(XML_NS_URN_PREFIX, "filter", XML_NS_URN_NS);
			
			// Create element: Envelope | Body | getMessageList | filter | archive
			xmlWriter.writeStartElement(XML_NS_URN1_PREFIX, "archive", XML_NS_URN1_NS);
			xmlWriter.writeCharacters("false");
			xmlWriter.writeEndElement();

			// Create element: Envelope | Body | getMessageList | filter | dateType
			xmlWriter.writeStartElement(XML_NS_URN1_PREFIX, "dateType", XML_NS_URN1_NS);
			xmlWriter.writeEndElement();
			
			// Create element: Envelope | Body | getMessageList | filter | direction
			xmlWriter.writeStartElement(XML_NS_URN1_PREFIX, "direction", XML_NS_URN1_NS);
			xmlWriter.writeCharacters("OUTBOUND");
			xmlWriter.writeEndElement();
			
			// Create element: Envelope | Body | getMessageList | filter | fromTime
			if (ico.getFromTime() != null) {
				xmlWriter.writeStartElement(XML_NS_URN1_PREFIX, "fromTime", XML_NS_URN1_NS);
				xmlWriter.writeCharacters(ico.getFromTime());
				xmlWriter.writeEndElement();	
			}
			
			// Create element: Envelope | Body | getMessageList | filter | interface
			xmlWriter.writeStartElement(XML_NS_URN1_PREFIX, "interface", XML_NS_URN1_NS);

			// Create element: Envelope | Body | getMessageList | filter | interface | name
			xmlWriter.writeStartElement(XML_NS_URN2_PREFIX, "name", XML_NS_URN2_NS);
			xmlWriter.writeCharacters(ico.getReceiverInterface());
			xmlWriter.writeEndElement();
			
			// Create element: Envelope | Body | getMessageList | filter | interface | namespace
			xmlWriter.writeStartElement(XML_NS_URN2_PREFIX, "namespace", XML_NS_URN2_NS);
			xmlWriter.writeCharacters(ico.getReceiverNamespace());
			xmlWriter.writeEndElement();
			
			// Close element: Envelope | Body | getMessageList | filter | interface
			xmlWriter.writeEndElement();
			
			// Create element: Envelope | Body | getMessageList | filter | nodeId
			xmlWriter.writeStartElement(XML_NS_URN1_PREFIX, "nodeId", XML_NS_URN1_NS);
			xmlWriter.writeEndElement();
			
			// Create element: Envelope | Body | getMessageList | filter | onlyFaultyMessages
			xmlWriter.writeStartElement(XML_NS_URN1_PREFIX, "onlyFaultyMessages", XML_NS_URN1_NS);
			xmlWriter.writeCharacters("false");
			xmlWriter.writeEndElement();
			
			// Create element: Envelope | Body | getMessageList | filter | qualityOfService
//			xmlWriter.writeStartElement(XML_NS_URN1_PREFIX, "qualityOfService", XML_NS_URN1_NS);
//			xmlWriter.writeCharacters(ico.getQualityOfService());
//			xmlWriter.writeEndElement();
			
			// Create element: Envelope | Body | getMessageList | filter | receiverName
			xmlWriter.writeStartElement(XML_NS_URN1_PREFIX, "receiverName", XML_NS_URN1_NS);
			xmlWriter.writeCharacters(ico.getReceiverComponent());
			xmlWriter.writeEndElement();
			
			// Create element: Envelope | Body | getMessageList | filter | retries
			xmlWriter.writeStartElement(XML_NS_URN1_PREFIX, "retries", XML_NS_URN1_NS);
			xmlWriter.writeEndElement();
			
			// Create element: Envelope | Body | getMessageList | filter | retryInterval
			xmlWriter.writeStartElement(XML_NS_URN1_PREFIX, "retryInterval", XML_NS_URN1_NS);
			xmlWriter.writeEndElement();
			
			// Create element: Envelope | Body | getMessageList | filter | senderInterface
			xmlWriter.writeStartElement(XML_NS_URN1_PREFIX, "senderInterface", XML_NS_URN1_NS);

			// Create element: Envelope | Body | getMessageList | filter | senderInterface | name
			xmlWriter.writeStartElement(XML_NS_URN2_PREFIX, "name", XML_NS_URN2_NS);
			xmlWriter.writeCharacters(ico.getSenderInterface());
			xmlWriter.writeEndElement();
			
			// Create element: Envelope | Body | getMessageList | filter | senderInterface | namespace
			xmlWriter.writeStartElement(XML_NS_URN2_PREFIX, "namespace", XML_NS_URN2_NS);
			xmlWriter.writeCharacters(ico.getSenderNamespace());
			xmlWriter.writeEndElement();
			
			// Close element: Envelope | Body | getMessageList | filter | senderInterface
			xmlWriter.writeEndElement();
			
			// Create element: Envelope | Body | getMessageList | filter | senderName
			xmlWriter.writeStartElement(XML_NS_URN1_PREFIX, "senderName", XML_NS_URN1_NS);
			xmlWriter.writeCharacters(ico.getSenderComponent());
			xmlWriter.writeEndElement();
			
			// Create element: Envelope | Body | getMessageList | filter | status
			xmlWriter.writeStartElement(XML_NS_URN1_PREFIX, "status", XML_NS_URN1_NS);
			xmlWriter.writeCharacters("success");
			xmlWriter.writeEndElement();

			// Create element: Envelope | Body | getMessageList | filter | timesFailed
			xmlWriter.writeStartElement(XML_NS_URN1_PREFIX, "timesFailed", XML_NS_URN1_NS);
			xmlWriter.writeEndElement();
			
			// Create element: Envelope | Body | getMessageList | filter | toTime
			if (ico.getToTime() != null) {
	 			xmlWriter.writeStartElement(XML_NS_URN1_PREFIX, "toTime", XML_NS_URN1_NS);
				xmlWriter.writeCharacters(ico.getToTime());
				xmlWriter.writeEndElement();
			}
			
			// Create element: Envelope | Body | getMessageList | filter | wasEdited
			xmlWriter.writeStartElement(XML_NS_URN1_PREFIX, "wasEdited", XML_NS_URN1_NS);
			xmlWriter.writeCharacters("false");
			xmlWriter.writeEndElement();
			
			// Close element: Envelope | Body | getMessageList | filter
			xmlWriter.writeEndElement();
			
			// Create element: Envelope | Body | getMessageList | filter | maxMessages
			xmlWriter.writeStartElement(XML_NS_URN1_PREFIX, "maxMessages", XML_NS_URN1_NS);
			xmlWriter.writeCharacters("" + ico.getMaxMessages());
			xmlWriter.writeEndElement();
			
			// Close tags
			xmlWriter.writeEndElement(); // Envelope | Body | getMessageList
			xmlWriter.writeEndElement(); // Envelope | Body
			xmlWriter.writeEndElement(); // Envelope

			// Finalize writing
			xmlWriter.flush();
			xmlWriter.close();
			stringWriter.flush();
			
			return stringWriter.toString().getBytes();
		} catch (XMLStreamException e) {
			String msg = "Error creating SOAP request for GetMessageList. " + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		}
	}
	
	
	/**
	 * Create request message for GetMessagesWithSuccessors
	 * @param messageIds			List of Message IDs to get message details from. Map(key, value) = Map(original extract message id, inject message id)
	 * @return
	 */
	static byte[] createRequestGetMessagesWithSuccessors(Collection<String> messageIds) {
		final String SIGNATURE = "createRequestGetMessagesWithSuccessors(Collection<String>)";
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
	 * Create request message for GetPredecessorMessageId
	 * @param messageId			Message ID to get message details from.
	 * @return
	 */
	private static byte[] createRequestGetPredecessorMessageId(String messageId) {
		final String SIGNATURE = "createRequestGetMessagesWithSuccessors(Collection<String>)";
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

			// Create element: Envelope | Body | getPredecessorMessageId
			xmlWriter.writeStartElement(XML_NS_URN_PREFIX, "getPredecessorMessageId", XML_NS_URN_NS);

			// Create element: Envelope | Body | getPredecessorMessageId | messageId
			xmlWriter.writeStartElement(XML_NS_URN_PREFIX, "messageId", XML_NS_URN_NS);
			xmlWriter.writeCharacters(messageId);
	        xmlWriter.writeEndElement();			
	                
			// Create element: Envelope | Body | getPredecessorMessageId | direction
			xmlWriter.writeStartElement(XML_NS_URN_PREFIX, "direction", XML_NS_URN_NS);
			xmlWriter.writeCharacters("OUTBOUND");
			xmlWriter.writeEndElement();
			
			// Close tags
	        xmlWriter.writeEndElement(); // Envelope | Body | getPredecessorMessageId
			xmlWriter.writeEndElement(); // Envelope | Body
			xmlWriter.writeEndElement(); // Envelope

			// Finalize writing
			xmlWriter.flush();
			xmlWriter.close();
			stringWriter.flush();
			
			return stringWriter.toString().getBytes();
		} catch (XMLStreamException e) {
			String msg = "Error creating SOAP request for GetPredecessorMessageId. " + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		}
	}
	

	/**
	 * Create request message for GetMessagesByIDsRequest
	 * @param messageId
	 * @return
	 */
	private static byte[] createRequestGetMessagesByIDs(String messageId) {
		final String SIGNATURE = "createRequestGetMessagesByIDs(String)";
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
