package com.invixo.main;

import com.invixo.common.util.PropertyAccessor;

public class GlobalParameters {
	public static final String ENCODING = PropertyAccessor.getProperty("ENCODING");
	public static final boolean DEBUG = Boolean.parseBoolean(PropertyAccessor.getProperty("DEBUG"));
	

}
