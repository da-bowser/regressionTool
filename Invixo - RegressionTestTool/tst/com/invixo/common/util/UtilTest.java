package com.invixo.common.util;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.invixo.main.GlobalParameters;

class UtilTest {
		
	@Test
	@DisplayName("Test getRelevantMessageIds with empty filter")
	void getRelevantMessageIdsWithEmptyFilter() {
		try {
			// Get path: Message ID mapping test file 
			String systemMapping = "../../../../resources/util/testfiles/TST_to_TST_msgId_map.txt";
			URL urlSystemMapping = getClass().getResource(systemMapping);
			String pathSystemMapping = Paths.get(urlSystemMapping.toURI()).toString();
			
			// Get message ids from file matching scenario
			Map<String, String> map = Util.getMessageIdsFromFile(pathSystemMapping, GlobalParameters.FILE_DELIMITER, "", 1, 2);
		
			// Test: all content of map file is expected to be returned
			assertEquals(10, map.size());
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
	
	@Test
	@DisplayName("Test getRelevantMessageIds with non-empty filter")
	void getRelevantMessageIdsWithNonEmptyFilter() {
		try {
			// Get path: Message ID mapping test file 
			String systemMapping = "../../../../resources/util/testfiles/TST_to_TST_msgId_map.txt";
			URL urlSystemMapping = getClass().getResource(systemMapping);
			String pathSystemMapping = Paths.get(urlSystemMapping.toURI()).toString();
			
			// Get message ids from file matching scenario
			String scenario = "Scenario 2";
			Map<String, String> map = Util.getMessageIdsFromFile(pathSystemMapping, GlobalParameters.FILE_DELIMITER, scenario, 1, 2);
		
			// Test: all lines with a matching scenario is expected to be returned
			assertEquals(3, map.size());
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
