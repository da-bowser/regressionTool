package com.invixo.extraction;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.invixo.common.IcoOverviewDeserializer;
import com.invixo.common.IcoOverviewInstance;
import com.invixo.common.util.XmlUtil;
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
	@DisplayName("Test creation of request for service GetMessageBytesJavaLangStringIntBoolean (FIRST)")
	void createWsRequestFirst() {
		try {
			// Get stream to ICO overview file
			String icoOverviewPath = "../../../resources/testfiles/com/invixo/extraction/TST_IntegratedConfigurationsOverview.xml";
			InputStream overviewStream = this.getClass().getResourceAsStream(icoOverviewPath);
			ArrayList<IcoOverviewInstance> icoOverviewList =  IcoOverviewDeserializer.deserialize(overviewStream);
			
			// Get path: System Component mapping file
			String systemMapping = "../../../resources/config/systemMapping.txt";
			URL urlSystemMapping = this.getClass().getResource(systemMapping);
			String pathSystemMapping = Paths.get(urlSystemMapping.toURI()).toString();
			
			// Create GetMessageList request
			IntegratedConfiguration ico = new IntegratedConfiguration(icoOverviewList.get(0), pathSystemMapping, "PRD", "TST");
			
			// Create message key
			String messageKey = "a3386b2a-1383-11e9-a723-000000554e16\\OUTBOUND\\5590550\\EO\\0";
			MessageKey msgKey = new MessageKey(ico, messageKey);
			
			// Test creation of a new request message (FIRST)
			InputStream is = msgKey.createRequestGetMessageBytesJavaLangStringIntBoolean(messageKey, 0);
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
			// Get stream to ICO overview file
			String icoOverviewPath = "../../../resources/testfiles/com/invixo/extraction/TST_IntegratedConfigurationsOverview.xml";
			InputStream overviewStream = this.getClass().getResourceAsStream(icoOverviewPath);
			ArrayList<IcoOverviewInstance> icoOverviewList =  IcoOverviewDeserializer.deserialize(overviewStream);
			
			// Get path: System Component mapping file
			String systemMapping = "../../../resources/config/systemMapping.txt";
			URL urlSystemMapping = this.getClass().getResource(systemMapping);
			String pathSystemMapping = Paths.get(urlSystemMapping.toURI()).toString();
			
			// Create GetMessageList request
			IntegratedConfiguration ico = new IntegratedConfiguration(icoOverviewList.get(0), pathSystemMapping, "PRD", "TST");
			
			// Create message key
			String messageKey = "a3386b2a-1383-11e9-a723-000000554e16\\OUTBOUND\\5590550\\EO\\0";
			MessageKey msgKey = new MessageKey(ico, messageKey);
			
			// Test creation of a new request message (LAST)
			InputStream is = msgKey.createRequestGetMessageBytesJavaLangStringIntBoolean(messageKey, -1);
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
			// Get stream to ICO overview file
			String icoOverviewPath = "../../../resources/testfiles/com/invixo/extraction/TST_IntegratedConfigurationsOverview.xml";
			InputStream overviewStream = this.getClass().getResourceAsStream(icoOverviewPath);
			ArrayList<IcoOverviewInstance> icoOverviewList =  IcoOverviewDeserializer.deserialize(overviewStream);
			
			// Get path: System Component mapping file
			String systemMapping = "../../../resources/config/systemMapping.txt";
			URL urlSystemMapping = this.getClass().getResource(systemMapping);
			String pathSystemMapping = Paths.get(urlSystemMapping.toURI()).toString();
			
			// Create GetMessageList request
			IntegratedConfiguration ico = new IntegratedConfiguration(icoOverviewList.get(0), pathSystemMapping, "PRD", "TST");
			
			// Create message key
			String messageKey = "6bf95597-293e-11e9-bf0f-000000554e16\\OUTBOUND\\5590550\\EO\\0\\";
			MessageKey msgKey = new MessageKey(ico, messageKey);
			
			// Process message key
			msgKey.extractAllPayloads(msgKey.getSapMessageKey());
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
	
	@Test
	@DisplayName("Test creation of request for service GetMessagesByIDs")
	void createWsRequestGetMessagesByIDs() {
		try {
			
			// Create collection of messageIds
			List<String> msgIdList = Arrays.asList("060fb733-3481-11e9-bf85-000000554e16");
			
			// Create request
			byte[] requestBytes = XmlUtil.createGetMessagesWithSuccessorsRequest(msgIdList);
						
			// Check
			assertNotNull(requestBytes);
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
	
	@Test
	@DisplayName("Test extraction of messagKey from GetMessagesByIDs WS response")
	void extractMessageKeyFromWsGetMessagesByIDsResponse() {
		try {
			// Get response file from resources
			String getMessagesByIDsResponse = "tst/resources/testfiles/com/invixo/extraction/GetMessagesByIDsResponse.xml";
			
			// Convert to byte array
			File f = new File(getMessagesByIDsResponse);
			byte[] responseBytes = Files.readAllBytes(f.toPath());
			
			// Extract messageKey from  response
			String messageKey = MessageKey.extractMessageKeyFromResponse(responseBytes);
			
			// Check
			assertEquals("060fb733-3481-11e9-bf85-000000554e16\\OUTBOUND\\5590550\\EOIO\\1\\", messageKey);
			
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
	
	@Test
	@DisplayName("Test extraction of parentId from GetMessagesWithSuccessors WS response")
	void extractMessageKeyFromWsGetMessagesWithSuccessorsResponse() {
		try {
			// Get response file from resources
			String getMessagesByIDsResponse = "tst/resources/testfiles/com/invixo/extraction/GetMessagesWithSuccessorsResponse.xml";
			
			// Convert to byte array
			File f = new File(getMessagesByIDsResponse);
			byte[] responseBytes = Files.readAllBytes(f.toPath());
			
			// Extract messageKey from  response
			String parentId = MessageKey.extractParentIdsFromResponse(responseBytes);
			
			// Check
			assertEquals("060fb733-3481-11e9-bf85-000000554e16", parentId);
			
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}
}
