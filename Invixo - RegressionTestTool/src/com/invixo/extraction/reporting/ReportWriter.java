package com.invixo.extraction.reporting;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import com.invixo.consistency.FileStructure;
import com.invixo.extraction.IntegratedConfiguration;
import com.invixo.extraction.MessageKey;
import com.invixo.main.GlobalParameters;

public class ReportWriter {
	private static final String	XML_PREFIX = "inv";
	private static final String	XML_NS = "urn:invixo.com.extract.report";

	private final String REPORT_FILE = FileStructure.DIR_REPORTS + "ExtractReport_" + System.currentTimeMillis() + ".xml";
	private ArrayList<IntegratedConfiguration>	icoList;

	// ICO general
	private int	countIcoTotal = 0;					// Total number of ICOs processed
	private int	countIcoErr = 0;					// Total number of ICOs processed with error
	private int	countIcoOk = 0;						// Total number of ICOs processed successfully
	private boolean	fetchPayloadFirst = false;
	private boolean	fetchPayloadLast = false;

	// MessageKey general
	private int	countMsgKeyTotal		= 0;
	private int	countMsgKeyErr			= 0;
	private int	countMsgKeyOk			= 0;
	private int	countMsgKeyNopayload	= 0;


	public ReportWriter(ArrayList<IntegratedConfiguration> icoList) {
		this.icoList = icoList;

		// Interpret general data
		this.evaluateGeneralResults();
	}


	public void evaluateGeneralResults() {
		// Set total number of ICO's processed
		this.countIcoTotal = icoList.size();

		// Determine number of successfully and erroneous ICOs
		for (IntegratedConfiguration ico : icoList) {
			if (ico.getEx() == null) {
				this.countIcoOk++;

				// Evaluate MessageKeys processed
				evaluateMessageKeys(ico);
			} else {
				this.countIcoErr++;
			}
		}

		// Determine if FIRST / LAST payload where to be extracted
		this.fetchPayloadFirst = IntegratedConfiguration.EXTRACT_FIRST_PAYLOAD;
		this.fetchPayloadLast = IntegratedConfiguration.EXTRACT_LAST_PAYLOAD;
	}


	private void evaluateMessageKeys(IntegratedConfiguration ico) {
		// Set total number of MessageKeys processed
		this.countMsgKeyTotal = ico.getMessageKeys().size();

		// Determine number of successfully and erroneous MessageKeys
		for (MessageKey key : ico.getMessageKeys()) {
			if (key.getEx() == null) {
				if (MessageKey.PAYLOAD_FOUND.equals(key.getXiMessageInResponse())) {
					// No exception occurred and a XI message was returned by Web Service.
					// All is good
					this.countMsgKeyOk++;
				} else {
					// No exception occurred, but an XI message was not returned by Web Service
					this.countMsgKeyNopayload++;
				}
			} else {
				this.countMsgKeyErr++;
			}
		}
	}


	public String create(ArrayList<IntegratedConfiguration> icoList) {
		try {
			XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
			XMLStreamWriter xmlWriter = xMLOutputFactory.createXMLStreamWriter(new FileOutputStream(REPORT_FILE), GlobalParameters.ENCODING);

			// Add xml version and encoding to output
			xmlWriter.writeStartDocument(GlobalParameters.ENCODING, "1.0");

			// Create element: ExtractReport
			xmlWriter.writeStartElement(XML_PREFIX, "ExtractReport", XML_NS);
			xmlWriter.writeNamespace(XML_PREFIX, XML_NS);

			// Add structure: ExtractReport | Overview
			addIntegrationConfigurationOverview(xmlWriter);

			// Create element: ExtractReport | Details
			xmlWriter.writeStartElement(XML_PREFIX, "Details", XML_NS);

			// Add list: ExtractReport | Details | IntegratedConfiguration
			addIcoDetails(icoList, xmlWriter);

			// Close element: ExtractReport | Details
			xmlWriter.writeEndElement();

			// Close element: ExtractReport
			xmlWriter.writeEndElement();

			// Finalize writing
			xmlWriter.flush();
			xmlWriter.close();

			// Return report name
			return REPORT_FILE;
		} catch (XMLStreamException | FileNotFoundException e) {
			throw new RuntimeException("Error generating report! " + e);
		}
	}


