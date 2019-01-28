package com.invixo.compare.reporting;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import com.invixo.consistency.FileStructure;
import com.invixo.main.GlobalParameters;
import com.invixo.compare.IntegratedConfiguration;

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
		for (IntegratedConfiguration ico : icoList) {
			if (ico.compareExeptionsThrown.size() > 0) {
				this.countIcoNotCompared++;
			} else {
				this.countIcoCompared++;
			}
		}
		
		
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
			xmlWriter.writeCharacters(ico.getSourceIcoName());
			// Close element: CompareReport | IntegratedConfiguration | Name
			xmlWriter.writeEndElement();

			// Create element: CompareReport | IntegratedConfiguration | Error
			xmlWriter.writeStartElement(XML_PREFIX, "Error", XML_NS);
			
			if (ico.compareExeptionsThrown.size() > 0) {
				xmlWriter.writeCharacters(ico.compareExeptionsThrown.get(0).getMessage());
			}
			
			// Close element: CompareReport | IntegratedConfiguration | Error
			xmlWriter.writeEndElement();
			
			// Create element: CompareReport | IntegratedConfiguration | CompareOverview
			xmlWriter.writeStartElement(XML_PREFIX, "CompareOverview", XML_NS);

			// Add compare header data
			addCompareOverview(xmlWriter, ico);	
			
			// Close element: CompareReport | IntegratedConfiguration | CompareOverview
			xmlWriter.writeEndElement();
			
			// Close element: CompareReport | IntegratedConfiguration
			xmlWriter.writeEndElement();
		}
	}
	
	private void addCompareOverview(XMLStreamWriter xmlWriter, IntegratedConfiguration ico) throws XMLStreamException {
		// Create Element: CompareReport | IntegratedConfiguration | CompareOverview | Total
		xmlWriter.writeStartElement(XML_PREFIX, "Total", XML_NS);
		xmlWriter.writeCharacters("" + ico.compareFilesProcessed.size());
		// Close Element: CompareReport | IntegratedConfiguration | CompareOverview | Total
		xmlWriter.writeEndElement();
		
		// Create element: CompareReport | IntegratedConfiguration | CompareOverview | Success
		xmlWriter.writeStartElement(XML_PREFIX, "Success", XML_NS);
		xmlWriter.writeCharacters("" + ico.compareSuccessCounter);
		// Close element: CompareReport | IntegratedConfiguration | CompareOverview | Success
		xmlWriter.writeEndElement();
		
		// Create element: CompareReport | IntegratedConfiguration | CompareOverview | Error
		xmlWriter.writeStartElement(XML_PREFIX, "Error", XML_NS);
		xmlWriter.writeCharacters("" + ico.compareErrorsCounter);
		// Close element: CompareReport | IntegratedConfiguration | CompareOverview | Error
		xmlWriter.writeEndElement();
		
		// Create element: CompareReport | IntegratedConfiguration | CompareOverview | CompareDetails | Compare | ConfiguredExceptions
		xmlWriter.writeStartElement(XML_PREFIX, "ConfiguredExceptions", XML_NS);
		// Add exception data
		addCompareExceptionInfo(xmlWriter, ico);
		
		// Close element: CompareReport | IntegratedConfiguration | CompareOverview | CompareDetails | Compare | ConfiguredExceptions
		xmlWriter.writeEndElement();
		
		// Create element: CompareReport | IntegratedConfiguration | CompareOverview | CompareDetails
		xmlWriter.writeStartElement(XML_PREFIX, "CompareDetails", XML_NS);
		// Add compare details
		addCompareDetails(xmlWriter, ico);
		// Close element: CompareReport | IntegratedConfiguration | CompareOverview | CompareDetails
		xmlWriter.writeEndElement();
	}
	
	private void addCompareDetails(XMLStreamWriter xmlWriter, IntegratedConfiguration ico) throws XMLStreamException {
		
		// Create for each file compare set
		Map<Path, Path> comparedFiles = ico.compareFilesProcessed;
		for (Map.Entry<Path, Path> compareSet : comparedFiles.entrySet())
		{
			// Create element: | CompareDetails | Compare | 
			xmlWriter.writeStartElement(XML_PREFIX, "Compare", XML_NS);
			
			// Create element: | CompareDetails | Compare | Files
			xmlWriter.writeStartElement(XML_PREFIX, "Files", XML_NS);
			
			// Create element:  | CompareDetails | Compare | Files | Source
			xmlWriter.writeStartElement(XML_PREFIX, "Source", XML_NS);
			xmlWriter.writeCharacters(compareSet.getKey().toString());
			// Close element: | CompareDetails | Compare | Source
			xmlWriter.writeEndElement();
			
			// Create element:  | CompareDetails | Compare | Files | Target
			xmlWriter.writeStartElement(XML_PREFIX, "Target", XML_NS);
			xmlWriter.writeCharacters(compareSet.getValue().toString());
			// Close element: | CompareDetails | Compare | Target
			xmlWriter.writeEndElement();
			
			// Close element:  | CompareDetails | Compare | Files
			xmlWriter.writeEndElement();
			
			// Create element: | CompareDetails | Compare | Result
			xmlWriter.writeStartElement(XML_PREFIX, "Result", XML_NS);
			addCompareResult(xmlWriter, ico);			
			// Close element: | CompareDetails | Compare | Result
			xmlWriter.writeEndElement();
			
			// Close element: | CompareDetails | Compare |
			xmlWriter.writeEndElement();
		}
	}
	
	private void addCompareResult(XMLStreamWriter xmlWriter, IntegratedConfiguration ico) throws XMLStreamException {
		// Create element: | Found
		int diffsFoundCounter = ico.compareDiffsFound.size();
		xmlWriter.writeStartElement(XML_PREFIX, "Found", XML_NS);
		xmlWriter.writeCharacters("" + diffsFoundCounter);
		// Close element: | Found
		xmlWriter.writeEndElement();
		
		// Create element: | Ignored
		int diffsIgnoredCounter = ico.compareExceptionsIgnored.size();
		xmlWriter.writeStartElement(XML_PREFIX, "Ignored", XML_NS);
		xmlWriter.writeCharacters("" + diffsIgnoredCounter);
		// Close element: | Ignored
		xmlWriter.writeEndElement();	
		
		// Create element: | Total
		xmlWriter.writeStartElement(XML_PREFIX, "Total", XML_NS);
		xmlWriter.writeCharacters("" + (diffsFoundCounter - diffsIgnoredCounter));
		// Close element: | Total
		xmlWriter.writeEndElement();	
		
		// Create element: | DifferenceList
		xmlWriter.writeStartElement(XML_PREFIX, "DifferenceList", XML_NS);
		
		// Create for each difference found
		List<String> diffsFound = ico.compareDiffsFound;
		for (String diff : diffsFound) {
			
			// Create element: Difference
			xmlWriter.writeStartElement(XML_PREFIX, "Difference", XML_NS);
			
			// Create element: | DifferenceList | Difference | XPath
			xmlWriter.writeStartElement(XML_PREFIX, "XPath", XML_NS);
			xmlWriter.writeCharacters(diff);
			// Close element: | Difference | XPath
			xmlWriter.writeEndElement();
			
			// Create element: | DifferenceList | Difference | IgnoredByConfiguration
			xmlWriter.writeStartElement(XML_PREFIX, "IgnoredByConfiguration", XML_NS);
			
			String xpathIgnoreString = ico.compareExceptionsIgnored.get(diff);
			if ( xpathIgnoreString == null) {
				xpathIgnoreString = "false";
			}
			xmlWriter.writeCharacters(xpathIgnoreString);
			
			// Close element: | DifferenceList | Difference | IgnoredByConfiguration
			xmlWriter.writeEndElement();
			
			// Close element: | DifferenceList | Difference
			xmlWriter.writeEndElement();
		}

		// Close element: | DifferenceList
		xmlWriter.writeEndElement();
	}


	private void addCompareExceptionInfo(XMLStreamWriter xmlWriter, IntegratedConfiguration ico) throws XMLStreamException {
		
		// Add custom exceptions to report for each ICO
		List<String> xpathExceptions = ico.xpathExceptions;
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

		// Create element: CompareReport | IcoOverview | Total
		xmlWriter.writeStartElement(XML_PREFIX, "Total", XML_NS);
		xmlWriter.writeCharacters("" + this.countIcoTotal);
		xmlWriter.writeEndElement();

		// Create element: CompareReport | IcoOverview | Success
		xmlWriter.writeStartElement(XML_PREFIX, "Success", XML_NS);
		xmlWriter.writeCharacters("" + this.countIcoCompared);
		xmlWriter.writeEndElement();

		// Create element: CompareReport | IcoOverview | TechnicalError
		xmlWriter.writeStartElement(XML_PREFIX, "TechnicalError", XML_NS);
		xmlWriter.writeCharacters("" + this. countIcoNotCompared);
		xmlWriter.writeEndElement();

		// Close element: CompareReport | IcoOverview
		xmlWriter.writeEndElement();
	}
}
