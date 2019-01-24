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
import com.invixo.common.util.Util;
import com.invixo.consistency.FileStructure2;
import com.invixo.main.Main;


public abstract class IntegratedConfigurationMain {
	/*====================================================================================
	 *------------- Class variables
	 *====================================================================================*/
	private static Logger logger 			= Logger.getInstance();
	private static final String LOCATION 	= IntegratedConfigurationMain.class.getName();	
	
	private static final String ELEMENT_QOS				= "{urn:com.sap.aii.mdt.server.adapterframework.ws}qualityOfService";
	private static final String ELEMENT_MAX_MSG			= "{urn:com.sap.aii.mdt.server.adapterframework.ws}maxMessages";
	private static final String ELEMENT_ITF_NAME		= "{urn:com.sap.aii.mdt.api.data}name";
	private static final String ELEMENT_ITF_NS			= "{urn:com.sap.aii.mdt.api.data}namespace";
	private static final String ELEMENT_ITF_SPARTY		= "{urn:com.sap.aii.mdt.api.data}senderParty";
	private static final String ELEMENT_ITF_SCOMPONENT	= "{urn:com.sap.aii.mdt.api.data}senderComponent";
	private static final String ELEMENT_ITF_RPARTY		= "{urn:com.sap.aii.mdt.api.data}receiverParty";
	private static final String ELEMENT_ITF_RCOMPONENT	= "{urn:com.sap.aii.mdt.api.data}receiverComponent";
	
	private static final String MAP_FILE				= FileStructure2.DIR_CONFIG + "systemMapping.txt";
	private static final String SOURCE_ENV_ICO_REQUESTS	= Main.PARAM_VAL_ICO_REQUEST_FILES_ENV;
//	private static final String SOURCE_ENV 				= Main.PARAM_VAL_SOURCE_ENV;
	private static final String TARGET_ENV 				= Main.PARAM_VAL_TARGET_ENV;
	protected static HashMap<String, String> SYSTEM_MAP	= initializeSystemMap();
	
	
	/*====================================================================================
	 *------------- Instance variables
	 *====================================================================================*/
	protected String name = null;			// Name of ICO
	protected String fileName = null;		// Complete path to ICO request file
	
