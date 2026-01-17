package com.example.shared.exception.error;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode implements IErrorCode {
    INVALID_REQUEST("error.invalid_request", "Invalid request", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("error.unauthorized", "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("error.forbidden", "Forbidden", HttpStatus.FORBIDDEN),
    NOT_FOUND("error.not_found", "Not found", HttpStatus.NOT_FOUND),
    INTERNAL_SERVER_ERROR("error.internal_server_error", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    BAD_GATEWAY("error.bad_gateway", "Bad gateway", HttpStatus.BAD_GATEWAY),
    SERVICE_UNAVAILABLE("error.service_unavailable", "Service unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    GATEWAY_TIMEOUT("error.gateway_timeout", "Gateway timeout", HttpStatus.GATEWAY_TIMEOUT),
    PRECONDITION_FAILED("error.precondition_failed", "Precondition failed", HttpStatus.PRECONDITION_FAILED),
    UNSUPPORTED_MEDIA_TYPE("error.unsupported_media_type", "Unsupported media type", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    UNPROCESSABLE_ENTITY("error.unprocessable_entity", "Unprocessable entity", HttpStatus.UNPROCESSABLE_ENTITY),
    TOO_MANY_REQUESTS("error.too_many_requests", "Too many requests", HttpStatus.TOO_MANY_REQUESTS),
    METHOD_NOT_ALLOWED("error.method_not_allowed", "Method not allowed", HttpStatus.METHOD_NOT_ALLOWED);

    private final String key;
    private final String message;
    private final HttpStatus status;

    ErrorCode(String code, String message, HttpStatus status) {
        this.key = code;
        this.message = message;
        this.status = status;
    }
}
