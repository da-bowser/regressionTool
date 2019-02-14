package com.invixo.directory.api;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.invixo.main.GlobalParameters;

public class OrchestratorTest {
	
	@BeforeAll
    static void initAll() {
		GlobalParameters.PARAM_VAL_BASE_DIR = "c:\\RTT\\UnitTest";
		GlobalParameters.PARAM_VAL_OPERATION = "createIcoOverview";
		
		GlobalParameters.PARAM_VAL_ICO_REQUEST_FILES_ENV="PRD";
		GlobalParameters.PARAM_VAL_SOURCE_ENV="TST";
		GlobalParameters.PARAM_VAL_TARGET_ENV="TST";
		
		GlobalParameters.SAP_PO_HTTP_HOST_AND_PORT="http://ipod.invixo.com:50000";
		GlobalParameters.CREDENTIAL_USER = "rttuser";
		GlobalParameters.CREDENTIAL_PASS = "aLvD#l^[R\\52";
    }
	
	
	@Test
	@DisplayName("Test response payload extract and make sure it returns correct number of 'Senders'")
	void verifyCorrectSenderCountInExtract() {
		try {
			// Get file from resources
			String icoReadResponseSingle = "tst/resources/testfiles/com/invixo/directory/api/SingleIcoReadResponse.xml";

			// Convert to input stream
			File f = new File(icoReadResponseSingle);
			InputStream responseBytes;
			responseBytes = new FileInputStream(f);

			// Extract sender information
			ArrayList<IntegratedConfigurationReadRequest> icorr = Orchestrator.extractIcoDataFromQueryResponse(responseBytes);

			// Do test
			assertEquals(1, icorr.size());

		} catch (FileNotFoundException | DirectoryApiException e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
	
	@Test
	@DisplayName("Test response payload extract and make sure it returns correct number of 'Receivers'")
	void verifyCorrectReceiverCountInExtract() {
		try {
			// Get file from resources
			String icoReadResponseSingle = "tst/resources/testfiles/com/invixo/directory/api/SingleIcoReadResponse.xml";

			// Convert to input stream
			File f = new File(icoReadResponseSingle);
			InputStream responseBytes;
			responseBytes = new FileInputStream(f);

			// Extract ico information from read response
			ArrayList<IntegratedConfiguration> icoList = Orchestrator.extractIcoInformationFromReadResponse(responseBytes);
			
			// Test: Two receivers found for single ico
			assertEquals(2, icoList.get(0).getReceiverList().size());

		} catch (FileNotFoundException | DirectoryApiException e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
	
	@Test
	@DisplayName("Test response payload extract and make sure it returns correct number of 'ReceiverInterfaceRules'")
	void verifyCorrectReceiverInterfaceRuleCountInExtract() {
		try {
			// Get file from resources
			String icoReadResponseSingle = "tst/resources/testfiles/com/invixo/directory/api/SingleIcoReadResponse.xml";

			// Convert to input stream
			File f = new File(icoReadResponseSingle);
			InputStream responseBytes;
			responseBytes = new FileInputStream(f);

			// Extract ico information from read response
			ArrayList<IntegratedConfiguration> icoList = Orchestrator.extractIcoInformationFromReadResponse(responseBytes);
			int receiverInterfaceRuleCounter = 0;
			
			for (IntegratedConfiguration ico : icoList) {
				ArrayList<Receiver> reciverList = ico.getReceiverList();
				
				for (Receiver r : reciverList) {
					// Calculate total number of receiver interface rules
					receiverInterfaceRuleCounter += r.getReceiverInterfaceRules().size();
				}
			}
			
			// Test: Three receiver interface rules in total
			assertEquals(3, receiverInterfaceRuleCounter);

		} catch (FileNotFoundException | DirectoryApiException e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
	
	@Test
	@DisplayName("Test creation of overview file")
	void verifyCreationOfOverviewFile() {
		try {
			// Get file from resources
			String icoReadResponseSingle = "tst/resources/testfiles/com/invixo/directory/api/SingleIcoReadResponse.xml";

			// Convert to input stream
			File f = new File(icoReadResponseSingle);
			InputStream responseBytes;
			responseBytes = new FileInputStream(f);

			// Extract ico information from read response
			ArrayList<IntegratedConfiguration> icoList = Orchestrator.extractIcoInformationFromReadResponse(responseBytes);
			
			// Create file
			String icoOverviewFilePath = Orchestrator.createCompleteIcoOverviewFile(icoList);
			
			// Get icoOverviewFile
			File iovf = new File(icoOverviewFilePath);
			
			// Test: exists
			assertEquals(true, iovf.exists());
			

		} catch (FileNotFoundException | DirectoryApiException | XMLStreamException e) {
			fail("It aint cooking chef! " + e);
		}
	}
}
