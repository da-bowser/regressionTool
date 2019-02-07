package com.invixo.common.util;

import java.io.FileWriter;
import java.io.StringWriter;

import com.invixo.consistency.FileStructure;
import com.invixo.main.GlobalParameters;

public class Logger {
	// Variables
	public static final String LOGGING_TYPE = PropertyAccessor.getProperty("LOG_TYPE");			// CONSOLE | FILE
	private static final String LOCATION = Logger.class.getName();
	private static final String LOG_TYPE_ERROR_TXT = "[ERROR]";
    private static final String LOG_TYPE_DEBUG_TXT = "[DEBUG]";
	
	private final String logFileName = "RunLog_" + System.currentTimeMillis() + "_" + GlobalParameters.PARAM_VAL_OPERATION + ".txt";
	private FileWriter fileWriter = null;
    private static Logger instance;
    private enum LoggingTypes {CONSOLE, FILE};
    
    // Constructor
    private Logger() {}

       
    // Get instance
    public static synchronized Logger getInstance() {
    	final String SIGNATURE = "getInstance()";
    	try {
        	if (instance == null) {
        		// Initialize new logger instance
                instance = new Logger();

                // Initialize log file
                if (LoggingTypes.FILE.toString().equals(LOGGING_TYPE)) {
                	// TODO FileStructure is not initialized at this point and thus cannot be used... This should be fixed at some point...
                	String logDir = GlobalParameters.PARAM_VAL_BASE_DIR + "\\Logs\\";
                	String logFile = logDir + instance.logFileName;
                	FileStructure.createDirIfNotExists(logDir);
        			instance.fileWriter = new FileWriter(logFile, true);            	
                }
        	}
        	return instance;
    	} catch (Exception e) {
    		String ex = createLogMessage(Logger.LOCATION, SIGNATURE, e.getMessage(), true);
    		throw new RuntimeException(ex);    		
    	}
    }
	
	
    private void writeEntry(String location, String signature, String msg, boolean isError) {
    	final String SIGNATURE = "writeEntry(String, String, String, boolean)";
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
