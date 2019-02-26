package com.invixo.directory.api;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
		
		GlobalParameters.SAP_PO_HTTP_HOST_AND_PORT="http://ipod.invixo.com:50000/";
		GlobalParameters.CREDENTIAL_USER = "rttuser";
		GlobalParameters.CREDENTIAL_PASS = "aLvD#l^[R(52";
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

		} catch (IOException | DirectoryApiException e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
	
	@Test
	@DisplayName("Test response payload extract and make sure it returns correct Receiver componentId")
	void verifyExctractReceiverComponent() {
		try {
			// Get file from resources
			String icoReadResponseSingle = "tst/resources/testfiles/com/invixo/directory/api/SingleIcoReadResponse2.xml";

			// Convert to input stream
			File f = new File(icoReadResponseSingle);
			InputStream responseBytes;
			responseBytes = new FileInputStream(f);

			// Extract sender information
			ArrayList<IntegratedConfiguration> icoList = Orchestrator.extractIcoInformationFromReadResponse(responseBytes);
			
			// Get single ICO
			IntegratedConfiguration  ico = icoList.get(0);
			
			// Get receiver components in ICO
			Receiver r1 = ico.getReceiverList().get(0);
			Receiver r2 = ico.getReceiverList().get(1);
			
			assertEquals("Sys_QA3_011", r1.getComponentId());
			assertEquals("Sys_T_WMS_KOLDING", r2.getComponentId());

		} catch (IOException | DirectoryApiException e) {
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
	@DisplayName("Test correct receiver mapping data is found")
	void verifyCorretReceivingMappingData() {
		try {
			// Get file from resources
			String icoReadResponseSingle = "tst/resources/testfiles/com/invixo/directory/api/SingleIcoReadResponse.xml";

			// Convert to input stream
			File f = new File(icoReadResponseSingle);
			InputStream responseBytes;
			responseBytes = new FileInputStream(f);

			// Extract ico information from read response
			ArrayList<IntegratedConfiguration> icoList = Orchestrator.extractIcoInformationFromReadResponse(responseBytes);
			
			for(IntegratedConfiguration ico : icoList) {
				
				for(Receiver r : ico.getReceiverList()) {
					
					for(ReceiverInterfaceRule rir : r.getReceiverInterfaceRules()) {
						// Verify the spicific interface has mapping info
						if (rir.getInterfaceName().equals("Data_In_Async")) {
							assertEquals("Data_Out_Async_to_Data_In_Async", rir.getInterfaceMappingName());
							assertEquals("urn:invixo.com:sandbox:regressionTestTool", rir.getInterfaceMappingNamespace());
							assertEquals("9befd2e14c3611e7cd4ad94cac1f1354", rir.getInterfaceMappingSoftwareComponentVersionId());
						}
						
					}
				}
			}
		
		} catch (FileNotFoundException | DirectoryApiException e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
	
	@Test
	@DisplayName("Test multiplicity extract from simple query response")
	void verifyMultiplicityExtract() {
		try {
			// Get file from resources
			String icoReadResponseSingle = "tst/resources/testfiles/com/invixo/directory/api/RepositorySimpleQueryMultiplicityResponse.xml";

			// Convert to input stream
			File f = new File(icoReadResponseSingle);
			InputStream responseBytes;
			responseBytes = new FileInputStream(f);
			
			ReceiverInterfaceRule rir = new ReceiverInterfaceRule();
			
			Orchestrator.extractInterfaceMultiplicityFromResponse(responseBytes, rir);
			
			// Test
			assertEquals("1:n", rir.getInterfaceMultiplicity());
			
		
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
