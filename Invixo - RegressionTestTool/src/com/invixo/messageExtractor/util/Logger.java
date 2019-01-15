package com.invixo.messageExtractor.util;

import java.io.FileWriter;
import java.io.StringWriter;


public class Logger {

	// Variables
	public static final String LOGGING_TYPE = PropertyAccessor.getProperty("LOG_TYPE");			// CONSOLE | FILE
	private static final String LOCATION = Logger.class.getName();
	private static final String LOG_TYPE_ERROR_TXT = "[ERROR]";
    private static final String LOG_TYPE_DEBUG_TXT = "[DEBUG]";
	
	private final String LOG_FILE = System.currentTimeMillis() + ".txt";
	private FileWriter fileWriter = null;
    private static Logger instance;
    private enum LoggingTypes {CONSOLE, FILE};
    
    // Constructor
    private Logger() {}

       
    // Get instance
    public static synchronized Logger getInstance() {
    	String SIGNATURE = "getInstance()";
    	try {
        	if(instance == null){
        		// Initialize new logger instance
                instance = new Logger();

                // Initialize log file
                if (LoggingTypes.FILE.toString().equals(LOGGING_TYPE)) {
        			instance.fileWriter = new FileWriter(PropertyAccessor.getProperty("LOG_LOCATION") + instance.LOG_FILE, true);            	
                }
        	}
        	return instance;
    	} catch (Exception e) {
    		String ex = createLogMessage(Logger.LOCATION, SIGNATURE, e.getMessage(), true);
    		throw new RuntimeException(ex);    		
    	}
    }
	
	
    private void writeEntry(String location, String signature, String msg, boolean isError) {
    	String SIGNATURE = "writeEntry(String, String, String, boolean)";
    	try {
    		// Build message string
			String newLogMessage = createLogMessage(location, signature, msg, isError); 
			
			// Write entry to specified trace
			if (LoggingTypes.FILE.toString().equals(LOGGING_TYPE)) {
            	this.fileWriter.write(newLogMessage);            	
    			this.fileWriter.flush();
            } else {
            	System.out.print(newLogMessage);
            }
    	} catch (Exception e) {
    		String ex = createLogMessage(Logger.LOCATION, SIGNATURE, e.getMessage(), true);
    		throw new RuntimeException(ex);
    	}
    }
    
    
    private static String createLogMessage(String location, String signature, String msg, boolean isError) {
    	StringWriter sw = new StringWriter();
    	
    	// Handle type of entry (error or debug)
		if (isError) {
			sw.write(LOG_TYPE_ERROR_TXT);
		} else {
			sw.write(LOG_TYPE_DEBUG_TXT);
		}
		
		// Add remaining to string
		sw.write(" | " + System.nanoTime() + " | " + location + "@" + signature + " | " + msg + "\n");
    	
		sw.flush();
		return sw.toString();
    }
    
    
    public void writeDebug(String location, String signature, String msg) {
    	this.writeEntry(location, signature, msg, false);
    }
    
    
    public void writeError(String location, String signature, String msg) {
    	this.writeEntry(location, signature, msg, true);
    }
}
