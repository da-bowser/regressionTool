package com.invixo.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Base64;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.invixo.common.util.Logger;
import com.invixo.common.util.PropertyAccessor;
import com.invixo.main.GlobalParameters;


public class HttpHandler {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = HttpHandler.class.getName();
	private static final int TIMEOUT = Integer.parseInt(PropertyAccessor.getProperty("TIMEOUT"));

	private static final String CID_PAYLOAD = "INJECTION_PAYLOAD";
	private static final String CID_HEADER = "INJECTION_HEADER";
	
	private static CredentialsProvider credentialProvider = null;
	private static CloseableHttpClient httpclient = null;
	
	private static String encodedCredentials = Base64.getEncoder().encodeToString((GlobalParameters.CREDENTIAL_USER + ":" + GlobalParameters.CREDENTIAL_PASS).getBytes());
	private static String basicAuthHeaderValue = "Basic " + encodedCredentials;
	
	
	static {
		logger.writeDebug(LOCATION, "static init", "HTTP Host: " 		+ GlobalParameters.PARAM_VAL_HTTP_HOST);
		logger.writeDebug(LOCATION, "static init", "HTTP Port: "		+ GlobalParameters.PARAM_VAL_HTTP_PORT);
		logger.writeDebug(LOCATION, "static init", "User: " 			+ GlobalParameters.CREDENTIAL_USER);
		logger.writeDebug(LOCATION, "static init", "Timeout value: "	+ TIMEOUT);
		
		// Create basic authentication provider
		credentialProvider = new BasicCredentialsProvider();
		credentialProvider.setCredentials(
                new AuthScope(GlobalParameters.PARAM_VAL_HTTP_HOST, Integer.parseInt(GlobalParameters.PARAM_VAL_HTTP_PORT)),
                new UsernamePasswordCredentials(GlobalParameters.CREDENTIAL_USER, GlobalParameters.CREDENTIAL_PASS));

		// Set timeout values
		RequestConfig requestConfig = RequestConfig.custom()
										.setConnectionRequestTimeout(TIMEOUT)
										.setConnectTimeout(TIMEOUT)
										.setSocketTimeout(TIMEOUT)
										.build();
		
		// Create http client
		httpclient = HttpClients.custom()
						.setDefaultCredentialsProvider(credentialProvider)
						.setDefaultRequestConfig(requestConfig)
						.setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
						.build();
	}
	
	
	/**
	 * Perform HTTP GET
	 * @param endpoint			HTTP endpoint to get data from
	 * @return
	 * @throws HttpException
	 */
	public static byte[] get(String endpoint) throws HttpException {
		final String SIGNATURE = "get(String)";
		try {
			logger.writeDebug(LOCATION, SIGNATURE, "Endpoint: " + endpoint);

			// Create HTTP Get
			HttpGet httpGet = new HttpGet(endpoint);

			// Add basic auth
			httpGet.addHeader("Authorization", basicAuthHeaderValue);

			// Do the GET
			try (final CloseableHttpResponse response = httpclient.execute(httpGet)) {
				// Handle HTTP response
				InputStream positiveResponseContent = processHttpResponse(response);

				// Return
				return positiveResponseContent.readAllBytes();
			}
		} catch (IOException e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String ex = "Technical error executing HTTP Get call.\n" + sw.toString();
			logger.writeError(LOCATION, SIGNATURE, ex);
			throw new HttpException(ex);
		}
	}
	
	
	/**
	 * General purpose HTTP post. 
	 * Creates a plain HTTP post request to the specified endpoint with the specified content having the specified content-type. 
	 * @param endpoint				HTTP endpoint to call
	 * @param contentType			Content-type of request
	 * @param requestContent		Content of request
	 * @return
	 * @throws HttpException
	 */
	public static byte[] post(String endpoint, ContentType contentType, byte[] requestContent) throws HttpException {
		final String SIGNATURE = "post(String, ContentType, byte[])";
		logger.writeDebug(LOCATION, SIGNATURE, "Initiate HTTP post (plain)...");
		
		// Build plain HTTP request (no multipart)
		HttpPost httpPostRequest = buildHttpPostRequest(endpoint, contentType, requestContent);

		// Perform HTTP post
		byte[] positiveResponse = post(httpPostRequest);
		
		// Return
		return positiveResponse;
	}

	
	/**
	 * General purpose HTTP post
	 * @param httpPost		HTTP post request to be sent
	 * @return
	 * @throws HttpException
	 */
	public static byte[] post(HttpPost httpPost) throws HttpException {
		final String SIGNATURE = "post(HttpPost)";
		try {
			logger.writeDebug(LOCATION, SIGNATURE, "Initiate HTTP post (plain)...");
			
			// Send http post request
			try (final CloseableHttpResponse response = httpclient.execute(httpPost)) {
				// Handle HTTP response
				InputStream positiveResponseContent = processHttpResponse(response);
				
				// Return
				return positiveResponseContent.readAllBytes();
			}
		} catch (IOException e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String ex = "Technical error executing HTTP Post call.\n" + sw.toString();
			logger.writeError(LOCATION, SIGNATURE, ex);
			throw new HttpException(ex);
		}
	}
	
	
	/**
	 * Create a plain (no multipart) HTTP post request
	 * @param endpoint			HTTP endpoint to call
	 * @param contentType		Content-type of request
	 * @param requestContent	Content of request
	 * @return
	 */
	private static HttpPost buildHttpPostRequest(String endpoint, ContentType contentType, byte[] requestContent) {
		final String SIGNATURE = "buildHttpPostRequest(String, ContentType, byte[])";
		logger.writeDebug(LOCATION, SIGNATURE, "Endpoint: " + endpoint);
		logger.writeDebug(LOCATION, SIGNATURE, "ContentType: " + contentType);
		logger.writeDebug(LOCATION, SIGNATURE, "Request content size: " + requestContent.length);

		// Create HTTP Post
		HttpPost httpPost = new HttpPost(endpoint);
		
		EntityBuilder entityBuilder = EntityBuilder.create();
		HttpEntity entity = entityBuilder
								.setContentType(contentType)
//								.setContentEncoding(encoding)
								.setBinary(requestContent)
//								.gzipCompress()
								.build();
		
        // Add request to HTTP Post
        httpPost.setEntity(entity);
        
		return httpPost;
	}
	
	
	/**
	 * General purpose HTTP response handler.
	 * @param CloseableHttpResponse			HTTP response
	 * @return
	 * @throws HttpException
	 */
	private static InputStream processHttpResponse(CloseableHttpResponse response) throws HttpException {
		final String SIGNATURE = "processHttpResponse(CloseableHttpResponse)";
		try {
			// Get HTTP response code
			int responseCode = response.getStatusLine().getStatusCode();
			logger.writeDebug(LOCATION, SIGNATURE, "Response has HTTP status code: " + responseCode);
			
			// Handle response
			if (responseCode == 200 || responseCode == 202) {
				// Positive response
				logger.writeDebug(LOCATION, SIGNATURE, "Response is positive");
			} else {
				// Negative response
				String responseString = EntityUtils.toString(response.getEntity());
				String ex = "Negative HTTP response received: " + responseCode + ". Response: \n" + responseString;
				logger.writeError(LOCATION, SIGNATURE, ex);
				throw new HttpException(ex);
			}
			
			// Get positive response
			InputStream responseBytes = response.getEntity().getContent();
			
			// Return data
			return responseBytes;
		} catch (IOException|ParseException e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String ex = "Technical error processing HTTP response.\n" + sw.toString();
			logger.writeError(LOCATION, SIGNATURE, ex);
			throw new HttpException(ex);
		}
	}
	
		
	/**
	 * Multipart util function.
	 * Create a SAP XI multipart HTTP Post request.
	 * @param endpoint
	 * @param requestXiHeader
	 * @param requestPayload
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static HttpPost buildMultipartHttpPostRequest(String endpoint, byte[] requestXiHeader, byte[] requestPayload) throws UnsupportedEncodingException {
		// Create multipart boundary
		final String boundary = "-- Invixo Injection: boundary --";

		// Create HTTP Post call
		HttpPost httpPost = new HttpPost(endpoint);

		// Create multipart entity containing 2 parts: SAP PO Header and SAP PO Payload
		MultipartEntityBuilder entityBuilder = buildMultipartEntity(requestXiHeader, requestPayload, boundary);
        
        // Add multipart to request
        HttpEntity entity = entityBuilder.build();
        httpPost.setEntity(entity);
        
		return httpPost;
	}
	
	
	/**
	 * Multipart util function.
	 * Builds a multipart request message consisting of both HEADER and PAYLOAD part
	 * @param requestXiHeader
	 * @param requestPayload
	 * @param boundary
	 * @return
	 */
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
	
	
	/**
	 * Multipart util function.
	 * Create a multipart part (either HEADER or PAYLOAD)
	 * @param content
	 * @param isHeader
	 * @return
	 */
	private static FormBodyPart createMimePart(byte[] content, boolean isHeader) {
		ContentBody cb = new ByteArrayBody(content, GlobalParameters.CONTENT_TYPE_APP_XML, "dummy");
		FormBodyPart fbp = FormBodyPartBuilder.create("Invixo Injection Header", cb).build();
		fbp.addField( "Content-ID", (isHeader) ? CID_HEADER : CID_PAYLOAD);
		return fbp;
	}
	
}
