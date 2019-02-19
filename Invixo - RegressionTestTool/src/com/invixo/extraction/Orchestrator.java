package com.invixo.extraction;

import java.util.ArrayList;

import com.invixo.common.GeneralException;
import com.invixo.common.IcoOverviewInstance;
import com.invixo.common.util.Logger;
import com.invixo.main.GlobalParameters;


/**
 * This class uses SAP PO Message API.
 * It orchestrates the process of extracting message payloads (from SAP PO database) from all configured ICOs.
 */
public class Orchestrator {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = Orchestrator.class.getName();	
	private static ArrayList<IntegratedConfiguration> icoExtractList = new ArrayList<IntegratedConfiguration>();
	

	/**
	 * This method extracts data from SAP PO based on list of active ICOs in ICO Overview XML file 
	 */
	public static ArrayList<IntegratedConfiguration> start(ArrayList<IcoOverviewInstance> icoOverviewList) {
		final String SIGNATURE = "start(ArrayList<IcoOverviewInstance>)";
		logger.writeInfo(LOCATION, SIGNATURE, "Extract Mode Initial: " + GlobalParameters.PARAM_VAL_EXTRACT_MODE_INIT);
				
		// Process each ICO request file
		for (IcoOverviewInstance ico : icoOverviewList) {
			processSingleIco(ico);
		}
		
		logger.writeDebug(LOCATION, SIGNATURE, "Finished processing all ICO's");
		return icoExtractList;
	}
	
	
	private static void processSingleIco(IcoOverviewInstance icoInstance) {
		final String SIGNATURE = "processSingleIco(IcoOverviewInstance)";
		try {
			// Prepare
			IntegratedConfiguration ico = new IntegratedConfiguration(icoInstance);
			icoExtractList.add(ico);

			// Process
			ico.startExtraction();
		} catch (GeneralException e) {
			String ex = "Error instantiating Integrated Configuration object! " + e;
			logger.writeError(LOCATION, SIGNATURE, ex);
			throw new RuntimeException(e);
		}
	}
	
}
