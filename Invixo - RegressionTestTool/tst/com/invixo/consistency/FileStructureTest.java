package com.invixo.consistency;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
		FileStructure.startCheck();
	}

}
