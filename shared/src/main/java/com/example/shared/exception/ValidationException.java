package com.example.shared.exception;

import java.util.Map;

import lombok.Getter;

@Getter
public class ValidationException extends BaseException {
    
    private static final String ERROR_CODE = "VALIDATION_ERROR";
    private static final int STATUS_CODE = 400;
    
    private final Map<String, String> fieldErrors;
    
    public ValidationException(String message) {
        super(message, ERROR_CODE, STATUS_CODE);
        this.fieldErrors = null;
    }
    
    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message, ERROR_CODE, STATUS_CODE);
        this.fieldErrors = fieldErrors;
    }
    
    public ValidationException(Map<String, String> fieldErrors) {
        super("Validation failed", ERROR_CODE, STATUS_CODE);
        this.fieldErrors = fieldErrors;
    }
}

