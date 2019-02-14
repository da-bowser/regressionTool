package com.invixo.common.util;

public class HttpException extends Exception {
	private static final long serialVersionUID = -3122525570438020347L;

	public HttpException(String message) {
        super(message);
    }
    
    public HttpException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public HttpException(Throwable cause) {
        super(cause);
    }
}