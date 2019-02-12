package com.invixo.extraction;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.invixo.main.GlobalParameters;

class IntegratedConfigurationTest {

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
	@DisplayName("Verify extraction works in initial mode")
	void verifyOrchestratorExecutesWithoutErrors() {
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
			byte[] request = IntegratedConfiguration.createGetMessageListRequest(ico);
			
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
			String response = "../../../resources/extraction/testfiles/extractMessageInfo_Input.xml";
			URL urlResponse = this.getClass().getResource(response);
			String pathResponse = Paths.get(urlResponse.toURI()).toString();
			
			// Read response bytes
			InputStream is = new FileInputStream(new File(pathResponse));
			
			// Extract data from response
			MessageInfo msgInfo = IntegratedConfiguration.extractMessageInfo(is, "ia_CrossApplicationLogging");
			
			// Check
			assertEquals(20, msgInfo.getSplitMessageIds().size());
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
	
	@Test
	@DisplayName("Verify buildListOfMessageIdsToBeExtracted builds a correct list of non-split message IDs and split message IDs")
	void checkBuildingOfMessageIdListWorks() {
		try {
			// Get path: web service response (GetMessagesWithSuccessors)
			String response = "../../../resources/extraction/testfiles/extractMessageInfo_Input2.xml";
			URL urlResponse = this.getClass().getResource(response);
			String pathResponse = Paths.get(urlResponse.toURI()).toString();
			
			// Read response bytes
			InputStream is = new FileInputStream(new File(pathResponse));
			
			// Extract data from response
			MessageInfo msgInfo = IntegratedConfiguration.extractMessageInfo(is, "Data_In_Async_Split");
			
			// Build list of Message IDs to be extracted
			HashSet<String> result = IntegratedConfiguration.buildListOfMessageIdsToBeExtracted(msgInfo.getObjectKeys(), msgInfo.getSplitMessageIds());
			
			// Check
			assertEquals(2, result.size());
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
	
	@Test
	@DisplayName("Verify extractMessageInfo extracts data properly")
	void checkExtractMessageInfo() {
		try {
			// Get path: web service response (GetMessagesWithSuccessors)
			String response = "../../../resources/extraction/testfiles/extractMessageInfo_Input3.xml";
			URL urlResponse = this.getClass().getResource(response);
			String pathResponse = Paths.get(urlResponse.toURI()).toString();
			
			// Read response bytes
			InputStream is = new FileInputStream(new File(pathResponse));
			
			// Extract data from response
			MessageInfo msgInfo = IntegratedConfiguration.extractMessageInfo(is, "ia_PackingList_KOL");

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
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
}
