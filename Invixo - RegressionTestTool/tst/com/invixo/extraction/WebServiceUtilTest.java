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
	@DisplayName("Verify extracted data from GetMessagesWithSuccessors response when batching")
	void extractDataFromSuccessorsResponseWhenBatching() {
		try {
			// Get path: web service response (GetMessagesWithSuccessors)
			String response = "../../../resources/testfiles/com/invixo/extraction/GetMessagesWithSuccessors_BatchResponse.xml";
			InputStream wsResponse = this.getClass().getResourceAsStream(response);
			
			// Prepare variables
			String senderInterface = "oa_PlannerProposal";
			String receiverInterface = "SHP_OBDLV_CHANGE.SHP_OBDLV_CHANGE01";
			
			ArrayList<String> requestMsgIds = new ArrayList<String>();
			requestMsgIds.add("14b78098-3a00-11e9-9945-0000273d8d22");
			requestMsgIds.add("17b72adb-3a00-11e9-8560-0000273d8d23");
			requestMsgIds.add("11b882c6-3a00-11e9-8d1b-0000273d8d23");
			requestMsgIds.add("b82d5ee1-3a00-11e9-8f6f-0000273d8d23");
			
			// Get data to be checked (map<message id, parent id>)
			HashMap<String, String> extractMap = WebServiceUtil.extractSuccessorsBatch(wsResponse.readAllBytes(), senderInterface, receiverInterface);
				
			// Build expected result
			HashMap<String, String> expectedResult = new HashMap<String, String>();
			expectedResult.put("1eb89717-3a00-11e9-9dca-0000273d8d22\\OUTBOUND\\658345250\\EOIO\\7\\", "14baf4f3-3a00-11e9-ac22-0000273d8d22");
			expectedResult.put("1e25c8eb-3a00-11e9-a586-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\18\\", "17ba2672-3a00-11e9-c3a1-0000273d8d23");
			expectedResult.put("1b1ff769-3a00-11e9-9dcc-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\32\\", "11bc17d6-3a00-11e9-b8d4-0000273d8d23");
			expectedResult.put("17ba2672-3a00-11e9-c3a1-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\2\\", "17b72adb-3a00-11e9-8560-0000273d8d23");
			expectedResult.put("1e25acc9-3a00-11e9-bc55-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\8\\", "17ba2672-3a00-11e9-c3a1-0000273d8d23");
			expectedResult.put("1eb89719-3a00-11e9-ad0d-0000273d8d22\\OUTBOUND\\658345250\\EOIO\\8\\", "14baf4f3-3a00-11e9-ac22-0000273d8d22");
			expectedResult.put("1e260428-3a00-11e9-a573-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\20\\", "17ba2672-3a00-11e9-c3a1-0000273d8d23");
			expectedResult.put("11b882c6-3a00-11e9-8d1b-0000273d8d23\\OUTBOUND\\0\\EO\\0\\", null);
			expectedResult.put("1b201a33-3a00-11e9-814d-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\41\\", "11bc17d6-3a00-11e9-b8d4-0000273d8d23");
			expectedResult.put("1b201a29-3a00-11e9-af14-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\36\\", "11bc17d6-3a00-11e9-b8d4-0000273d8d23");
			expectedResult.put("1b1ff76d-3a00-11e9-b80f-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\34\\", "11bc17d6-3a00-11e9-b8d4-0000273d8d23");
			expectedResult.put("1b1ff75b-3a00-11e9-c3eb-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\25\\", "11bc17d6-3a00-11e9-b8d4-0000273d8d23");
			expectedResult.put("1eb8971d-3a00-11e9-a27f-0000273d8d22\\OUTBOUND\\658345250\\EOIO\\10\\", "14baf4f3-3a00-11e9-ac22-0000273d8d22");
			expectedResult.put("bb0bf433-3a00-11e9-ca4f-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\2\\", "b830e19f-3a00-11e9-8768-0000273d8d23");
			expectedResult.put("1eb8b56b-3a00-11e9-bb33-0000273d8d22\\OUTBOUND\\658345250\\EOIO\\15\\", "14baf4f3-3a00-11e9-ac22-0000273d8d22");
			expectedResult.put("1eb8b56f-3a00-11e9-a959-0000273d8d22\\OUTBOUND\\658345250\\EOIO\\17\\", "14baf4f3-3a00-11e9-ac22-0000273d8d22");
			expectedResult.put("1eb8b579-3a00-11e9-8198-0000273d8d22\\OUTBOUND\\658345250\\EOIO\\22\\", "14baf4f3-3a00-11e9-ac22-0000273d8d22");
			expectedResult.put("1b201a2f-3a00-11e9-ab0c-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\39\\", "11bc17d6-3a00-11e9-b8d4-0000273d8d23");
			expectedResult.put("1b1ff76f-3a00-11e9-96de-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\35\\", "11bc17d6-3a00-11e9-b8d4-0000273d8d23");
			expectedResult.put("1eb8b575-3a00-11e9-b5a8-0000273d8d22\\OUTBOUND\\658345250\\EOIO\\20\\", "14baf4f3-3a00-11e9-ac22-0000273d8d22");
			expectedResult.put("bb0bf439-3a00-11e9-8c87-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\5\\", "b830e19f-3a00-11e9-8768-0000273d8d23");
			expectedResult.put("14baf4f3-3a00-11e9-ac22-0000273d8d22\\OUTBOUND\\658345250\\EOIO\\8\\", "14b78098-3a00-11e9-9945-0000273d8d22");
			expectedResult.put("1e25c8e9-3a00-11e9-ce29-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\17\\", "17ba2672-3a00-11e9-c3a1-0000273d8d23");
			expectedResult.put("bb0bc4dc-3a00-11e9-bba5-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\1\\", "b830e19f-3a00-11e9-8768-0000273d8d23");
			expectedResult.put("1eb89711-3a00-11e9-b321-0000273d8d22\\OUTBOUND\\658345250\\EOIO\\4\\", "14baf4f3-3a00-11e9-ac22-0000273d8d22");
			expectedResult.put("1e260426-3a00-11e9-acb5-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\19\\", "17ba2672-3a00-11e9-c3a1-0000273d8d23");
			expectedResult.put("1b1ff761-3a00-11e9-826a-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\28\\", "11bc17d6-3a00-11e9-b8d4-0000273d8d23");
			expectedResult.put("bb0bf435-3a00-11e9-9002-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\3\\", "b830e19f-3a00-11e9-8768-0000273d8d23");
			expectedResult.put("1eb89715-3a00-11e9-8ca5-0000273d8d22\\OUTBOUND\\658345250\\EOIO\\6\\", "14baf4f3-3a00-11e9-ac22-0000273d8d22");
			expectedResult.put("1eb8971b-3a00-11e9-958a-0000273d8d22\\OUTBOUND\\658345250\\EOIO\\9\\", "14baf4f3-3a00-11e9-ac22-0000273d8d22");
			expectedResult.put("17b72adb-3a00-11e9-8560-0000273d8d23\\OUTBOUND\\0\\EO\\0\\", null);
			expectedResult.put("1b1ff757-3a00-11e9-899c-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\23\\", "11bc17d6-3a00-11e9-b8d4-0000273d8d23");
			expectedResult.put("1eb8b56d-3a00-11e9-9494-0000273d8d22\\OUTBOUND\\658345250\\EOIO\\16\\", "14baf4f3-3a00-11e9-ac22-0000273d8d22");
			expectedResult.put("1b1ff76b-3a00-11e9-984b-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\33\\", "11bc17d6-3a00-11e9-b8d4-0000273d8d23");
			expectedResult.put("1eb8b569-3a00-11e9-8867-0000273d8d22\\OUTBOUND\\658345250\\EOIO\\14\\", "14baf4f3-3a00-11e9-ac22-0000273d8d22");
			expectedResult.put("11bc17d6-3a00-11e9-b8d4-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\2\\", "11b882c6-3a00-11e9-8d1b-0000273d8d23");
			expectedResult.put("1eb89723-3a00-11e9-96a7-0000273d8d22\\OUTBOUND\\658345250\\EOIO\\13\\", "14baf4f3-3a00-11e9-ac22-0000273d8d22");
			expectedResult.put("1eb89713-3a00-11e9-be96-0000273d8d22\\OUTBOUND\\658345250\\EOIO\\5\\", "14baf4f3-3a00-11e9-ac22-0000273d8d22");
			expectedResult.put("1b1ff75d-3a00-11e9-9b81-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\26\\", "11bc17d6-3a00-11e9-b8d4-0000273d8d23");
			expectedResult.put("b82d5ee1-3a00-11e9-8f6f-0000273d8d23\\OUTBOUND\\0\\EO\\0\\", null);
			expectedResult.put("1b1fec0f-3a00-11e9-99cd-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\22\\", "11bc17d6-3a00-11e9-b8d4-0000273d8d23");
			expectedResult.put("1e25c8e1-3a00-11e9-a8a1-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\13\\", "17ba2672-3a00-11e9-c3a1-0000273d8d23");
			expectedResult.put("1e25accb-3a00-11e9-a363-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\9\\", "17ba2672-3a00-11e9-c3a1-0000273d8d23");
			expectedResult.put("1eb8970d-3a00-11e9-a062-0000273d8d22\\OUTBOUND\\658345250\\EOIO\\2\\", "14baf4f3-3a00-11e9-ac22-0000273d8d22");
			expectedResult.put("1eb89721-3a00-11e9-bf33-0000273d8d22\\OUTBOUND\\658345250\\EOIO\\12\\", "14baf4f3-3a00-11e9-ac22-0000273d8d22");
			expectedResult.put("bb0bf43b-3a00-11e9-b151-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\6\\", "b830e19f-3a00-11e9-8768-0000273d8d23");
			expectedResult.put("1eb8971f-3a00-11e9-c8d7-0000273d8d22\\OUTBOUND\\658345250\\EOIO\\11\\", "14baf4f3-3a00-11e9-ac22-0000273d8d22");
			expectedResult.put("1b201a2d-3a00-11e9-b142-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\38\\", "11bc17d6-3a00-11e9-b8d4-0000273d8d23");
			expectedResult.put("1b1ff767-3a00-11e9-8617-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\31\\", "11bc17d6-3a00-11e9-b8d4-0000273d8d23");
			expectedResult.put("1eb8b573-3a00-11e9-af91-0000273d8d22\\OUTBOUND\\658345250\\EOIO\\19\\", "14baf4f3-3a00-11e9-ac22-0000273d8d22");
			expectedResult.put("1b201a2b-3a00-11e9-8e90-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\37\\", "11bc17d6-3a00-11e9-b8d4-0000273d8d23");
			expectedResult.put("1eb8970f-3a00-11e9-8c5a-0000273d8d22\\OUTBOUND\\658345250\\EOIO\\3\\", "14baf4f3-3a00-11e9-ac22-0000273d8d22");
			expectedResult.put("1b1ff765-3a00-11e9-c30f-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\30\\", "11bc17d6-3a00-11e9-b8d4-0000273d8d23");
			expectedResult.put("1eb85a6b-3a00-11e9-be21-0000273d8d22\\OUTBOUND\\658345250\\EOIO\\1\\", "14baf4f3-3a00-11e9-ac22-0000273d8d22");
			expectedResult.put("bb0bf437-3a00-11e9-aa43-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\4\\", "b830e19f-3a00-11e9-8768-0000273d8d23");
			expectedResult.put("1b1ff763-3a00-11e9-cbe2-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\29\\", "11bc17d6-3a00-11e9-b8d4-0000273d8d23");
			expectedResult.put("1e25c8e3-3a00-11e9-9df8-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\14\\", "17ba2672-3a00-11e9-c3a1-0000273d8d23");
			expectedResult.put("14b78098-3a00-11e9-9945-0000273d8d22\\OUTBOUND\\0\\EO\\0\\", null);
			expectedResult.put("1b1ff75f-3a00-11e9-b674-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\27\\", "11bc17d6-3a00-11e9-b8d4-0000273d8d23");
			expectedResult.put("1e25c8e7-3a00-11e9-8a33-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\16\\", "17ba2672-3a00-11e9-c3a1-0000273d8d23");
			expectedResult.put("1e25c8df-3a00-11e9-9f76-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\12\\", "17ba2672-3a00-11e9-c3a1-0000273d8d23");
			expectedResult.put("1eb8b577-3a00-11e9-aade-0000273d8d22\\OUTBOUND\\658345250\\EOIO\\21\\", "14baf4f3-3a00-11e9-ac22-0000273d8d22");
			expectedResult.put("1e25c8dd-3a00-11e9-b109-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\11\\", "17ba2672-3a00-11e9-c3a1-0000273d8d23");
			expectedResult.put("1b201a31-3a00-11e9-934c-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\40\\", "11bc17d6-3a00-11e9-b8d4-0000273d8d23");
			expectedResult.put("1b1ff759-3a00-11e9-b513-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\24\\", "11bc17d6-3a00-11e9-b8d4-0000273d8d23");
			expectedResult.put("1e25c8db-3a00-11e9-834e-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\10\\", "17ba2672-3a00-11e9-c3a1-0000273d8d23");
			expectedResult.put("b830e19f-3a00-11e9-8768-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\2\\", "b82d5ee1-3a00-11e9-8f6f-0000273d8d23");
			expectedResult.put("1eb8b571-3a00-11e9-b27e-0000273d8d22\\OUTBOUND\\658345250\\EOIO\\18\\", "14baf4f3-3a00-11e9-ac22-0000273d8d22");
			expectedResult.put("1e25c8e5-3a00-11e9-b38c-0000273d8d23\\OUTBOUND\\658345251\\EOIO\\15\\", "17ba2672-3a00-11e9-c3a1-0000273d8d23");

			// Check
			assertEquals(expectedResult, extractMap);
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
