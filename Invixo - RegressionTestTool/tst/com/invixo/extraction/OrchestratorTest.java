package com.invixo.extraction;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.invixo.common.IcoOverviewDeserializer;
import com.invixo.common.IcoOverviewInstance;
import com.invixo.consistency.FileStructure;
import com.invixo.main.GlobalParameters;

class OrchestratorTest {

	@BeforeAll
    static void initAll() {
		GlobalParameters.PARAM_VAL_BASE_DIR = "c:\\RTT\\UnitTest";
		GlobalParameters.PARAM_VAL_OPERATION = "extract";
		GlobalParameters.PARAM_VAL_EXTRACT_MODE_INIT="true";
		
		GlobalParameters.PARAM_VAL_ICO_REQUEST_FILES_ENV="PRD";
		GlobalParameters.PARAM_VAL_SOURCE_ENV="TST";
		GlobalParameters.PARAM_VAL_TARGET_ENV="TST";
		
		GlobalParameters.PARAM_VAL_HTTP_HOST = "ipod.invixo.com";
		GlobalParameters.PARAM_VAL_HTTP_PORT = "50000";
		GlobalParameters.SAP_PO_HTTP_HOST_AND_PORT="http://ipod.invixo.com:50000/";
		GlobalParameters.CREDENTIAL_USER = "rttuser";
		GlobalParameters.CREDENTIAL_PASS = "aLvD#l^[R(52";
    }
	
	
	@Test
	@DisplayName("Verify extraction works in initial mode")
	void verifyOrchestratorExecutesWithoutErrors() {
		try {
			// System Mapping file: Prepare
			Path sysMapFile = Paths.get(FileStructure.FILE_CONFIG_SYSTEM_MAPPING);
			String entry1 = "RTT_Sender|RTT_Sender|RTT_Sender\n";
			String entry2 = "RTT_Receiver|RTT_Receiver|RTT_Receiver\n";
			
			// System Mapping file: Delete existing entries (if any) matching entries
			List<String> lines = Files.readAllLines(sysMapFile);
			lines.removeIf(line -> !line.contains(entry1));
			lines.removeIf(line -> !line.contains(entry2));
			
			// System Mapping file: Write required entries
			Files.write(sysMapFile, entry1.getBytes(), StandardOpenOption.APPEND);
			Files.write(sysMapFile, entry2.getBytes(), StandardOpenOption.APPEND);
			
			// Get stream to ICO overview file
			String icoOverviewPath = "../../../resources/testfiles/com/invixo/extraction/TST_IntegratedConfigurationsOverview.xml";
			InputStream overviewStream = this.getClass().getResourceAsStream(icoOverviewPath);
			ArrayList<IcoOverviewInstance> icoOverviewList =  IcoOverviewDeserializer.deserialize(overviewStream);
			
			// Check execution works without errors
			Orchestrator.start(icoOverviewList);
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}

}
