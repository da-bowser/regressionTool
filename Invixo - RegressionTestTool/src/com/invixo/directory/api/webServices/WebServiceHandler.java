package com.invixo.directory.api.webServices;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import com.invixo.common.util.Logger;
import com.invixo.common.util.PropertyAccessor;
import com.invixo.common.util.Util;
import com.invixo.directory.api.DirectoryApiException;
import com.invixo.main.GlobalParameters;

public abstract class WebServiceHandler {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = WebServiceHandler.class.getName();
	private static final boolean ENABLE_WS_LOGGING = Boolean.valueOf(PropertyAccessor.getProperty("ENABLE_WS_LOGGING"));	
	private static final String ENCODING = GlobalParameters.ENCODING;
	private static final String WEB_SERVICE_USER = GlobalParameters.CREDENTIAL_USER;
	private static final String WEB_SERVICE_PASS = GlobalParameters.CREDENTIAL_PASS;
	private static final String ENDPOINT = GlobalParameters.SAP_PO_HTTP_HOST_AND_PORT + PropertyAccessor.getProperty("SERVICE_PATH_DIR_API");
	private static final int TIMEOUT = Integer.parseInt(PropertyAccessor.getProperty("TIMEOUT"));

	public static ByteArrayInputStream callWebService(byte[] requestBytes) throws DirectoryApiException {
		String SIGNATURE = "callWebService(byte[])";
		HttpURLConnection conn = null;
		InputStream response = null;
		try {		
			URL url = new URL(ENDPOINT);
			logMessage(SIGNATURE, "--------------- Web Service Call: begin -----------------------");
			logMessage(SIGNATURE, "Endpoint: " + url.toString());
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			
			// Set request header values
			String encoded = Base64.getEncoder().encodeToString((WEB_SERVICE_USER + ":" + WEB_SERVICE_PASS).getBytes(ENCODING));
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
				response = conn.getInputStream();
			} else {
				// Error
				response = conn.getErrorStream();
				String resp = Util.inputstreamToString(response, ENCODING);
				logger.writeError(LOCATION, SIGNATURE, "Negative Web Service response (HTTP status code " + status +"): \n" + resp);
				throw new RuntimeException("Error calling web service with endpoint: " + ENDPOINT + ". See the trace for details.");
			}
			
			// Return web service response
			ByteArrayInputStream bais = new ByteArrayInputStream(response.readAllBytes());
			return bais;
		} catch (IOException e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String ex = "Error calling web service.\n" + sw.toString();
			logger.writeError(LOCATION, SIGNATURE, ex);
			DirectoryApiException dae = new DirectoryApiException(ex);
			throw dae;
		} finally {
			if (response != null) {
				try { 
					response.close();	
				} catch (IOException e) {
					// Too bad...
				}
			}
			logMessage(SIGNATURE, "--------------- Web Service Call: end -----------------------\n");
		}
	}
	
	
	protected static void setHttpRequest(HttpURLConnection con, byte[] requestBytes) throws DirectoryApiException {
		final String SIGNATURE = "setHttpRequest(HttpURLConnection, byte[])";
		try {
			con.setDoOutput(true);
			DataOutputStream dos = new DataOutputStream(con.getOutputStream());
			dos.write(requestBytes);
			dos.flush();
			dos.close();
		} catch (IllegalStateException|IOException e) {
			String msg = "Error setting http request. " + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new DirectoryApiException(msg);
		}
	}
	
		
	protected static void logMessage(String signature, String msg) {
		if (ENABLE_WS_LOGGING) {
			logger.writeDebug(LOCATION, signature, msg);
		}
	}
}
