package com.example.ticketservice.exception;

public class TicketOperationException extends RuntimeException {
	
	public TicketOperationException(String message) {
		super(message);
	}
	
	public TicketOperationException(String message, Throwable cause) {
		super(message, cause);
	}
}

