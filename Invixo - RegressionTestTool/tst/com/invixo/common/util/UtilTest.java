package com.invixo.common.util;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UtilTest {
		
	@Test
	@DisplayName("Test extraction of Message ID from MessageKey")
	void checkExtractionOfMessageIdFromMessageKey() {
		try {
			// Prepare
			String messageKey = "a3386b2a-1383-11e9-a723-000000554e16\\OUTBOUND\\5590550\\EO\\0";
			
			// Extract Message ID from MessageKey
			String msgId = Util.extractMessageIdFromKey(messageKey);
			
			// Check
			assertEquals("a3386b2a-1383-11e9-a723-000000554e16", msgId);
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}
		
	
	@Test
	@DisplayName("Test conversion from bytes to megabytes")
	void convertBytesToMegaBytes() {
		// Set input
		int bytes = 1234;
		
		// Do conversion
		String megabytes = Util.convertBytesToMegaBytes(bytes);
				
		// Test
		assertEquals("0,001177", megabytes);
	}

}
