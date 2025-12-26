package com.example.inventoryservice.exception;

import java.time.LocalDate;

public class ScheduleNotFoundException extends RuntimeException {
    
    public ScheduleNotFoundException(Long id) {
        super("Schedule not found with id: " + id);
    }
    
    public ScheduleNotFoundException(Long trainId, LocalDate date) {
        super(String.format("Schedule not found for train %d on %s", trainId, date));
    }
}

