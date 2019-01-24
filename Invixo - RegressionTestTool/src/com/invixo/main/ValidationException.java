package com.invixo.main;

public class ValidationException extends Exception {
	private static final long serialVersionUID = 5694583002323453092L;

	public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ValidationException(Throwable cause) {
        super(cause);
    }
}