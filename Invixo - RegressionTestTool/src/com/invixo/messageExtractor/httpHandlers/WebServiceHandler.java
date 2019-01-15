package com.invixo.messageExtractor.httpHandlers;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.invixo.common.util.Util;
import com.invixo.messageExtractor.util.Logger;
import com.invixo.messageExtractor.util.PropertyAccessor;

public abstract class WebServiceHandler {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = WebServiceHandler.class.getName();
	private static final boolean ENABLE_WS_LOGGING = Boolean.valueOf(PropertyAccessor.getProperty("ENABLE_WS_LOGGING"));	
	private static final String ENCODING = PropertyAccessor.getProperty("ENCODING");
	private static final String WEB_SERVICE_USER = PropertyAccessor.getProperty("USER");
	private static final String WEB_SERVICE_PASS = PropertyAccessor.getProperty("PASSWORD");
	private static final String ENDPOINT = PropertyAccessor.getProperty("ENDPOINT");
	private static final int TIMEOUT = Integer.parseInt(PropertyAccessor.getProperty("TIMEOUT"));

	
	protected static InputStream callWebService(byte[] requestBytes) throws Exception {
		String SIGNATURE = "callWebService(byte[])";
		HttpURLConnection conn = null;
		
		try {
			URL url = new URL(ENDPOINT);
			logMessage(SIGNATURE, "---------------Web Service Call: begin -----------------------");
			logMessage(SIGNATURE, "Endpoint: " + url.toString());
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			
			// Set request header values
			String encoded = Base64.getEncoder().encodeToString((WEB_SERVICE_USER + ":" + WEB_SERVICE_PASS).getBytes(StandardCharsets.UTF_8));
			conn.setRequestProperty("Authorization", "Basic " + encoded);
			conn.setRequestProperty("Content-Type", "text/xml");
			conn.setRequestProperty("SOAPAction", "http://sap.com/xi/WebService/soap1.1");
			
			// Set timeouts
			conn.setConnectTimeout(TIMEOUT);
			conn.setReadTimeout(TIMEOUT);
						
			// Set request
			logMessage(SIGNATURE, "Setting web service request" );
			setHttpRequest(conn, requestBytes);
			
			// Call service
			logMessage(SIGNATURE, "Calling web service...");
			int status = conn.getResponseCode();
			logMessage(SIGNATURE, "Web service returned HTTP status code: " + status);
			
			// Handle response based on HTTP response code
			if (status == 200) {
				// Success
				logMessage(SIGNATURE, "--> Response is positive...");
			} else {
				// Error
				String response = Util.inputstreamToString(conn.getInputStream(), ENCODING);
				logMessage(SIGNATURE, "Response: " + response);
			}
			
			// Return the web service response
			ByteArrayInputStream bais = new ByteArrayInputStream(conn.getInputStream().readAllBytes());
			return bais;
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String ex = "Error calling web service.\n" + sw.toString();
			logger.writeError(LOCATION, SIGNATURE, ex);
			throw e;
		} finally { 
			if (conn.getInputStream() != null) {
				conn.getInputStream().close();	
			}
			logMessage(SIGNATURE, "---------------Web Service Call: end -----------------------\n");
		}
	}
	
	
	protected static void setHttpRequest(HttpURLConnection con, byte[] requestBytes) {
		try {
			con.setDoOutput(true);
			DataOutputStream dos = new DataOutputStream(con.getOutputStream());
			dos.write(requestBytes);
			dos.flush();
			dos.close();
		} catch (IllegalStateException|IOException e) {
			throw new RuntimeException("*setHttpRequest* Error setting http request.\n" + e);
		}
	}
	
		
	protected static void logMessage(String signature, String msg) {
		if (ENABLE_WS_LOGGING) {
			logger.writeDebug(LOCATION, signature, msg);
		}
	}
}
