package com.invixo.common;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamSource;

import com.invixo.common.util.Logger;
import com.invixo.common.util.PropertyAccessor;
import com.invixo.common.util.Util;
import com.invixo.consistency.FileStructure;
import com.invixo.main.Main;


public class IntegratedConfigurationMain {
	/*====================================================================================
	 *------------- Class variables
	 *====================================================================================*/
	private static Logger logger 			= Logger.getInstance();
	private static final String LOCATION 	= IntegratedConfigurationMain.class.getName();	
	
	// XML Elements: Sender details
	private static final String ELEMENT_SITF_SCOMPONENT	= "{urn:com.sap.aii.mdt.server.adapterframework.ws}senderName";
	private static final String ELEMENT_SITF_ROOT		= "{urn:com.sap.aii.mdt.server.adapterframework.ws}senderInterface";
	private static final String ELEMENT_SITF_NAME		= "{urn:com.sap.aii.mdt.api.data}name";
	private static final String ELEMENT_SITF_NS			= "{urn:com.sap.aii.mdt.api.data}namespace";
	
	// XML Elements: Receiver details
	private static final String ELEMENT_RITF_COMPONENT	= "{urn:com.sap.aii.mdt.server.adapterframework.ws}receiverName";
	private static final String ELEMENT_RITF_ROOT		= "{urn:com.sap.aii.mdt.server.adapterframework.ws}interface";
	private static final String ELEMENT_RITF_NAME		= "{urn:com.sap.aii.mdt.api.data}name";
	private static final String ELEMENT_RITF_NS			= "{urn:com.sap.aii.mdt.api.data}namespace";
	
	// XML Elements: Other
	private static final String ELEMENT_TIME_FROM		= "{urn:com.sap.aii.mdt.server.adapterframework.ws}toTime";
	private static final String ELEMENT_TIME_TO			= "{urn:com.sap.aii.mdt.server.adapterframework.ws}fromTime";
	private static final String ELEMENT_QOS				= "{urn:com.sap.aii.mdt.server.adapterframework.ws}qualityOfService";
	private static final String ELEMENT_MAX_MSG			= "{urn:AdapterMessageMonitoringVi}maxMessages";

	
	private static final boolean OVERRULE_MSG_SIZE 		= Boolean.parseBoolean(PropertyAccessor.getProperty("OVERRULE_MSG_SIZE"));
	private static final int MAX_MSG_SIZE_OVERRULED 	= Integer.parseInt(PropertyAccessor.getProperty("MESSAGE_SIZE_OVERRULED"));
	
	private static final String MAP_FILE				= FileStructure.DIR_CONFIG + "systemMapping.txt";
	private static final String SOURCE_ENV_ICO_REQUESTS	= Main.PARAM_VAL_ICO_REQUEST_FILES_ENV;
	private static final String TARGET_ENV 				= Main.PARAM_VAL_TARGET_ENV;
	private static HashMap<String, String> SYSTEM_MAP	= initializeSystemMap();
	
	
	/*====================================================================================
	 *------------- Instance variables
	 *====================================================================================*/
	protected String name = null;					// Name of ICO
	protected String fileName = null;				// Complete path to ICO request file
	
	// Extracts from ICO request file: SENDER
	protected String senderParty = null;			// NOT EXTRACTED YET (need to see how it works and where to extract from)
	protected String senderComponent = null;		// /urn:getMessageList/urn:filter/urn1:senderName
	protected String senderInterface = null;		// /urn:getMessageList/urn:filter/urn1:senderInterface/urn2:name
	protected String senderNamespace = null;		// /urn:getMessageList/urn:filter/urn1:senderInterface/urn2:namespace
	
	// Extracts from ICO request file: RECEIVER
	protected String receiverParty = null;			// NOT EXTRACTED YET (need to see how it works and where to extract from)
	protected String receiverComponent = null;		// /urn:getMessageList/urn:filter/urn1:receiverName
	protected String receiverInterfaceName = null;	// /urn:getMessageList/urn:filter/urn1:interface/urn2:name
	protected String receiverNamespace = null;		// /urn:getMessageList/urn:filter/urn1:interface/urn2:namespace
	
	// Extracts from ICO request file: VARIOUS
	protected String qualityOfService = null;		// /urn:getMessageList/urn:filter/urn1:qualityOfService
	protected String fetchFromTime = null;			// /urn:getMessageList/urn:filter/urn1:fromTime
	protected String fetchToTime = null;			// /urn:getMessageList/urn:filter/urn1:toTime
	protected int maxMessages = 0;					// /urn:getMessageList/urn:maxMessages
	
