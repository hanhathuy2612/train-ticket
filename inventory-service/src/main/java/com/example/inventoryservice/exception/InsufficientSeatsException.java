package com.example.inventoryservice.exception;

public class InsufficientSeatsException extends RuntimeException {
    
    public InsufficientSeatsException(int requested, int available) {
        super(String.format("Insufficient seats. Requested: %d, Available: %d", requested, available));
    }
    
    public InsufficientSeatsException(String seatClass, int requested, int available) {
        super(String.format("Insufficient %s seats. Requested: %d, Available: %d", 
                seatClass, requested, available));
    }
}

