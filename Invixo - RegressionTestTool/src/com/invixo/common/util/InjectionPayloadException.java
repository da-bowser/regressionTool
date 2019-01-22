package com.invixo.common.util;

public class InjectionPayloadException extends Exception {
	private static final long serialVersionUID = -4972910327426014556L;

	public InjectionPayloadException(String message) {
        super(message);
    }
    
    public InjectionPayloadException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public InjectionPayloadException(Throwable cause) {
        super(cause);
    }
}