package com.invixo.messageExtractor.httpHandlers;

import java.io.InputStream;
import com.invixo.common.util.Util;
import com.invixo.messageExtractor.util.PropertyAccessor;

public class HGetMessageBytesJavaLangStringIntBoolean extends WebServiceHandler {
	
	private static final String FILE_BASE_LOCATION 	= PropertyAccessor.getProperty("BASE_DIRECTORY");									// Base directory
	public static final String DIR_RESPONSE 		= FILE_BASE_LOCATION + "GetMessageBytesJavaLangStringIntBoolean\\Responses\\";		// Relative path to specific directory

	
	public static void main(String[] args) {
		try {
			// Test: get bytes from request file
			String requestFile = FILE_BASE_LOCATION + "Test\\GetMessageBytesJavaLangStringIntBoolean\\Requests\\" + "resp_GetMessageListRequest.xml_key1_POST.xml";
			byte[] requestBytes = Util.readFile(requestFile);
			
			// Test: call web service
			InputStream responseBytes = HGetMessageBytesJavaLangStringIntBoolean.invoke(requestBytes);
			System.out.println("Response: \n" + new String(responseBytes.readAllBytes()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public static InputStream invoke(byte[] requestBytes) throws Exception {
		return WebServiceHandler.callWebService(requestBytes);
	}
	
}
