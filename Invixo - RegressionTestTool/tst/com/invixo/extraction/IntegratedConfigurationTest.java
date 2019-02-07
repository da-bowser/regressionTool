package com.invixo.extraction;

import static org.junit.Assert.fail;

import java.net.URL;
import java.nio.file.Paths;

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
		
		GlobalParameters.SAP_PO_HTTP_HOST_AND_PORT="http://ipod.invixo.com:50000";
		GlobalParameters.CREDENTIAL_USER = "rttuser";
		GlobalParameters.CREDENTIAL_PASS = "aLvD#l^[R\\52";
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
			
			if (request == null) {
				fail();
			}
		} catch (Exception e) {
			System.out.println("It aint cooking chef! " + e);
			fail();
		}
	}

}
