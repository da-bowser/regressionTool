package com.invixo.common.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.invixo.common.IcoOverviewDeserializer;
import com.invixo.common.IcoOverviewInstance;
import com.invixo.injection.InjectionRequest;
import com.invixo.injection.IntegratedConfiguration;
import com.invixo.injection.RequestGeneratorUtil;
import com.invixo.main.GlobalParameters;

class WebServiceHandlerTest {
		
	private static final String resourceBasePath = "../../../../resources/testfiles/com/invixo/common/util/";
	
	@BeforeAll
    static void initAll() {
		GlobalParameters.PARAM_VAL_BASE_DIR 			= "c:\\RTT\\UnitTest";
		GlobalParameters.PARAM_VAL_OPERATION 			= "extract";
				
		GlobalParameters.PARAM_VAL_HTTP_HOST 			= "ipod.invixo.com";
		GlobalParameters.PARAM_VAL_HTTP_PORT 			= "50000";
		GlobalParameters.SAP_PO_HTTP_HOST_AND_PORT 		= "http://ipod.invixo.com:50000/";
		GlobalParameters.CREDENTIAL_USER 				= "rttuser";
		GlobalParameters.CREDENTIAL_PASS 				= "aLvD#l^[R(52";
		
		GlobalParameters.PARAM_VAL_SENDER_COMPONENT		= "RTT_Dummy";
		GlobalParameters.PARAM_VAL_XI_SENDER_ADAPTER	= "SOAP_XI_Sender";
    }	
	
	
	@Test
	@DisplayName("Test HTTP Post: positive, single")
	void httpPostWithPositiveResponse() {
		try {
			// Set endpoint: SAP PO Monitoring API
			String endpoint = GlobalParameters.SAP_PO_HTTP_HOST_AND_PORT
							+ PropertyAccessor.getProperty("SERVICE_PATH_EXTRACT");
			
			// Set Content-Type
			ContentType contentType = ContentType.parse("text/xml");
			
			// Set content
			byte[] requestContent = getFilecontent("httpPost_positive.xml");
			
			// Call endpoint
			byte[] positiveResponse = HttpHandler.post(endpoint, contentType, requestContent);
			
			// Check
			assertTrue(positiveResponse.length > 0);
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
	
	@Test
	@DisplayName("Test HTTP Post: negative")
	void httpPostWithNegativeResponse() {
		assertThrows(HttpException.class,
            () -> {
    			// Set endpoint: SAP PO Monitoring API
    			String endpoint = GlobalParameters.SAP_PO_HTTP_HOST_AND_PORT
    							+ PropertyAccessor.getProperty("SERVICE_PATH_EXTRACT");
    			
    			// Set Content-Type
    			ContentType contentType = ContentType.parse("text/xml");
    			
    			// Set content
		    	byte[] requestContent = getFilecontent("httpPost_negative.xml");
		    		
		    	// Call endpoint
		    	HttpHandler.post(endpoint, contentType, requestContent);
            });
	}

	
	@Test
	@DisplayName("Test HTTP Post: positive, multiple")
	void multiplehttpPostWithPositiveResponse() {
		try {
			// Prepare
			String endpoint 		= GlobalParameters.SAP_PO_HTTP_HOST_AND_PORT 
									+ PropertyAccessor.getProperty("SERVICE_PATH_EXTRACT");
			ContentType contentType = ContentType.parse("text/xml");
			byte[] request1 = getFilecontent("httpPost_positive.xml");
			byte[] request2 = getFilecontent("httpPost_positive2.xml");
			
			// Call endpoint: 1
			byte[] resp1 = HttpHandler.post(endpoint, contentType, request1);
			
			// Call endpoint: 2
			byte[] resp2 = HttpHandler.post(endpoint, contentType, request2);
						
			// Check
			assertTrue(resp1.length > 0);
			assertTrue(resp2.length > 0);
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
	
	@Test
	@DisplayName("Test multipart: positive, single")
	void httpMultipartPostWithPositiveResponse() {
		try {
			// Set endpoint: SAP PO Monitoring API
			String endpoint = GlobalParameters.SAP_PO_HTTP_HOST_AND_PORT
							+ PropertyAccessor.getProperty("SERVICE_PATH_INJECT")
							+ GlobalParameters.PARAM_VAL_SENDER_COMPONENT 
							+ ":" 
							+ GlobalParameters.PARAM_VAL_XI_SENDER_ADAPTER;
			
			// Get payload
			byte[] payload = getFilecontent("multipartPayload.xml");
			
			// Build SAP XI Header - PREPARE # Get path: System Component mapping file
			String systemMapping = resourceBasePath + "systemMapping.txt";
			URL urlSystemMapping = this.getClass().getResource(systemMapping);
			String pathSystemMapping = Paths.get(urlSystemMapping.toURI()).toString();
			
			// BUILD SAP XI Header: PREPARE # Get ICO list from ICO Overview file
			String icoOverviewPath = resourceBasePath + "TST_IntegratedConfigurationsOverview.xml";
			InputStream overviewStream = this.getClass().getResourceAsStream(icoOverviewPath);
			ArrayList<IcoOverviewInstance> icoOverviewList =  IcoOverviewDeserializer.deserialize(overviewStream);
			
			// Build SAP XI Header - PREPARE # Create GetMessageList request
			IntegratedConfiguration ico = new IntegratedConfiguration(icoOverviewList.get(0), pathSystemMapping, "PRD", "TST");

			// Build SAP XI Header
			InjectionRequest ir = new InjectionRequest();
			String xiHeader = RequestGeneratorUtil.generateSoapXiHeaderPart(ico, ir.getMessageId());
			
			// Perform test
			HttpPost httpPost = HttpHandler.buildMultipartHttpPostRequest(endpoint, xiHeader.getBytes(), payload);
//			httpPost.getEntity().writeTo(System.out);
			byte[] positiveResponse = HttpHandler.post(httpPost);
			
			// Check
			assertTrue(positiveResponse.length > 0);
		} catch (Exception e) {
			fail("It aint cooking chef! " + e);
		}
	}
	
	
	private byte[] getFilecontent(String fileName) throws IOException, URISyntaxException {
		String httpRequest = resourceBasePath + fileName;
		URL urlHttpRequest = getClass().getResource(httpRequest);
		Path pathHttpRequest = Paths.get(urlHttpRequest.toURI());
		byte[] fileContent = Files.readAllBytes(pathHttpRequest);
		return fileContent;
	}
}
