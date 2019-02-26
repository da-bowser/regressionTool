package com.invixo.common;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

import com.invixo.common.util.Logger;
import com.invixo.common.util.PropertyAccessor;
import com.invixo.common.util.Util;
import com.invixo.consistency.FileStructure;
import com.invixo.main.GlobalParameters;

public class IntegratedConfigurationMain {
	/*====================================================================================
	 *------------- Class variables
	 *====================================================================================*/
	private static Logger logger 			= Logger.getInstance();
	private static final String LOCATION 	= IntegratedConfigurationMain.class.getName();	

	private static final boolean OVERRULE_MSG_SIZE 		= Boolean.parseBoolean(PropertyAccessor.getProperty("OVERRULE_MSG_SIZE"));
	private static final int MAX_MSG_SIZE_OVERRULED 	= Integer.parseInt(PropertyAccessor.getProperty("MESSAGE_SIZE_OVERRULED"));
	
	private static final String SOURCE_ENV_ICO_REQUESTS	= GlobalParameters.PARAM_VAL_ICO_REQUEST_FILES_ENV;
	private static final String TARGET_ENV 				= GlobalParameters.PARAM_VAL_TARGET_ENV;
	private static HashMap<String, String> SYSTEM_MAP	= null;
	private static int counter							= 1;		// Number of ICOs processed in total
	
	
	
	/*====================================================================================
	 *------------- Instance variables
	 *====================================================================================*/
	private String name = null;
	private boolean active = false;
	private String qualityOfService = null;
	private String fromTime = null;
	private String toTime = null;
	private int maxMessages = 0;

	private boolean isUsingMultiMapping = false;
	
	private String senderParty = null;
	private String senderComponent = null;
	private String senderInterface = null;
	private String senderNamespace = null;

	private String receiverParty = null;
	private String receiverComponent = null;
	private String receiverInterface = null;
	private String receiverNamespace = null;
	
	protected int internalObjectId = -1;				// Internal object id
	private Exception ex = null;						// Error details, if any
	private long startTime = 0;							// Processing time, start
	protected long endTime = 0;							// Processing time, end
	
	private String filePathFirstPayloads = null;
	private String filePathLastPayloads = null;
	
	
	
	/*====================================================================================
	 *------------- Constructors
	 *====================================================================================*/
	public IntegratedConfigurationMain(IcoOverviewInstance icoOverviewInstance) throws GeneralException {
		this(icoOverviewInstance, FileStructure.FILE_CONFIG_SYSTEM_MAPPING, SOURCE_ENV_ICO_REQUESTS, TARGET_ENV);
	}
	

	public IntegratedConfigurationMain(IcoOverviewInstance icoOverviewInstance, String mapfilePath, String sourceEnv, String targetEnv) throws GeneralException {
		// Set start time
		this.startTime = Util.getTime();
		
		// Set internal object id (used for logging purposes)
		this.internalObjectId = counter;
		counter++;
		
		// Validate input data from ICO overview instance
		this.checkIcoExtract(icoOverviewInstance);
		
		// Initialize SAP PO Business Component mapping
		SYSTEM_MAP = initializeSystemMap(mapfilePath, sourceEnv, targetEnv);		
		
		// Set basic data based on ICO overview instance
		this.setName(icoOverviewInstance.getName());
		this.setActive(icoOverviewInstance.isActive());
		this.setQualityOfService(icoOverviewInstance.getQualityOfService());
		this.setFromTime(icoOverviewInstance.getFromTime());
		this.setToTime(icoOverviewInstance.getToTime());
		this.setMaxMessages(icoOverviewInstance.getMaxMessages());
		this.setUsingMultiMapping(icoOverviewInstance.isUsingMultiMapping());
		this.setSenderParty(icoOverviewInstance.getSenderParty());
		this.setSenderComponent(icoOverviewInstance.getSenderComponent());
		this.setSenderInterface(icoOverviewInstance.getSenderInterface());
		this.setSenderNamespace(icoOverviewInstance.getSenderNamespace());
		this.setReceiverParty(icoOverviewInstance.getReceiverParty());
		this.setReceiverComponent(icoOverviewInstance.getReceiverComponent());
		this.setReceiverInterface(icoOverviewInstance.getReceiverInterface());
		this.setReceiverNamespace(icoOverviewInstance.getReceiverNamespace());
		
		// Set file path: FIRST payloads
		this.filePathFirstPayloads 	= FileStructure.DIR_EXTRACT_OUTPUT_PRE 
									+ this.name 
									+ "\\" 
									+ GlobalParameters.PARAM_VAL_SOURCE_ENV 
									+ FileStructure.DIR_EXTRACT_OUTPUT_POST_FIRST_ENVLESS;
		
		// Set file path: LAST payloads
		this.filePathLastPayloads 	= FileStructure.DIR_EXTRACT_OUTPUT_PRE 
									+ this.name 
									+ "\\" 
									+ GlobalParameters.PARAM_VAL_TARGET_ENV 
									+ FileStructure.DIR_EXTRACT_OUTPUT_POST_LAST_ENVLESS;
		
		// Generate target directories
		Util.createDirIfNotExists(this.filePathFirstPayloads);
		Util.createDirIfNotExists(this.filePathLastPayloads);
	}
	
	
	