	private void addIntegrationConfigurationOverview(XMLStreamWriter xmlWriter) throws XMLStreamException {
		// Create element: ExtractReport | Overview
		xmlWriter.writeStartElement(XML_PREFIX, "Overview", XML_NS);

		// Create element: ExtractReport | Overview | Total
		xmlWriter.writeStartElement(XML_PREFIX, "Total", XML_NS);
		xmlWriter.writeCharacters("" + this.countIcoTotal);
		xmlWriter.writeEndElement();

		// Create element: ExtractReport | Overview | Success
		xmlWriter.writeStartElement(XML_PREFIX, "Success", XML_NS);
		xmlWriter.writeCharacters("" + this.countIcoOk);
		xmlWriter.writeEndElement();

		// Create element: ExtractReport | Overview | Error
		xmlWriter.writeStartElement(XML_PREFIX, "Error", XML_NS);
		xmlWriter.writeCharacters("" + this.countIcoErr);
		xmlWriter.writeEndElement();

		// Create element: ExtractReport | Overview | FetchPayLoadFirst
		xmlWriter.writeStartElement(XML_PREFIX, "FetchPayLoadFirst", XML_NS);
		xmlWriter.writeCharacters("" + this.fetchPayloadFirst);
		xmlWriter.writeEndElement();

		// Create element: ExtractReport | Overview | FetchPayLoadLast
		xmlWriter.writeStartElement(XML_PREFIX, "FetchPayLoadLast", XML_NS);
		xmlWriter.writeCharacters("" + this.fetchPayloadLast);
		xmlWriter.writeEndElement();

		// Close element: ExtractReport | Overview
		xmlWriter.writeEndElement();
	}


	private void addIcoDetails(ArrayList<IntegratedConfiguration> icoList,
			XMLStreamWriter xmlWriter) throws XMLStreamException {
		// Add detail info per ICO
		for (IntegratedConfiguration ico : icoList) {
			// Create element: ExtractReport | IntegratedConfiguration
			xmlWriter.writeStartElement(XML_PREFIX, "IntegratedConfiguration",
					XML_NS);

			// Create element: ExtractReport | IntegratedConfiguration | Error
			xmlWriter.writeStartElement(XML_PREFIX, "Error", XML_NS);
			if (ico.getEx() != null) {
				StringWriter sw = new StringWriter();
				ico.getEx().printStackTrace(new PrintWriter(sw));
				xmlWriter.writeCData(sw.toString());
			}
			xmlWriter.writeEndElement();

			// Create element: ExtractReport | IntegratedConfiguration | Name
			xmlWriter.writeStartElement(XML_PREFIX, "Name", XML_NS);
			xmlWriter.writeCharacters(ico.getName());
			xmlWriter.writeEndElement();

			// Create element: ExtractReport | IntegratedConfiguration | File
			xmlWriter.writeStartElement(XML_PREFIX, "File", XML_NS);
			xmlWriter.writeCharacters(ico.getFileName());
			xmlWriter.writeEndElement();

			// Add structure: ExtractReport | IntegratedConfiguration | Details
			addHeaderDetails(xmlWriter, ico);

			// Add structure: ExtractReport | IntegratedConfiguration | MessageKeys
			addMessageKeysStructure(xmlWriter, ico);

			// Close element: ExtractReport | IntegratedConfiguration
			xmlWriter.writeEndElement();
		}
	}


	private void addMessageKeysStructure(XMLStreamWriter xmlWriter,	IntegratedConfiguration ico) throws XMLStreamException {
		// Create element: ExtractReport | IntegratedConfiguration | MessageKeys
		xmlWriter.writeStartElement(XML_PREFIX, "MessageKeys", XML_NS);

		// Add structure: ExtractReport | IntegratedConfiguration | MessageKeys | Overview
		addMessageKeysOverview(xmlWriter, ico);

		// Build MessageKey list
		ArrayList<MessageKey> keys = ico.getMessageKeys();
		for (MessageKey key : keys) {
			// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | List
			xmlWriter.writeStartElement(XML_PREFIX, "List", XML_NS);

			// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | List | Error
			xmlWriter.writeStartElement(XML_PREFIX, "Error", XML_NS);
			if (key.getEx() != null) {
				StringWriter sw = new StringWriter();
				key.getEx().printStackTrace(new PrintWriter(sw));
				xmlWriter.writeCData(sw.toString());
			}
			xmlWriter.writeEndElement();
			
			// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | List | PayloadExist
			xmlWriter.writeStartElement(XML_PREFIX, "PayloadExist", XML_NS);
			if (MessageKey.PAYLOAD_FOUND.equals(key.getXiMessageInResponse())) {
				xmlWriter.writeCharacters("true");
			} else {
				xmlWriter.writeCharacters("false");
			}
			xmlWriter.writeEndElement();			

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
			if (MessageKey.PAYLOAD_FOUND.equals(key.getXiMessageInResponse())) {
				xmlWriter.writeCharacters(key.getFileName());
			}
			xmlWriter.writeEndElement();

			// Close element: ExtractReport | IntegratedConfiguration | MessageKeys | List
			xmlWriter.writeEndElement();
		}

		// Close element: ExtractReport | IntegratedConfiguration | MessageKeys
		xmlWriter.writeEndElement();
	}


