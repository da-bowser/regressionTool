package com.invixo.compare;

public class CompareException extends Exception {
	private static final long serialVersionUID = 5694583002323453092L;

	public CompareException(String message) {
        super(message);
    }
    
    public CompareException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CompareException(Throwable cause) {
        super(cause);
    }
}