package com.invixo.common;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.invixo.common.util.PropertyAccessor;
import com.invixo.main.GlobalParameters;

public class IntegratedConfigurationMainTest {
	
	@BeforeAll
    static void initAll() {
		GlobalParameters.PARAM_VAL_BASE_DIR = "c:\\RTT\\UnitTest";	
		GlobalParameters.PARAM_VAL_ICO_REQUEST_FILES_ENV="PRD";
    }
	
	@Test
	@DisplayName("Test constructor extracts data properly")
	void constructorExtractionVerification() {
		try {
			// Get path: ICO request file
			String icoRequest = "../../../resources/testfiles/com/invixo/common/RegressionTestTool - RTT_Sender, Data_Out_Async.xml";
			URL urlicoRequest = this.getClass().getResource(icoRequest);
			String pathIcoRequest = Paths.get(urlicoRequest.toURI()).toString();
			
			// Get path: System Component mapping file
			String systemMapping = "../../../resources/testfiles/com/invixo/common/systemMapping.txt";
			URL urlSystemMapping = this.getClass().getResource(systemMapping);
			String pathSystemMapping = Paths.get(urlSystemMapping.toURI()).toString();
			
			// Create GetMessageList request
			IntegratedConfigurationMain ico = new IntegratedConfigurationMain(pathIcoRequest, pathSystemMapping, "PRD", "TST");
			
			// Check: fail if message properties are not set correcly
			final String overruleKey = "OVERRULE_MSG_SIZE";
			boolean isOverruleEnabled = Boolean.parseBoolean(PropertyAccessor.getProperty(overruleKey));
			if (isOverruleEnabled) {
				fail("Overruling is enabled in messageProperties. Disable '"+ overruleKey + "' before running!");
			}
			
			// Check: sender
			assertEquals(null, ico.getSenderParty(), "SenderParty");
			assertEquals("RTT_SenderTST", ico.getSenderComponent(), "SenderComponent");
			assertEquals("Data_Out_Async", ico.getSenderInterface(), "SenderInterfaceName");
			assertEquals("urn:invixo.com:sandbox:regressionTestTool", ico.getSenderNamespace(), "SenderNamespace");
			
			// Check: receiver
			assertEquals(null, ico.getReceiverParty(), "ReceiverParty");
			assertEquals("RTT_ReceiverTST", ico.getReceiverComponent(), "ReceiverComponent");
			assertEquals("Data_In_Async", ico.getReceiverInterfaceName(), "ReceiverInterfaceName");
			assertEquals("urn:invixo.com:sandbox:regressionTestTool", ico.getReceiverNamespace(), "ReceiverNamespace");
			
			// Check: various
			assertEquals(null, ico.getFetchFromTime(), "FromTime");
			assertEquals(null, ico.getFetchToTime(), "ToTime");
			assertEquals(3, ico.getMaxMessages(), "MaxMessages");
			assertEquals("EO", ico.getQualityOfService(), "QoS");
			assertEquals(null, ico.getEx(), "Exception");
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
}
