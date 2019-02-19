package com.invixo.common;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


public class IcoOverviewDeserializerTest {

	@Test
	@DisplayName("Test deserialization of XML Overview")
	void testPositiveDeserialization() {
		try {
			// Get ICO list from ICO Overview file
			String icoOverviewPath = "../../../resources/testfiles/com/invixo/common/TST_IntegratedConfigurationsOverview.xml";
			InputStream overviewStream = this.getClass().getResourceAsStream(icoOverviewPath);
			ArrayList<IcoOverviewInstance> icoOverviewList =  IcoOverviewDeserializer.deserialize(overviewStream);
			IcoOverviewInstance ico = icoOverviewList.get(0);
					
			// Check: size
			assertEquals(1, icoOverviewList.size(), "Size of list");
			
			// Check: sender
			assertEquals(null, ico.getSenderParty(), "SenderParty");
			assertEquals("RTT_SenderPRD", ico.getSenderComponent(), "SenderComponent");
			assertEquals("Data_Out_Async", ico.getSenderInterface(), "SenderInterfaceName");
			assertEquals("urn:invixo.com:sandbox:regressionTestTool", ico.getSenderNamespace(), "SenderNamespace");
			
			// Check: receiver
			assertEquals(null, ico.getReceiverParty(), "ReceiverParty");
			assertEquals("RTT_ReceiverPRD", ico.getReceiverComponent(), "ReceiverComponent");
			assertEquals("Data_In_Async", ico.getReceiverInterface(), "ReceiverInterfaceName");
			assertEquals("urn:invixo.com:sandbox:regressionTestTool", ico.getReceiverNamespace(), "ReceiverNamespace");
			
			// Check: various
			assertEquals(null, ico.getFromTime(), "FromTime");
			assertEquals(null, ico.getToTime(), "ToTime");
			assertEquals(3, ico.getMaxMessages(), "MaxMessages");
			assertEquals("EO", ico.getQualityOfService(), "QoS");
			assertTrue(ico.isUsingMultiMapping(), "Uses MultiMapping");
			assertTrue(ico.isActive(), "IsActive");
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
}
