package com.invixo.extraction;

public class ExtractorException extends Exception {
	private static final long serialVersionUID = 5694583002323453092L;

	public ExtractorException(String message) {
        super(message);
    }
    
    public ExtractorException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ExtractorException(Throwable cause) {
        super(cause);
    }
}