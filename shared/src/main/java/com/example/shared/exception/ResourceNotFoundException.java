package com.example.shared.exception;

public class ResourceNotFoundException extends BaseException {
    
    private static final String ERROR_CODE = "RESOURCE_NOT_FOUND";
    private static final int STATUS_CODE = 404;
    
    public ResourceNotFoundException(String resourceName, Long id) {
        super(String.format("%s not found with id: %d", resourceName, id), ERROR_CODE, STATUS_CODE);
    }
    
    public ResourceNotFoundException(String resourceName, String fieldName, String fieldValue) {
        super(String.format("%s not found with %s: %s", resourceName, fieldName, fieldValue), 
                ERROR_CODE, STATUS_CODE);
    }
    
    public ResourceNotFoundException(String message) {
        super(message, ERROR_CODE, STATUS_CODE);
    }
}

