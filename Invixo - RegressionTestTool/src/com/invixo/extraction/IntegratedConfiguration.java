package com.invixo.extraction;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

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
import com.invixo.common.util.WebServiceHandler;
import com.invixo.common.util.XmlUtil;
import com.invixo.consistency.FileStructure;
import com.invixo.main.GlobalParameters;

public class IntegratedConfiguration extends IntegratedConfigurationMain {
	/*====================================================================================
	 *------------- Class variables
	 *====================================================================================*/
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = IntegratedConfiguration.class.getName();
	public static final String ENDPOINT = GlobalParameters.SAP_PO_HTTP_HOST_AND_PORT + PropertyAccessor.getProperty("SERVICE_PATH_EXTRACT");


	
	/*====================================================================================
	 *------------- Instance variables
	 *====================================================================================*/
	private HashSet<String> responseMessageKeys = new HashSet<String>();		// MessageKey IDs returned by Web Service GetMessageList
	private ArrayList<MessageKey> messageKeys = new ArrayList<MessageKey>();	// List of MessageKeys created/processed
	
	
	
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
	 */
	public void startExtraction() {
		final String SIGNATURE = "startExtraction()";
		try {
			logger.writeDebug(LOCATION, SIGNATURE, "*********** (" + this.internalObjectId + ") Start processing ICO request file: " + this.fileName);
			
			// Housekeeping: Delete old ICO extract data
			if (GlobalParameters.PARAM_VAL_ALLOW_SAME_ENV) {
				// Do not delete data (this is a special test parameter)
				logger.writeDebug(LOCATION, SIGNATURE, "Deletion of existing files in Extract output directory disabled due to program parameter.");
			} else {
				// Delete data
				deleteOldRunData();				
			}
			
			// Check: execution can take place in 2 modes: 
			// 1) init (first extraction of data from a given system environment (DEV, TST, PRD))
			// 2) non-init (extract injected messages, so that a comparison can be made at later stage)
			if (Boolean.parseBoolean(GlobalParameters.PARAM_VAL_EXTRACT_MODE_INIT)) {
				// Extract whatever data is in SAP PO matching the ICO
				extractModeInit();
			} else {
				// Extract only Message IDs previously injected for ICO 
				extractModeNonInit();
			}
		} catch (ExtractorException|GeneralException e) {
			this.ex = e;
		} finally {
			logger.writeDebug(LOCATION, SIGNATURE, "*********** Finished processing ICO request file");
		}
	}
	
	
	/**
	 * Makes sure all old run data for target environment is deleted before a new run.
	 */
	private void deleteOldRunData() {
		final String SIGNATURE = "deleteOldRunData()";
		try {       
			// Build output directory path
			String outputDirWithIcoName = FileStructure.DIR_EXTRACT_OUTPUT_PRE + "\\" + this.getName();
			
			// Cleanup: delete all files contained in "Extract Output" for current ico. Only done for sub-directories part of the specified target environment
			deletePayloadFiles(outputDirWithIcoName, GlobalParameters.PARAM_VAL_TARGET_ENV);
			logger.writeDebug(LOCATION, SIGNATURE, "Housekeeping: all old payload files deleted from: " + outputDirWithIcoName + " for environment: " + GlobalParameters.PARAM_VAL_TARGET_ENV);
		} catch (Exception e) {
			String ex = "Housekeeping terminated with error! " + e;
			logger.writeError(LOCATION, SIGNATURE, ex);
			throw new RuntimeException(e);
		}       
	}


	private void deletePayloadFiles(String rootDirectory, String environment) {
		// Create pathMatcher which will match all files and directories (in the world of this tool, only files) that
		// are located in FIRST or LAST directories for the specified environment.
		String pattern = "^(?=.*\\\\" + environment + "\\\\.*\\\\.*\\\\.*\\\\).*$";
		PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("regex:" + pattern);
		
		// Find all matches to above regex starting from the specified DIR
		try (Stream<Path> paths = Files.find(Paths.get(rootDirectory), 100, (path, f)->pathMatcher.matches(path))) {
			// Delete all matches
			paths.forEach(path -> {
				try {
					Files.delete(path);
				} catch (IOException e) {
					throw new RuntimeException("*deletePayloadFiles* Error deleting file '" + path + "'\n" + e);
				}
			});
		} catch (IOException e) {
			throw new RuntimeException("*deletePayloadFiles* Error finding files." + e);
		}	
	}


