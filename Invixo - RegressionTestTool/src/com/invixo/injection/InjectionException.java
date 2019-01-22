package com.invixo.injection;

public class InjectionException extends Exception {
	private static final long serialVersionUID = 2683683154443921199L;

	public InjectionException(String message) {
        super(message);
    }
    
    public InjectionException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public InjectionException(Throwable cause) {
        super(cause);
    }
}