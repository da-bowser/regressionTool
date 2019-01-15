package com.invixo.messageExtractor.blocks;

import java.io.ByteArrayInputStream;
import java.util.Base64;

import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamSource;

import com.invixo.common.util.Util;
import com.invixo.messageExtractor.util.Logger;
import com.invixo.messageExtractor.util.PropertyAccessor;

public class BMultipartHandler {
	private static Logger logger 						= Logger.getInstance();
	private static final String LOCATION 				= BMultipartHandler.class.getName();
	private static final String FILE_BASE_LOCATION 		= PropertyAccessor.getProperty("BASE_DIRECTORY");					// Base directory
	private static final String DIR_EXTRACTED_PAYLOAD 	= FILE_BASE_LOCATION + "EXTRACTS\\";
	
	
	public static void main(String[] args) {
		try {
			// Test processing of single file
			String file = FILE_BASE_LOCATION + "Test\\GetMessageBytesJavaLangStringIntBoolean\\Responses\\0000001_payload_50dcdec7-157c-11e9-a97d-000000554e16_OUTBOUND_5590550_BE_0_FIRST.xml";
			String fileName = processSingle(file);
			System.out.println("File created: " + fileName);
		} catch (Exception e) {
			System.err.println(e);
		}
	}

	
	/**
	 * Reads a multipart message originated from the Web Service response of service: GetMessageBytesJavaLangStringIntBoolean.
	 * This multipart message is interpreted and the SAP PO main payload extracted and stored on file system.
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public static String processSingle(String file) throws Exception {
		// Get content of XML payload file
		byte[] content = Util.readFile(file);
		
		// Get multipart message from XML payload contained in response XML file
		MimeMultipart mmp = getMultipartMessageFromResponse(content);

		// Get SAP Message Id from Envelope (most likely useful for comparison and when storing the payload with msg id in the file name)
		String messageId = getSapMessageIdFromMessage(mmp.getBodyPart(0));		// bodyPart(0) = SAP PO internal envelope
		
		// Get main payload (classic SAP PO main payload) from multipart message
		BodyPart bp = mmp.getBodyPart(1);	// 0 = SAP PO internal envelope (no payload), 1 = payload / body

		// Store body on file system for later injection or comparison
		String fileName = DIR_EXTRACTED_PAYLOAD + messageId + ".xml";
		Util.writeFileToFileSystem(fileName, bp.getInputStream().readAllBytes());
		
		return fileName;
	}
	
	
	/**
	 * Extract the MimeMultipart message from web service response (getMessageBytesJavaLangStringIntBoolean)
	 * @param responseBytes
	 * @return
	 */
	private static MimeMultipart getMultipartMessageFromResponse(byte[] responseBytes) {
		String SIGNATURE = "getMultipartMessageFromResponse(bute[])";
		try {
			// Extract base64 payload
			String encodedPayload = extractEncodedPayload(responseBytes);

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
			throw new RuntimeException(msg);
		}
	}
	
	
	/**
	 * Extract SAP Message Id from a SOAP XI/PO Envelope
	 * @param envelope
	 * @return
	 */
	private static String getSapMessageIdFromMessage(BodyPart envelope) {
		String SIGNATURE = "getSapMessageIdFromMessage(BodyPart)";
		try {
			String messageId = null;
			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLEventReader eventReader = factory.createXMLEventReader(envelope.getInputStream());
			
			boolean fetchData = false;
			while (eventReader.hasNext()) {
			    XMLEvent event = eventReader.nextEvent();
			    
			    switch(event.getEventType()) {
			    case XMLStreamConstants.START_ELEMENT:
			    	String currentElementName = event.asStartElement().getName().toString();
			    	if ("{http://sap.com/xi/XI/Message/30}Main".equals(currentElementName)) {
			    		// This is the structure containing the MessageId of interest
			    		fetchData = true;
			    	} else if (fetchData && "{http://sap.com/xi/XI/Message/30}MessageId".equals(currentElementName)) {
			    		// Extract message id
			    		messageId = eventReader.peek().asCharacters().getData();
			    		
			    		// Ensure we do not waste time fetching more data
			    		fetchData = false;
			    	}
			    	break;
			    }
			}
			return messageId;
		} catch (Exception e) {
			String msg = "Error extracting SAP message Id from envelope.\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		} 
	}
	
	
	private static String extractEncodedPayload(byte[] fileContent) {
		String SIGNATURE = "extractEncodedPayload(byte[])";
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
		} catch (Exception e) {
			String msg = "Error extracting encoded (base64) payload from response.\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		} 
	}

}