	// Extracts from ICO request file
	protected String senderParty = null;
	protected String senderComponent = null;
	protected String interfaceName = null;
	protected String namespace = null;
	protected String receiverParty = null;
	protected String receiverComponent = null;
	protected String qualityOfService = null;
	protected int maxMessages = 0;
	protected Exception ex = null;			// Error details
	
	
	
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
	public void setSenderComponent(String senderComponent) {
		this.senderComponent = senderComponent;
	}
	public String getinterfaceName() {
		return interfaceName;
	}
	public void setInterfaceName(String interfaceName) {
		this.interfaceName = interfaceName;
	}
	public String getNamespace() {
		return namespace;
	}
	public void setNamespace(String namespace) {
		this.namespace = namespace;
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
	public void setReceiverComponent(String receiverComponent) {
		this.receiverComponent = receiverComponent;
	}
	public String getQualityOfService() {
		return qualityOfService;
	}
	public void setQualityOfService(String qualityOfService) {
		this.qualityOfService = qualityOfService;
	}
	public int getMaxMessages() {
		return maxMessages;
	}
	public void setMaxMessages(int maxMessages) {
		this.maxMessages = maxMessages;
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
			if ("DEV".equals(SOURCE_ENV_ICO_REQUESTS)) {
				sourceIndex = 0;
			} else if ("TST".equals(SOURCE_ENV_ICO_REQUESTS)) {
				sourceIndex = 1;
			} else {
				sourceIndex = 2;
			}
			
			// Determine target index (which target system to map to when injecting)
			int targetIndex = -1;
			if ("DEV".equals(TARGET_ENV)) {
				targetIndex = 0;
			} else if ("TST".equals(TARGET_ENV)) {
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
	protected abstract void initialize() throws GeneralException;
	
	
	public void checkDataExtract() throws GeneralException {
		final String SIGNATURE = "checkDataExtract()";
		
		StringWriter sw = new StringWriter();
		if (this.getSenderComponent() == null) {
			sw.write("'senderComponent' not present in ICO request file. This is mandatory.\n");
		}
		if (this.getinterfaceName() == null) {
			sw.write("'name' (interface name) not present in ICO request file. This is mandatory.\n");
		}
		if (this.getNamespace() == null) {
			sw.write("'namespace' not present in ICO request file. This is mandatory.\n");
		}
		
		if (this.getQualityOfService() == null) {
			sw.write("'qualityOfService' not present in ICO request file. This is mandatory.\n");
		}
		
		// Throw exception in case errors was found
		if (!sw.toString().equals("")) {
			String msg = "Input validation error.\n" + sw.toString();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new GeneralException(msg);
		}
	}
	
	
	/**
	 * Extract routing info etc from an Integrated Configuration request file used also when extracting data from SAP PO system.
	 * @param containerStartElement
	 * @throws GeneralException
	 */
	public void extractInfoFromIcoRequest(String containerStartElement) throws GeneralException {
		final String SIGNATURE = "extractInfoFromIcoRequest(String)";
		try {
			// Read file
			byte[] fileContent = Util.readFile(getFileName());
			
			// Read XML file and extract data
			XMLInputFactory factory = XMLInputFactory.newInstance();
			StreamSource ss = new StreamSource(new ByteArrayInputStream(fileContent));
			XMLEventReader eventReader = factory.createXMLEventReader(ss);
			
			boolean fetchData = false;
			while (eventReader.hasNext()) {
			    XMLEvent event = eventReader.nextEvent();
			    
			    switch(event.getEventType()) {
			    case XMLStreamConstants.START_ELEMENT:
			    	String currentElementName = event.asStartElement().getName().toString();

					// Quality of Service
					if (ELEMENT_QOS.equals(currentElementName)) {
						if (eventReader.peek().isCharacters()) {
							setQualityOfService(eventReader.peek().asCharacters().getData());	
						}
					
					// Max message count (only relevant for extraction)
					} else if (ELEMENT_MAX_MSG.equals(currentElementName)) {
						if (eventReader.peek().isCharacters()) {
							setMaxMessages(Integer.parseInt(eventReader.peek().asCharacters().getData()));	
						}
			    	
			    	// interface start
					} else if (containerStartElement.equals(currentElementName)) {
			    		fetchData = true;
			    		
			    	// Sender interface name
			    	} else if (fetchData && ELEMENT_ITF_NAME.equals(currentElementName)) {
			    		if (eventReader.peek().isCharacters()) {
			    			setInterfaceName(eventReader.peek().asCharacters().getData());
			    		}
			    		
			    	// Sender interface namespace
			    	} else if (fetchData && ELEMENT_ITF_NS.equals(currentElementName)) {
			    		if (eventReader.peek().isCharacters()) {
			    			setNamespace(eventReader.peek().asCharacters().getData());	
			    		}
			    		
			    	// Sender party
			    	} else if (fetchData && ELEMENT_ITF_SPARTY.equals(currentElementName)) {
			    		if (eventReader.peek().isCharacters()) {
			    			setSenderParty(eventReader.peek().asCharacters().getData());	
			    		}
			    		
			    	// Sender component
			    	} else if (fetchData && ELEMENT_ITF_SCOMPONENT.equals(currentElementName)) {
			    		if (eventReader.peek().isCharacters()) {
			    			String sender = eventReader.peek().asCharacters().getData();
			    			setSenderComponent(SYSTEM_MAP.get(sender));
			    			
			    			// Check
			    			if (getSenderComponent() == null) {
			    				String ex = "System Mapping: missing entry for source system " + sender;
			    				logger.writeError(LOCATION, SIGNATURE, ex);
			    				throw new GeneralException(ex);
			    			}
			    		}
			    	
			    	// Receiver party
			    	} else if (fetchData && ELEMENT_ITF_RPARTY.equals(currentElementName)) {
			    		if (eventReader.peek().isCharacters()) {
			    			setReceiverParty(eventReader.peek().asCharacters().getData());	
			    		}
			    		
			    	// Receiver component
			    	} else if (fetchData && ELEMENT_ITF_RCOMPONENT.equals(currentElementName)) {
			    		if (eventReader.peek().isCharacters()) {
			    			setReceiverComponent(eventReader.peek().asCharacters().getData());	
			    		}
			    	}	    	
			    	break;
			    case XMLStreamConstants.END_ELEMENT:
			    	if (containerStartElement.equals(event.asEndElement().getName().toString())) {
			    		fetchData = false;
			    	}
			    	break;
			    }
			}
		} catch (XMLStreamException e) {
			String msg = "Error extracting routing info from ICO request file: " + getFileName() + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new GeneralException(msg);
		} 
	}

}
