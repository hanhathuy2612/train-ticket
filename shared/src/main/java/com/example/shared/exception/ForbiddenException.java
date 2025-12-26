package com.example.shared.exception;

public class ForbiddenException extends BaseException {
    
    private static final String ERROR_CODE = "FORBIDDEN";
    private static final int STATUS_CODE = 403;
    
    public ForbiddenException(String message) {
        super(message, ERROR_CODE, STATUS_CODE);
    }
    
    public ForbiddenException() {
        super("Access denied", ERROR_CODE, STATUS_CODE);
    }
}

