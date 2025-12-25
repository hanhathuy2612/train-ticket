package com.example.ticketservice.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.ticketservice.dto.ApiResponse;

import feign.FeignException;

@RestControllerAdvice
public class GlobalExceptionHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(TicketNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleTicketNotFound(TicketNotFoundException ex) {
		logger.warn("Ticket not found: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error(ex.getMessage(), HttpStatus.NOT_FOUND.value()));
	}

	@ExceptionHandler(InsufficientSeatsException.class)
	public ResponseEntity<ApiResponse<Void>> handleInsufficientSeats(InsufficientSeatsException ex) {
		logger.warn("Insufficient seats: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
	}

	@ExceptionHandler(TicketOperationException.class)
	public ResponseEntity<ApiResponse<Void>> handleTicketOperation(TicketOperationException ex) {
		logger.error("Ticket operation error: {}", ex.getMessage(), ex);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
	}

	@ExceptionHandler(UnauthorizedAccessException.class)
	public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedAccessException ex) {
		logger.warn("Unauthorized access: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ApiResponse.error(ex.getMessage(), HttpStatus.FORBIDDEN.value()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getAllErrors().forEach(error -> {
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
		});
		logger.warn("Validation errors: {}", errors);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error("Validation failed", HttpStatus.BAD_REQUEST.value(), errors));
	}

	@ExceptionHandler(FeignException.class)
	public ResponseEntity<ApiResponse<Void>> handleFeignException(FeignException ex) {
		logger.error("Feign client error: {}", ex.getMessage());
		String message = "Service communication error";
		if (ex.status() == 404) {
			message = "Requested resource not found in external service";
		} else if (ex.status() == 503) {
			message = "External service is temporarily unavailable";
		}
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(ApiResponse.error(message, HttpStatus.SERVICE_UNAVAILABLE.value()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
		logger.error("Unexpected error: {}", ex.getMessage(), ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
	}
}