	// Others
	protected Exception ex = null;					// Error details
	
	

	/*====================================================================================
	 *------------- Main (for testing)
	 *====================================================================================*/
	public static void main(String[] args) throws GeneralException {
	}
	
	
	
	/*====================================================================================
	 *------------- Constructors
	 *====================================================================================*/
	public IntegratedConfigurationMain(String icoFileName) throws GeneralException {
		this.fileName = icoFileName;
		this.name = Util.getFileName(icoFileName, false);
	}
	

	
	/*====================================================================================
	 *------------- Getters and Setters
	 *====================================================================================*/
	public String getName() {
		return this.name;
	}
	public String getFileName() {
		return this.fileName;
	}	
	public String getSenderParty() {
		return senderParty;
	}
	public void setSenderParty(String senderParty) {
		this.senderParty = senderParty;
	}
	public String getSenderComponent() {
		return senderComponent;
	}
	public void setSenderComponent(String senderComponent) throws GeneralException {
		final String SIGNATURE = "setSenderComponent";
		String mappedSys = mapSystem(senderComponent);
		if (mappedSys == null) {
			String ex = "Sender System '" + senderComponent + "' not found in mapping table.";
			logger.writeError(LOCATION, SIGNATURE, ex);
			throw new GeneralException(ex);
		} else {
			this.senderComponent = mappedSys;	
		}
	}
	public String getSenderInterface() {
		return senderInterface;
	}
	public void setSenderInterface(String senderInterface) {
		this.senderInterface = senderInterface;
	}
	public String getSenderNamespace() {
		return senderNamespace;
	}
	public void setSenderNamespace(String senderNamespace) {
		this.senderNamespace = senderNamespace;
	}
	public String getReceiverParty() {
		return receiverParty;
	}
	public void setReceiverParty(String receiverParty) {
		this.receiverParty = receiverParty;
	}
	public String getReceiverComponent() {
		return receiverComponent;
	}
	public void setReceiverComponent(String receiverComponent) throws GeneralException {
		final String SIGNATURE = "setReceiverComponent";
		String mappedSys = mapSystem(receiverComponent);
		if (mappedSys == null) {
			String ex = "Receiver System '" + receiverComponent + "' not found in mapping table.";
			logger.writeError(LOCATION, SIGNATURE, ex);
			throw new GeneralException(ex);
		} else {
			this.receiverComponent = mappedSys;			
		}
	}
	public String getReceiverInterfaceName() {
		return receiverInterfaceName;
	}
	public void setReceiverInterfaceName(String receiverInterfaceName) {
		this.receiverInterfaceName = receiverInterfaceName;
	}
	public String getReceiverNamespace() {
		return receiverNamespace;
	}
	public void setReceiverNamespace(String receiverNamespace) {
		this.receiverNamespace = receiverNamespace;
	}
	public String getQualityOfService() {
		return qualityOfService;
	}
	public void setQualityOfService(String qualityOfService) {
		this.qualityOfService = qualityOfService;
	}
	public String getFetchFromTime() {
		return fetchFromTime;
	}
	public void setFetchFromTime(String fetchFromTime) {
		this.fetchFromTime = fetchFromTime;
	}
	public String getFetchToTime() {
		return fetchToTime;
	}
	public void setFetchToTime(String fetchToTime) {
		this.fetchToTime = fetchToTime;
	}
	public int getMaxMessages() {
		return maxMessages;
	}
	public void setMaxMessages(int maxMessages) {
		final String SIGNATURE = "setMaxMessages(int)";
		if (OVERRULE_MSG_SIZE) {
			this.maxMessages = MAX_MSG_SIZE_OVERRULED;
			logger.writeDebug(LOCATION, SIGNATURE, "Max message size is being overruled due to technical parameter set. Value used for all: " + MAX_MSG_SIZE_OVERRULED);
		} else {
			this.maxMessages = maxMessages;			
		}
	}
	public Exception getEx() {
		return this.ex;
	}
	public Exception setEx(Exception ex) {
		return this.ex;
	}
	

