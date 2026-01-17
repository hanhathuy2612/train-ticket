package com.example.shared.exception.error;

import org.springframework.http.HttpStatus;

public interface IErrorCode {
    String getKey();

    String getMessage();

    HttpStatus getStatus();

}
