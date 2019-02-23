package com.invixo.extraction.reporting;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.invixo.common.Payload;
import com.invixo.common.util.Util;
import com.invixo.consistency.FileStructure;
import com.invixo.extraction.IntegratedConfiguration;
import com.invixo.extraction.MessageKey;
import com.invixo.main.GlobalParameters;

public class ReportWriter {
	private static final String	XML_PREFIX = "inv";
	private static final String	XML_NS = "urn:invixo.com.extract.report";

	private final String REPORT_FILE	= FileStructure.DIR_REPORTS 
										+ "ExtractReport_" 
										+ GlobalParameters.PARAM_VAL_TARGET_ENV 
										+ "_" 
										+ System.currentTimeMillis() 
										+ ".xml";
	
	private ArrayList<IntegratedConfiguration>	icoList;

	// ICO general
	private int	countIcoTotal = 0;					// Total number of ICOs processed
	private int	countIcoErr = 0;					// Total number of ICOs processed with error
	private int	countIcoOk = 0;						// Total number of ICOs processed successfully
	private boolean	fetchPayloadFirst = Boolean.parseBoolean(GlobalParameters.PARAM_VAL_EXTRACT_MODE_INIT);
	private boolean	fetchPayloadLast = true;
	
	// MessageKey general
	private int	countMsgKeyTotal				= 0;	// Total message keys processed
	private int	countMsgKeyTechErr				= 0;	// Total, with technical error
	private int	countMsgKeyTechOkButNoPayload	= 0;	// Total, no technical error, but FIRST and/or LAST does not exist
	private int	countMsgKeyOk					= 0;	// Total, ok (FIRST and LAST exists)
	private int	countMsgVersionFirstNopayload	= 0;	// Total, missing FIRST
	private int	countMsgVersionLastNopayload	= 0;	// Total, missing LAST
	private int	countMsgPayloadsCreated			= 0;	// Total FIRST and LAST created payloads
	private int countMsgPayloadsCreatedFirst 	= 0;	// Total FIRST payloads created
	private int countMsgPayloadsCreatedLast 	= 0;	// Total LAST payloads created	

	public ReportWriter(ArrayList<IntegratedConfiguration> icoList) {
		this.icoList = icoList;

		// Interpret general data
		this.evaluateGeneralResults();
	}


	private void evaluateGeneralResults() {
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
	}


