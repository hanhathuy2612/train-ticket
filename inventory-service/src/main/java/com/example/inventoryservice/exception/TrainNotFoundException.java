package com.example.inventoryservice.exception;

public class TrainNotFoundException extends RuntimeException {
    
    public TrainNotFoundException(Long id) {
        super("Train not found with id: " + id);
    }
    
    public TrainNotFoundException(String trainNumber) {
        super("Train not found with number: " + trainNumber);
    }
}

