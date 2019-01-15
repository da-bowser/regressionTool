package com.invixo.common.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.invixo.messageExtractor.blocks.BGetMessageList;
import com.invixo.messageExtractor.util.Logger;

public class PropertyAccessor {

	private static final String LOCATION 			= BGetMessageList.class.getName();
	private static final String propertiesFileName 	= "payloadExtractor.properties";
	private static Logger logger 					= Logger.getInstance();
	private static Properties properties 			= null;
	
	
	public static void main(String[] args) {
		try {
			String value;
			
			// Test fetching property
			value = getProperty("USER");
			System.out.println(value);

			// Test fetching property
			value = getProperty("PASSWORD");
			System.out.println(value);
		} catch (Exception e) {
			System.err.println(e);
		}
	}
	
	
	public static String getProperty(String key) {
		String SIGNATURE = "getProperty(String)";
		try {
			// Load properties if not loaded already
			if (properties == null) {
				properties = new Properties();
				loadProperties();
			}
			
			// Get value
			String value = properties.getProperty(key);
			
			// Check to ensure we only continue if a value was found
			if (value == null) {
				String msg = "Value not found for key '" + key + "'";
				logger.writeError(LOCATION, SIGNATURE, msg);
				throw new RuntimeException(msg);
			} else {
				return value;
			}
		} catch (Exception e) {
			String msg = "Error reading property " + key + ".\n" + e.getMessage();
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		}
	}

	
	private static void loadProperties() throws IOException {
		String SIGNATURE = "loadProperties()";
 		InputStream is = null;
		try {
			// Get properties file
			is = PropertyAccessor.class.getClassLoader().getResourceAsStream(propertiesFileName);
 
			// Load property values
			if (is == null) {
				// Property file not found
				String msg = "Property file '" + propertiesFileName + "' not found.";
				logger.writeError(LOCATION, SIGNATURE, msg);
				throw new FileNotFoundException(msg);			
			} else {
				// Property file is found. Load values.
				properties.load(is);
			}
		} catch (Exception e) {
			String msg = "Error loading properties. " + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);	
		} finally {
			is.close();
		}
	}
}
