package com.invixo.injection.reporting;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import com.invixo.consistency.FileStructure;
import com.invixo.injection.InjectionRequest;
import com.invixo.injection.IntegratedConfiguration;
import com.invixo.main.GlobalParameters;

public class ReportWriter {
	private static final String XML_PREFIX 	= "inv";
	private static final String XML_NS 		= "urn:invixo.com.inject.report";
	
	private final String REPORT_FILE = FileStructure.FILE_BASE_LOCATION + System.currentTimeMillis() +  "_InjectReport.xml";
	private int countIcoTotal = 0;	// Total number of ICOs processed
	private int countIcoErr = 0;	// Total number of ICOs processed with error
	private int countIcoOk = 0;		// Total number of ICOs processed successfully
	
	
	public void interpretResult(ArrayList<IntegratedConfiguration> icoList) {
		// Set total number of ICO's processed
		this.countIcoTotal = icoList.size();
		
		// Determine number of successfully and erroneous ICOs 
		for (IntegratedConfiguration ico : icoList) {
			if (ico.getEx() == null) {
				this.countIcoOk++;
			} else {
				this.countIcoErr++;
			}
		}
	}
	
	
	public String create(ArrayList<IntegratedConfiguration> icoList) {
		try {
			XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
			XMLStreamWriter xmlWriter = xMLOutputFactory.createXMLStreamWriter(new FileOutputStream(REPORT_FILE), GlobalParameters.ENCODING);

			// Add xml version and encoding to output
			xmlWriter.writeStartDocument(GlobalParameters.ENCODING, "1.0");

			// Create element: InjectReport
			xmlWriter.writeStartElement(XML_PREFIX, "InjectReport", XML_NS);
			xmlWriter.writeNamespace(XML_PREFIX, XML_NS);

			// Create element: InjectReport | Header
			xmlWriter.writeStartElement(XML_PREFIX, "Header", XML_NS);

			// Create element: InjectReport | Header | IcoTotal
			xmlWriter.writeStartElement(XML_PREFIX, "IcoTotal", XML_NS);
			xmlWriter.writeCharacters("" + this.countIcoTotal);
			xmlWriter.writeEndElement();

			// Create element: InjectReport | Header | IcoTotalOk
			xmlWriter.writeStartElement(XML_PREFIX, "IcoTotalOk", XML_NS);
			xmlWriter.writeCharacters("" + this.countIcoOk);
			xmlWriter.writeEndElement();

			// Create element: InjectReport | Header | IcoTotalError
			xmlWriter.writeStartElement(XML_PREFIX, "IcoTotalError", XML_NS);
			xmlWriter.writeCharacters("" + this.countIcoErr);
			xmlWriter.writeEndElement();
									
			// Close element: InjectReport | Header
			xmlWriter.writeEndElement();	
			
			// Create element: InjectReport | Details
			xmlWriter.writeStartElement(XML_PREFIX, "Details", XML_NS);
			
			// Add detail info per ICO
			for (IntegratedConfiguration ico : icoList) {
				// Create element: InjectReport | IntegratedConfiguration
				xmlWriter.writeStartElement(XML_PREFIX, "IntegratedConfiguration", XML_NS);
				
				// Create element: InjectReport | IntegratedConfiguration | Injections | info | Error
				xmlWriter.writeStartElement(XML_PREFIX, "Error", XML_NS);
				if (ico.getEx() != null) {
					StringWriter sw = new StringWriter();
					ico.getEx().printStackTrace(new PrintWriter(sw));
					xmlWriter.writeCData(sw.toString());		
				}
				xmlWriter.writeEndElement();
				
				// Create element: InjectReport | IntegratedConfiguration | Name
				xmlWriter.writeStartElement(XML_PREFIX, "Name", XML_NS);
				xmlWriter.writeCharacters(ico.getName());
				xmlWriter.writeEndElement();	

				// Create element: InjectReport | IntegratedConfiguration | File
				xmlWriter.writeStartElement(XML_PREFIX, "File", XML_NS);
				xmlWriter.writeCharacters(ico.getFileName());
				xmlWriter.writeEndElement();
				
				// Create element: InjectReport | IntegratedConfiguration | QoS
				xmlWriter.writeStartElement(XML_PREFIX, "QoS", XML_NS);
				xmlWriter.writeCharacters("" + ico.getQualityOfService());
				xmlWriter.writeEndElement();

				// Add header details
				addHeaderDetails(xmlWriter, ico);
				
				// Create element: InjectReport | IntegratedConfiguration | Injections
				xmlWriter.writeStartElement(XML_PREFIX, "Injections", XML_NS);
								
				// Build InjectionRequest list
				ArrayList<InjectionRequest> requests = ico.getInjections();
				for (InjectionRequest req : requests) {
					
					// Create element: InjectReport | IntegratedConfiguration | Injections | info | Error
					xmlWriter.writeStartElement(XML_PREFIX, "Error", XML_NS);
					if (req.getError() != null) {
						StringWriter sw = new StringWriter();
						req.getError().printStackTrace(new PrintWriter(sw));
						xmlWriter.writeCData(sw.toString());		
					}
					xmlWriter.writeEndElement();
					
					// Create element: InjectReport | IntegratedConfiguration | Injections | info
					xmlWriter.writeStartElement(XML_PREFIX, "info", XML_NS);
					
					// Create element: InjectReport | IntegratedConfiguration | Injections | info | MessageId
					xmlWriter.writeStartElement(XML_PREFIX, "MessageId", XML_NS);					
					xmlWriter.writeCharacters(req.getMessageId());
					xmlWriter.writeEndElement();		
					
					// Create element: InjectReport | IntegratedConfiguration | Injections | info | SourcePayloadFile
					xmlWriter.writeStartElement(XML_PREFIX, "SourcePayloadFile", XML_NS);					
					xmlWriter.writeCharacters(req.getSourcePayloadFile());
					xmlWriter.writeEndElement();
					
					// Create element: InjectReport | IntegratedConfiguration | Injections | info | GeneratedRequestFile
					xmlWriter.writeStartElement(XML_PREFIX, "GeneratedRequestFile", XML_NS);					
					xmlWriter.writeCharacters(req.getInjectionRequestFile());
					xmlWriter.writeEndElement();
					
					// Close element: InjectReport | IntegratedConfiguration | Injections | info
					xmlWriter.writeEndElement();
				}		

				// Close element: InjectReport | IntegratedConfiguration | Injections
				xmlWriter.writeEndElement();
				
				// Close element: InjectReport | IntegratedConfiguration
				xmlWriter.writeEndElement();					
			}
			
			// Close element: InjectReport | Details
			xmlWriter.writeEndElement();	
			
			// Close element: InjectReport
			xmlWriter.writeEndElement();

			// Finalize writing
			xmlWriter.flush();
			xmlWriter.close();

			// Return report name
			return REPORT_FILE;
		} catch (XMLStreamException|FileNotFoundException e) {
			throw new RuntimeException("Error generating report! " + e);
		} 
	}
	
	
	/**
	 * Build elements:	InjectReport | Header | Details
	 * @param xmlWriter
	 * @param ico
	 */
	private static void addHeaderDetails(XMLStreamWriter xmlWriter, IntegratedConfiguration ico) throws XMLStreamException {
		// Create element Details
		xmlWriter.writeStartElement(XML_PREFIX, "Details", XML_NS);

		// Create element: Details | senderParty
		xmlWriter.writeStartElement(XML_PREFIX, "senderParty", XML_NS);
		xmlWriter.writeCharacters(ico.getSenderParty());
		xmlWriter.writeEndElement();

		// Create element: Details | senderComponent
		xmlWriter.writeStartElement(XML_PREFIX, "senderComponent", XML_NS);
		xmlWriter.writeCharacters(ico.getSenderComponent());
		xmlWriter.writeEndElement();
		
		// Create element: Details | senderInterface
		xmlWriter.writeStartElement(XML_PREFIX, "senderInterface", XML_NS);
		xmlWriter.writeCharacters(ico.getSenderInterface());
		xmlWriter.writeEndElement();
		
		// Create element: Details | senderNamespace
		xmlWriter.writeStartElement(XML_PREFIX, "senderNamespace", XML_NS);
		xmlWriter.writeCharacters(ico.getSenderInterface());
		xmlWriter.writeEndElement();
		
		// Create element: Details | receiverParty
		xmlWriter.writeStartElement(XML_PREFIX, "receiverParty", XML_NS);
		xmlWriter.writeCharacters(ico.getReceiverParty());
		xmlWriter.writeEndElement();
		
		// Create element: Details | receiverComponent
		xmlWriter.writeStartElement(XML_PREFIX, "receiverComponent", XML_NS);
		xmlWriter.writeCharacters(ico.getReceiverComponent());
		xmlWriter.writeEndElement();
		
		// Create element: Details | qualityOfService
		xmlWriter.writeStartElement(XML_PREFIX, "qualityOfService", XML_NS);
		xmlWriter.writeCharacters(ico.getQualityOfService());
		xmlWriter.writeEndElement();
		
		// Close element: Details
		xmlWriter.writeEndElement();
	}
}
