package com.invixo.extraction;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Base64;

import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamSource;

import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
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
	public static final String PAYLOAD_FOUND = "Found";
	public static final String PAYLOAD_NOT_FOUND = "Not found";


	
	/*====================================================================================
	 *------------- Instance variables
	 *====================================================================================*/
	private String sapMessageKey = null;			// SAP Message Key from Web Service response of GetMessageList
	private String sapMessageId = null;				// SAP Message Id 
	private IntegratedConfiguration ico	= null;		// Integrated Configuration
	private String xiMessageInResponseFirst = "unknown";	// (First) 	Indicates if a payload/message was returned by SAP PO or not
	private String xiMessageInResponseLast	= "unknown";	// (Last) 	Indicates if a payload/message was returned by SAP PO or not
	private int payloadFilesCreated = 0;			// Total number of payload files created on file system
	
	// Target file(s)
	private String targetPathFirst = null;			// Path (no filename) to create target payload file, FIRST	
	private String targetPathLast = null;			// Path (no filename) to create target payload file, LAST
	private String fileName = null;					// File name
	
	// Error Indicator
	private Exception ex = null;					// Error details
	
	
	
	/*====================================================================================
	 *------------- Constructors
	 *====================================================================================*/
	MessageKey(IntegratedConfiguration ico, String messageKey) {
		this.ico 				= ico;
		this.sapMessageKey 		= messageKey;
		this.sapMessageId 		= extractMessageIdFromKey(messageKey);
		this.targetPathFirst 	= FileStructure.DIR_EXTRACT_OUTPUT_PRE + this.ico.getName() + "\\" + GlobalParameters.PARAM_VAL_TARGET_ENV + FileStructure.DIR_EXTRACT_OUTPUT_POST_FIRST_ENVLESS;
		this.targetPathLast 	= FileStructure.DIR_EXTRACT_OUTPUT_PRE + this.ico.getName() + "\\" + GlobalParameters.PARAM_VAL_TARGET_ENV + FileStructure.DIR_EXTRACT_OUTPUT_POST_LAST_ENVLESS;
		this.fileName 			= this.sapMessageId + FileStructure.PAYLOAD_FILE_EXTENSION;

	}
	
	
	
	/*====================================================================================
	 *------------- Getters and Setters
	 *====================================================================================*/
	public String getSapMessageKey() {
		return sapMessageKey;
	}

	public String getSapMessageId() {
		return sapMessageId;
	}

	public String getXiMessageInResponseFirst() {
		return xiMessageInResponseFirst;
	}

	public void setXiMessageInResponseFirst(String xiMessageInResponseFirst) {
		this.xiMessageInResponseFirst = xiMessageInResponseFirst;
	}

	public String getXiMessageInResponseLast() {
		return xiMessageInResponseLast;
	}

	public void setXiMessageInResponseLast(String xiMessageInResponseLast) {
		this.xiMessageInResponseLast = xiMessageInResponseLast;
	}	

	public String getTargetPathFirst() {
		return targetPathFirst;
	}

	public String getTargetPathLast() {
		return targetPathLast;
	}

	public String getFileName() {
		return fileName;
	}

	public Exception getEx() {
		return ex;
	}
	
	public void setEx(Exception e) {
		this.ex = e;
	}
	
	public int getPayloadFilesCreated() {
		return payloadFilesCreated;
	}
	
	
	
	/*====================================================================================
	 *------------- Instance methods
	 *====================================================================================*/
	/**
	 * Main entry point for processing a Message Key.
	 * Call Web Service for fetching SAP PO message data (SOAP envelope). 
	 * A normal web service response will contain an XML payload containing base64 encoded SAP XI multipart message.
	 * This method is responsible for extracting the actual payload data from the multipart message and storing the payload on file system.
	 * @param messageKey
	 * @param getFirstPayload
	 * @throws ExtractorException			Other errors during extraction
	 * @throws HttpException				Web Service call failed
	 */
	void processMessageKey(String messageKey, boolean getFirstPayload) throws ExtractorException, HttpException {
		final String SIGNATURE = "processMessageKey(String, boolean)";
		try {
			logger.writeDebug(LOCATION, SIGNATURE, "MessageKey [" + ((getFirstPayload)?"FIRST":"LAST") + "] processing started...");
			
			// Build request payload (service: getMessageBytesJavaLangStringIntBoolean)
			int version = getFirstPayload ? 0 : -1;		// 0 = FIRST, -1 = LAST
			InputStream wsRequest = createNewRequest(messageKey, version);
			logger.writeDebug(LOCATION, SIGNATURE, "Web Service request payload created for Message Key " + messageKey + " with version " + version);
			
			// Call Web Service fetching the payload
			byte[] wsResponse = HttpHandler.post(IntegratedConfiguration.ENDPOINT, GlobalParameters.CONTENT_TYPE_TEXT_XML, wsRequest.readAllBytes());
			logger.writeDebug(LOCATION, SIGNATURE, "Web Service called");
				
			// Extract the actual SAP PO payload from the Web Service response message and store it on file system
			String sapPayloadFileName = storePayload(wsResponse, getFirstPayload);
			logger.writeDebug(LOCATION, SIGNATURE, "File with SAP PO payload created: " + sapPayloadFileName);			
		} catch (IOException e) {
			String msg = "Error reading all bytes from generated web service request\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			ExtractorException ex = new ExtractorException(msg);
			this.ex = ex;
			throw ex;
		} catch (NoMsgFoundException e) {
			// Do nothing (an instance property has already been set indicating this).
		} finally {
			logger.writeDebug(LOCATION, SIGNATURE, "MessageKey [" + ((getFirstPayload)?"FIRST":"LAST") + "] processing finished...");
		}
	}

	
	/**
	 * Reads a multipart message originated from the Web Service response of service: GetMessageBytesJavaLangStringIntBoolean.
	 * This multipart message is interpreted and the SAP PO main payload extracted and stored on file system.
	 * @param content
	 * @param isFirst
	 * @return
	 * @throws NoMsgFoundException
	 * @throws ExtractorException
	 */
	private String storePayload(byte[] content, boolean isFirst) throws NoMsgFoundException, ExtractorException {
		final String SIGNATURE = "storePayload(byte[], boolean)";
		try {
			// Write GetMessageBytesJavaLangStringIntBoolean response to file system if debug for this is enabled (property)
			if (GlobalParameters.DEBUG) {
				String file = FileStructure.getDebugFileName("GetMessageBytesJavaLangStringIntBoolean", false, this.sapMessageId + (isFirst?"_FIRST":"_LAST") , "xml");
				Util.writeFileToFileSystem(file, content);
				logger.writeDebug(LOCATION, SIGNATURE, "<debug enabled> GetMessageBytesJavaLangStringIntBoolean response message is stored here: " + file);
			}
			
			// Get multipart message from XML payload contained in Web Service response XML file
			MimeMultipart mmp = getMultipartMessageFromResponse(content, isFirst);
			logger.writeDebug(LOCATION, SIGNATURE, "Multipart message generated");
			
			// Get main payload (classic SAP PO main payload) from multipart message
			BodyPart bp = mmp.getBodyPart(1);				// bodyPart(0) = SAP PO internal envelope (no payload), bodyPart(1) = Payload
			logger.writeDebug(LOCATION, SIGNATURE, "Payload fetched from multipart message");
			
			// Create target directory where the payload file is placed
			String targetDirectory = generatePayloadDirectory(isFirst);
			logger.writeDebug(LOCATION, SIGNATURE, "Target directory generated");
			
			// Store body on file system for later injection or comparison
			String fileName = targetDirectory + this.fileName;
			Util.writeFileToFileSystem(fileName, bp.getInputStream().readAllBytes());
			logger.writeDebug(LOCATION, SIGNATURE, "Payload file written to file system");
			
			// Increment counter indicating number of payload files created in total
			payloadFilesCreated++;
			
			return fileName;
		} catch (ArrayIndexOutOfBoundsException|MessagingException|IOException e) {
			String msg = "Error extracting payload from multipart message and storing it on file system\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			ExtractorException ex = new ExtractorException(msg);
			throw ex;
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
	
	
	/**
	 * Generate directory (if missing) where extracted payload files are to be placed
	 * @param getFirstPayload
	 * @return
	 */
	private String generatePayloadDirectory(boolean getFirstPayload) {
		String targetDir = null;
		if (getFirstPayload) {
			targetDir = this.targetPathFirst;
		} else {
			targetDir = this.targetPathLast;
		}
	
		// Make sure the new dynamic directory is created
		Util.createDirIfNotExists(targetDir);
		return targetDir;
	}
	
	
	/**
	 * Extract the MimeMultipart message from web service response (getMessageBytesJavaLangStringIntBoolean)
	 * @param responseBytes
	 * @param isFirst
	 * @return
	 * @throws NoMsgFoundException
	 * @throws ExtractorException
	 */
	private MimeMultipart getMultipartMessageFromResponse(byte[] responseBytes, boolean isFirst) throws NoMsgFoundException, ExtractorException {
		final String SIGNATURE = "getMultipartMessageFromResponse(byte[], boolean)";
		try {
			// Extract base64 payload
			String encodedPayload = this.extractEncodedPayload(responseBytes);

			// Check if payload was found
			if ("".equals(encodedPayload)) {
				String msg = "Web Service response contains no payload.";
				logger.writeDebug(LOCATION, SIGNATURE, "Web Service response contains no XI message.");
				if (isFirst) this.setXiMessageInResponseFirst(PAYLOAD_NOT_FOUND); else this.setXiMessageInResponseLast(PAYLOAD_NOT_FOUND);
				throw new NoMsgFoundException(msg);
			} else {
				if (isFirst) this.setXiMessageInResponseFirst(PAYLOAD_FOUND); else this.setXiMessageInResponseLast(PAYLOAD_FOUND);
				logger.writeDebug(LOCATION, SIGNATURE, "Web Service response contains XI message.");
			}
			
			// Decode base64
			byte[] decodedPayload = Base64.getMimeDecoder().decode(encodedPayload.getBytes());

			// Create multipart message from decoded base64
			DataSource ds = new ByteArrayDataSource(decodedPayload, "multipart/related");
			MimeMultipart mmp = new MimeMultipart(ds);
			
			// Return
			return mmp;			
		} catch (MessagingException e) {
			String msg = "Error extracting multipart message.\n" + e.getMessage();
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
	 * Extract SAP Message Id from Message Key.
	 * Example: a3386b2a-1383-11e9-a723-000000554e16\OUTBOUND\5590550\EO\0
	 * @param key
	 * @return
	 */
	static String extractMessageIdFromKey(String key) {
		String messageId = key.substring(0, key.indexOf("\\"));
		return messageId;
	}

}