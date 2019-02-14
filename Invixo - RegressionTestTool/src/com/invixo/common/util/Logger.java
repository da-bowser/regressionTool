package com.invixo.common.util;

import java.io.FileWriter;
import java.io.StringWriter;

import com.invixo.main.GlobalParameters;

public class Logger {
	// Variables
	private static final String PROP_LOG_TYPE = PropertyAccessor.getProperty("LOG_TYPE");		// CONSOLE | FILE
	private static final String PROP_LOG_LEVEL = PropertyAccessor.getProperty("LOG_LEVEL");		// INFO | DEBUG | ERROR
	
	private static final String LOCATION = Logger.class.getName();
    private static final String LOG_TYPE_INFO_TXT = "[INFO]";
    private static final String LOG_TYPE_DEBUG_TXT = "[DEBUG]";
    private static final String LOG_TYPE_ERROR_TXT = "[ERROR]";
	
	private final String logFileName = "RunLog_" + System.currentTimeMillis() + "_" + GlobalParameters.PARAM_VAL_OPERATION + ".txt";
	private FileWriter fileWriter = null;
    private static Logger instance;
    
    private boolean logInfo = false;
    private boolean logDebug = false;
    
    private enum LoggingTypes {CONSOLE, FILE};
    private enum LOG_LEVEL { INFO, DEBUG, ERROR };
    
    
    // Constructor
    private Logger() {}
       
    // Get instance
    public static synchronized Logger getInstance() {
    	final String SIGNATURE = "getInstance()";
    	try {
        	if (instance == null) {
        		// Initialize new logger instance
                instance = new Logger();

                // Set log levels
                instance.logInfo = LOG_LEVEL.INFO.toString().equals(PROP_LOG_LEVEL);
                instance.logDebug = LOG_LEVEL.DEBUG.toString().equals(PROP_LOG_LEVEL);
                
                // Initialize log file
                if (LoggingTypes.FILE.toString().equals(PROP_LOG_TYPE)) {
                	// TODO FileStructure is not initialized at this point and thus cannot be used... This should be fixed at some point...
                	String logDir = GlobalParameters.PARAM_VAL_BASE_DIR + "\\Logs\\";
                	String logFile = logDir + instance.logFileName;
                	Util.createDirIfNotExists(logDir);
        			instance.fileWriter = new FileWriter(logFile, true);            	
                }
        	}
        	return instance;
    	} catch (Exception e) {
    		String ex = createLogMessage(Logger.LOCATION, SIGNATURE, e.getMessage(), LOG_LEVEL.ERROR);
    		throw new RuntimeException(ex);    		
    	}
    }
	
	
    private void writeEntry(String location, String signature, String msg, LOG_LEVEL logLevel) {
    	final String SIGNATURE = "writeEntry(String, String, String, boolean)";
    	try {
    		// Build message string
			String newLogMessage = createLogMessage(location, signature, msg, logLevel); 
			
			// Write entry to specified trace
			if (LoggingTypes.FILE.toString().equals(PROP_LOG_TYPE)) {
            	this.fileWriter.write(newLogMessage);            	
    			this.fileWriter.flush();
            } else {
            	System.out.print(newLogMessage);
            }
    	} catch (Exception e) {
    		String ex = createLogMessage(Logger.LOCATION, SIGNATURE, e.getMessage(), LOG_LEVEL.ERROR);
    		throw new RuntimeException(ex);
    	}
    }
    
    
    private static String createLogMessage(String location, String signature, String msg, LOG_LEVEL logLevel) {
    	StringWriter sw = new StringWriter();
    	
    	// Handle type of entry (error or debug)
		switch (logLevel) {
			case INFO	: sw.write(LOG_TYPE_INFO_TXT); break;
			case DEBUG 	: sw.write(LOG_TYPE_DEBUG_TXT); break;
			case ERROR 	: sw.write(LOG_TYPE_ERROR_TXT); break;
		}
		
		// Add remaining to string
		sw.write(" | " + System.currentTimeMillis() + " | " + location + "@" + signature + " | " + msg + "\n");
    	
		sw.flush();
		return sw.toString();
    }
    
    
    /**
     * Log info (high level info)
     * @param location
     * @param signature
     * @param msg
     */
    public void writeInfo(String location, String signature, String msg) {
    	if (this.logInfo || this.logDebug) {
    		this.writeEntry(location, signature, msg, LOG_LEVEL.INFO);
    	}
    }
    
    
    /**
     * Log debug details (detailed)
     * @param location
     * @param signature
     * @param msg
     */
    public void writeDebug(String location, String signature, String msg) {
    	if (this.logDebug) {
    		this.writeEntry(location, signature, msg, LOG_LEVEL.DEBUG);
    	}
    }
    
    
    /**
     * Log errors (always)
     * @param location
     * @param signature
     * @param msg
     */
    public void writeError(String location, String signature, String msg) {
    	this.writeEntry(location, signature, msg, LOG_LEVEL.ERROR);
    }
    
}
