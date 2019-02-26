package com.invixo.common.util;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import javax.mail.Multipart;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class XiMessageUtilTest {
	private static final String resourceBasePath = "../../../../resources/testfiles/com/invixo/common/util/";
	
	@Test
	@DisplayName("Test deserialization of XiHeader")
	void checkDeserializationOfXiHeader() {
		try {
			// Get inputstream to multipart message
			String multipartPath = resourceBasePath + "c4b6a300-2ea5-4691-88f2-fbc1a3d24ca5.multipart";
			InputStream multipartStream = this.getClass().getResourceAsStream(multipartPath);
			
			// Get MultiPart from Stream
			Multipart multpart = XiMessageUtil.createMultiPartMessage(multipartStream.readAllBytes());
			
			// Deserialize
			XiHeader header = XiMessageUtil.deserializeXiHeader(multpart);
			
			// Check
			assertEquals("c4b6a300-2ea5-4691-88f2-fbc1a3d24ca5", header.getMessageId(), "Message Id");
			assertEquals("asynchronous", header.getProcessingMode(), "Processing Mode");
			assertEquals("ExactlyOnce", header.getQualityOfService(), "Quality Of Service");
			assertTrue(header.getDynamicConfList().size() == 5, "Number of Dynamic Configuration Records");
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
}
