package com.invixo.extraction;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.util.HashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.invixo.common.XiMessage;

class IntegratedConfigurationTest {
		
	@Test
	@DisplayName("Verify LAST message determination: Message Split (no condition), and no multimapping")
	void verifyLastMessageDetermination0() {
		try {
			// Get WS response
			String response = "../../../resources/testfiles/com/invixo/extraction/GetMessagesWithSuccessors_BatchResponse2.xml";
			InputStream wsResponseStream = this.getClass().getResourceAsStream(response);

			// Extract data from WS response
			String senderInterface = "Data_Out_Async";
			String receiverInterface = "Data_In_Async";
			HashMap<String, String> dataMap = WebServiceUtil.extractSuccessorsBatch(wsResponseStream.readAllBytes(), senderInterface, receiverInterface);
						
			// Get Last Messages:
			String firstMsgKey = "5cb97936-bafb-4b30-80ec-d4917dcfc413\\OUTBOUND\\0\\EO\\0\\";
			XiMessage firstPayload = new XiMessage();
			firstPayload.setSapMessageKey(firstMsgKey);
			XiMessages payloads = IntegratedConfiguration.getLastMessagesForFirstEntry(dataMap, firstPayload);
					
			// Check
			assertTrue("Too many LAST messages found", payloads.getLastMessageList().size() == 1);
			assertEquals("5fd149b0-3f3d-11e9-bc16-000000554e16", payloads.getLastMessageList().get(0).getSapMessageId());
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
	
	@Test
	@DisplayName("Verify LAST message determination: No Message Split, and multimapping generates multiple messages")
	void verifyLastMessageDetermination() {
		try {
			// Get WS response
			String response = "../../../resources/testfiles/com/invixo/extraction/GetMessagesWithSuccessors_BatchResponseLarge.xml";
			InputStream wsResponseStream = this.getClass().getResourceAsStream(response);

			// Extract data from WS response
			String senderInterface = "oa_PlannerProposal";
			String receiverInterface = "SHP_OBDLV_CHANGE.SHP_OBDLV_CHANGE01";
			HashMap<String, String> dataMap = WebServiceUtil.extractSuccessorsBatch(wsResponseStream.readAllBytes(), senderInterface, receiverInterface);
						
			// Get Last Messages:
			String firstMsgKey = "ddffb25a-38db-11e9-c594-0000273d8d22\\OUTBOUND\\658345250\\EOIO\\1\\";
			XiMessage firstPayload = new XiMessage();
			firstPayload.setSapMessageKey(firstMsgKey);
			XiMessages payloads = IntegratedConfiguration.getLastMessagesForFirstEntry(dataMap, firstPayload);
					
			// Check
			assertTrue(payloads.getLastMessageList().size() == 4);
			assertEquals("de0c626f-38db-11e9-90e0-0000273d8d22", payloads.getLastMessageList().get(0).getSapMessageId());
			assertEquals("de0c626d-38db-11e9-a337-0000273d8d22", payloads.getLastMessageList().get(1).getSapMessageId());
			assertEquals("de0c6f90-38db-11e9-c2c9-0000273d8d22", payloads.getLastMessageList().get(2).getSapMessageId());
			assertEquals("de0c626b-38db-11e9-911f-0000273d8d22", payloads.getLastMessageList().get(3).getSapMessageId());
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
	
	@Test
	@DisplayName("Verify LAST message determination: No Message Split, and multimapping generates 1 message")
	void verifyLastMessageDetermination2() {
		try {
			// Get WS response
			String response = "../../../resources/testfiles/com/invixo/extraction/GetMessagesWithSuccessors_BatchResponseLarge.xml";
			InputStream wsResponseStream = this.getClass().getResourceAsStream(response);

			// Extract data from WS response
			String senderInterface = "oa_PlannerProposal";
			String receiverInterface = "SHP_OBDLV_CHANGE.SHP_OBDLV_CHANGE01";
			HashMap<String, String> dataMap = WebServiceUtil.extractSuccessorsBatch(wsResponseStream.readAllBytes(), senderInterface, receiverInterface);
						
			// Get Last Messages:
			String firstMsgKey = "9652fd9c-3901-11e9-bc00-0000273d8d22\\OUTBOUND\\658345250\\EOIO\\3\\";
			XiMessage firstPayload = new XiMessage();
			firstPayload.setSapMessageKey(firstMsgKey);
			XiMessages payloads = IntegratedConfiguration.getLastMessagesForFirstEntry(dataMap, firstPayload);
					
			// Check
			assertTrue(payloads.getLastMessageList().size() == 1);
			assertEquals("965d8ef0-3901-11e9-88f7-0000273d8d22", payloads.getLastMessageList().get(0).getSapMessageId());
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
	
	@Test
	@DisplayName("Verify LAST message determination: Message Split, and multimapping generates multiple messages")
	void verifyLastMessageDetermination3() {
		try {
			// Get WS response
			String response = "../../../resources/testfiles/com/invixo/extraction/GetMessagesWithSuccessors_BatchResponseLarge.xml";
			InputStream wsResponseStream = this.getClass().getResourceAsStream(response);

			// Extract data from WS response
			String senderInterface = "oa_PlannerProposal";
			String receiverInterface = "SHP_OBDLV_CHANGE.SHP_OBDLV_CHANGE01";
			HashMap<String, String> dataMap = WebServiceUtil.extractSuccessorsBatch(wsResponseStream.readAllBytes(), senderInterface, receiverInterface);
						
			// Get Last Messages:
			String firstMsgKey = "1111ebc7-3932-11e9-8392-0000273d8d22\\OUTBOUND\\0\\EO\\0\\";
			XiMessage firstPayload = new XiMessage();
			firstPayload.setSapMessageKey(firstMsgKey);
			XiMessages payloads = IntegratedConfiguration.getLastMessagesForFirstEntry(dataMap, firstPayload);
					
			// Check
			assertTrue(payloads.getLastMessageList().size() == 18);
			assertEquals("11a94043-3932-11e9-cf54-0000273d8d22", payloads.getLastMessageList().get(0).getSapMessageId());
			assertEquals("11a9804f-3932-11e9-c1f8-0000273d8d22", payloads.getLastMessageList().get(1).getSapMessageId());
			assertEquals("11a98057-3932-11e9-c76f-0000273d8d22", payloads.getLastMessageList().get(2).getSapMessageId());
			assertEquals("11a9804d-3932-11e9-b3a1-0000273d8d22", payloads.getLastMessageList().get(3).getSapMessageId());
			assertEquals("11a9805b-3932-11e9-9837-0000273d8d22", payloads.getLastMessageList().get(4).getSapMessageId());
			assertEquals("11a94049-3932-11e9-9338-0000273d8d22", payloads.getLastMessageList().get(5).getSapMessageId());
			assertEquals("11a9805d-3932-11e9-8c5a-0000273d8d22", payloads.getLastMessageList().get(6).getSapMessageId());
			assertEquals("11a9404b-3932-11e9-9b16-0000273d8d22", payloads.getLastMessageList().get(7).getSapMessageId());
			assertEquals("11a94045-3932-11e9-9651-0000273d8d22", payloads.getLastMessageList().get(8).getSapMessageId());
			assertEquals("11a98051-3932-11e9-cc7b-0000273d8d22", payloads.getLastMessageList().get(9).getSapMessageId());
			assertEquals("11a98049-3932-11e9-8fdd-0000273d8d22", payloads.getLastMessageList().get(10).getSapMessageId());
			assertEquals("11a987f3-3932-11e9-9948-0000273d8d22", payloads.getLastMessageList().get(11).getSapMessageId());
			assertEquals("11a98059-3932-11e9-c6f0-0000273d8d22", payloads.getLastMessageList().get(12).getSapMessageId());
			assertEquals("11a9804b-3932-11e9-a4e1-0000273d8d22", payloads.getLastMessageList().get(13).getSapMessageId());
			assertEquals("11a98047-3932-11e9-cb10-0000273d8d22", payloads.getLastMessageList().get(14).getSapMessageId());
			assertEquals("11a94047-3932-11e9-8bc5-0000273d8d22", payloads.getLastMessageList().get(15).getSapMessageId());
			assertEquals("11a98055-3932-11e9-c06f-0000273d8d22", payloads.getLastMessageList().get(16).getSapMessageId());
			assertEquals("11a98053-3932-11e9-9181-0000273d8d22", payloads.getLastMessageList().get(17).getSapMessageId());
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}	
	
	
	@Test
	@DisplayName("Verify LAST message determination: Message Split, and multimapping generates 1 message")
	void verifyLastMessageDetermination4() {
		try {
			// Get WS response
			String response = "../../../resources/testfiles/com/invixo/extraction/GetMessagesWithSuccessors_BatchResponseLarge.xml";
			InputStream wsResponseStream = this.getClass().getResourceAsStream(response);

			// Extract data from WS response
			String senderInterface = "oa_PlannerProposal";
			String receiverInterface = "SHP_OBDLV_CHANGE.SHP_OBDLV_CHANGE01";
			HashMap<String, String> dataMap = WebServiceUtil.extractSuccessorsBatch(wsResponseStream.readAllBytes(), senderInterface, receiverInterface);
						
			// Get Last Messages:
			String firstMsgKey = "e8610cae-3929-11e9-b6ac-0000273d8d22\\OUTBOUND\\0\\EO\\0\\";
			XiMessage firstPayload = new XiMessage();
			firstPayload.setSapMessageKey(firstMsgKey);
			XiMessages payloads = IntegratedConfiguration.getLastMessagesForFirstEntry(dataMap, firstPayload);
					
			// Check
			assertTrue(payloads.getLastMessageList().size() == 1);
			assertEquals("e8b3c302-3929-11e9-c0e1-0000273d8d22", payloads.getLastMessageList().get(0).getSapMessageId());
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}		
	
}
