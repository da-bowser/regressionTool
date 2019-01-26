package com.invixo.extraction;

public class NoMsgFoundException extends Exception {
	private static final long serialVersionUID = -4727076396062592151L;

	public NoMsgFoundException(String message) {
        super(message);
    }
    
    public NoMsgFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public NoMsgFoundException(Throwable cause) {
        super(cause);
    }
}