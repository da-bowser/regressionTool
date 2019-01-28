package com.invixo.compare.reporting;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import com.invixo.consistency.FileStructure;
import com.invixo.compare.IntegratedConfiguration;

public class ReportWriter {
	private static final String	XML_PREFIX = "inv";
	private static final String	XML_NS = "urn:invixo.com.compare.report";

	private final String REPORT_FILE = FileStructure.DIR_REPORTS + "ExtractReport_" + System.currentTimeMillis() + ".xml";
	private ArrayList<IntegratedConfiguration>	icoList;

	// ICO general
	private int	countIcoTotal = 0;						// Total number of ICOs processed
	private int	countIcoCompared = 0;					// Total number of ICOs compared
	private int	countIcoNotCompared = 0;				// Total number of ICOs not compared


	// MessageKey general
	private int	countMsgKeyTotal				= 0;	// Total message keys processed
	

	public ReportWriter(ArrayList<IntegratedConfiguration> icoList) {
		this.icoList = icoList;

		// Interpret general data
		this.evaluateGeneralResults();
	}


	public void evaluateGeneralResults() {
		
	}
	
	public String create(ArrayList<IntegratedConfiguration> icoList) {
		return REPORT_FILE;
		
	}
}
