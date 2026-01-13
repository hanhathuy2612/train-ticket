package com.example.apigateway.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fallback controller for circuit breaker
 * Returns default responses when services are unavailable
 */
@RestController
public class FallbackController {

    @RequestMapping("/fallback/user-service")
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        return createFallbackResponse("User service is temporarily unavailable");
    }

    @RequestMapping("/fallback/ticket-service")
    public ResponseEntity<Map<String, Object>> ticketServiceFallback() {
        return createFallbackResponse("Ticket service is temporarily unavailable");
    }

    @RequestMapping("/fallback/inventory-service")
    public ResponseEntity<Map<String, Object>> inventoryServiceFallback() {
        return createFallbackResponse("Inventory service is temporarily unavailable");
    }

    @RequestMapping("/fallback/payment-service")
    public ResponseEntity<Map<String, Object>> paymentServiceFallback() {
        return createFallbackResponse("Payment service is temporarily unavailable");
    }

    @RequestMapping("/fallback/notification-service")
    public ResponseEntity<Map<String, Object>> notificationServiceFallback() {
        return createFallbackResponse("Notification service is temporarily unavailable");
    }

    private ResponseEntity<Map<String, Object>> createFallbackResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("statusCode", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("errorCode", "SERVICE_UNAVAILABLE");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
