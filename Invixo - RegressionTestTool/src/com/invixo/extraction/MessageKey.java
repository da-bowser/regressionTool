package com.invixo.extraction;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamSource;

import com.invixo.common.util.Logger;
import com.invixo.common.util.PropertyAccessor;
import com.invixo.common.util.Util;
import com.invixo.common.GeneralException;
import com.invixo.common.Payload;
import com.invixo.common.PayloadException;
import com.invixo.common.StateHandler;
import com.invixo.common.util.HttpException;
import com.invixo.common.util.HttpHandler;
import com.invixo.common.util.XmlUtil;
import com.invixo.consistency.FileStructure;
import com.invixo.main.GlobalParameters;

public class MessageKey {
	/*====================================================================================
	 *------------- Class variables
	 *====================================================================================*/
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = MessageKey.class.getName();	
	
	static final String ENDPOINT = GlobalParameters.SAP_PO_HTTP_HOST_AND_PORT + PropertyAccessor.getProperty("SERVICE_PATH_EXTRACT");


	
	/*====================================================================================
	 *------------- Instance variables
	 *====================================================================================*/
	private String sapMessageKey = null;			// SAP Message Key from Web Service response of GetMessageList
	private String sapMessageId = null;				// SAP Message Id 
	private IntegratedConfiguration ico	= null;		// Integrated Configuration
	private Payload payloadFirst = new Payload(); 	// FIRST payload
	private Payload payloadLast = new Payload();	// LAST payload
	private ArrayList<String> multiMapMessageKeys;	// List of Parent Message Keys in the case of Multimapping scenario
	private Exception ex = null;					// Error details
	
	
	
	/*====================================================================================
	 *------------- Constructors
	 *====================================================================================*/
	MessageKey(IntegratedConfiguration ico, String messageKey) {
		this.ico = ico;
		this.setSapMessageKey(messageKey);
		this.setSapMessageId(messageKey);
		
		if (this.ico.isUsingMultiMapping()) {
			this.multiMapMessageKeys = new ArrayList<String>();
		}
	}



	/*====================================================================================
	 *------------- Getters and Setters
	 *====================================================================================*/
	public String getSapMessageKey() {
		return sapMessageKey;
	}
	
	public void setSapMessageKey(String sapMessageKey) {
		this.sapMessageKey = sapMessageKey;
	}
	
	public String getSapMessageId() {
		return sapMessageId;
	}
	
	public void setSapMessageId(String sapMessageKey) {
		this.sapMessageId = Util.extractMessageIdFromKey(sapMessageKey);
	}

	public Exception getEx() {
		return ex;
	}
	
	public void setEx(Exception e) {
		this.ex = e;
	}
	
	public ArrayList<String> getMultiMapMessageKeys() {
		return multiMapMessageKeys;
	}
	
	public Payload getPayloadFirst() {
		return payloadFirst;
	}

	public Payload getPayloadLast() {
		return payloadLast;
	}

	
	
	/*====================================================================================
	 *------------- Instance methods
	 *====================================================================================*/
	
