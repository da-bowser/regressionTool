package com.invixo.messageExtractor.httpHandlers;

import java.io.InputStream;
import com.invixo.common.util.Util;
import com.invixo.consistency.FileStructure;
import com.invixo.injection.Injector;

public class HAaeInjector {
	
	public static void main(String[] args) {
		try {
			// Test: generate request payload and inject to SAP PO
			
			// Create injection file
			String ico = "NoScenario - Sys_QA3_011 oa_GoodsReceipt";
			Injector injector = new Injector(FileStructure.DIR_REGRESSION_INPUT_ICO + ico + ".xml");
			injector.injectSingleFile(FileStructure.DIR_REGRESSION_OUTPUT_PAYLOADS_FIRST_MSG_VERSION + ico + "\\8ef2d54b-9302-1ee9-85fb-feb9d9f44ddd.xml");
			System.out.println("Payload file: " + FileStructure.DIR_REGRESSION_OUTPUT_PAYLOADS_FIRST_MSG_VERSION + ico + "\\8ef2d54b-9302-1ee9-85fb-feb9d9f44ddd.xml");
			System.out.println("Scenario configuration file: " + FileStructure.DIR_REGRESSION_INPUT_ICO + ico + ".xml");
			
			String requestFile = FileStructure.DIR_REGRESSION_INPUT_INJECTION + "injectionfile.xml";
			System.out.println("Request file to be sent: " + requestFile);
			byte[] requestBytes = Util.readFile(requestFile);
			
			// Test: call web service
			InputStream responseBytes = HAaeInjector.invoke(requestBytes);
			System.out.println("--- TEST: Response: \n" + new String(responseBytes.readAllBytes()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public static InputStream invoke(byte[] requestBytes) throws Exception {
		return WebServiceHandler.callWebService("INJECT", requestBytes);
	}
	
}
