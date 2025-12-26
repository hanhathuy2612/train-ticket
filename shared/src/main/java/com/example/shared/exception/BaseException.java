package com.example.shared.exception;

import lombok.Getter;

@Getter
public abstract class BaseException extends RuntimeException {
    
    private final String errorCode;
    private final int statusCode;
    
    protected BaseException(String message, String errorCode, int statusCode) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }
    
    protected BaseException(String message, String errorCode, int statusCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }
}