	/**
	 * Main entry point
	 * Extract FIRST and/or LAST payload.
	 * @param messageKey
	 * @throws ExtractorException
	 */
	void extractAllPayloads(String messageKey) throws ExtractorException {
		// Extract FIRST payload
		if (Boolean.parseBoolean(GlobalParameters.PARAM_VAL_EXTRACT_MODE_INIT)) {
			this.payloadFirst = this.extractFirstPayload(messageKey);
		}
			
		// Extract LAST payload
		this.payloadLast = this.extractLastPayload(messageKey);
	}
	
	
	void storeState(Payload first, Payload last) throws ExtractorException {
		final String SIGNATURE = "storeState(Payload, Payload)";
		try {
			// Persist message: FIRST
			first.persistMessage(this.ico.getFilePathFirstPayloads());
			
			// Persist message: LAST
			last.persistMessage(this.ico.getFilePathLastPayloads());
			
			// Update PayloadStateOverview
			String newEntry = StateHandler.createExtractEntry(this.ico.getName(), first, last);
			StateHandler.writeEntry(newEntry);
		} catch (GeneralException|PayloadException e) {
			String msg = "Error saving state for MessageKey!\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			ExtractorException ex = new ExtractorException(msg);
			this.ex = ex;
			throw ex;
		}
	}
	
	
	/**
	 * Extract FIRST or LAST payload for ICOs with a mapping multiplicitiy of 1:1.
	 * Call Web Service for fetching SAP PO message data (SOAP envelope). 
	 * A normal web service response will contain an XML payload containing base64 encoded SAP XI multipart message.
	 * This method is responsible for creating a Payload object.
	 * @param messageKey
	 * @param isFirst
	 * @return
	 * @throws ExtractorException			Other errors during extraction
	 * @throws HttpException				Web Service call failed
	 * @throws PayloadException				Error setting state on Payload
	 */
	private Payload extractPayload(String messageKey, boolean isFirst) throws ExtractorException, HttpException, PayloadException {
		final String SIGNATURE = "extractPayload(String, boolean)";
		try {
			logger.writeDebug(LOCATION, SIGNATURE, "MessageKey [" + (isFirst?"FIRST":"LAST") + "] processing started...");
			
			// Build request payload (service: getMessageBytesJavaLangStringIntBoolean)
			int version = isFirst ? 0 : -1;		// 0 = FIRST, -1 = LAST
			InputStream wsRequest = createNewRequest(messageKey, version);
			logger.writeDebug(LOCATION, SIGNATURE, "Web Service request payload created for Message Key " + messageKey + " with version " + version);
			
			// Call Web Service fetching the payload
			byte[] wsResponse = HttpHandler.post(IntegratedConfiguration.ENDPOINT, GlobalParameters.CONTENT_TYPE_TEXT_XML, wsRequest.readAllBytes());
			logger.writeDebug(LOCATION, SIGNATURE, "Web Service called");

			// Extract base64 encoded message from Web Service response
			String base64EncodedMessage = this.extractEncodedPayload(wsResponse);

			// Create Payload object
			Payload payload = new Payload();
			payload.setSapMessageKey(messageKey);
			
			// Check if payload was found
			if ("".equals(base64EncodedMessage)) {
				logger.writeDebug(LOCATION, SIGNATURE, "Web Service response contains no XI message.");
				payload.setPayloadFoundStatus(Payload.STATUS.NOT_FOUND);
			} else {
				logger.writeDebug(LOCATION, SIGNATURE, "Web Service response contains XI message.");
				payload.setPayloadFoundStatus(Payload.STATUS.FOUND);
				payload.setMultipartBase64Bytes(base64EncodedMessage);
			}
						
			return payload;
		} catch (IOException e) {
			String msg = "Error reading all bytes from generated web service request\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			ExtractorException ex = new ExtractorException(msg);
			this.ex = ex;
			throw ex;
		} finally {
			logger.writeDebug(LOCATION, SIGNATURE, "MessageKey [" + (isFirst?"FIRST":"LAST") + "] processing finished...");
		}
	}
	
	
	/**
	 * Create a new XML Request based on the specified parameters matching Web Service method GetMessageBytesJavaLangStringIntBoolean. 
	 * @param messageKey
	 * @param version
	 * @return
	 * @throws ExtractorException
	 */
	ByteArrayInputStream createNewRequest(String messageKey, int version) throws ExtractorException {
		final String SIGNATURE = "createNewRequest(String, int)";
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
		
	
	private String extractEncodedPayload(byte[] fileContent) throws ExtractorException {
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
	 * Extract original FIRST message from PO of a MultiMapping interface (1:n multiplicity).
	 * @param payload
	 * @return
	 * @throws ExtractorException
	 */
	Payload processMessageKeyMultiMapping(Payload payload) throws ExtractorException, PayloadException {
		final String SIGNATURE = "processMessageKeyMultiMapping(Payload)";
		try {
			logger.writeDebug(LOCATION, SIGNATURE, "MessageKey [FIRST] MultiMapping processing start");
			
			// Add messageId to collection before creating request
			List<String> msgIdList = Arrays.asList(payload.getSapMessageId());
			
			// Create "GetMessagesWithSuccessors" request
			byte[] getMessagesWithSuccessorsRequestBytes = XmlUtil.createGetMessagesWithSuccessorsRequest(msgIdList);
			logger.writeDebug(LOCATION, SIGNATURE, "GetMessagesWithSuccessors request created");
			
			// Write request to file system if debug for this is enabled (property)
			if (GlobalParameters.DEBUG) {
				String file = FileStructure.getDebugFileName("GetMessagesWithSuccessors", true, ico.getName(), "xml");
				Util.writeFileToFileSystem(file, getMessagesWithSuccessorsRequestBytes);
				logger.writeDebug(LOCATION, SIGNATURE, "<debug enabled> MultiMapping scenario: GetMessagesWithSuccessors request message to be sent to SAP PO is stored here: " + file);
			}
						
			// Call web service (GetMessagesWithSuccessors)
			byte[] getMessagesWithSuccessorsResponseBytes = HttpHandler.post(ENDPOINT, GlobalParameters.CONTENT_TYPE_TEXT_XML, getMessagesWithSuccessorsRequestBytes);
			logger.writeDebug(LOCATION, SIGNATURE, "Web Service (GetMessagesWithSuccessors) called");
			
			// Extract parentId from response
			String parentId = extractParentIdsFromResponse(getMessagesWithSuccessorsResponseBytes);
			
			// Create "GetMessagesByIDs" request
			byte[] getMessageByIdsRequestBytes = XmlUtil.createGetMessagesByIDsRequest(parentId);
			
			// Write request to file system if debug for this is enabled (property)
			if (GlobalParameters.DEBUG) {
				String file = FileStructure.getDebugFileName("GetMessagesByIDs", true, ico.getName(), "xml");
				Util.writeFileToFileSystem(file, getMessageByIdsRequestBytes);
				logger.writeDebug(LOCATION, SIGNATURE, "<debug enabled> MultiMapping scenario: GetMessagesByIDs request message to be sent to SAP PO is stored here: " + file);
			}
			
			// Call web service (GetMessagesByIDs)
			byte[] getMessageByIdsResponseBytes = HttpHandler.post(ENDPOINT, GlobalParameters.CONTENT_TYPE_TEXT_XML, getMessageByIdsRequestBytes);
			logger.writeDebug(LOCATION, SIGNATURE, "Web Service (GetMessagesByIDs) called");
			
			// Extract messageKey from response
			String messageKey = extractMessageKeyFromResponse(getMessageByIdsResponseBytes);
			
			// Prevent processing an storing the same messageKey several times and ensure payloadFilesCreated consistency
			if (ico.getMultiMapMessageKeys().contains(messageKey)) {
				// MessageKey already processed and FIRST message is found
				// Do nothing
			} else {
				// Fetch FIRST payload using the original FIRST messageKey
				payload =  this.extractPayload(messageKey, true);
			}
			
			// Return;
			return payload;
		} catch (HttpException e) {
			String msg = "Error during web service call\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			ExtractorException ex = new ExtractorException(msg);
			this.ex = ex;
			throw ex;
		} finally {		
			logger.writeDebug(LOCATION, SIGNATURE, "MessageKey [FIRST] MultiMapping processing finished...");
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
	 * Extract parentId from getMessagesWithSuccessors response.
	 * @param responseBytes
	 * @return
	 * @throws ExtractorException
	 */
	static String extractParentIdsFromResponse(byte[] responseBytes) throws ExtractorException {
		final String SIGNATURE = "extractParentIdsFromResponse(byte[])";
		try {
	        String parentId = "";
	        
			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLEventReader eventReader = factory.createXMLEventReader(new ByteArrayInputStream(responseBytes));

			while (eventReader.hasNext()) {
				XMLEvent event = eventReader.nextEvent();

				switch (event.getEventType()) {
				case XMLStreamConstants.START_ELEMENT:
					String currentElementName = event.asStartElement().getName().getLocalPart();

					if ("parentID".equals(currentElementName)) {
						parentId = eventReader.peek().asCharacters().getData();
					}
					break;
				}
			}
			
			// Return parentId found in response
			return parentId;
		} catch (XMLStreamException e) {
			String msg = "Error extracting parentIds from 'GetMessagesWithSuccessors' Web Service response.\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);
		}
	}
	

	/**
	 * Extract FIRST payload.
	 * @param key
	 * @return
	 * @throws PayloadException
	 * @throws ExtractorException 
	 */
	private Payload extractFirstPayload(String key) throws ExtractorException {
		final String SIGNATURE = "extractFirstPayload(String)";
		try {
			Payload payload = new Payload();
			payload.setSapMessageKey(key);
			
			// Process according to multiplicity
			if (this.ico.isUsingMultiMapping()) {
				// Fetch payload: FIRST for multimapping interface (1:n multiplicity)
				payload = this.processMessageKeyMultiMapping(payload);
				
				// Save key to make sure it is only used once, as one FIRST messageKey can create multiple LAST
				this.getMultiMapMessageKeys().add(payload.getSapMessageId());
			} else {
				// Fetch payload: FIRST for non-multimapping interface (1:1 multiplicity)	
				payload = this.extractPayload(key, true);
			}
			
			return payload;
		} catch (PayloadException|ExtractorException|HttpException e) {
			this.setEx(e);
			String msg = "Error processing FIRST key: " + key + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);
		}
	}

	
	/**
	 * Extract LAST payload.
	 * @param key
	 * @returns
	 * @throws ExtractorException 
	 */
	private Payload extractLastPayload(String key) throws ExtractorException {
		final String SIGNATURE = "extractLastPayload(String)";
		try {
			// Fetch payload: LAST
			Payload payload = extractPayload(key, false);
			return payload;
		} catch (PayloadException|ExtractorException|HttpException e) {
			this.setEx(e);
			String msg = "Error processing LAST key: " + key + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);
		}
	}
	
	
//	private void setMessageStatus(boolean isFirst, String status) {
//		if (isFirst) {
//			this.payloadsFirst.setPayloadFoundStatus(status);
//		} else {
//			this.payloadsLast.setPayloadFoundStatus(status);
//		}
//	}

}