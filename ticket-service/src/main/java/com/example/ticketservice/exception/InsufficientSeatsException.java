package com.example.ticketservice.exception;

public class InsufficientSeatsException extends RuntimeException {
	
	public InsufficientSeatsException(Long trainId, int requested, int available) {
		super(String.format("Not enough seats available for train %d. Requested: %d, Available: %d", 
				trainId, requested, available));
	}
	
	public InsufficientSeatsException(String message) {
		super(message);
	}
}

