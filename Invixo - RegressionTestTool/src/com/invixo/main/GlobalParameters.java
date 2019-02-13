package com.invixo.main;

import org.apache.http.entity.ContentType;

import com.invixo.common.util.PropertyAccessor;

public class GlobalParameters {
	public static final String ENCODING 					= PropertyAccessor.getProperty("ENCODING");
	public static final boolean DEBUG 						= Boolean.parseBoolean(PropertyAccessor.getProperty("DEBUG"));
	public static final String FILE_DELIMITER 				= "#";
	public static final ContentType CONTENT_TYPE_TEXT_XML 	= ContentType.TEXT_XML.withCharset(ENCODING);
	public static final ContentType CONTENT_TYPE_APP_XML 	= ContentType.APPLICATION_XML.withCharset(ENCODING);
	
	public enum Environment { DEV, TST, PRD };
	public enum Operation { extract, inject, compare , createIcoOverview};
	

	// Parameter: dictates which environment *all* ICO request files are based on. (used for translation/mapping of sender system)
	public static String PARAM_VAL_ICO_REQUEST_FILES_ENV 	= null;
	
	// Parameter: base directory for all reading and writing to/from file system
	public static String PARAM_VAL_BASE_DIR 				= null;
	
	// Parameter: target environment to extract from, inject to or compare with
	public static String PARAM_VAL_TARGET_ENV 				= null;
	
	// Parameter: operation for program to perform (extract, inject, compare)
	public static String PARAM_VAL_OPERATION 				= null;
	
	// Parameter: source environment to compare to targetEnvironment or in the case on injection, which environment to inject payload files from
	public static String PARAM_VAL_SOURCE_ENV 				= null;
		
	// SAP PO user/password
	public static String CREDENTIAL_USER					= null;
	public static String CREDENTIAL_PASS 					= null;
		
	// SAP PO URL PREFIX/START. Example result: http://ipod.invixo.com:50000/
	public static String SAP_PO_HTTP_HOST_AND_PORT			= null;
	
	// SAP PO host and port
	public static String PARAM_VAL_HTTP_HOST				= null;
	public static String PARAM_VAL_HTTP_PORT 				= null;
	
	// Parameter: SAP PO, name of Sender SOAP XI adapter
	public static String PARAM_VAL_XI_SENDER_ADAPTER 		= null;

	// Parameter: SAP PO name of Sender Component containing the SOAP XI adapter
	public static String PARAM_VAL_SENDER_COMPONENT 		= null;

	// Parameter: internal test parameter to skip deletion of target env payload files when source and target env are identical 
	public static boolean PARAM_VAL_ALLOW_SAME_ENV 			= false;
	
	// Parameter: From time (for extraction)
	public static String PARAM_VAL_FROM_TIME 				= null;

	// Parameter: To time (for extraction)
	public static String PARAM_VAL_TO_TIME 					= null;
	
	// Parameter: Extract mode (init or non-init)
	public static String PARAM_VAL_EXTRACT_MODE_INIT 		= null;

}