	/**
	 * Build elements: InjectReport | Header | Details
	 * 
	 * @param xmlWriter
	 * @param ico
	 */
	private static void addHeaderDetails(XMLStreamWriter xmlWriter, IntegratedConfiguration ico) throws XMLStreamException {
		// Create element Details
		xmlWriter.writeStartElement(XML_PREFIX, "Details", XML_NS);

		// Create element: ExtractReport | Header | ExtractFromTime
		xmlWriter.writeStartElement(XML_PREFIX, "ExtractFromTime", XML_NS);
		xmlWriter.writeCharacters(ico.getFetchFromTime());
		xmlWriter.writeEndElement();

		// Create element: ExtractReport | Header | ExtractToTime
		xmlWriter.writeStartElement(XML_PREFIX, "ExtractToTime", XML_NS);
		xmlWriter.writeCharacters(ico.getFetchToTime());
		xmlWriter.writeEndElement();

		// Create element: InjectReport | IntegratedConfiguration | QoS
		xmlWriter.writeStartElement(XML_PREFIX, "QoS", XML_NS);
		xmlWriter.writeCharacters(ico.getQualityOfService());
		xmlWriter.writeEndElement();

		// Create element: ExtractReport | IntegratedConfiguration | SenderParty
		xmlWriter.writeStartElement(XML_PREFIX, "SenderParty", XML_NS);
		xmlWriter.writeCharacters(ico.getSenderParty());
		xmlWriter.writeEndElement();

		// Create element: ExtractReport | IntegratedConfiguration | SenderComponent
		xmlWriter.writeStartElement(XML_PREFIX, "SenderComponent", XML_NS);
		xmlWriter.writeCharacters(ico.getSenderComponent());
		xmlWriter.writeEndElement();

		// Create element: ExtractReport | IntegratedConfiguration | ReceiverInterface
		xmlWriter.writeStartElement(XML_PREFIX, "ReceiverInterface", XML_NS);
		xmlWriter.writeCharacters(ico.getReceiverInterfaceName());
		xmlWriter.writeEndElement();

		// Create element: ExtractReport | IntegratedConfiguration | ReceiverNamespace
		xmlWriter.writeStartElement(XML_PREFIX, "ReceiverNamespace", XML_NS);
		xmlWriter.writeCharacters(ico.getReceiverNamespace());
		xmlWriter.writeEndElement();

		// Create element: ExtractReport | IntegratedConfiguration | ReceiverParty
		xmlWriter.writeStartElement(XML_PREFIX, "ReceiverParty", XML_NS);
		xmlWriter.writeCharacters(ico.getReceiverParty());
		xmlWriter.writeEndElement();

		// Create element: ExtractReport | IntegratedConfiguration | ReceiverComponent
		xmlWriter.writeStartElement(XML_PREFIX, "ReceiverComponent", XML_NS);
		xmlWriter.writeCharacters(ico.getReceiverComponent());
		xmlWriter.writeEndElement();

		// Close element: Details
		xmlWriter.writeEndElement();
	}


	private void addMessageKeysOverview(XMLStreamWriter xmlWriter, IntegratedConfiguration ico) throws XMLStreamException {
		// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | Overview
		xmlWriter.writeStartElement(XML_PREFIX, "Overview", XML_NS);

		// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | Overview | Max
		xmlWriter.writeStartElement(XML_PREFIX, "Max", XML_NS);
		xmlWriter.writeCharacters("" + ico.getMaxMessages());
		xmlWriter.writeEndElement();				
		
		// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | Overview | Total
		xmlWriter.writeStartElement(XML_PREFIX, "Total", XML_NS);
		xmlWriter.writeCharacters("" + this.countMsgKeyTotal);
		xmlWriter.writeEndElement();

		// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | Overview | Success
		xmlWriter.writeStartElement(XML_PREFIX, "Success", XML_NS);
		xmlWriter.writeCharacters("" + this.countMsgKeyOk);
		xmlWriter.writeEndElement();

		// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | Overview | Error
		xmlWriter.writeStartElement(XML_PREFIX, "Error", XML_NS);
		xmlWriter.writeCharacters("" + this.countMsgKeyErr);
		xmlWriter.writeEndElement();
		
		// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | Overview | NoPayload
		xmlWriter.writeStartElement(XML_PREFIX, "NoPayload", XML_NS);
		xmlWriter.writeCharacters("" + this.countMsgKeyNopayload);
		xmlWriter.writeEndElement();					
				
		// Close element: ExtractReport | IntegratedConfiguration | MessageKeys | Overview
		xmlWriter.writeEndElement();
	}
}
