package com.example.inventoryservice.exception;

import org.springframework.http.HttpStatus;

import com.example.shared.exception.error.IErrorCode;

import lombok.Getter;

@Getter
public enum InventoryErrorCode implements IErrorCode {
    TRAIN_NOT_FOUND("train.not_found", "Train not found", HttpStatus.NOT_FOUND),
    INSUFFICIENT_SEATS("insufficient.seats", "Insufficient seats", HttpStatus.BAD_REQUEST),
    ROUTE_NOT_FOUND("route.not_found", "Route not found", HttpStatus.NOT_FOUND),
    SCHEDULE_NOT_FOUND("schedule.not_found", "Schedule not found", HttpStatus.NOT_FOUND),
    SCHEDULE_NOT_AVAILABLE("schedule.not_available", "Schedule not available", HttpStatus.BAD_REQUEST);

    private final String key;
    private final String message;
    private final HttpStatus status;

    InventoryErrorCode(String key, String message, HttpStatus status) {
        this.key = key;
        this.message = message;
        this.status = status;
    }
}