	/*====================================================================================
	 *------------- Getters and Setters
	 *====================================================================================*/
	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public boolean isActive() {
		return active;
	}


	public void setActive(boolean active) throws GeneralException {
		final String SIGNATURE = "setActive(boolean)";
		if (active) {
			this.active = active;			
		} else {
			String ex = "Internal program error. An inactive ICO was sent for processing. This is not expected and should be filtered before reaching this point.";
			logger.writeError(LOCATION, SIGNATURE, ex);
			throw new GeneralException(ex);
		}
	}


	public String getQualityOfService() {
		return qualityOfService;
	}


	public void setQualityOfService(String qualityOfService) {
		this.qualityOfService = qualityOfService;
	}


	public String getFromTime() {
		return fromTime;
	}


	public void setFromTime(String fromTime) {
		final String SIGNATURE = "setFromTime(String)";
		if (GlobalParameters.PARAM_VAL_FROM_TIME == null) {
			// Use data from ICO overview
			this.fromTime = fromTime;
		} else {
			// Use value from program parameter
			this.fromTime = GlobalParameters.PARAM_VAL_FROM_TIME;
			logger.writeInfo(LOCATION, SIGNATURE, "'FromTime' is being overruled due to program parameter set. Value used for all: " + GlobalParameters.PARAM_VAL_FROM_TIME);
		}
	}


	public String getToTime() {
		return toTime;
	}


	public void setToTime(String toTime) {
		final String SIGNATURE = "setToTime(String)";
		if (GlobalParameters.PARAM_VAL_TO_TIME == null) {
			// Use data from ICO overview
			this.toTime = toTime;
		} else {
			// Use value from program parameter
			this.toTime = GlobalParameters.PARAM_VAL_TO_TIME;
			logger.writeInfo(LOCATION, SIGNATURE, "'ToTime' is being overruled due to program parameter set. Value used for all: " + GlobalParameters.PARAM_VAL_TO_TIME);
		}
	}


	public int getMaxMessages() {
		return maxMessages;
	}


	public void setMaxMessages(int maxMessages) throws GeneralException {
		final String SIGNATURE = "setMaxMessages(int)";
		if (OVERRULE_MSG_SIZE) {
			// Overruling enabled: use technical property
			if (MAX_MSG_SIZE_OVERRULED < 1) {
				String ex = "Max message size enabled for overruling, but value '" + MAX_MSG_SIZE_OVERRULED + "' is not supported. Value must be >= 1.";
				logger.writeError(LOCATION, SIGNATURE, ex);
				throw new GeneralException(ex);
			} else {
				this.maxMessages = MAX_MSG_SIZE_OVERRULED;
				logger.writeInfo(LOCATION, SIGNATURE, "Max message size is being overruled due to technical parameter set. Value used for all: " + MAX_MSG_SIZE_OVERRULED);				
			}
		} else {
			// Overruling disabled: use value from ICO overview instance
			this.maxMessages = maxMessages;			
		}
	}


	public boolean isUsingMultiMapping() {
		return isUsingMultiMapping;
	}


	public void setUsingMultiMapping(boolean isUsingMultiMapping) {
		this.isUsingMultiMapping = isUsingMultiMapping;
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
		final String SIGNATURE = "setSenderComponent(String)";
		String mappedSys = mapSystem(senderComponent);
		if (mappedSys == null) {
			String ex = "Sender System '" + senderComponent + "' not found in System Mapping table.";
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
		final String SIGNATURE = "setReceiverComponent(String)";
		String mappedSys = mapSystem(receiverComponent);
		if (mappedSys == null) {
			String ex = "Receiver System '" + receiverComponent + "' not found in System Mapping table.";
			logger.writeError(LOCATION, SIGNATURE, ex);
			throw new GeneralException(ex);
		} else {
			this.receiverComponent = mappedSys;			
		}
	}


	public String getReceiverInterface() {
		return receiverInterface;
	}


	public void setReceiverInterface(String receiverInterface) {
		this.receiverInterface = receiverInterface;
	}


	public String getReceiverNamespace() {
		return receiverNamespace;
	}


	public void setReceiverNamespace(String receiverNamespace) {
		this.receiverNamespace = receiverNamespace;
	}


	public int getInternalObjectId() {
		return internalObjectId;
	}


	public void setInternalObjectId(int internalObjectId) {
		this.internalObjectId = internalObjectId;
	}


	public Exception getEx() {
		return ex;
	}


	public void setEx(Exception ex) {
		this.ex = ex;
	}


	public long getStartTime() {
		return startTime;
	}


	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}


