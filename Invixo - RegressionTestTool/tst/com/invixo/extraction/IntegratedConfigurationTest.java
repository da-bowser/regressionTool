package com.invixo.extraction;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
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
	@DisplayName("Verify buildListOfMessageIdsToBeExtracted builds a correct list of non-split message IDs and split message IDs")
	void checkBuildingOfMessageIdListWorks() {
		try {
			// Get path: web service response (GetMessagesWithSuccessors)
			String response = "../../../resources/testfiles/com/invixo/extraction/extractMessageInfo_Input2.xml";
			URL urlResponse = this.getClass().getResource(response);
			String pathResponse = Paths.get(urlResponse.toURI()).toString();
			
			// Read response bytes
			try (InputStream is = new FileInputStream(new File(pathResponse))) {
				// Extract data from response
				MessageInfo msgInfo = WebServiceUtil.extractMessageInfo(is.readAllBytes(), "Data_In_Async_Split");
				
				// Build list of Message IDs to be extracted
				HashSet<String> result = IntegratedConfiguration.buildListOfMessageIdsToBeExtracted(msgInfo.getObjectKeys(), msgInfo.getSplitMessageIds());
				
				// Check
				assertEquals(2, result.size());				
			}
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}
		
}
