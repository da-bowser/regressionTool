package com.invixo.consistency;

import java.io.InputStream;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.invixo.common.IcoOverviewDeserializer;
import com.invixo.common.IcoOverviewInstance;
import com.invixo.main.GlobalParameters;

class FileStructureTest {

	@BeforeAll
    static void initAll() {
		GlobalParameters.PARAM_VAL_BASE_DIR = "c:\\RTT\\UnitTest";
		GlobalParameters.PARAM_VAL_OPERATION = "extract";
    }
	
	
	@Test
	@DisplayName("Verify startCheck has no initialization issues")
	void verifyFileStructureCheckCanExecute() {
		// Get stream to ICO overview file
		String icoOverviewPath = "../../../resources/testfiles/com/invixo/consistency/TST_IntegratedConfigurationsOverview.xml";
		InputStream overviewStream = this.getClass().getResourceAsStream(icoOverviewPath);
		ArrayList<IcoOverviewInstance> icoOverviewList =  IcoOverviewDeserializer.deserialize(overviewStream);
		
		// Check execution works without errors
		FileStructure.startCheck(icoOverviewList);
	}

}
