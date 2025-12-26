package com.example.shared.exception;

public class ServiceUnavailableException extends BaseException {
    
    private static final String ERROR_CODE = "SERVICE_UNAVAILABLE";
    private static final int STATUS_CODE = 503;
    
    public ServiceUnavailableException(String serviceName) {
        super(String.format("Service %s is currently unavailable", serviceName), ERROR_CODE, STATUS_CODE);
    }
    
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, ERROR_CODE, STATUS_CODE, cause);
    }
}

