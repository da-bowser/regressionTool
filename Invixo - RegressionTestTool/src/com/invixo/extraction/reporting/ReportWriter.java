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

import com.invixo.common.XiMessage;
import com.invixo.common.util.Util;
import com.invixo.consistency.FileStructure;
import com.invixo.extraction.IntegratedConfiguration;
import com.invixo.extraction.XiMessages;
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
	private int	countMsgKeyTechOkButNoPayload	= 0;	// Total, no technical error, but FIRST and/or LAST does not exist
	private int	countMsgKeyOk					= 0;	// Total, ok (FIRST and LAST exists)
	private int	countMsgVersionFirstNopayload	= 0;	// Total, missing FIRST
	private int	countMsgVersionLastNopayload	= 0;	// Total, missing LAST
	private int	countMsgPayloadsCreated			= 0;	// Total FIRST and LAST created payloads
	private int countMsgPayloadsCreatedFirst 	= 0;	// Total FIRST payloads created
	private int countMsgPayloadsCreatedLast 	= 0;	// Total LAST payloads created	
	private int countMsgKeyErrorTotal			= 0;	// Total number of errors on XiMessage level (measured on FIRST entries only)

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
		
		// Calc status
		for (XiMessages linkedList : ico.getPayloadsLinkList()) {
			XiMessage firstPayload = linkedList.getFirstMessage();
			
			if (firstPayload.getEx() == null) {
				for (XiMessage lastPayload : linkedList.getLastMessageList()) {
					// Add to messageKeys
					messageKeysFirst.add(firstPayload.getSapMessageKey());
					messageKeysLast.add(lastPayload.getSapMessageKey());
					
					// Check: only FIRST payloads is enabled
					if (fetchPayloadFirst && !fetchPayloadLast) {
						// Check: No technical error and FIRST payload was found
						if (XiMessage.STATUS.FOUND.equals(firstPayload.getPayloadFoundStatus())) {
							this.countMsgKeyOk++;
						}
						
						// Check: No exception occurred, but FIRST XI message was not returned by Web Service
						if (XiMessage.STATUS.NOT_FOUND.equals(firstPayload.getPayloadFoundStatus())) {
							this.countMsgVersionFirstNopayload++;
						}
					}
					
					// Check: only LAST payloads is enabled
					if (!fetchPayloadFirst && fetchPayloadLast) {
						// Check: No technical error and LAST payload was found
						if (XiMessage.STATUS.FOUND.equals(lastPayload.getPayloadFoundStatus())) {
							this.countMsgKeyOk++;
						}
						
						// Check: No exception occurred, but LAST XI message was not returned by Web Service
						if (XiMessage.STATUS.NOT_FOUND.equals(lastPayload.getPayloadFoundStatus())) {
							this.countMsgVersionLastNopayload++;
						}
					}
					
					// Check: FIRST and LAST payloads is enabled
					if (fetchPayloadFirst && fetchPayloadLast) {
						// Check: No technical error, but either FIRST or LAST payload is missing
						if (	XiMessage.STATUS.NOT_FOUND.equals(firstPayload.getPayloadFoundStatus()) 
							|| 	XiMessage.STATUS.NOT_FOUND.equals(lastPayload.getPayloadFoundStatus())) {
							this.countMsgKeyTechOkButNoPayload++;
						} 
	
						// Check: No technical error and FIRST or LAST payload was found
						if (	XiMessage.STATUS.FOUND.equals(firstPayload.getPayloadFoundStatus()) 
							&& 	XiMessage.STATUS.FOUND.equals(lastPayload.getPayloadFoundStatus())) {
							this.countMsgKeyOk++;
						}
						
						// Check: No exception occurred, but FIRST XI message was not returned by Web Service
						if (XiMessage.STATUS.NOT_FOUND.equals(firstPayload.getPayloadFoundStatus())) {
							this.countMsgVersionFirstNopayload++;	
						} 
	
						// Check: No exception occurred, but LAST XI message was not returned by Web Service
						if (XiMessage.STATUS.NOT_FOUND.equals(lastPayload.getPayloadFoundStatus())) {
							this.countMsgVersionLastNopayload++;
						}
					}
				}
			} else {
				// An error existed on FIRST message
				countMsgKeyErrorTotal++;
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
			String mode = Boolean.parseBoolean(GlobalParameters.PARAM_VAL_EXTRACT_MODE_INIT) ? "Init (1)" : "Non-Init (3)";
			xmlWriter.writeAttribute("Mode", mode);
			xmlWriter.writeAttribute("SourceEnv", GlobalParameters.PARAM_VAL_SOURCE_ENV);
			xmlWriter.writeAttribute("TargetEnv", GlobalParameters.PARAM_VAL_TARGET_ENV);
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
		// Create element: ExtractReport | IntegratedConfiguration | XiMessages
		xmlWriter.writeStartElement(XML_PREFIX, "XiMessages", XML_NS);	
		
		// Add structure: ExtractReport | IntegratedConfiguration | XiMessages | Overview
		addMessageKeysLocalOverview(xmlWriter, ico);
		
		// Create element: ExtractReport | IntegratedConfiguration | XiMessages | MessageLink
		xmlWriter.writeStartElement(XML_PREFIX, "MessageLink", XML_NS);
		
		for (XiMessages linkedList : ico.getPayloadsLinkList()) {
			XiMessage firstPayload = linkedList.getFirstMessage();
			
			// Create element: ExtractReport | IntegratedConfiguration | XiMessages | MessageLink | First
			xmlWriter.writeStartElement(XML_PREFIX, "First", XML_NS);
			
			// Create element: ExtractReport | IntegratedConfiguration | XiMessages | MessageLink | First | Error
			xmlWriter.writeStartElement(XML_PREFIX, "Error", XML_NS);
			if (firstPayload.getEx() != null) {
				StringWriter sw = new StringWriter();
				firstPayload.getEx().printStackTrace(new PrintWriter(sw));
				xmlWriter.writeCData(sw.toString());
			}
			xmlWriter.writeEndElement();
						
			// Create element: ExtractReport | IntegratedConfiguration | XiMessages | MessageLink | First | PayloadStatus
			xmlWriter.writeStartElement(XML_PREFIX, "PayloadStatus", XML_NS);
			xmlWriter.writeCharacters(firstPayload.getPayloadFoundStatus().toString());
			xmlWriter.writeEndElement();
						
			// Create element: ExtractReport | IntegratedConfiguration | XiMessages | MessageLink | First | Key
			xmlWriter.writeStartElement(XML_PREFIX, "Key", XML_NS);
			xmlWriter.writeCharacters(firstPayload.getSapMessageKey());
			xmlWriter.writeEndElement();

			// Create element: ExtractReport | IntegratedConfiguration | XiMessages | MessageLink | First | Path
			xmlWriter.writeStartElement(XML_PREFIX, "Path", XML_NS);
			xmlWriter.writeCharacters(ico.getFilePathFirstPayloads());
			xmlWriter.writeEndElement();

			// Create element: ExtractReport | IntegratedConfiguration | XiMessages | MessageLink | First | FileName
			xmlWriter.writeStartElement(XML_PREFIX, "FileName", XML_NS);
			xmlWriter.writeCharacters(firstPayload.getFileName());
			xmlWriter.writeEndElement();
			
			// Create element: ExtractReport | IntegratedConfiguration | XiMessages | MessageLink | LastList
			xmlWriter.writeStartElement(XML_PREFIX, "LastList", XML_NS);
			
			for (XiMessage lastPayload : linkedList.getLastMessageList()) {
				// Create element: ExtractReport | IntegratedConfiguration | XiMessages | MessageLink | LastList | Last
				xmlWriter.writeStartElement(XML_PREFIX, "Last", XML_NS);
				
				// Create element: ExtractReport | IntegratedConfiguration | XiMessages | MessageLink | LastList | Last | PayloadStatus
				xmlWriter.writeStartElement(XML_PREFIX, "PayloadStatus", XML_NS);
				xmlWriter.writeCharacters(lastPayload.getPayloadFoundStatus().toString());
				xmlWriter.writeEndElement();
				
				// Create element: ExtractReport | IntegratedConfiguration | XiMessages | MessageLink | LastList | Last | Key
				xmlWriter.writeStartElement(XML_PREFIX, "Key", XML_NS);
				xmlWriter.writeCharacters(lastPayload.getSapMessageKey().toString());
				xmlWriter.writeEndElement();
				
				// Create element: ExtractReport | IntegratedConfiguration | XiMessages | MessageLink | LastList | Last | Path
				xmlWriter.writeStartElement(XML_PREFIX, "Path", XML_NS);
				xmlWriter.writeCharacters(ico.getFilePathFirstPayloads());
				xmlWriter.writeEndElement();

				// Create element: ExtractReport | IntegratedConfiguration | XiMessages | MessageLink | LastList | Last | FileName
				xmlWriter.writeStartElement(XML_PREFIX, "FileName", XML_NS);
				xmlWriter.writeCharacters(lastPayload.getFileName());
				xmlWriter.writeEndElement();
				
				// Close element: ExtractReport | IntegratedConfiguration | XiMessages | MessageLink | LastList | Last
				xmlWriter.writeEndElement();	
			}
			
			// Close element: ExtractReport | IntegratedConfiguration | XiMessages | MessageLink | LastList
			xmlWriter.writeEndElement();	
			
			// Close element: ExtractReport | IntegratedConfiguration | XiMessages | MessageLink | First
			xmlWriter.writeEndElement();				
		}

		// Close element: ExtractReport | IntegratedConfiguration | XiMessages | MessageLink
		xmlWriter.writeEndElement();
		
		// Close element: ExtractReport | IntegratedConfiguration | XiMessages
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
		int keysError = 0;		// Error on FIRST message
		int keysTechOk = 0;		// No technical errors, but either FIST and/or LAST is missing
		int keysNoFirstPayload = 0;
		int keysNoLastPayload = 0;
		int keysPayloadsCreated = 0;
			
		HashSet<String> messageKeysFirst = new HashSet<String>();
		HashSet<String> messageKeysLast = new HashSet<String>();
				
		// Calc status
		for (XiMessages linkedList : ico.getPayloadsLinkList()) {
			XiMessage firstPayload = linkedList.getFirstMessage();
			
			if (firstPayload.getEx() == null) {
				for (XiMessage lastPayload : linkedList.getLastMessageList()) {
					// Add to messageKeys
					messageKeysFirst.add(firstPayload.getSapMessageKey());
					messageKeysLast.add(lastPayload.getSapMessageKey());

					// Check: only FIRST payloads is enabled
					if (fetchPayloadFirst && !fetchPayloadLast) {
						// Check: No technical error, FIRST payload is missing
						if (XiMessage.STATUS.NOT_FOUND.equals(firstPayload.getPayloadFoundStatus())) {
							keysTechOk++;
						} 

						// Check: No technical error and FIRST payload was found
						if (XiMessage.STATUS.FOUND.equals(firstPayload.getPayloadFoundStatus())) {
							keysOk++;
						}
						
						// Check: No exception occurred, but FIRST XI message was not returned by Web Service
						if (XiMessage.STATUS.NOT_FOUND.equals(firstPayload.getPayloadFoundStatus())) {
							keysNoFirstPayload++;
						}
					}
					
					// Check: only LAST payloads is enabled
					if (!fetchPayloadFirst && fetchPayloadLast) {
						// Check: No technical error, LAST payload is missing
						if (XiMessage.STATUS.NOT_FOUND.equals(lastPayload.getPayloadFoundStatus())) {
							keysTechOk++;
						} 

						// Check: No technical error and LAST payload was found
						if (XiMessage.STATUS.FOUND.equals(lastPayload.getPayloadFoundStatus())) {
							keysOk++;
						}
						
						// Check: No exception occurred, but LAST XI message was not returned by Web Service
						if (XiMessage.STATUS.NOT_FOUND.equals(lastPayload.getPayloadFoundStatus())) {
							keysNoLastPayload++;
						}
					}
					
					// Check: FIRST and LAST payloads is enabled
					if (fetchPayloadFirst && fetchPayloadLast) {
						// Check: No technical error, but either FIRST or LAST payload is missing
						if (	XiMessage.STATUS.NOT_FOUND.equals(firstPayload.getPayloadFoundStatus()) 
							|| 	XiMessage.STATUS.NOT_FOUND.equals(lastPayload.getPayloadFoundStatus())) {
							keysTechOk++;
						} 

						// Check: No technical error and FIRST or LAST payload was found
						if (	XiMessage.STATUS.FOUND.equals(firstPayload.getPayloadFoundStatus()) 
							&& 	XiMessage.STATUS.FOUND.equals(lastPayload.getPayloadFoundStatus())) {
							keysOk++;
						}
						
						// Check: No exception occurred, but FIRST XI message was not returned by Web Service
						if (XiMessage.STATUS.NOT_FOUND.equals(firstPayload.getPayloadFoundStatus())) {
							keysNoFirstPayload++;	
						} 

						// Check: No exception occurred, but LAST XI message was not returned by Web Service
						if (XiMessage.STATUS.NOT_FOUND.equals(lastPayload.getPayloadFoundStatus())) {
							keysNoLastPayload++;
						}
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

		// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | Overview | MaxFirstPayloads
		xmlWriter.writeStartElement(XML_PREFIX, "MaxFirstPayloads", XML_NS);
		xmlWriter.writeCharacters("" + ico.getMaxMessages());
		xmlWriter.writeEndElement();				
		
		// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | Overview | ActualFirstPayloads
		xmlWriter.writeStartElement(XML_PREFIX, "ActualFirstPayloads", XML_NS);
		xmlWriter.writeCharacters("" + keysTotal);
		xmlWriter.writeEndElement();
		
		// Create element: ExtractReport | XiMessagesOverview | FirstMessagesInError
		xmlWriter.writeStartElement(XML_PREFIX, "FirstMessagesInError", XML_NS);
		xmlWriter.writeCharacters("" + keysError);
		xmlWriter.writeEndElement();

		// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | Overview | SuccessAllPayloadsFound
		xmlWriter.writeStartElement(XML_PREFIX, "SuccessAllPayloadsFound", XML_NS);
		xmlWriter.writeCharacters("" + keysOk);
		xmlWriter.writeEndElement();
		
		// Create element: ExtractReport | IntegratedConfiguration | MessageKeys | Overview | SuccessNotAllPayloadsFound
		xmlWriter.writeStartElement(XML_PREFIX, "SuccessNotAllPayloadsFound", XML_NS);
		xmlWriter.writeCharacters("" + keysTechOk);
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
		// Create element: ExtractReport | XiMessagesOverview
		xmlWriter.writeStartElement(XML_PREFIX, "XiMessagesOverview", XML_NS);
		
		// Create element: ExtractReport | XiMessagesOverview | Total
		xmlWriter.writeStartElement(XML_PREFIX, "Total", XML_NS);
		xmlWriter.writeCharacters("" + this.countMsgKeyTotal);
		xmlWriter.writeEndElement();

		// Create element: ExtractReport | XiMessagesOverview | TotalErrors
		xmlWriter.writeStartElement(XML_PREFIX, "TotalErrors", XML_NS);
		xmlWriter.writeCharacters("" + this.countMsgKeyErrorTotal);
		xmlWriter.writeEndElement();
		
		// Create element: ExtractReport | XiMessagesOverview | SuccessAllPayloadsFound
		xmlWriter.writeStartElement(XML_PREFIX, "SuccessAllPayloadsFound", XML_NS);
		xmlWriter.writeCharacters("" + this.countMsgKeyOk);
		xmlWriter.writeEndElement();	
		
		// Create element: ExtractReport | XiMessagesOverview | SuccessNotAllPayloadsFound
		xmlWriter.writeStartElement(XML_PREFIX, "SuccessNotAllPayloadsFound", XML_NS);
		xmlWriter.writeCharacters("" + this.countMsgKeyTechOkButNoPayload);
		xmlWriter.writeEndElement();
		
		// Create element: ExtractReport | XiMessagesOverview | FirstPayloadMissing
		xmlWriter.writeStartElement(XML_PREFIX, "FirstPayloadMissing", XML_NS);
		xmlWriter.writeCharacters("" + this.countMsgVersionFirstNopayload);
		xmlWriter.writeEndElement();
		
		// Create element: ExtractReport | XiMessagesOverview | LastPayloadMissing
		xmlWriter.writeStartElement(XML_PREFIX, "LastPayloadMissing", XML_NS);
		xmlWriter.writeCharacters("" + this.countMsgVersionLastNopayload);
		xmlWriter.writeEndElement();
		
		// Create element: ExtractReport | XiMessagesOverview | PayloadsFilesCreatedTotal
		xmlWriter.writeStartElement(XML_PREFIX, "PayloadsFilesCreatedTotal", XML_NS);
		xmlWriter.writeCharacters("" + this.countMsgPayloadsCreated);
		xmlWriter.writeEndElement();
		
		// Create element: ExtractReport | XiMessagesOverview | PayloadsFilesCreatedTotalFirst
		xmlWriter.writeStartElement(XML_PREFIX, "PayloadsFilesCreatedTotalFirst", XML_NS);
		xmlWriter.writeCharacters("" + this.countMsgPayloadsCreatedFirst);
		xmlWriter.writeEndElement();
				
		// Create element: ExtractReport | XiMessagesOverview | PayloadsFilesCreatedTotalLast
		xmlWriter.writeStartElement(XML_PREFIX, "PayloadsFilesCreatedTotalLast", XML_NS);
		xmlWriter.writeCharacters("" + this.countMsgPayloadsCreatedLast);
		xmlWriter.writeEndElement();
				
		// Close element: ExtractReport | XiMessagesOverview
		xmlWriter.writeEndElement();
	}
}
