package com.invixo.common;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;

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
		GlobalParameters.PARAM_VAL_SOURCE_ENV = "TST";
    }
	
	@Test
	@DisplayName("Test constructor extracts data properly")
	void constructorExtractionVerification() {
		try {
			// Get ICO list from ICO Overview file
			String icoOverviewPath = "../../../resources/testfiles/com/invixo/common/TST_IntegratedConfigurationsOverview.xml";
			InputStream overviewStream = this.getClass().getResourceAsStream(icoOverviewPath);
			ArrayList<IcoOverviewInstance> icoOverviewList =  IcoOverviewDeserializer.deserialize(overviewStream);
			
			// Get path: System Component mapping file
			String systemMapping = "../../../resources/testfiles/com/invixo/common/systemMapping.txt";
			URL urlSystemMapping = this.getClass().getResource(systemMapping);
			String pathSystemMapping = Paths.get(urlSystemMapping.toURI()).toString();
			
			// Create GetMessageList request
			IntegratedConfigurationMain ico = new IntegratedConfigurationMain(icoOverviewList.get(0), pathSystemMapping, "PRD", "TST");
			
			// Check: fail if message properties are not set correctly
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
			assertEquals("Data_In_Async", ico.getReceiverInterface(), "ReceiverInterfaceName");
			assertEquals("urn:invixo.com:sandbox:regressionTestTool", ico.getReceiverNamespace(), "ReceiverNamespace");
			
			// Check: various
			assertEquals(null, ico.getFromTime(), "FromTime");
			assertEquals(null, ico.getToTime(), "ToTime");
			assertEquals(3, ico.getMaxMessages(), "MaxMessages");
			assertEquals("EO", ico.getQualityOfService(), "QoS");
			assertEquals(null, ico.getEx(), "Exception");
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
}
