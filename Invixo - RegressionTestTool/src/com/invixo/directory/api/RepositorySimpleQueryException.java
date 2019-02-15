package com.invixo.directory.api;

public class RepositorySimpleQueryException extends Exception {
	private static final long serialVersionUID = 5694583002323453092L;

	public RepositorySimpleQueryException(String message) {
        super(message);
    }
    
    public RepositorySimpleQueryException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public RepositorySimpleQueryException(Throwable cause) {
        super(cause);
    }
}