package com.example.shared.exception;

public class ConflictException extends BaseException {
    
    private static final String ERROR_CODE = "CONFLICT";
    private static final int STATUS_CODE = 409;
    
    public ConflictException(String message) {
        super(message, ERROR_CODE, STATUS_CODE);
    }
    
    public ConflictException(String resourceName, String fieldName, String fieldValue) {
        super(String.format("%s already exists with %s: %s", resourceName, fieldName, fieldValue), 
                ERROR_CODE, STATUS_CODE);
    }
}

