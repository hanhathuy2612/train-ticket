package com.example.shared.exception;

public class UnauthorizedException extends BaseException {
    
    private static final String ERROR_CODE = "UNAUTHORIZED";
    private static final int STATUS_CODE = 401;
    
    public UnauthorizedException(String message) {
        super(message, ERROR_CODE, STATUS_CODE);
    }
    
    public UnauthorizedException() {
        super("Unauthorized access", ERROR_CODE, STATUS_CODE);
    }
}