	private void evaluateMessageKeys(IntegratedConfiguration ico) {
		// Set total number of MessageKeys processed
		this.countMsgKeyTotal += ico.getMessageKeys().size();
		HashSet<String> messageKeysFirst = new HashSet<String>();
		HashSet<String> messageKeysLast = new HashSet<String>();
		
		// Determine number of successfully and erroneous MessageKeys
		for (MessageKey key : ico.getMessageKeys()) {
			Payload firstPayload = key.getPayloadFirst();
			Payload lastPayload = key.getPayloadLast();
			
			// Add to messageKeys
			messageKeysFirst.add(firstPayload.getSapMessageKey());
			messageKeysLast.add(lastPayload.getSapMessageKey());
			
			// Set other counters
			if (key.getEx() == null) {
				
				// Check: only FIRST payloads is enabled
				if (fetchPayloadFirst && !fetchPayloadLast) {
					// Check: No technical error and FIRST payload was found
					if (Payload.STATUS.FOUND.equals(firstPayload.getPayloadFoundStatus())) {
						this.countMsgKeyOk++;
					}
					
					// Check: No exception occurred, but FIRST XI message was not returned by Web Service
					if (Payload.STATUS.NOT_FOUND.equals(firstPayload.getPayloadFoundStatus())) {
						this.countMsgVersionFirstNopayload++;
					}
				}
				
				// Check: only LAST payloads is enabled
				if (!fetchPayloadFirst && fetchPayloadLast) {
					// Check: No technical error and LAST payload was found
					if (Payload.STATUS.FOUND.equals(lastPayload.getPayloadFoundStatus())) {
						this.countMsgKeyOk++;
					}
					
					// Check: No exception occurred, but LAST XI message was not returned by Web Service
					if (Payload.STATUS.NOT_FOUND.equals(lastPayload.getPayloadFoundStatus())) {
						this.countMsgVersionLastNopayload++;
					}
				}
				
				// Check: FIRST and LAST payloads is enabled
				if (fetchPayloadFirst && fetchPayloadLast) {
					// Check: No technical error, but either FIRST or LAST payload is missing
					if (	Payload.STATUS.NOT_FOUND.equals(firstPayload.getPayloadFoundStatus()) 
						|| 	Payload.STATUS.NOT_FOUND.equals(lastPayload.getPayloadFoundStatus())) {
						this.countMsgKeyTechOkButNoPayload++;
					} 

					// Check: No technical error and FIRST or LAST payload was found
					if (	Payload.STATUS.FOUND.equals(firstPayload.getPayloadFoundStatus()) 
						&& 	Payload.STATUS.FOUND.equals(lastPayload.getPayloadFoundStatus())) {
						this.countMsgKeyOk++;
					}
					
					// Check: No exception occurred, but FIRST XI message was not returned by Web Service
					if (Payload.STATUS.NOT_FOUND.equals(firstPayload.getPayloadFoundStatus())) {
						this.countMsgVersionFirstNopayload++;	
					} 

					// Check: No exception occurred, but LAST XI message was not returned by Web Service
					if (Payload.STATUS.NOT_FOUND.equals(lastPayload.getPayloadFoundStatus())) {
						this.countMsgVersionLastNopayload++;
					}
				}
			} else {
				// Technical error
				this.countMsgKeyTechErr++;
			}
		}
				
		// Set global counters for total payloads created
		countMsgPayloadsCreatedFirst += messageKeysFirst.size();
		countMsgPayloadsCreatedLast += messageKeysLast.size();
		countMsgPayloadsCreated += messageKeysFirst.size() + messageKeysLast.size();
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

			// Add structure: ExtractReport | IcoOverview
			addIntegrationConfigurationGlobalOverview(xmlWriter);
			
			// Add structure: ExtractReport | MessageKeysOverview
			addMessageKeysGlobalOverview(xmlWriter);

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


	private void addIntegrationConfigurationGlobalOverview(XMLStreamWriter xmlWriter) throws XMLStreamException {
		// Create element: ExtractReport | IcoOverview
		xmlWriter.writeStartElement(XML_PREFIX, "IcoOverview", XML_NS);

		// Create element: ExtractReport | IcoOverview | Total
		xmlWriter.writeStartElement(XML_PREFIX, "Total", XML_NS);
		xmlWriter.writeCharacters("" + this.countIcoTotal);
		xmlWriter.writeEndElement();

		// Create element: ExtractReport | IcoOverview | Success
		xmlWriter.writeStartElement(XML_PREFIX, "Success", XML_NS);
		xmlWriter.writeCharacters("" + this.countIcoOk);
		xmlWriter.writeEndElement();

		// Create element: ExtractReport | IcoOverview | TechnicalError
		xmlWriter.writeStartElement(XML_PREFIX, "TechnicalError", XML_NS);
		xmlWriter.writeCharacters("" + this.countIcoErr);
		xmlWriter.writeEndElement();

		// Create element: ExtractReport | IcoOverview | FetchPayLoadFirst
		xmlWriter.writeStartElement(XML_PREFIX, "FetchPayLoadFirst", XML_NS);
		xmlWriter.writeCharacters("" + this.fetchPayloadFirst);
		xmlWriter.writeEndElement();

		// Create element: ExtractReport | IcoOverview | FetchPayLoadLast
		xmlWriter.writeStartElement(XML_PREFIX, "FetchPayLoadLast", XML_NS);
		xmlWriter.writeCharacters("" + this.fetchPayloadLast);
		xmlWriter.writeEndElement();

		// Close element: ExtractReport | IcoOverview
		xmlWriter.writeEndElement();
	}


	private void addIcoDetails(ArrayList<IntegratedConfiguration> icoList, XMLStreamWriter xmlWriter) throws XMLStreamException {
		// Add detail info per ICO
		for (IntegratedConfiguration ico : icoList) {
			// Create element: ExtractReport | IntegratedConfiguration
			xmlWriter.writeStartElement(XML_PREFIX, "IntegratedConfiguration", XML_NS);
			xmlWriter.writeAttribute("ProcessingTime", "" + Util.measureTimeTaken(ico.getStartTime(), ico.getEndTime()));
			xmlWriter.writeAttribute("ProcessingTimeUnit", "seconds");

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
		addMessageKeysLocalOverview(xmlWriter, ico);

		// Build MessageKey list
		ArrayList<MessageKey> keys = ico.getMessageKeys();
		for (MessageKey key : keys) {
			// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | List
			xmlWriter.writeStartElement(XML_PREFIX, "List", XML_NS);

			// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | List | TechnicalError
			xmlWriter.writeStartElement(XML_PREFIX, "TechnicalError", XML_NS);
			if (key.getEx() != null) {
				StringWriter sw = new StringWriter();
				key.getEx().printStackTrace(new PrintWriter(sw));
				xmlWriter.writeCData(sw.toString());
			}
			xmlWriter.writeEndElement();
			
			// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | List | FirstPayload
			xmlWriter.writeStartElement(XML_PREFIX, "FirstPayload", XML_NS);
			
			// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | List | FirstPayload | PayloadStatus
			xmlWriter.writeStartElement(XML_PREFIX, "PayloadStatus", XML_NS);
			xmlWriter.writeCharacters(key.getPayloadFirst().getPayloadFoundStatus().toString());
			xmlWriter.writeEndElement();	

			// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | List | FirstPayload | Key
			xmlWriter.writeStartElement(XML_PREFIX, "Key", XML_NS);
			xmlWriter.writeCharacters(key.getPayloadFirst().getSapMessageKey());
			xmlWriter.writeEndElement();

			// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | List | FirstPayload | Path
			xmlWriter.writeStartElement(XML_PREFIX, "Path", XML_NS);
			xmlWriter.writeCharacters(ico.getFilePathFirstPayloads());
			xmlWriter.writeEndElement();

			// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | List | FirstPayload | FileName
			xmlWriter.writeStartElement(XML_PREFIX, "FileName", XML_NS);
			xmlWriter.writeCharacters(key.getPayloadFirst().getFileName());
			xmlWriter.writeEndElement();
			
			// Close element: ExtractReport | IntegratedConfiguration | MessageKeys | List | FirstPayload
			xmlWriter.writeEndElement();	
			
			// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | List | LastPayload
			xmlWriter.writeStartElement(XML_PREFIX, "LastPayload", XML_NS);
			
			// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | List | LastPayload | PayloadStatus
			xmlWriter.writeStartElement(XML_PREFIX, "PayloadStatus", XML_NS);
			xmlWriter.writeCharacters(key.getPayloadLast().getPayloadFoundStatus().toString());
			xmlWriter.writeEndElement();	

			// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | List | LastPayload | Key
			xmlWriter.writeStartElement(XML_PREFIX, "Key", XML_NS);
			xmlWriter.writeCharacters(key.getPayloadLast().getSapMessageKey());
			xmlWriter.writeEndElement();

			// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | List | LastPayload | Path
			xmlWriter.writeStartElement(XML_PREFIX, "Path", XML_NS);
			xmlWriter.writeCharacters(ico.getFilePathLastPayloads());
			xmlWriter.writeEndElement();

			// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | List | LastPayload | FileName
			xmlWriter.writeStartElement(XML_PREFIX, "FileName", XML_NS);
			if (Payload.STATUS.FOUND.equals(key.getPayloadLast().getPayloadFoundStatus())) {
				xmlWriter.writeCharacters(key.getPayloadLast().getFileName());
			}
			xmlWriter.writeEndElement();
			
			// Close element: ExtractReport | IntegratedConfiguration | MessageKeys | List | LastPayload
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
		xmlWriter.writeCharacters(ico.getFromTime());
		xmlWriter.writeEndElement();

		// Create element: ExtractReport | Header | ExtractToTime
		xmlWriter.writeStartElement(XML_PREFIX, "ExtractToTime", XML_NS);
		xmlWriter.writeCharacters(ico.getToTime());
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
		xmlWriter.writeCharacters(ico.getReceiverInterface());
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


	private void addMessageKeysLocalOverview(XMLStreamWriter xmlWriter, IntegratedConfiguration ico) throws XMLStreamException {
		// Calc overview numbers
		int keysTotal = ico.getMessageKeys().size();
		int keysOk = 0;			// No technical errors, both FIRST and LAST was found
		int keysTechOk = 0;		// No technical errors, but either FIST and/or LAST is missing
		int keysError = 0;		// Technical error
		int keysNoFirstPayload = 0;
		int keysNoLastPayload = 0;
		int keysPayloadsCreated = 0;
		HashSet<String> messageKeysFirst = new HashSet<String>();
		HashSet<String> messageKeysLast = new HashSet<String>();
		
		for (MessageKey key : ico.getMessageKeys()) {
			Payload firstPayload = key.getPayloadFirst();
			Payload lastPayload = key.getPayloadLast();
			
			// Add to messageKeys
			messageKeysFirst.add(firstPayload.getSapMessageKey());
			messageKeysLast.add(lastPayload.getSapMessageKey());
						
			// Set other counters
			if (key.getEx() == null) {
				
				// Check: only FIRST payloads is enabled
				if (fetchPayloadFirst && !fetchPayloadLast) {
					// Check: No technical error, FIRST payload is missing
					if (Payload.STATUS.NOT_FOUND.equals(firstPayload.getPayloadFoundStatus())) {
						keysTechOk++;
					} 

					// Check: No technical error and FIRST payload was found
					if (Payload.STATUS.FOUND.equals(firstPayload.getPayloadFoundStatus())) {
						keysOk++;
					}
					
					// Check: No exception occurred, but FIRST XI message was not returned by Web Service
					if (Payload.STATUS.NOT_FOUND.equals(firstPayload.getPayloadFoundStatus())) {
						keysNoFirstPayload++;
					}
				}
				
				// Check: only LAST payloads is enabled
				if (!fetchPayloadFirst && fetchPayloadLast) {
					// Check: No technical error, LAST payload is missing
					if (Payload.STATUS.NOT_FOUND.equals(lastPayload.getPayloadFoundStatus())) {
						keysTechOk++;
					} 

					// Check: No technical error and LAST payload was found
					if (Payload.STATUS.FOUND.equals(lastPayload.getPayloadFoundStatus())) {
						keysOk++;
					}
					
					// Check: No exception occurred, but LAST XI message was not returned by Web Service
					if (Payload.STATUS.NOT_FOUND.equals(lastPayload.getPayloadFoundStatus())) {
						keysNoLastPayload++;
					}
				}
				
				// Check: FIRST and LAST payloads is enabled
				if (fetchPayloadFirst && fetchPayloadLast) {
					// Check: No technical error, but either FIRST or LAST payload is missing
					if (	Payload.STATUS.NOT_FOUND.equals(firstPayload.getPayloadFoundStatus()) 
						|| 	Payload.STATUS.NOT_FOUND.equals(lastPayload.getPayloadFoundStatus())) {
						keysTechOk++;
					} 

					// Check: No technical error and FIRST or LAST payload was found
					if (	Payload.STATUS.FOUND.equals(firstPayload.getPayloadFoundStatus()) 
						&& 	Payload.STATUS.FOUND.equals(lastPayload.getPayloadFoundStatus())) {
						keysOk++;
					}
					
					// Check: No exception occurred, but FIRST XI message was not returned by Web Service
					if (Payload.STATUS.NOT_FOUND.equals(firstPayload.getPayloadFoundStatus())) {
						keysNoFirstPayload++;	
					} 

					// Check: No exception occurred, but LAST XI message was not returned by Web Service
					if (Payload.STATUS.NOT_FOUND.equals(lastPayload.getPayloadFoundStatus())) {
						keysNoLastPayload++;
					}
				}
					
			} else {
				keysError++;
			}
		}	
		
		// Set local counter
		keysPayloadsCreated = messageKeysFirst.size() + messageKeysLast.size();
		
		// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | Overview
		xmlWriter.writeStartElement(XML_PREFIX, "Overview", XML_NS);

		// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | Overview | Max
		xmlWriter.writeStartElement(XML_PREFIX, "Max", XML_NS);
		xmlWriter.writeCharacters("" + ico.getMaxMessages());
		xmlWriter.writeEndElement();				
		
		// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | Overview | Actual
		xmlWriter.writeStartElement(XML_PREFIX, "Actual", XML_NS);
		xmlWriter.writeCharacters("" + keysTotal);
		xmlWriter.writeEndElement();

		// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | Overview | SuccessAllPayloadsFound
		xmlWriter.writeStartElement(XML_PREFIX, "SuccessAllPayloadsFound", XML_NS);
		xmlWriter.writeCharacters("" + keysOk);
		xmlWriter.writeEndElement();
		
		// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | Overview | SuccessNotAllPayloadsFound
		xmlWriter.writeStartElement(XML_PREFIX, "SuccessNotAllPayloadsFound", XML_NS);
		xmlWriter.writeCharacters("" + keysTechOk);
		xmlWriter.writeEndElement();

		// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | Overview | TechnicalError
		xmlWriter.writeStartElement(XML_PREFIX, "TechnicalError", XML_NS);
		xmlWriter.writeCharacters("" + keysError);
		xmlWriter.writeEndElement();
		
		// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | Overview | FirstPayloadMissing
		xmlWriter.writeStartElement(XML_PREFIX, "FirstPayloadMissing", XML_NS);
		xmlWriter.writeCharacters("" + keysNoFirstPayload);
		xmlWriter.writeEndElement();
		
		// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | Overview | LastPayloadMissing
		xmlWriter.writeStartElement(XML_PREFIX, "LastPayloadMissing", XML_NS);
		xmlWriter.writeCharacters("" + keysNoLastPayload);
		xmlWriter.writeEndElement();
		
		// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | Overview | PayloadsFilesCreatedTotal
		xmlWriter.writeStartElement(XML_PREFIX, "PayloadsFilesCreatedTotal", XML_NS);
		xmlWriter.writeCharacters("" + keysPayloadsCreated);
		xmlWriter.writeEndElement();
		
		// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | Overview | PayloadsFilesCreatedTotalFirst
		xmlWriter.writeStartElement(XML_PREFIX, "PayloadsFilesCreatedTotalFirst", XML_NS);
		xmlWriter.writeCharacters("" + messageKeysFirst.size());
		xmlWriter.writeEndElement();
				
		// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | Overview | PayloadsFilesCreatedTotalLast
		xmlWriter.writeStartElement(XML_PREFIX, "PayloadsFilesCreatedTotalLast", XML_NS);
		xmlWriter.writeCharacters("" + messageKeysLast.size());
		xmlWriter.writeEndElement();
				
		// Close element: ExtractReport | IntegratedConfiguration | MessageKeys | Overview
		xmlWriter.writeEndElement();
	}
	
	
	private void addMessageKeysGlobalOverview(XMLStreamWriter xmlWriter) throws XMLStreamException {
		// Create element: ExtractReport | MessageKeysOverview
		xmlWriter.writeStartElement(XML_PREFIX, "MessageKeysOverview", XML_NS);
		
		// Create element: ExtractReport | MessageKeysOverview | Total
		xmlWriter.writeStartElement(XML_PREFIX, "Total", XML_NS);
		xmlWriter.writeCharacters("" + this.countMsgKeyTotal);
		xmlWriter.writeEndElement();

		// Create element: ExtractReport | MessageKeysOverview | SuccessAllPayloadsFound
		xmlWriter.writeStartElement(XML_PREFIX, "SuccessAllPayloadsFound", XML_NS);
		xmlWriter.writeCharacters("" + this.countMsgKeyOk);
		xmlWriter.writeEndElement();	
		
		// Create element: ExtractReport | MessageKeysOverview | SuccessNotAllPayloadsFound
		xmlWriter.writeStartElement(XML_PREFIX, "SuccessNotAllPayloadsFound", XML_NS);
		xmlWriter.writeCharacters("" + this.countMsgKeyTechOkButNoPayload);
		xmlWriter.writeEndElement();
		
		// Create element: ExtractReport | MessageKeysOverview | TechnicalError
		xmlWriter.writeStartElement(XML_PREFIX, "TechnicalError", XML_NS);
		xmlWriter.writeCharacters("" + this.countMsgKeyTechErr);
		xmlWriter.writeEndElement();
		
		// Create element: ExtractReport | MessageKeysOverview | FirstPayloadMissing
		xmlWriter.writeStartElement(XML_PREFIX, "FirstPayloadMissing", XML_NS);
		xmlWriter.writeCharacters("" + this.countMsgVersionFirstNopayload);
		xmlWriter.writeEndElement();
		
		// Create element: ExtractReport | MessageKeysOverview | LastPayloadMissing
		xmlWriter.writeStartElement(XML_PREFIX, "LastPayloadMissing", XML_NS);
		xmlWriter.writeCharacters("" + this.countMsgVersionLastNopayload);
		xmlWriter.writeEndElement();
		
		// Create element: ExtractReport | MessageKeysOverview | PayloadsFilesCreatedTotal
		xmlWriter.writeStartElement(XML_PREFIX, "PayloadsFilesCreatedTotal", XML_NS);
		xmlWriter.writeCharacters("" + this.countMsgPayloadsCreated);
		xmlWriter.writeEndElement();
		
		// Create element: ExtractReport | MessageKeysOverview | PayloadsFilesCreatedTotalFirst
		xmlWriter.writeStartElement(XML_PREFIX, "PayloadsFilesCreatedTotalFirst", XML_NS);
		xmlWriter.writeCharacters("" + this.countMsgPayloadsCreatedFirst);
		xmlWriter.writeEndElement();
				
		// Create element: ExtractReport | MessageKeysOverview | PayloadsFilesCreatedTotalLast
		xmlWriter.writeStartElement(XML_PREFIX, "PayloadsFilesCreatedTotalLast", XML_NS);
		xmlWriter.writeCharacters("" + this.countMsgPayloadsCreatedLast);
		xmlWriter.writeEndElement();
				
		// Close element: ExtractReport | MessageKeysOverview
		xmlWriter.writeEndElement();
	}
}