	/**
	 * Method extracts messages from SAP PO.
	 * The messages extracted are those that has already been injected previously by this tool.
	 * List of Message IDs previously injected is contained in the Message Mapping Id file.
	 * NB: messages resulting from a Message Split is also extracted.
	 * @throws ExtractorException
	 */
	private void extractModeNonInit() throws ExtractorException, GeneralException {
		final String SIGNATURE = "extractModeNonInit()";
		try {
			// Get list of Message IDs to be extracted.
			// NB: these Message IDs are taken from the Message ID Mapping file (this was created during injection)
	        Map<String, String> messageIdMap = Util.getMessageIdsFromFile(FileStructure.FILE_MSG_ID_MAPPING, GlobalParameters.FILE_DELIMITER, this.getName(), 1, 2);
	        logger.writeDebug(LOCATION, SIGNATURE, "Number of entries (matching ICO) fetched from Message Id Mapping file: " + messageIdMap.size());
			
	        // Split and process map in batches
	        Map<String, String> currentBatch = new HashMap<String, String>();
	        for (Entry<String, String> entry : messageIdMap.entrySet()) {
	        	// Process batches if max batch size is reached
	        	if (currentBatch.size() >= 100) {
	        		logger.writeDebug(LOCATION, SIGNATURE, "Batch size reached. Current batch is being processed...");
	        		processNonInitInBatch(currentBatch);
	        		currentBatch.clear();
	        	}
	        	
	        	// Add current entry to batch
	        	currentBatch.put(entry.getKey(), entry.getValue());
	        }
	        
	        // Process remaining/leftover maps
        	logger.writeDebug(LOCATION, SIGNATURE, "Batch leftovers to be processed: " + currentBatch.size());
        	if (currentBatch.size() > 0) {
    	        processNonInitInBatch(currentBatch);        		
        	}
		} catch (IllegalStateException|IOException e) {
			String msg = "Error reading Message Id Map file: " + FileStructure.FILE_MSG_ID_MAPPING + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new ExtractorException(msg);			
		}
	}


	/**
	 * Extract data (LAST payloads) for a single non-init batch run.
	 * In non-init mode data extracted comes from the Message ID mapping file generated during injection.
	 * As some ICOs may split messages, this needs to be handled, since split messages are given a new Message ID by SAP at runtime.
	 * This means that a Message ID in the Message ID mapping file cannot be used to get LAST payloads for split messages.
	 * As such all Message IDs in the Message Mapping file must be checked to see if they are in fact 'Parent ID' to other messages.
	 * The Web Service 'GetMessagesWithSuccessors' is used to determine this, since it returns details for the message itself along with 
	 * any messages spawned (split) by it (messages that the message is parent to).
	 * @param messageIdMap					Message Id map build from Message Id mapping file created during injection.
	 * @throws ExtractorException
	 */
	private void processNonInitInBatch(Map<String, String> messageIdMap) throws ExtractorException, GeneralException {
		final String SIGNATURE = "processNonInitInBatch(Map<String, String>)";
		// Create request for GetMessagesWithSuccessors
		byte[] requestBytes = createGetMessagesWithSuccessors(this, messageIdMap);
		logger.writeDebug(LOCATION, SIGNATURE, "GetMessagesWithSuccessors request created");
		
		// Write request to file system if debug for this is enabled (property)
		if (GlobalParameters.DEBUG) {
			String file = FileStructure.getDebugFileName("GetMessagesWithSuccessors", true, this.getName(), "xml");
			Util.writeFileToFileSystem(file, requestBytes);
			logger.writeDebug(LOCATION, SIGNATURE, "<debug enabled> GetMessagesWithSuccessors request message to be sent to SAP PO is stored here: " + file);
		}
					
		// Call web service (GetMessagesWithSuccessors)
		byte[] responseBytes = WebServiceHandler.post(ENDPOINT, GlobalParameters.CONTENT_TYPE_TEXT_XML, requestBytes);
		logger.writeDebug(LOCATION, SIGNATURE, "Web Service (GetMessagesWithSuccessors) called");

		// Extract message info from Web Service response
		MessageInfo msgInfo = extractMessageInfo(responseBytes, this.getReceiverInterfaceName());
		
		// Correct Message Mapping Id's in file. This is a special situation. 
		// EXPLANATION: If an extract is made (after an injection) for an ICO performing message split, then 
		// the Message Id Mapping file must be corrected for existing entries made during injection.
		// This is because during a message split, SAP assigns the split interfaces new message ID's. 
		// These new interfaces will have a Parent Message Id matching the original request (inject ID).
		// For this reason the Message ID Mapping must be corrected, so that the new, SAP Message IDs generated by SAP
		// for split cases is replaced with the original "Parent ID's" from the injection.
		// If this is not done then the Message ID Mapping file will not make sense and cannot be used during Comparison.
		correctMessageMappingFile(msgInfo.getSplitMessageIds());
			
		// Set list of Message Keys to be extracted. This list is consist of all MessageKeys extracted from WS response matching 
		// the ICO receiver interface at hand. Should any of the MessageKeys be a parent Message, then the key is replaced with 
		// the Message ID from the split message.
		this.responseMessageKeys = buildListOfMessageIdsToBeExtracted(msgInfo.getObjectKeys(), msgInfo.getSplitMessageIds());
		logger.writeDebug(LOCATION, SIGNATURE, "Number of MessageKeys to be extracted: " + this.responseMessageKeys.size());
		
		// Process extracted message keys
		processMessageKeysMultiple(this.responseMessageKeys, this.internalObjectId);		
	}


