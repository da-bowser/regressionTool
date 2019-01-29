package com.invixo.compare.reporting;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xmlunit.diff.Difference;

import com.invixo.consistency.FileStructure;
import com.invixo.main.GlobalParameters;
import com.invixo.compare.Comparer;
import com.invixo.compare.IntegratedConfiguration;
import com.invixo.compare.Orchestrator;

public class ReportWriter {
	private static final String	XML_PREFIX = "inv";
	private static final String	XML_NS = "urn:invixo.com.compare.report";

	private final String REPORT_FILE = FileStructure.DIR_REPORTS + "CompareReport_" + System.currentTimeMillis() + ".xml";
	private ArrayList<IntegratedConfiguration>	icoList;

	// ICO general
	private int	countIcoTotal = 0;						// Total number of ICOs processed
	private int	countIcoCompared = 0;					// Total number of ICOs compared
	private int	countIcoNotCompared = 0;				// Total number of ICOs not compared
	
	public ReportWriter(ArrayList<IntegratedConfiguration> icoList) {
		this.icoList = icoList;

		// Interpret general data
		this.evaluateGeneralResults();
	}


	public void evaluateGeneralResults() {
		// Set total number of ICO's processed
		this.countIcoTotal = icoList.size();
		
		// Determine number of successfully and erroneous ICOs
		this.countIcoCompared = Orchestrator.getIcoProcessSuccess();
		this.countIcoNotCompared = Orchestrator.getIcoProccesError();
		this.countIcoTotal = this.countIcoCompared + this.countIcoNotCompared;
	}
	
