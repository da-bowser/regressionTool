package com.invixo.messageExtractor.httpHandlers;

import java.io.InputStream;
import com.invixo.common.util.Util;
import com.invixo.messageExtractor.util.Logger;
import com.invixo.messageExtractor.util.PropertyAccessor;

public class HGetMessageList {
	private static final String LOCATION 			= HGetMessageList.class.getName();
	private static final String FILE_BASE_LOCATION 	= PropertyAccessor.getProperty("BASE_DIRECTORY");			// Base directory
	public static final String DIR_REQUEST			= FILE_BASE_LOCATION + "GetMessageList\\Requests\\";		// Relative path to specific directory
	private static Logger logger 					= Logger.getInstance();

	
	public static void main(String[] args) {
		try {
			InputStream responseBytes = HGetMessageList.invoke("GetMessageListRequest.xml");
			System.out.println("Response: \n" + new String(responseBytes.readAllBytes()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
	
	public static InputStream invoke(String requestFile) throws Exception {
		String SIGNATURE = "invoke(String)";
		
		// Get bytes from request file
		final String fileLocation = DIR_REQUEST + requestFile;
		byte[] requestBytes = Util.readFile(fileLocation);
		logger.writeDebug(LOCATION, SIGNATURE, "Processing request file: " + fileLocation);
		
		// Call web service
		return WebServiceHandler.callWebService(requestBytes);
	}
	
}
