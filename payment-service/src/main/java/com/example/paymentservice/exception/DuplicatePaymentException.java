package com.example.paymentservice.exception;

public class DuplicatePaymentException extends RuntimeException {
    
    public DuplicatePaymentException(Long ticketId) {
        super("Payment already exists for ticket: " + ticketId);
    }
}

