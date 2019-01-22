package com.invixo.common;

public class GeneralException extends Exception {
	private static final long serialVersionUID = -2434935612228040928L;

	public GeneralException(String message) {
        super(message);
    }
    
    public GeneralException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public GeneralException(Throwable cause) {
        super(cause);
    }
}