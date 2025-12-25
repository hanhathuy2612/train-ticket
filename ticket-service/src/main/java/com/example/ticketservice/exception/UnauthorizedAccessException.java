package com.example.ticketservice.exception;

public class UnauthorizedAccessException extends RuntimeException {
	
	public UnauthorizedAccessException(String message) {
		super(message);
	}
	
	public UnauthorizedAccessException(Long userId, Long ticketId) {
		super(String.format("User %d is not authorized to access ticket %d", userId, ticketId));
	}
}

