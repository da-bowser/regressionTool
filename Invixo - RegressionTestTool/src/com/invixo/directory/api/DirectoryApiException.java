package com.invixo.directory.api;

public class DirectoryApiException extends Exception {
	private static final long serialVersionUID = 5694583002323453092L;

	public DirectoryApiException(String message) {
        super(message);
    }
    
    public DirectoryApiException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public DirectoryApiException(Throwable cause) {
        super(cause);
    }
}