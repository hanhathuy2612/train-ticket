package com.example.inventoryservice.exception;

public class RouteNotFoundException extends RuntimeException {
    
    public RouteNotFoundException(Long id) {
        super("Route not found with id: " + id);
    }
    
    public RouteNotFoundException(String origin, String destination) {
        super(String.format("Route not found from %s to %s", origin, destination));
    }
}

