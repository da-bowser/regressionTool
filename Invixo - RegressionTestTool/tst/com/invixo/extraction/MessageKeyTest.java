package com.invixo.extraction;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.invixo.main.GlobalParameters;

class MessageKeyTest {

	@BeforeAll
    static void initAll() {
		GlobalParameters.PARAM_VAL_BASE_DIR = "c:\\RTT\\UnitTest";
		GlobalParameters.PARAM_VAL_OPERATION = "extract";
		GlobalParameters.PARAM_VAL_EXTRACT_MODE_INIT="true";
		
		GlobalParameters.PARAM_VAL_ICO_REQUEST_FILES_ENV="PRD";
		GlobalParameters.PARAM_VAL_SOURCE_ENV="TST";
		GlobalParameters.PARAM_VAL_TARGET_ENV="TST";
		
		GlobalParameters.PARAM_VAL_HTTP_HOST = "ipod.invixo.com";
		GlobalParameters.PARAM_VAL_HTTP_PORT = "50000";
		GlobalParameters.SAP_PO_HTTP_HOST_AND_PORT="http://ipod.invixo.com:50000/";
		GlobalParameters.CREDENTIAL_USER = "rttuser";
		GlobalParameters.CREDENTIAL_PASS = "aLvD#l^[R(52";
    }
	
	
	@Test
	@DisplayName("Test extraction of Message ID from MessageKey")
	void checkExtractionOfMessageIdFromMessageKey() {
		try {
			// Prepare
			String messageKey = "a3386b2a-1383-11e9-a723-000000554e16\\OUTBOUND\\5590550\\EO\\0";
			
			// Extract Message ID from MessageKey
			String msgId = MessageKey.extractMessageIdFromKey(messageKey);
			
			// Check
			assertEquals("a3386b2a-1383-11e9-a723-000000554e16", msgId);
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}

	
	@Test
	@DisplayName("Test creation of request for service GetMessageBytesJavaLangStringIntBoolean (FIRST)")
	void createWsRequestFirst() {
		try {
			// Get path: ICO request file
			String icoRequest = "../../../resources/extraction/input/integratedconfigurations/RegressionTestTool - RTT_Sender, Data_Out_Async.xml";
			URL urlicoRequest = this.getClass().getResource(icoRequest);
			String pathIcoRequest = Paths.get(urlicoRequest.toURI()).toString();
			
			// Get path: System Component mapping file
			String systemMapping = "../../../resources/config/systemMapping.txt";
			URL urlSystemMapping = this.getClass().getResource(systemMapping);
			String pathSystemMapping = Paths.get(urlSystemMapping.toURI()).toString();
			
			// Create GetMessageList request
			IntegratedConfiguration ico = new IntegratedConfiguration(pathIcoRequest, pathSystemMapping, "PRD", "TST");
			
			// Create message key
			String messageKey = "a3386b2a-1383-11e9-a723-000000554e16\\OUTBOUND\\5590550\\EO\\0";
			MessageKey msgKey = new MessageKey(ico, messageKey);
			
			// Test creation of a new request message (FIRST)
			InputStream is = msgKey.createNewRequest(messageKey, 0);
//			System.out.println("-- UNIT TEST -- Request payload created: \n" + new String(is.readAllBytes()));
			
			// Check
			assertNotNull(is);
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
	
	@Test
	@DisplayName("Test creation of request for service GetMessageBytesJavaLangStringIntBoolean (LAST)")
	void createWsRequestLast() {
		try {
			// Get path: ICO request file
			String icoRequest = "../../../resources/extraction/input/integratedconfigurations/RegressionTestTool - RTT_Sender, Data_Out_Async.xml";
			URL urlicoRequest = this.getClass().getResource(icoRequest);
			String pathIcoRequest = Paths.get(urlicoRequest.toURI()).toString();
			
			// Get path: System Component mapping file
			String systemMapping = "../../../resources/config/systemMapping.txt";
			URL urlSystemMapping = this.getClass().getResource(systemMapping);
			String pathSystemMapping = Paths.get(urlSystemMapping.toURI()).toString();
			
			// Create GetMessageList request
			IntegratedConfiguration ico = new IntegratedConfiguration(pathIcoRequest, pathSystemMapping, "PRD", "TST");
			
			// Create message key
			String messageKey = "a3386b2a-1383-11e9-a723-000000554e16\\OUTBOUND\\5590550\\EO\\0";
			MessageKey msgKey = new MessageKey(ico, messageKey);
			
			// Test creation of a new request message (LAST)
			InputStream is = msgKey.createNewRequest(messageKey, -1);
//			System.out.println("Request payload created: \n" + new String(is.readAllBytes()));
			
			// Check
			assertNotNull(is);
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
	
	@Test
	@DisplayName("Test processing a single message key")
	void processSingleMessageKeyWithNoErrors() {
		try {
			// Get path: ICO request file
			String icoRequest = "../../../resources/extraction/input/integratedconfigurations/RegressionTestTool - RTT_Sender, Data_Out_Async.xml";
			URL urlicoRequest = this.getClass().getResource(icoRequest);
			String pathIcoRequest = Paths.get(urlicoRequest.toURI()).toString();
			
			// Get path: System Component mapping file
			String systemMapping = "../../../resources/config/systemMapping.txt";
			URL urlSystemMapping = this.getClass().getResource(systemMapping);
			String pathSystemMapping = Paths.get(urlSystemMapping.toURI()).toString();
			
			// Create GetMessageList request
			IntegratedConfiguration ico = new IntegratedConfiguration(pathIcoRequest, pathSystemMapping, "PRD", "TST");
			
			// Create message key
			String messageKey = "6bf95597-293e-11e9-bf0f-000000554e16\\OUTBOUND\\5590550\\EO\\0\\";
			MessageKey msgKey = new MessageKey(ico, messageKey);
			
			// Process message key
			msgKey.processMessageKey(msgKey.getSapMessageKey(), true);
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
}
