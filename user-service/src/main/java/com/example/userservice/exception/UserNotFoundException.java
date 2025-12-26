package com.example.userservice.exception;

public class UserNotFoundException extends RuntimeException {
    
    public UserNotFoundException(Long id) {
        super("User not found with id: " + id);
    }
    
    public UserNotFoundException(String field, String value) {
        super(String.format("User not found with %s: %s", field, value));
    }
}

