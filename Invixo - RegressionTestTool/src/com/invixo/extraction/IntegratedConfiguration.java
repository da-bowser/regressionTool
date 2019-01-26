package com.invixo.extraction;

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

import com.invixo.common.GeneralException;
import com.invixo.common.IntegratedConfigurationMain;
import com.invixo.common.util.Logger;
import com.invixo.common.util.PropertyAccessor;
import com.invixo.common.util.Util;
import com.invixo.common.util.XmlUtil;
import com.invixo.consistency.FileStructure;
import com.invixo.extraction.webServices.WebServiceHandler;
import com.invixo.main.GlobalParameters;


public class IntegratedConfiguration extends IntegratedConfigurationMain {
	/*====================================================================================
	 *------------- Class variables
	 *====================================================================================*/
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = IntegratedConfiguration.class.getName();	

	// Indicators: should payloads (FIRST and/or LAST) be extracted or not
	public static final boolean EXTRACT_FIRST_PAYLOAD = Boolean.parseBoolean(PropertyAccessor.getProperty("EXTRACT_FIRST_PAYLOAD"));
	public static final boolean EXTRACT_LAST_PAYLOAD = Boolean.parseBoolean(PropertyAccessor.getProperty("EXTRACT_LAST_PAYLOAD"));
		
	// Debugging
	private static final boolean LOG_GET_MSG_REQ = Boolean.parseBoolean(PropertyAccessor.getProperty("LOG_GET_MSG_REQUEST"));
	private static final String LOG_GET_MSG_REQ_PATH = PropertyAccessor.getProperty("LOG_GET_MSG_REQUEST_PATH");
	
	
	/*====================================================================================
	 *------------- Instance variables
	 *====================================================================================*/
	private ArrayList<String> responseMessageKeys = new ArrayList<String>();	// MessageKey IDs returned by Web Service GetMessageList
	private ArrayList<MessageKey> messageKeys = new ArrayList<MessageKey>();	// List of MessageKeys created/processed

	
	
	/*====================================================================================
	 *------------- Main (for testing purposes)
	 *====================================================================================*/	
	public static void main(String[] args) throws GeneralException {
		// Test GetMessageList request creation
		String file = "c:\\Users\\dhek\\Desktop\\_beginning_\\_Extract\\Input\\Integrated Configurations\\Varemodtagelse - Sys_P_WMS_ODENSE oa_GoodsNotification_ODE_to_Sys_PRD_011_ia_GoodsReceipt.xml";
		String mappingFile = "c:\\Users\\dhek\\Desktop\\_beginning_\\Config\\systemMapping.txt";
		IntegratedConfiguration ico = new IntegratedConfiguration(file, mappingFile, "PRD", "TST");
		createGetMessageListRequest(ico);
	}
	
	
	
	/*====================================================================================
	 *------------- Constructors
	 *====================================================================================*/
	public IntegratedConfiguration(String icoFileName) throws GeneralException {
		super(icoFileName);
	}

	
	public IntegratedConfiguration(String icoFileName, String mapfilePath, String sourceEnv, String targetEnv) throws GeneralException {
		super(icoFileName, mapfilePath, sourceEnv, targetEnv);
	}
	
	
	
	/*====================================================================================
	 *------------- Getters and Setters
	 *====================================================================================*/
	public ArrayList<MessageKey> getMessageKeys() {
		return this.messageKeys;
	}

		
	