	/*====================================================================================
	 *------------- Class methods
	 *====================================================================================*/
	private static HashMap<String, String> initializeSystemMap() {
		final String SIGNATURE = "initializeSystemMap()";
		try {
			// Determine source index (how the request ICO's are created)
			int sourceIndex = -1;
			if (Main.Environment.DEV.toString().equals(SOURCE_ENV_ICO_REQUESTS)) {
				sourceIndex = 0;
			} else if (Main.Environment.TST.toString().equals(SOURCE_ENV_ICO_REQUESTS)) {
				sourceIndex = 1;
			} else {
				sourceIndex = 2;
			}
			
			// Determine target index (which target system to map to when injecting)
			int targetIndex = -1;
			if (Main.Environment.DEV.toString().equals(TARGET_ENV)) {
				targetIndex = 0;
			} else if (Main.Environment.TST.toString().equals(TARGET_ENV)) {
				targetIndex = 1;
			} else {
				targetIndex = 2;
			}
			
			// Populate map
	 		SYSTEM_MAP = new HashMap<String, String>();
	 		String line;
	 		FileReader fileReader = new FileReader(MAP_FILE);
	 		try (BufferedReader bufferedReader = new BufferedReader(fileReader)) {
	 			while((line = bufferedReader.readLine()) != null) {
	 				String[] str = line.split("\\|");
	 				SYSTEM_MAP.put(str[sourceIndex], str[targetIndex]);
	 			}			   
	 		}

		    // Return initialized map
		    logger.writeDebug(LOCATION, SIGNATURE, "System mapping initialized. Source ENV '" + SOURCE_ENV_ICO_REQUESTS + "'. Target ENV '" + TARGET_ENV + "'. Number of entries: " + SYSTEM_MAP.size());
		    return SYSTEM_MAP;			
		} catch (IOException e) {
			String msg = "Error generating system mapping\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		}
	}

	
	
	/*====================================================================================
	 *------------- Instance methods
	 *====================================================================================*/
	public void checkDataExtract() throws GeneralException {
		final String SIGNATURE = "checkDataExtract()";
		
		StringWriter sw = new StringWriter();
		if (this.getSenderComponent() == null) {
			sw.write("'/urn:getMessageList/urn:filter/urn1:senderName' not present in ICO request file. This is mandatory.\n");
		}
		
		if (this.getSenderInterface() == null) {
			sw.write("'/urn:getMessageList/urn:filter/urn1:senderInterface/urn2:name' not present in ICO request file. This is mandatory.\n");
		}
		
		if (this.getSenderNamespace() == null) {
			sw.write("'/urn:getMessageList/urn:filter/urn1:senderInterface/urn2:namespace' not present in ICO request file. This is mandatory.\n");
		}
		
		if (this.getReceiverComponent() == null) {
			sw.write("'/urn:getMessageList/urn:filter/urn1:receiverName' not present in ICO request file. This is mandatory.\n");
		}
		
		if (this.getReceiverInterfaceName() == null) {
			sw.write("'/urn:getMessageList/urn:filter/urn1:interface/urn2:name' not present in ICO request file. This is mandatory.\n");
		}
		
		if (this.getReceiverNamespace() == null) {
			sw.write("'/urn:getMessageList/urn:filter/urn1:interface/urn2:namespace' not present in ICO request file. This is mandatory.\n");
		}
		
		if (this.getQualityOfService() == null) {
			sw.write("'/urn:getMessageList/urn:filter/urn1:qualityOfService' not present in ICO request file. This is mandatory.\n");
		}
		
		if (this.getMaxMessages() == 0) {
			sw.write("'/urn:getMessageList/urn:maxMessages' must be set to a value greather than 0 in ICO request file. This is mandatory.\n");
		}
		
		// Throw exception in case errors was found
		if (!sw.toString().equals("")) {
			String msg = "Input validation error.\n" + sw.toString();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new GeneralException(msg);
		}
	}
	
	
	/**
	 * Extract various basic info from an Integrated Configuration request file. The extracted info is required when 
	 * extracting and injecting to SAP PO.
	 * @throws GeneralException
	 */
	public void extractInfoFromIcoRequest() throws GeneralException {
		final String SIGNATURE = "extractInfoFromIcoRequest()";
		try {
			// Get ICO file content
			byte[] fileContent = Util.readFile(this.getFileName());
			
			// Prepare
			XMLInputFactory factory = XMLInputFactory.newInstance();
			StreamSource ss = new StreamSource(new ByteArrayInputStream(fileContent));
			XMLEventReader eventReader = factory.createXMLEventReader(ss);
			
			// Parse XML file and extract data
			boolean fetchSenderData = false;
			boolean fetchReceiverData = false;
			while (eventReader.hasNext()) {
			    XMLEvent event = eventReader.nextEvent();
			    
			    switch(event.getEventType()) {
			    case XMLStreamConstants.START_ELEMENT:
			    	String currentStartElementName = event.asStartElement().getName().toString();
			    	System.out.println("START: " + currentStartElementName);

					// Quality of Service
					if (ELEMENT_QOS.equals(currentStartElementName)) {
						if (eventReader.peek().isCharacters()) {
							this.setQualityOfService(eventReader.peek().asCharacters().getData());	
						}
					
					// Max message count (only relevant for extraction)
					} else if (ELEMENT_MAX_MSG.equals(currentStartElementName)) {
						if (eventReader.peek().isCharacters()) {
							this.setMaxMessages(Integer.parseInt(eventReader.peek().asCharacters().getData()));	
						}
			    	
			    	// Time, From
					} else if (ELEMENT_TIME_FROM.equals(currentStartElementName)) {
						if (eventReader.peek().isCharacters()) {
							this.setFetchFromTime(eventReader.peek().asCharacters().getData());	
						}
			    		
			    	// Time, To
					} else if (ELEMENT_TIME_TO.equals(currentStartElementName)) {
						if (eventReader.peek().isCharacters()) {
							this.setFetchToTime(eventReader.peek().asCharacters().getData());	
						}
						
			    	// Sender Component
			    	} else if (ELEMENT_SITF_SCOMPONENT.equals(currentStartElementName)) {
			    		if (eventReader.peek().isCharacters()) {
			    			this.setSenderComponent(eventReader.peek().asCharacters().getData());
			    		}
			    		
			    	// Contained for Sender Interface and Sender Namespace
			    	} else if (ELEMENT_SITF_ROOT.equals(currentStartElementName)) {
			    			fetchSenderData = true;
			    		
			    	// Sender Interface Name
			    	} else if (fetchSenderData && ELEMENT_SITF_NAME.equals(currentStartElementName)) {
			    		if (eventReader.peek().isCharacters()) {
			    			this.setSenderInterface(eventReader.peek().asCharacters().getData());
			    		}
			    		
			    	// Sender Interface Namespace
			    	} else if (fetchSenderData && ELEMENT_SITF_NS.equals(currentStartElementName)) {
			    		if (eventReader.peek().isCharacters()) {
			    			this.setSenderNamespace(eventReader.peek().asCharacters().getData());	
			    		}
			    		
			    	// Receiver Component
			    	} else if (ELEMENT_RITF_COMPONENT.equals(currentStartElementName)) {
			    		if (eventReader.peek().isCharacters()) {
			    			this.setReceiverComponent(eventReader.peek().asCharacters().getData());
			    		}
			    		
			    	// Contained for Receiver Interface and Receiver Namespace
			    	} else if (ELEMENT_RITF_ROOT.equals(currentStartElementName)) {
			    			fetchReceiverData = true;
			    		
			    	// Receiver Interface Name
			    	} else if (fetchReceiverData && ELEMENT_RITF_NAME.equals(currentStartElementName)) {
			    		if (eventReader.peek().isCharacters()) {
			    			this.setReceiverInterfaceName(eventReader.peek().asCharacters().getData());
			    		}
			    		
			    	// Render Interface Namespace
			    	} else if (ELEMENT_RITF_NS.equals(currentStartElementName)) {
			    		if (eventReader.peek().isCharacters()) {
			    			this.setReceiverNamespace(eventReader.peek().asCharacters().getData());	
			    		}
			    	}	    	
			    	break;
			    	
			    case XMLStreamConstants.END_ELEMENT:
			    	String currentEndElementName = event.asEndElement().getName().toString();
			    	System.out.println("END: " + currentEndElementName);
			    	
			    	if (ELEMENT_SITF_ROOT.equals(currentEndElementName)) {
			    		fetchSenderData = false;
			    	} else if (ELEMENT_RITF_ROOT.equals(currentEndElementName)) {
			    		fetchReceiverData = false;
			    	}
			    	break;
			    }
			}
		} catch (XMLStreamException e) {
			String msg = "Error extracting info from ICO request file: " + getFileName() + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new GeneralException(msg);
		} 
	}
	
	
	private static String mapSystem(String key) {
		String value = SYSTEM_MAP.get(key);
		if (value == null) {
			value = "";
		}
		return value;
	}

}
