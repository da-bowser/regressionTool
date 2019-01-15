package com.invixo.messageExtractor.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyAccessor {

	private static final String propertiesFileName = "messageExtractor.properties";
	private static Properties properties = null;
	
	
	public static void main(String[] args) {
		try {
			String value;
			
			value = getProperty("USER");
			System.out.println(value);
			
			value = getProperty("PASSWORD");
			System.out.println(value);
		} catch (Exception e) {
			System.err.println(e);
		}
	}
	
	
	public static String getProperty(String key) {
		try {
			// Load properties if not loaded already
			if (properties == null) {
				properties = new Properties();
				loadProperties();
//				System.out.println("Properties loaded");
			}
			
			// Get value
			String value = properties.getProperty(key);
//			System.out.println("Value loaded: " + value);
			
			// Check to ensure we only continue if a value was found
			if (value == null) {
				throw new RuntimeException("*getProperty* Value not found for key '" + key + "'");
			} else {
				return value;
			}
		} catch (Exception e) {
			throw new RuntimeException("*getProperty* Error reading property. " + e);
		}
	}

	
	private static void loadProperties() throws IOException {
 		InputStream is = null;
		try {
			// Get properties file
			is = PropertyAccessor.class.getClassLoader().getResourceAsStream(propertiesFileName);
 
			// Load property values
			if (is == null) {
				// Property file not found
				throw new FileNotFoundException("*loadProperties* Property file '" + propertiesFileName + "' not found.");
			} else {
				// Property file is found. Load values.
				properties.load(is);
			}
		} catch (Exception e) {
			throw new RuntimeException("*loadProperties* Error loading properties. " + e);
		} finally {
			is.close();
		}
	}
}
