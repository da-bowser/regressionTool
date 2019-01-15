package com.invixo.messageExtractor.blocks;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import com.invixo.common.util.Util;
import com.invixo.common.util.XmlUtil;
import com.invixo.messageExtractor.httpHandlers.HGetMessageBytesJavaLangStringIntBoolean;
import com.invixo.messageExtractor.util.Logger;
import com.invixo.messageExtractor.util.PropertyAccessor;

public class BGetMessageBytesJavaLangStringIntBoolean {
	private static int fileNameCounter 		= 1;
	private static Logger logger 			= Logger.getInstance();
	private static final String LOCATION 	= BGetMessageBytesJavaLangStringIntBoolean.class.getName();
	private static final String ENCODING 	= PropertyAccessor.getProperty("ENCODING");
	
	
	public static void main(String[] args) {
		try {
			// Test creation of a new request message
			InputStream is = createNewRequest("a3386b2a-1383-11e9-a723-000000554e16\\OUTBOUND\\5590550\\EO\\0\\", 0);
			System.out.println("\nRequest payload created: \n" + new String(is.readAllBytes()));			
		} catch (Exception e) {
			System.err.println(e);
		}
	}
	
	
	/**
	 * Create a new XML Request based on the specified parameters matching Web Service method GetMessageBytesJavaLangStringIntBoolean. 
	 * @param messageKey
	 * @param version
	 * @return
	 * @throws Exception
	 */
	public static ByteArrayInputStream createNewRequest(String messageKey, int version) throws Exception {
		String SIGNATURE = "createNewRequest(String, int)";
		
		try {
			StringWriter stringWriter = new StringWriter();
			XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
			XMLStreamWriter xmlWriter = xMLOutputFactory.createXMLStreamWriter(stringWriter);

			// Add xml version and encoding to output
			xmlWriter.writeStartDocument(ENCODING, "1.0");

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
			logger.writeDebug(LOCATION, SIGNATURE, "Request payload created for " + messageKey + " with version " + version);
			
			return bais;
		} catch (Exception e) {
			String msg = "Error creating request payload for messageKey: " + messageKey + " with version " + version;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		}
	}

	
	/**
	 * Call Web Service for fetching SAP PO message data (SOAP envelope). Based on the message data the SAP PO payload is 
	 * extracted. Both the raw web service response and the extracted SAP PO payload is stored on file system.
	 * @param messageKey
	 * @param getFirstPayload
	 * @throws Exception
	 */
	public static void processSingleMessageKey(String messageKey, boolean getFirstPayload) throws Exception {
		String SIGNATURE = "processSingleMessageKey(String, boolean)";
		
		// Build request payload
		InputStream requestFirst = createNewRequest(messageKey, (getFirstPayload?0:-1));
		
		// Call Web Service
		InputStream response = HGetMessageBytesJavaLangStringIntBoolean.invoke(requestFirst.readAllBytes());
		
		// Write Web Service response to file system
		String rawMultipartFileName = HGetMessageBytesJavaLangStringIntBoolean.DIR_RESPONSE + getFileName(messageKey, getFirstPayload);
		Util.writeFileToFileSystem(rawMultipartFileName, response.readAllBytes());
		
		// Log success
		logger.writeDebug(LOCATION, SIGNATURE, "	# File with response payload (multipart) created: " + rawMultipartFileName);
		
		// Extract the actual SAP PO payload from the Multipart message and store it on file system also
		String sapPayloadFileName = BMultipartHandler.processSingle(rawMultipartFileName);
		
		// Log success
		logger.writeDebug(LOCATION, SIGNATURE, "	# File with SAP PO payload created: " + sapPayloadFileName);
	}
	
	
	/**
	 * 
	 * @param messageKey				Format: cb8d39f4-1386-11e9-bbd4-000000554e16\OUTBOUND\5590550\BE\0\
	 * @param isFirst
	 * @return
	 */
	public static synchronized String getFileName(String messageKey, boolean isFirst) {
		String modifiedMessageKey = messageKey.replaceAll("\\\\", "_");
		String fileName = String.format("%07d", fileNameCounter) + "_payload_" + modifiedMessageKey + (isFirst?"FIRST":"LAST") + ".xml";
		fileNameCounter++;
		return fileName;
	}
	
}