	/**
	 * Build a list of message IDs to be extracted.
	 * This method adds all message keys to the result list. IFF any of these message keys have a parent id (= message split), then
	 * the message ID part of the message key is replaced (parent id replaced with message id):
	 * 		Parent ID 	= inject message ID
	 * 		Message ID 	= for a split message, this is the new message id created by SAP during message split.
	 * @param objectKeys
	 * @param splitMessageIds
	 * @return
	 */
	public static HashSet<String> buildListOfMessageIdsToBeExtracted(HashSet<String> objectKeys, HashMap<String, String> splitMessageIds) {
		HashSet<String> result = new HashSet<String>();
		
		for (Object messageKey : objectKeys.toArray()) {
			String currentMsgKey = messageKey.toString(); 
			
			for (Entry<String, String> entry : splitMessageIds.entrySet()) {
				// Check if current Message Key should be replaced
				if (currentMsgKey.contains(entry.getKey())) {
					String newKey = currentMsgKey.replace(entry.getKey(), entry.getValue());
					result.add(newKey);
					break;
				}				
			}
			result.add(currentMsgKey);
		}
		
		// Set list of message IDs to fetch LAST payloads from
		return result;
	}


	/**
	 * Method extracts messages from SAP PO.
	 * The messages extracted are which ever messages present in SAP PO matching the requests made by this tool.
	 * @throws ExtractorException
	 */
	private void extractModeInit() throws ExtractorException, GeneralException {
		final String SIGNATURE = "extractModeInit()";
		
		// Create request for GetMessageList
		byte[] requestBytes = createGetMessageListRequest(this);
		logger.writeDebug(LOCATION, SIGNATURE, "GetMessageList request created");
		
		// Write request to file system if debug for this is enabled (property)
		if (GlobalParameters.DEBUG) {
			String file = FileStructure.getDebugFileName("GetMessageList", true, this.getName(), "xml");
			Util.writeFileToFileSystem(file, requestBytes);
			logger.writeDebug(LOCATION, SIGNATURE, "<debug enabled> GetMessageList request message to be sent to SAP PO is stored here: " + file);
		}
					
		// Call web service (GetMessageList)
		byte[] responseBytes = WebServiceHandler.post(ENDPOINT, GlobalParameters.CONTENT_TYPE_TEXT_XML, requestBytes);
		logger.writeDebug(LOCATION, SIGNATURE, "Web Service (GetMessageList) called");
			
		// Extract MessageKeys from web Service response
		MessageInfo msgInfo = extractMessageInfo(responseBytes, this.getReceiverInterfaceName());
		
		// Set MessageKeys from web Service response
		this.responseMessageKeys = msgInfo.getObjectKeys();
		logger.writeDebug(LOCATION, SIGNATURE, "Number of MessageKeys contained in Web Service response: " + this.responseMessageKeys.size());
		
		// Process extracted message keys
		processMessageKeysMultiple(this.responseMessageKeys, this.internalObjectId);
	}


	/**
	 * Extract payloads for a list of SAP Message Keys and store these on the file system.
	 * @param messageKeys					List of SAP Message Keys to be processed.
	 * @param internalObjectId				Internal counter. Used to track which MessageKey number is being processed in the log.
	 * @throws ExtractorException
	 */
	private void processMessageKeysMultiple(HashSet<String> messageKeys, int internalObjectId) throws ExtractorException {
		final String SIGNATURE = "processMessageKeysMultiple(HashSet<String>, int)";
		
		// For each MessageKey fetch payloads (first and/or last)
		int counter = 1;
		for (String key : messageKeys) {
			// Process a single Message Key
			logger.writeDebug(LOCATION, SIGNATURE, "-----> [ICO " + internalObjectId + "], [MSG KEY " + counter + "] MessageKey processing started for key: " + key);
			this.processMessageKeySingle(key);
			logger.writeDebug(LOCATION, SIGNATURE, "-----> [ICO " + internalObjectId + "], [MSG KEY " + counter + "] MessageKey processing finished");
			counter++;
		}
	}


