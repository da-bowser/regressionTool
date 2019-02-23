package com.invixo.common;

public class PayloadException extends Exception {
	private static final long serialVersionUID = -200840751847925674L;

	public PayloadException(String message) {
        super(message);
    }
    
    public PayloadException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public PayloadException(Throwable cause) {
        super(cause);
    }
}