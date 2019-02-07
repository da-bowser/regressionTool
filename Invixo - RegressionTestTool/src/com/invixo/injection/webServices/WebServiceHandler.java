package com.invixo.injection.webServices;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Base64;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.invixo.common.util.Logger;
import com.invixo.common.util.PropertyAccessor;
import com.invixo.common.util.Util;
import com.invixo.injection.InjectionPayloadException;
import com.invixo.main.GlobalParameters;

public class WebServiceHandler {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = WebServiceHandler.class.getName();
	private static final String ENCODING = GlobalParameters.ENCODING;
	private static final String WEB_SERVICE_USER = GlobalParameters.CREDENTIAL_USER;
	private static final String WEB_SERVICE_PASS = GlobalParameters.CREDENTIAL_PASS;
	private static final String SERVICE_HOST_PORT = GlobalParameters.SAP_PO_HTTP_HOST_AND_PORT;
	private static final String SERVICE_PATH_INJECT = PropertyAccessor.getProperty("SERVICE_PATH_INJECT") + GlobalParameters.PARAM_VAL_SENDER_COMPONENT + ":" + GlobalParameters.PARAM_VAL_XI_SENDER_ADAPTER;
	private static final String ENDPOINT = SERVICE_HOST_PORT + SERVICE_PATH_INJECT;
	public static final String CID_PAYLOAD = "INJECTION_PAYLOAD";
	public static final String CID_HEADER = "INJECTION_HEADER";

	
	public static InputStream callWebService(HttpPost httpPostRequest) throws InjectionPayloadException {
		final String SIGNATURE = "callWebService(HttpPost)";
		try {
	        // Call service
			CloseableHttpClient httpclient = HttpClients.createDefault();
			CloseableHttpResponse response = httpclient.execute(httpPostRequest);
			
			// Handle response
			handleWebServiceResponse(response);
			
			// Return Web Service response
			return response.getEntity().getContent();
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String ex = "Error calling web service.\n" + sw.toString();
			logger.writeError(LOCATION, SIGNATURE, ex);
			throw new InjectionPayloadException(ex);
		}
	}


	public static HttpPost buildHttpPostRequest(byte[] requestXiHeader, byte[] requestPayload) throws UnsupportedEncodingException {
		// Create multipart boundary
		final String boundary = "-- Invixo Injection: boundary --";

		// Create HTTP Post call
		HttpPost httpPost = new HttpPost(ENDPOINT);

		// Set authentication
		String encoded = Base64.getEncoder().encodeToString((WEB_SERVICE_USER + ":" + WEB_SERVICE_PASS).getBytes(ENCODING));
		httpPost.setHeader("Authorization", "Basic " + encoded);
		httpPost.setHeader("Content-Type", "multipart/related; type=\"text/xml\"; start=\"" + CID_HEADER + "\"; boundary=" + boundary);

		// Create multipart entity containing 2 parts: SAP PO Header and SAP PO Payload
		MultipartEntityBuilder entityBuilder = buildMultipartEntity(requestXiHeader, requestPayload, boundary);
        
        // Add multipart to request
        HttpEntity entity = entityBuilder.build();
        httpPost.setEntity(entity);
        
		return httpPost;
	}
	
	
	private static void handleWebServiceResponse(CloseableHttpResponse response) throws UnsupportedOperationException, IOException {
		final String SIGNATURE = "handleWebServiceResponse(byte[], byte[])";
		
		int status = response.getStatusLine().getStatusCode();
		logger.writeDebug(LOCATION, SIGNATURE, "Injection Web Service returned HTTP status code: " + status);
		
		if (status == 200 || status == 202) {
			logger.writeDebug(LOCATION, SIGNATURE, "Message injected succesfully.");
		} else {
			String resp = Util.inputstreamToString(response.getEntity().getContent(), ENCODING);
			logger.writeDebug(LOCATION, SIGNATURE, "Web Service response: " + resp);
			
			String ex = "Error injecting message. Negative HTTP response received.";
			logger.writeError(LOCATION, SIGNATURE, ex);

			throw new RuntimeException(ex);
		}
	}


	private static MultipartEntityBuilder buildMultipartEntity(byte[] requestXiHeader, byte[] requestPayload, String boundary) {
		// Get a multipart builder
		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		entityBuilder.setBoundary(boundary);
		
		// Add mime part: Header
		FormBodyPart partHeader = createMimePart(requestXiHeader, true);
		entityBuilder.addPart(partHeader);
		
		// Add mime part: Payload
		FormBodyPart partPayload = createMimePart(requestPayload, false);
		entityBuilder.addPart(partPayload);
		
		return entityBuilder;
	}


	private static FormBodyPart createMimePart(byte[] content, boolean isHeader) {
		ContentBody cb = new ByteArrayBody(content, ContentType.APPLICATION_XML, "dummy");
		FormBodyPart fbp = FormBodyPartBuilder.create("Invixo Injection Header", cb).build();
		fbp.addField( "Content-ID", (isHeader) ? CID_HEADER : CID_PAYLOAD);
		return fbp;
	}

}
