package com.example.shared.exception;

import com.example.shared.exception.error.IErrorCode;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final IErrorCode errorCode;
    private final Object[] args;

    public BusinessException(IErrorCode errorCode, Object... args) {
        this.errorCode = errorCode;
        this.args = args;
    }
}