	/**
	 * Replace parent message ids with actual message ids in Message Id Mapping file.
	 * NB: Parent ID is ONLY EVAR exiting in response in the case of a message split for async messages.
	 * 
	 * This method updates/modifies the Message Mapping file.
	 * @param messageIds
	 * @throws ExtractorException
	 */
	private void correctMessageMappingFile(HashMap<String, String> messageIds) throws ExtractorException {
    	// Check: skip processing as message split is not relevant
        if (qualityOfService.equals("BE")) {
        	return;
        }
        
        // Modify relevant, existing entries in Message Mapping file
        updateMessageIdMappingFile(messageIds);
	}


	private void updateMessageIdMappingFile(HashMap<String, String> updatedMessageIds) {
		final String SIGNATURE = "updateMessageIdMappingFile(HashMap<String, String>)";
		String messageId;
		String parentId;
		File file = new File(FileStructure.FILE_MSG_ID_MAPPING);
		
		// Update Message Mapping file
		if (updatedMessageIds.size() > 0 && file.exists()) {
			String content = new String(Util.readFile(FileStructure.FILE_MSG_ID_MAPPING));
			for (HashMap.Entry<String, String> entry : updatedMessageIds.entrySet()) {
				parentId = entry.getKey();
				messageId = entry.getValue();
				logger.writeDebug(LOCATION, SIGNATURE, "Message split scenario found!");
				logger.writeDebug(LOCATION, SIGNATURE, "Key: " + parentId + " will be replaced with key: " + messageId);
				content = content.replace(entry.getKey(), entry.getValue());
			}

			logger.writeDebug(LOCATION, SIGNATURE, "Changed file will be written to: " + FileStructure.FILE_MSG_ID_MAPPING);
			Util.writeFileToFileSystem(FileStructure.FILE_MSG_ID_MAPPING, content.getBytes());
		}
	}

	
	/**
	 * Extract message info from Web Service response (extraction is generic and used across multiple services responses)
	 * @param responseBytes					XML to extract data from 
	 * @param receiverInterfaceName			Only extract message info from integrated configurations having this receiver interface name
	 * @return
	 * @throws ExtractorException
	 */
	public static MessageInfo extractMessageInfo(byte[] responseBytes, String receiverInterfaceName) throws ExtractorException {
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
	 * Processes a single MessageKey returned in Web Service response for service GetMessageList.
	 * This involves calling service GetMessageBytesJavaLangStringIntBoolean to fetch actual payload and storing 
	 * this on file system.
	 * This method can/will generate both FIRST and LAST payload if requested.
	 * @param key
	 */
	private void processMessageKeySingle(String key) {
		MessageKey msgKey = null;
		try {
			// Create a new MessageKey object
			msgKey = new MessageKey(this, key);
			
			// Attach a reference to newly created MessageKey object to this ICO
			this.messageKeys.add(msgKey);
			
			// Fetch payload: FIRST
			if (Boolean.parseBoolean(GlobalParameters.PARAM_VAL_EXTRACT_MODE_INIT)) {
				msgKey.processMessageKey(key, true);
			}
			
			// Fetch payload: LAST
			msgKey.processMessageKey(key, false);			
		} catch (ExtractorException|GeneralException e) {
			if (msgKey != null) {
				msgKey.setEx(e);
			}
		}
	}

	
	
	/*====================================================================================
	 *------------- Class methods
	 *====================================================================================*/
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


	/**
	 * Create request message for GetMessagesWithSuccessors
	 * @param ico					Integration Configuration
	 * @param messageIdMap			List of Message IDs to get message details from. Map(key, value) = Map(original extract message id, inject message id)
	 * @return
	 */
	public static byte[] createGetMessagesWithSuccessors(IntegratedConfiguration ico, Map<String, String> messageIdMap) {
		final String SIGNATURE = "createGetMessagesWithSuccessors(IntegratedConfiguration, Map<String, String>)";
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
	        for (Map.Entry<String, String> entry : messageIdMap.entrySet()) {
				String injectMessageId = entry.getValue();
				
				// Create element: Envelope | Body | getMessagesWithSuccessors | messageIds | String
				xmlWriter.writeStartElement(XML_NS_LANG_PREFIX, "String", XML_NS_LANG_NS);				
				xmlWriter.writeCharacters(injectMessageId);
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
	
}
