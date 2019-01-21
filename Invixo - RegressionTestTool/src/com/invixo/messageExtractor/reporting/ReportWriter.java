package com.invixo.messageExtractor.reporting;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import com.invixo.consistency.FileStructure;
import com.invixo.main.GlobalParameters;
import com.invixo.messageExtractor.main.IntegratedConfiguration;
import com.invixo.messageExtractor.main.MessageKey;

public class ReportWriter {
	private static final String XML_PREFIX 	= "inv";
	private static final String XML_NS 		= "urn:invixo.com.extract.report";
	
	private final String REPORT_FILE = FileStructure.FILE_BASE_LOCATION + System.currentTimeMillis() +  "_Report.xml";
	private int countIcoTotal = 0;	// Total number of ICOs processed
	private int countIcoErr = 0;	// Total number of ICOs processed with error
	private int countIcoOk = 0;		// Total number of ICOs processed successfully
	
	private boolean fetchPayloadFirst = false;
	private boolean fetchPayloadLast = false;
	
	
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
		
		// Detemmine if FIRST / LAST payload where to be extracted
		this.fetchPayloadFirst = IntegratedConfiguration.EXTRACT_FIRST_PAYLOAD;
		this.fetchPayloadLast = IntegratedConfiguration.EXTRACT_LAST_PAYLOAD;
	}
	
	
	public void create(ArrayList<IntegratedConfiguration> icoList) {
		try {
			XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
			XMLStreamWriter xmlWriter = xMLOutputFactory.createXMLStreamWriter(new FileOutputStream(REPORT_FILE), GlobalParameters.ENCODING);

			// Add xml version and encoding to output
			xmlWriter.writeStartDocument(GlobalParameters.ENCODING, "1.0");

			// Create element: ExtractReport
			xmlWriter.writeStartElement(XML_PREFIX, "ExtractReport", XML_NS);
			xmlWriter.writeNamespace(XML_PREFIX, XML_NS);

			// Create element: ExtractReport | Header
			xmlWriter.writeStartElement(XML_PREFIX, "Header", XML_NS);

			// Create element: ExtractReport | Header | IcoTotal
			xmlWriter.writeStartElement(XML_PREFIX, "IcoTotal", XML_NS);
			xmlWriter.writeCharacters("" + this.countIcoTotal);
			xmlWriter.writeEndElement();

			// Create element: ExtractReport | Header | IcoTotalOk
			xmlWriter.writeStartElement(XML_PREFIX, "IcoTotalOk", XML_NS);
			xmlWriter.writeCharacters("" + this.countIcoOk);
			xmlWriter.writeEndElement();

			// Create element: ExtractReport | Header | IcoTotalError
			xmlWriter.writeStartElement(XML_PREFIX, "IcoTotalError", XML_NS);
			xmlWriter.writeCharacters("" + this.countIcoErr);
			xmlWriter.writeEndElement();
			
			// Create element: ExtractReport | Header | FetchPayLoadFirst
			xmlWriter.writeStartElement(XML_PREFIX, "FetchPayLoadFirst", XML_NS);
			xmlWriter.writeCharacters("" + this.fetchPayloadFirst);
			xmlWriter.writeEndElement();			

			// Create element: ExtractReport | Header | FetchPayLoadLast
			xmlWriter.writeStartElement(XML_PREFIX, "FetchPayLoadLast", XML_NS);
			xmlWriter.writeCharacters("" + this.fetchPayloadLast);
			xmlWriter.writeEndElement();			
			
			// Close element: ExtractReport | Header
			xmlWriter.writeEndElement();	
			
			// Create element: ExtractReport | Details
			xmlWriter.writeStartElement(XML_PREFIX, "Details", XML_NS);
			
			// Add detail info per ICO
			for (IntegratedConfiguration ico : icoList) {
				// Create element: ExtractReport | IntegratedConfiguration
				xmlWriter.writeStartElement(XML_PREFIX, "IntegratedConfiguration", XML_NS);
				
				// Create element: ExtractReport | IntegratedConfiguration | Name
				xmlWriter.writeStartElement(XML_PREFIX, "Name", XML_NS);
				xmlWriter.writeCharacters(ico.getName());
				xmlWriter.writeEndElement();	

				// Create element: ExtractReport | IntegratedConfiguration | File
				xmlWriter.writeStartElement(XML_PREFIX, "File", XML_NS);
				xmlWriter.writeCharacters(ico.getFileName());
				xmlWriter.writeEndElement();
				
				// Create element: ExtractReport | IntegratedConfiguration | QoS
				xmlWriter.writeStartElement(XML_PREFIX, "QoS", XML_NS);
				xmlWriter.writeCharacters("" + ico.getQualityOfService());
				xmlWriter.writeEndElement();

				// Create element: ExtractReport | IntegratedConfiguration | MessageKeys
				xmlWriter.writeStartElement(XML_PREFIX, "MessageKeys", XML_NS);
				
				// Create element: ExtractReport | IntegratedConfiguration | Max
				xmlWriter.writeStartElement(XML_PREFIX, "Max", XML_NS);
				xmlWriter.writeCharacters("" + ico.getMaxMessagesToFetch());
				xmlWriter.writeEndElement();				

				// Create element: ExtractReport | IntegratedConfiguration | Actual
				xmlWriter.writeStartElement(XML_PREFIX, "Actual", XML_NS);
				xmlWriter.writeCharacters("" + ico.getMessageKeys().size());
				xmlWriter.writeEndElement();

				// Build MessageKey list
				ArrayList<MessageKey> keys = ico.getMessageKeys();
				for (MessageKey key : keys) {
					// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | List
					xmlWriter.writeStartElement(XML_PREFIX, "List", XML_NS);
					
					// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | List | Key
					xmlWriter.writeStartElement(XML_PREFIX, "Key", XML_NS);					
					xmlWriter.writeCharacters(key.getSapMessageKey());
					xmlWriter.writeEndElement();		
					
					// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | List | PathToPayloadFirst
					xmlWriter.writeStartElement(XML_PREFIX, "PathToPayloadFirst", XML_NS);					
					xmlWriter.writeCharacters(key.getTargetPathFirst());
					xmlWriter.writeEndElement();
					
					// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | List | PathToPayloadLast
					xmlWriter.writeStartElement(XML_PREFIX, "PathToPayloadLast", XML_NS);					
					xmlWriter.writeCharacters(key.getTargetPathLast());
					xmlWriter.writeEndElement();
					
					// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | List | FileName
					xmlWriter.writeStartElement(XML_PREFIX, "FileName", XML_NS);					
					xmlWriter.writeCharacters(key.getFileName());
					xmlWriter.writeEndElement();
					
					// Close element: ExtractReport | IntegratedConfiguration | MessageKeys | List
					xmlWriter.writeEndElement();
				}		

				// Close element: ExtractReport | IntegratedConfiguration | MessageKeys
				xmlWriter.writeEndElement();
				
				// Close element: ExtractReport | IntegratedConfiguration
				xmlWriter.writeEndElement();					
			}
			
			// Close element: ExtractReport | Details
			xmlWriter.writeEndElement();	
			
			// Close element: ExtractReport
			xmlWriter.writeEndElement();

			// Finalize writing
			xmlWriter.flush();
			xmlWriter.close();

		} catch (XMLStreamException|FileNotFoundException e) {
			throw new RuntimeException("Error generating report! " + e);
		}	
	}
}
