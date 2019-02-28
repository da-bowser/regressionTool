package com.invixo.extraction;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.invixo.common.IcoOverviewDeserializer;
import com.invixo.common.IcoOverviewInstance;
import com.invixo.main.GlobalParameters;

class WebServiceUtilTest {

	@BeforeAll
    static void initAll() {
		GlobalParameters.PARAM_VAL_BASE_DIR = "c:\\RTT\\UnitTest";
		GlobalParameters.PARAM_VAL_OPERATION = "extract";
		GlobalParameters.PARAM_VAL_EXTRACT_MODE_INIT="true";
		
		GlobalParameters.PARAM_VAL_ICO_REQUEST_FILES_ENV="PRD";
		GlobalParameters.PARAM_VAL_SOURCE_ENV="TST";
		GlobalParameters.PARAM_VAL_TARGET_ENV="TST";
		
		GlobalParameters.SAP_PO_HTTP_HOST_AND_PORT="http://ipod.invixo.com:50000/";
		GlobalParameters.CREDENTIAL_USER = "rttuser";
		GlobalParameters.CREDENTIAL_PASS = "aLvD#l^[R(52";
    }
	
	
	@Test
	@DisplayName("Verify request creations works: GetMessageList")
	void verifyRequestCreationGetMessageList() {
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
			byte[] request = WebServiceUtil.createRequestGetMessageList(ico);
			
			// Check
			assertNotNull(request);
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}

	
	@Test
	@DisplayName("Verify extractMessageInfo filters split messages data correct")
	void extractMessageInfoFiltersCorrect() {
		try {
			// Get path: web service response (GetMessagesWithSuccessors)
			String response = "../../../resources/testfiles/com/invixo/extraction/extractMessageInfo_Input.xml";
			URL urlResponse = this.getClass().getResource(response);
			String pathResponse = Paths.get(urlResponse.toURI()).toString();
			
			// Read response bytes
			try (InputStream is = new FileInputStream(new File(pathResponse))) {				
				// Extract data from response
				MessageInfo msgInfo = WebServiceUtil.extractMessageInfo(is.readAllBytes(), "ia_CrossApplicationLogging");
				
				// Check
				assertEquals(20, msgInfo.getSplitMessageIds().size());
			};
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
	
	@Test
	@DisplayName("Verify extractMessageInfo extracts data properly")
	void checkExtractMessageInfo() {
		try {
			// Get path: web service response (GetMessagesWithSuccessors)
			String response = "../../../resources/testfiles/com/invixo/extraction/extractMessageInfo_Input3.xml";
			URL urlResponse = this.getClass().getResource(response);
			String pathResponse = Paths.get(urlResponse.toURI()).toString();
			
			// Read response bytes
			try (InputStream is = new FileInputStream(new File(pathResponse))) {			
				// Extract data from response
				MessageInfo msgInfo = WebServiceUtil.extractMessageInfo(is.readAllBytes(), "ia_PackingList_KOL");

				// Build expected result: Message Keys
				HashSet<String> objectKeys = new HashSet<String>();
				objectKeys.add("12a612be-2ec5-11e9-ce07-0000210ff2e6\\OUTBOUND\\554693350\\EOIO\\70156\\");
				objectKeys.add("127c91a0-2ec5-11e9-8cd1-0000210ff2e6\\OUTBOUND\\554693350\\EOIO\\70154\\");
				objectKeys.add("8d83d3d3-4d3a-4024-bfe6-5fa22dc4eba7\\OUTBOUND\\554693350\\EOIO\\70152\\");
				objectKeys.add("11b27a31-2ec5-11e9-8ec4-0000210ff2e6\\OUTBOUND\\554693350\\EOIO\\70151\\");
				
				// Build expected result: Split Message IDs <parent id, message id>
				HashMap<String, String> splitMessageIds = new HashMap<String, String>();
				splitMessageIds.put("0ef26453-d70c-4afa-a0d3-7378db07ed12", "12a612be-2ec5-11e9-ce07-0000210ff2e6");
				splitMessageIds.put("f399de86-106a-4746-b35c-8a9750fab942", "127c91a0-2ec5-11e9-8cd1-0000210ff2e6");
				splitMessageIds.put("1fb0fc01-e1a6-44aa-a8e2-aef233cf48fe", "11b27a31-2ec5-11e9-8ec4-0000210ff2e6");
				
				MessageInfo msgInfoRef = new MessageInfo();
				msgInfoRef.setObjectKeys(objectKeys);
				msgInfoRef.setSplitMessageIds(splitMessageIds);
				
				// Check: split message ids
				assertEquals(msgInfo.getSplitMessageIds(), splitMessageIds);

				// Check: message keys
				assertEquals(msgInfo.getObjectKeys(), objectKeys);
			}
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
	
	@Test
	@DisplayName("Verify request creations works: GetMessageBytesJavaLangStringIntBoolean (FIRST)")
	void createWsRequestFirst() {
		try {		
			// Create message key
			String messageKey = "a3386b2a-1383-11e9-a723-000000554e16\\OUTBOUND\\5590550\\EO\\0";

			// Test creation of a new request message (FIRST)
			InputStream is = WebServiceUtil.createRequestGetMessageBytesJavaLangStringIntBoolean(messageKey, 0);
//			System.out.println("-- UNIT TEST -- Request payload created: \n" + new String(is.readAllBytes()));
			
			// Check
			assertNotNull(is);
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
	
	@Test
	@DisplayName("Verify request creations works: GetMessageBytesJavaLangStringIntBoolean (LAST)")
	void createWsRequestLast() {
		try {	
			// Create message key
			String messageKey = "a3386b2a-1383-11e9-a723-000000554e16\\OUTBOUND\\5590550\\EO\\0";

			// Test creation of a new request message (LAST)
			InputStream is = WebServiceUtil.createRequestGetMessageBytesJavaLangStringIntBoolean(messageKey, -1);
//			System.out.println("Request payload created: \n" + new String(is.readAllBytes()));
			
			// Check
			assertNotNull(is);
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
			byte[] requestBytes = WebServiceUtil.createRequestGetMessagesWithSuccessors(msgIdList);
						
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
			String messageKey = WebServiceUtil.extractMessageKeyFromResponse(responseBytes);
			
			// Check
			assertEquals("060fb733-3481-11e9-bf85-000000554e16\\OUTBOUND\\5590550\\EOIO\\1\\", messageKey);
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}

}
