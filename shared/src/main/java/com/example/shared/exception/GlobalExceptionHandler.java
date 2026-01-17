package com.example.shared.exception;

import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

import com.example.shared.exception.error.ErrorCode;
import com.example.shared.exception.error.ErrorResponse;
import com.example.shared.exception.error.IErrorCode;

public abstract class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        IErrorCode errorCode = ex.getErrorCode();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .key(errorCode.getKey())
                .message(errorCode.getMessage())
                .args(ex.getArgs())
                .build();
        return ResponseEntity
                .status(errorCode.getStatus().value())
                .body(errorResponse);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException ex) {
        return handleBusinessException(new BusinessException(ErrorCode.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return handleBusinessException(new BusinessException(ErrorCode.INVALID_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        return handleBusinessException(new BusinessException(ErrorCode.PRECONDITION_FAILED, ex.getMessage()));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedOperationException(UnsupportedOperationException ex) {
        return handleBusinessException(new BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE, ex.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException ex) {
        return handleBusinessException(new BusinessException(ErrorCode.UNAUTHORIZED, ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        return handleBusinessException(new BusinessException(ErrorCode.FORBIDDEN, ex.getMessage()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex) {
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("requestedMethod", ex.getMethod());
        errorDetails.put("supportedMethods", ex.getSupportedMethods() != null
                ? java.util.Arrays.asList(ex.getSupportedMethods())
                : java.util.Collections.emptyList());
        errorDetails.put("message", ex.getMessage());

        return handleBusinessException(
                new BusinessException(ErrorCode.METHOD_NOT_ALLOWED, errorDetails));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {
        Map<String, Object> validationErrors = new HashMap<>();

        // Collect all field errors
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() != null
                                ? error.getDefaultMessage()
                                : "Invalid value",
                        (existing, replacement) -> existing // Keep first error if duplicate
                ));

        validationErrors.put("fieldErrors", fieldErrors);
        validationErrors.put("errorCount", ex.getBindingResult().getErrorCount());

        return handleBusinessException(new BusinessException(ErrorCode.INVALID_REQUEST, validationErrors));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex) {
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("parameterName", ex.getParameterName());
        errorDetails.put("parameterType", ex.getParameterType());
        errorDetails.put("message", "Missing required parameter: " + ex.getParameterName());

        return handleBusinessException(
                new BusinessException(ErrorCode.INVALID_REQUEST, errorDetails));
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ErrorResponse> handleMissingPathVariableException(
            MissingPathVariableException ex) {
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("variableName", ex.getVariableName());
        errorDetails.put("message", "Missing required path variable: " + ex.getVariableName());

        return handleBusinessException(
                new BusinessException(ErrorCode.INVALID_REQUEST, errorDetails));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestPartException(
            MissingServletRequestPartException ex) {
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("requestPartName", ex.getRequestPartName());
        errorDetails.put("message", "Missing required request part: " + ex.getRequestPartName());

        return handleBusinessException(
                new BusinessException(ErrorCode.INVALID_REQUEST, errorDetails));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return handleBusinessException(new BusinessException(ErrorCode.INVALID_REQUEST, "Malformed JSON request"));
    }

    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotWritableException(HttpMessageNotWritableException ex) {
        return handleBusinessException(
                new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to write HTTP message"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex) {
        return handleBusinessException(new BusinessException(ErrorCode.BAD_GATEWAY, ex.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        return handleBusinessException(
                new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                        ex.getReason() != null ? ex.getReason() : ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return handleBusinessException(
                new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred",
                        ex.getMessage()));
    }

}
