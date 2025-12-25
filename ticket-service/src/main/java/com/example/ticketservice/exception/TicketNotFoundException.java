package com.example.ticketservice.exception;

public class TicketNotFoundException extends RuntimeException {
	
	public TicketNotFoundException(Long ticketId) {
		super("Ticket not found with id: " + ticketId);
	}
	
	public TicketNotFoundException(String message) {
		super(message);
	}
}

