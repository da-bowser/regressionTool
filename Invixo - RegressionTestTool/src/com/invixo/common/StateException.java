package com.invixo.common;

public class StateException extends Exception {
	private static final long serialVersionUID = 4635125080407840590L;

	public StateException(String message) {
        super(message);
    }
    
    public StateException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public StateException(Throwable cause) {
        super(cause);
    }
}