	public long getEndTime() {
		return endTime;
	}


	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}
	
	
	public String getFilePathFirstPayloads() {
		return this.filePathFirstPayloads;
	}
	
	
	public String getFilePathLastPayloads() {
		return this.filePathLastPayloads;
	}
	
	

	/*====================================================================================
	 *------------- Class methods
	 *====================================================================================*/
	private static HashMap<String, String> initializeSystemMap(String mapFilePath, String sourceEnv, String targetEnv) {
		final String SIGNATURE = "initializeSystemMap(String, String, String)";
		try {
			// Determine source index (how the request ICO's are created)
			int sourceIndex = -1;
			if (GlobalParameters.Environment.DEV.toString().equals(sourceEnv)) {
				sourceIndex = 0;
			} else if (GlobalParameters.Environment.TST.toString().equals(sourceEnv)) {
				sourceIndex = 1;
			} else {
				sourceIndex = 2;
			}
			
			// Determine target index (which target system to map to when injecting)
			int targetIndex = -1;
			if (GlobalParameters.Environment.DEV.toString().equals(targetEnv)) {
				targetIndex = 0;
			} else if (GlobalParameters.Environment.TST.toString().equals(targetEnv)) {
				targetIndex = 1;
			} else {
				targetIndex = 2;
			}
			
			// Populate map
	 		SYSTEM_MAP = new HashMap<String, String>();
	 		String line;
	 		FileReader fileReader = new FileReader(mapFilePath);
	 		try (BufferedReader bufferedReader = new BufferedReader(fileReader)) {
	 			while((line = bufferedReader.readLine()) != null) {
	 				String[] str = line.split("\\|");
	 				SYSTEM_MAP.put(str[sourceIndex], str[targetIndex]);
	 			}			   
	 		}

		    // Return initialized map
		    logger.writeDebug(LOCATION, SIGNATURE, "System mapping initialized. Source ENV '" + sourceEnv + "'. Target ENV '" + targetEnv + "'. Number of entries: " + SYSTEM_MAP.size());
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
	private void checkIcoExtract(IcoOverviewInstance icoOverviewInstance) throws GeneralException {
		final String SIGNATURE = "checkIcoExtract(IcoOverviewInstance)";
		
		StringWriter sw = new StringWriter();
		if (icoOverviewInstance.getName() == null || "".equals(icoOverviewInstance.getName())) {
			sw.write("'/inv:IntegratedConfigurationList/inv:IntegratedConfiguration/inv:Name' is not set. This is unexpected at this point.\n");
		}
		
		if (!icoOverviewInstance.isActive()) {
			sw.write("'/inv:IntegratedConfigurationList/inv:IntegratedConfiguration/inv:Active' is set to false. This is unexpected at this point.\n");
		}
		
		if (icoOverviewInstance.getQualityOfService() == null || "".equals(icoOverviewInstance.getQualityOfService())) {
			sw.write("'/inv:IntegratedConfigurationList/inv:IntegratedConfiguration/inv:QualityOfService' not present. This is mandatory.\n");
		}
		
		if (icoOverviewInstance.getSenderComponent() == null || "".equals(icoOverviewInstance.getSenderComponent())) {
			sw.write("'/inv:IntegratedConfigurationList/inv:IntegratedConfiguration/inv:Sender/inv:Component' not present. This is mandatory.\n");
		}

		if (icoOverviewInstance.getSenderInterface() == null || "".equals(icoOverviewInstance.getSenderInterface())) {
			sw.write("'/inv:IntegratedConfigurationList/inv:IntegratedConfiguration/inv:Sender/inv:Interface' not present. This is mandatory.\n");
		}
		
		if (icoOverviewInstance.getSenderNamespace() == null || "".equals(icoOverviewInstance.getSenderNamespace())) {
			sw.write("'/inv:IntegratedConfigurationList/inv:IntegratedConfiguration/inv:Sender/inv:Interface' not present. This is mandatory.\n");
		}

		if (icoOverviewInstance.getReceiverComponent() == null || "".equals(icoOverviewInstance.getReceiverComponent())) {
			sw.write("'/inv:IntegratedConfigurationList/inv:IntegratedConfiguration/inv:Receiver/inv:Component' not present. This is mandatory.\n");
		}

		if (icoOverviewInstance.getReceiverInterface() == null || "".equals(icoOverviewInstance.getReceiverInterface())) {
			sw.write("'/inv:IntegratedConfigurationList/inv:IntegratedConfiguration/inv:Receiver/inv:Interface' not present. This is mandatory.\n");
		}
		
		if (icoOverviewInstance.getReceiverNamespace() == null || "".equals(icoOverviewInstance.getReceiverNamespace())) {
			sw.write("'/inv:IntegratedConfigurationList/inv:IntegratedConfiguration/inv:Receiver/inv:Interface' not present. This is mandatory.\n");
		}
		
		if (icoOverviewInstance.getMaxMessages() < 1) {
			sw.write("'/inv:IntegratedConfigurationList/inv:IntegratedConfiguration/inv:MaxMessages' must be set to a value greather than 0. This is mandatory.\n");
		}
		
		// Throw exception in case errors was found
		if (!sw.toString().equals("")) {
			String msg = "Data validation error for ICO overview instance.\n" + sw.toString();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new GeneralException(msg);
		}
	}
		
	
	private static String mapSystem(String key) {
		String value = SYSTEM_MAP.get(key);
		return value;
	}

}
