package com.invixo.messageExtractor.httpHandlers;

import java.io.InputStream;

import com.invixo.common.util.Logger;
import com.invixo.common.util.Util;
import com.invixo.consistency.FileStructure;

public class HGetMessageList {
	private static final String LOCATION 			= HGetMessageList.class.getName();
	private static Logger logger 					= Logger.getInstance();

	
	public static void main(String[] args) {
		try {
			// Test
			InputStream responseBytes = HGetMessageList.invoke("GetMessageListRequest.xml");
			System.out.println("Response: \n" + new String(responseBytes.readAllBytes()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
	
	public static InputStream invoke(String requestFile) throws Exception {
		String SIGNATURE = "invoke(String)";
		
		// Get bytes from request file
		final String fileLocation = FileStructure.DIR_REGRESSION_INPUT_ICO + requestFile;
		byte[] requestBytes = Util.readFile(fileLocation);
		logger.writeDebug(LOCATION, SIGNATURE, "Processing request file: " + fileLocation);
		
		// Call web service
		return WebServiceHandler.callWebService("EXTRACT", requestBytes);
	}
	
}
