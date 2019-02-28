package com.invixo.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamSource;


public class XiMessageUtil {

	public static MimeMultipart createMultiPartMessage(byte[] bytes) throws MessagingException {
		// Create multipart message from decoded base64
		DataSource ds = new ByteArrayDataSource(bytes, "multipart/related");
		MimeMultipart mmp = new MimeMultipart(ds);
		return mmp;
	}
	
	
	public static BodyPart getHeaderFromMultiPartMessage(Multipart multiPartMessage) throws MessagingException {
		BodyPart bp = multiPartMessage.getBodyPart(0);		// bodyPart(0) = SAP PO internal envelope (no payload), bodyPart(1) = Payload
		return bp;
	}
	
	
	public static BodyPart getPayloadFromMultiPartMessage(Multipart multiPartMessage) throws MessagingException {
		BodyPart bp = multiPartMessage.getBodyPart(1);		// bodyPart(0) = SAP PO internal envelope (no payload), bodyPart(1) = Payload
		return bp;
	}
	
	
	public static byte[] getPayloadBytesFromMultiPartMessage(Multipart multiPartMessage) throws IOException, MessagingException {
		BodyPart bp = getPayloadFromMultiPartMessage(multiPartMessage);
		return bp.getInputStream().readAllBytes();
	}
	
	
	public static byte[] getPayloadBytesFromMultiPart(byte[] multipartBytes) throws IOException, MessagingException {
		Multipart mmp = createMultiPartMessage(multipartBytes);
		byte[] payload = getPayloadBytesFromMultiPartMessage(mmp);
		return payload;
	}
	
	
	public static XiHeader deserializeXiHeader(Multipart multiPartMessage) throws IOException, MessagingException, XMLStreamException {
		// Get SAP XI Header from MultiPart
		InputStream multipartHeaderStream = getHeaderFromMultiPartMessage(multiPartMessage).getInputStream();
			
		// Prepare
		XMLInputFactory factory = XMLInputFactory.newInstance();
		StreamSource ss = new StreamSource(multipartHeaderStream);
		XMLEventReader eventReader = factory.createXMLEventReader(ss);
		
		// Parse XML file and extract data
		XiHeader header = new XiHeader();
		XiDynConfRecord dynConfRecord = null;
		boolean fetchDynamicConfiguration = false;
		while (eventReader.hasNext()) {
		    XMLEvent event = eventReader.nextEvent();
		    
		    switch(event.getEventType()) {
		    case XMLStreamConstants.START_ELEMENT:
		    	String currentStartElementName = event.asStartElement().getName().getLocalPart().toString();

				// QualityOfService
				if ("QualityOfService".equals(currentStartElementName)) {
					header.setQualityOfService(eventReader.peek().asCharacters().getData());

				// ProcessingMode
		    	} else if ("ProcessingMode".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
		    		header.setProcessingMode(eventReader.peek().asCharacters().getData());
				
		    	// MessageId
		    	} else if ("MessageId".equals(currentStartElementName) && eventReader.peek().isCharacters()) {
		    		header.setMessageId(eventReader.peek().asCharacters().getData());

		    	// DynamicConfiguration
		    	} else if ("DynamicConfiguration".equals(currentStartElementName)) {
		    		fetchDynamicConfiguration = true;
				
				// DynamicConfiguration Record
		    	} else if (fetchDynamicConfiguration && "Record".equals(currentStartElementName)) {
		    		dynConfRecord = new XiDynConfRecord();
		    		
		    		// Get attribute values
		    		Iterator<Attribute> iterator = event.asStartElement().getAttributes();
		            while (iterator.hasNext())
		            {
		                Attribute attribute = iterator.next();
		                String name = attribute.getName().toString();
		                if ("namespace".equals(name)) {
		                	dynConfRecord.setNamespace(attribute.getValue());
		                } else if ("name".equals(name)) {
		                	dynConfRecord.setName(attribute.getValue());
		                }
		            }
		            
	            	// Add element value
		            if (eventReader.peek().isCharacters()) {
			            dynConfRecord.setValue(eventReader.peek().asCharacters().getData());
					}
		            
		            // Add Dynamic Configuration Record to header
		            header.getDynamicConfList().add(dynConfRecord);
		    	}
		    	break;
		    				    	
		    case XMLStreamConstants.END_ELEMENT:
		    	String currentEndElementName = event.asEndElement().getName().getLocalPart().toString();

		    	if ("DynamicConfiguration".equals(currentEndElementName)) {
		    		fetchDynamicConfiguration = false;		    		
		    	}
		    	break;
		    }
		}
		
		return header;
	}
	
}