	public String create(ArrayList<IntegratedConfiguration> icoList) {
		try {
			XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
			XMLStreamWriter xmlWriter = xMLOutputFactory.createXMLStreamWriter(new FileOutputStream(REPORT_FILE), GlobalParameters.ENCODING);

			// Add xml version and encoding to output
			xmlWriter.writeStartDocument(GlobalParameters.ENCODING, "1.0");

			// Create element: CompareReport
			xmlWriter.writeStartElement(XML_PREFIX, "CompareReport", XML_NS);
			xmlWriter.writeNamespace(XML_PREFIX, XML_NS);

			// Add structure: CompareReport | IcoOverview
			addIntegrationConfigurationGlobalOverview(xmlWriter);
			
			// Create element: CompareReport | Details
			xmlWriter.writeStartElement(XML_PREFIX, "Details", XML_NS);

			// Add list: CompareReport | Details | IntegratedConfiguration
			addIcoDetails(icoList, xmlWriter);

			// Close element: CompareReport | Details
			xmlWriter.writeEndElement();

			// Close element: CompareReport
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
	
	
	private void addIcoDetails(ArrayList<IntegratedConfiguration> icoList,
			XMLStreamWriter xmlWriter) throws XMLStreamException {
		// Add detail info per ICO
		for (IntegratedConfiguration ico : icoList) {
			// Create element: CompareReport | IntegratedConfiguration
			xmlWriter.writeStartElement(XML_PREFIX, "IntegratedConfiguration", XML_NS);

			// Create element: CompareReport | IntegratedConfiguration | Name
			xmlWriter.writeStartElement(XML_PREFIX, "Name", XML_NS);
			xmlWriter.writeCharacters(ico.getName());
			// Close element: CompareReport | IntegratedConfiguration | Name
			xmlWriter.writeEndElement();

			// Create element: CompareReport | IntegratedConfiguration | Error
			xmlWriter.writeStartElement(XML_PREFIX, "Error", XML_NS);
			xmlWriter.writeCharacters(ico.getCompareException() == null ? "" : ico.getCompareException().getMessage());
			// Close element: CompareReport | IntegratedConfiguration | Error
			xmlWriter.writeEndElement();
			
			// Create element: CompareReport | IntegratedConfiguration | CompareOverview
			xmlWriter.writeStartElement(XML_PREFIX, "CompareOverview", XML_NS);

			// Create element: CompareReport | IntegratedConfiguration | CompareOverview | Success
			xmlWriter.writeStartElement(XML_PREFIX, "Compared", XML_NS);
			xmlWriter.writeCharacters("" + ico.getTotalCompareSuccess());
			xmlWriter.writeEndElement();

			// Create element: CompareReport | IntegratedConfiguration | CompareOverview | Skipped
			xmlWriter.writeStartElement(XML_PREFIX, "Skipped", XML_NS);
			xmlWriter.writeCharacters("" + ico.getTotalCompareSkipped());
			xmlWriter.writeEndElement();

			// Create element: CompareReport | IcoOverview | Total
			xmlWriter.writeStartElement(XML_PREFIX, "Total", XML_NS);
			xmlWriter.writeCharacters("" + ico.getTotalCompareProcessed());
			xmlWriter.writeEndElement();
			
			// Add compare header data
			addCompareOverview(xmlWriter, ico);	
			
			// Close element: CompareReport | IntegratedConfiguration | CompareOverview
			xmlWriter.writeEndElement();
			
			// Create element: CompareReport | IntegratedConfiguration | CompareOverview | CompareDetails
			xmlWriter.writeStartElement(XML_PREFIX, "CompareDetails", XML_NS);
			// Add compare details
			addCompareDetails(xmlWriter, ico);
			// Close element: CompareReport | IntegratedConfiguration | CompareOverview | CompareDetails
			xmlWriter.writeEndElement();
			
			// Close element: CompareReport | IntegratedConfiguration
			xmlWriter.writeEndElement();
		}
	}
	
	private void addCompareOverview(XMLStreamWriter xmlWriter, IntegratedConfiguration ico) throws XMLStreamException {
		// Create element: CompareReport | IntegratedConfiguration | CompareOverview | Differences
		xmlWriter.writeStartElement(XML_PREFIX, "Differences", XML_NS);
		
		// Create element: CompareReport | IntegratedConfiguration | CompareOverview | Differences | Found
		xmlWriter.writeStartElement(XML_PREFIX, "Found", XML_NS);
		xmlWriter.writeCharacters("" + ico.getTotalCompareDiffsFound());
		// Close element: CompareReport | IntegratedConfiguration | CompareOverview | Differences | Found
		xmlWriter.writeEndElement();
		
		// Create element: CompareReport | IntegratedConfiguration | CompareOverview | Differences | Handled
		xmlWriter.writeStartElement(XML_PREFIX, "Handled", XML_NS);
		xmlWriter.writeCharacters("" + ico.getTotalCompareDiffsIgnored());
		// Close element: CompareReport | IntegratedConfiguration | CompareOverview | Differences | Handled
		xmlWriter.writeEndElement();
		
		// Create element: CompareReport | IntegratedConfiguration | CompareOverview | Differences | Unhandled
		xmlWriter.writeStartElement(XML_PREFIX, "Unhandled", XML_NS);
		xmlWriter.writeCharacters("" + ico.getTotalCompareDiffsUnhandled());
		// Close element: CompareReport | IntegratedConfiguration | CompareOverview | Differences | Unhandled
		xmlWriter.writeEndElement();
		
		// Close element:  CompareReport | IntegratedConfiguration | CompareOverview | Differences
		xmlWriter.writeEndElement();
		
		// Create element: CompareReport | IntegratedConfiguration | CompareOverview | CompareDetails | Compare | ConfiguredExceptions
		xmlWriter.writeStartElement(XML_PREFIX, "ConfiguredExceptions", XML_NS);
		// Add exception data
		addCompareExceptionInfo(xmlWriter, ico);
		
		// Close element: CompareReport | IntegratedConfiguration | CompareOverview | CompareDetails | Compare | ConfiguredExceptions
		xmlWriter.writeEndElement();
	}
	
	private void addCompareDetails(XMLStreamWriter xmlWriter, IntegratedConfiguration ico) throws XMLStreamException {
		
		for (Comparer comp : ico.getCompareProcessedList()) {
			// Create element: | CompareDetails | Compare | 
			xmlWriter.writeStartElement(XML_PREFIX, "Compare", XML_NS);
			
			// Create element: | CompareDetails | Compare | Files
			xmlWriter.writeStartElement(XML_PREFIX, "Files", XML_NS);
			
			// Create element:  | CompareDetails | Compare | Files | Source
			xmlWriter.writeStartElement(XML_PREFIX, "Source", XML_NS);
			xmlWriter.writeCharacters(comp.getSourceFile().toString());
			// Close element: | CompareDetails | Compare | Source
			xmlWriter.writeEndElement();
			
			// Create element:  | CompareDetails | Compare | Files | Target
			xmlWriter.writeStartElement(XML_PREFIX, "Target", XML_NS);
			xmlWriter.writeCharacters(comp.getCompareFile().toString());
			// Close element: | CompareDetails | Compare | Target
			xmlWriter.writeEndElement();
			
			// Close element:  | CompareDetails | Compare | Files
			xmlWriter.writeEndElement();
			
			// Create element: | CompareDetails | Compare | Result
			xmlWriter.writeStartElement(XML_PREFIX, "Result", XML_NS);
			addCompareResult(xmlWriter, comp);			
			// Close element: | CompareDetails | Compare | Result
			xmlWriter.writeEndElement();
			
			// Close element: | CompareDetails | Compare |
			xmlWriter.writeEndElement();
		}
	}
	
	private void addCompareResult(XMLStreamWriter xmlWriter, Comparer comp) throws XMLStreamException {
		
		// Create element: | Status
		xmlWriter.writeStartElement(XML_PREFIX, "Status", XML_NS);
		xmlWriter.writeCharacters(comp.getCompareSkipped() == 1 ? "Skipped" : "Success");
		// Close element: | Found
		xmlWriter.writeEndElement();
		
		// Create element: | Error
		xmlWriter.writeStartElement(XML_PREFIX, "Error", XML_NS);
		xmlWriter.writeCharacters(comp.getCompareException() == null ? "" : comp.getCompareException().getMessage());
		// Close element: | Found
		xmlWriter.writeEndElement();
				
		// Create element: | Found
		xmlWriter.writeStartElement(XML_PREFIX, "Found", XML_NS);
		xmlWriter.writeCharacters("" + comp.getCompareDifferences().size());
		// Close element: | Found
		xmlWriter.writeEndElement();
		
		// Create element: | Handled
		xmlWriter.writeStartElement(XML_PREFIX, "Handled", XML_NS);
		xmlWriter.writeCharacters("" + comp.getDiffsIgnoredByConfiguration().size());
		// Close element: | Handled
		xmlWriter.writeEndElement();	
		
		// Create element: | Unhandled
		xmlWriter.writeStartElement(XML_PREFIX, "Unhandled", XML_NS);
		xmlWriter.writeCharacters("" + (comp.getCompareDifferences().size() - comp.getDiffsIgnoredByConfiguration().size()));
		// Close element: | Unhandled
		xmlWriter.writeEndElement();
				
		// Create element: | DifferenceList
		xmlWriter.writeStartElement(XML_PREFIX, "DifferenceList", XML_NS);
		
		addDifferenceData(xmlWriter, comp);

		// Close element: | DifferenceList
		xmlWriter.writeEndElement();
	}


	private void addDifferenceData(XMLStreamWriter xmlWriter, Comparer comp) throws XMLStreamException {
		// Create for each difference found
		for (Difference d : comp.getCompareDifferences()) {
			// Create element: Difference
			xmlWriter.writeStartElement(XML_PREFIX, "Difference", XML_NS);

			// Create element: | DifferenceList | Difference | XPath
			xmlWriter.writeStartElement(XML_PREFIX, "XPath", XML_NS);
			xmlWriter.writeCharacters(d.getComparison().getTestDetails().getXPath());
			// Close element: | Difference | XPath
			xmlWriter.writeEndElement();

			// Create element: | DifferenceList | Difference | Type
			xmlWriter.writeStartElement(XML_PREFIX, "Type", XML_NS);
			xmlWriter.writeCharacters(d.getResult().name());			
			// Close element: | DifferenceList | Difference | IgnoredByConfiguration
			xmlWriter.writeEndElement();
			
			// Create element: | DifferenceList | Difference | ValueExpected
			xmlWriter.writeStartElement(XML_PREFIX, "ValueExpected", XML_NS);
			xmlWriter.writeCharacters(d.getComparison().getControlDetails().getValue().toString());			
			// Close element: | DifferenceList | Difference | IgnoredByConfiguration
			xmlWriter.writeEndElement();
			
			// Create element: | DifferenceList | Difference | ValueFound
			xmlWriter.writeStartElement(XML_PREFIX, "ValueFound", XML_NS);
			xmlWriter.writeCharacters(d.getComparison().getTestDetails().getValue().toString());			
			// Close element: | DifferenceList | Difference | IgnoredByConfiguration
			xmlWriter.writeEndElement();

			String ignoredXpath = comp.getDiffsIgnoredByConfiguration().get(d.getComparison().getControlDetails().getXPath().toString());
			if (ignoredXpath != null) {
				// Create element: | DifferenceList | Difference | IgnoreMatchFound
				xmlWriter.writeStartElement(XML_PREFIX, "IgnoreMatchFound", XML_NS);
				xmlWriter.writeCharacters(ignoredXpath);			
				// Close element: | DifferenceList | Difference | IgnoredByConfiguration
				xmlWriter.writeEndElement();
			}
			
			// Close element: | DifferenceList | Difference
			xmlWriter.writeEndElement();
		}

	}


	private void addCompareExceptionInfo(XMLStreamWriter xmlWriter, IntegratedConfiguration ico) throws XMLStreamException {
		
		// Add custom exceptions to report for each ICO
		List<String> xpathExceptions = ico.getXpathExceptions();
		for (String xPath : xpathExceptions) {
			// Create element: | Exceptions | Configured | XPath
			xmlWriter.writeStartElement(XML_PREFIX, "XPath", XML_NS);
			xmlWriter.writeCharacters(xPath);
			// Close element: | Exceptions | Configured | XPath
			xmlWriter.writeEndElement();
		}
	}


	private void addIntegrationConfigurationGlobalOverview(XMLStreamWriter xmlWriter) throws XMLStreamException {
		// Create element: CompareReport | IcoOverview
		xmlWriter.writeStartElement(XML_PREFIX, "IcoOverview", XML_NS);

		// Create element: CompareReport | IcoOverview | Success
		xmlWriter.writeStartElement(XML_PREFIX, "Success", XML_NS);
		xmlWriter.writeCharacters("" + this.countIcoCompared);
		xmlWriter.writeEndElement();

		// Create element: CompareReport | IcoOverview | TechnicalError
		xmlWriter.writeStartElement(XML_PREFIX, "Error", XML_NS);
		xmlWriter.writeCharacters("" + this.countIcoNotCompared);
		xmlWriter.writeEndElement();

		// Create element: CompareReport | IcoOverview | Total
		xmlWriter.writeStartElement(XML_PREFIX, "Total", XML_NS);
		xmlWriter.writeCharacters("" + this.countIcoTotal);
		xmlWriter.writeEndElement();

		// Close element: CompareReport | IcoOverview
		xmlWriter.writeEndElement();
	}
}