	/*====================================================================================
	 *------------- Instance methods
	 *====================================================================================*/
	/**
	 * Process a single Integrated Configuration object.
	 * This also includes all MessageKeys related to this object.
	 * @param file
	 */
	public void startExtraction() {
		final String SIGNATURE = "startExtraction(String)";
		try {
			logger.writeDebug(LOCATION, SIGNATURE, "*********** Start processing ICO request file: " + this.fileName);
					
			// Create request for GetMessageList
			byte[] requestBytes = createGetMessageListRequest(this);
			logger.writeDebug(LOCATION, SIGNATURE, "GetMessageList request created");
			
			// Write request to file system if debug for this is enabled (property)
			if (LOG_GET_MSG_REQ) {
				FileStructure.createDirIfNotExists(LOG_GET_MSG_REQ_PATH);
				String file = LOG_GET_MSG_REQ_PATH + "GetMessageListReq_" + this.getName() + "_" + System.currentTimeMillis() + ".xml";
				Util.writeFileToFileSystem(file, requestBytes);
				logger.writeDebug(LOCATION, SIGNATURE, "<debug enabled> GetMessageList request message to be sent to SAP PO is stored here: " + file);
			}
						
			// Call web service (GetMessageList)
			InputStream responseBytes = WebServiceHandler.callWebService(requestBytes);
			logger.writeDebug(LOCATION, SIGNATURE, "Web Service (GetMessageList) called");
			
			// Extract MessageKeys from web Service response
			this.responseMessageKeys = extractMessageKeysFromSingleResponseFile(responseBytes);
			logger.writeDebug(LOCATION, SIGNATURE, "Number of MessageKeys contained in Web Service response: " + this.responseMessageKeys.size());
			
			// For each MessageKey fetch payloads (first and last)
			int counter = 1;
			for (String key : this.responseMessageKeys) {
				// Process a single Message Key
				logger.writeDebug(LOCATION, SIGNATURE, "-----> (" + counter + ") MessageKey processing started for key: " + key);
				this.processSingleMessageKey(key);
				logger.writeDebug(LOCATION, SIGNATURE, "-----> (" + counter + ") MessageKey processing finished");
				counter++;
			}	
		} catch (ExtractorException e) {
			this.ex = e;
		} finally {
			logger.writeDebug(LOCATION, SIGNATURE, "*********** Finished processing ICO request file");
		}
	}
	
	
	/**
	 * Processes a single MessageKey returned in Web Service response for service GetMessageList.
	 * This involves calling service GetMessageBytesJavaLangStringIntBoolean to fetch actual payload and storing 
	 * this on file system.
	 * This method can/will generate both FIRST and LAST payload if requested.
	 * @param key
	 * @throws ExtractorException
	 */
	private void processSingleMessageKey(String key) throws ExtractorException {
		final String SIGNATURE = "processSingleMessageKey(String)";
		MessageKey msgKey = null;
		try {
			// Create a new MessageKey object
			msgKey = new MessageKey(this, key);
			
			// Attach a reference to newly created MessageKey object to this ICO
			this.messageKeys.add(msgKey);
			
			// Fetch payload: FIRST
			if (EXTRACT_FIRST_PAYLOAD) {
				msgKey.processMessageKey(key, true);
				logger.writeDebug(LOCATION, SIGNATURE, "MessageKey processing finished for FIRST payload");
			}
			
			// Fetch payload: LAST
			if (EXTRACT_LAST_PAYLOAD) {
				msgKey.processMessageKey(key, false);
				logger.writeDebug(LOCATION, SIGNATURE, "MessageKey processing finished for LAST payload");
			}			
		} catch (ExtractorException e) {
			if (msgKey != null) {
				msgKey.setEx(e);
			}
		}
	}

	
	
	/*====================================================================================
	 *------------- Class methods
	 *====================================================================================*/
	/**
	 * Get list of 'messageKey' contained in a single response file.
	 * @param file
	 * @return
	 * @throws ExtractorException
	 */
	private static ArrayList<String> extractMessageKeysFromSingleResponseFile(InputStream responseBytes) throws ExtractorException {
		final String SIGNATURE = "extractMessageKeysFromSingleResponseFile(InputStream)";
		ArrayList<String> objectKeys = new ArrayList<String>();
		
		try {
			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLEventReader eventReader = factory.createXMLEventReader(responseBytes);
			
			while (eventReader.hasNext()) {
			    XMLEvent event = eventReader.nextEvent();
			    
			    switch(event.getEventType()) {
			    case XMLStreamConstants.START_ELEMENT:
			    	String currentElementName = event.asStartElement().getName().getLocalPart();
			    	if ("messageKey".equals(currentElementName)) {
			    		objectKeys.add(eventReader.peek().asCharacters().getData());
			    	}
			    }
			}
			
			return objectKeys;
		} catch (XMLStreamException e) {
			String msg = "Error extracting MessageKeys.\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);
		} 
	}
	
	
	/**
	 * Create request message for GetMessageList
	 * @param ico
	 * @return
	 */
	public static byte[] createGetMessageListRequest(IntegratedConfiguration ico) {
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
			if (ico.getFetchFromTime() != null) {
				xmlWriter.writeStartElement(XML_NS_URN1_PREFIX, "fromTime", XML_NS_URN1_NS);
				xmlWriter.writeCharacters(ico.getFetchFromTime());
				xmlWriter.writeEndElement();	
			}
			
			// Create element: Envelope | Body | getMessageList | filter | interface
			xmlWriter.writeStartElement(XML_NS_URN1_PREFIX, "interface", XML_NS_URN1_NS);

			// Create element: Envelope | Body | getMessageList | filter | interface | name
			xmlWriter.writeStartElement(XML_NS_URN2_PREFIX, "name", XML_NS_URN2_NS);
			xmlWriter.writeCharacters(ico.getReceiverInterfaceName());
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
			xmlWriter.writeStartElement(XML_NS_URN1_PREFIX, "qualityOfService", XML_NS_URN1_NS);
			xmlWriter.writeCharacters(ico.getQualityOfService());
			xmlWriter.writeEndElement();
			
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
			if (ico.getFetchToTime() != null) {
	 			xmlWriter.writeStartElement(XML_NS_URN1_PREFIX, "toTime", XML_NS_URN1_NS);
				xmlWriter.writeCharacters(ico.getFetchToTime());
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

}
