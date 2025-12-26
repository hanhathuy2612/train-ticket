package com.example.shared.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingEvent {
    
    private String eventType;
    private Long ticketId;
    private Long userId;
    private Long trainId;
    private String trainNumber;
    private String departureDate;
    private String origin;
    private String destination;
    private Integer numberOfSeats;
    private BigDecimal totalPrice;
    private String status;
    private String cancellationReason;
    private LocalDateTime timestamp;
    
    public enum EventType {
        BOOKING_CREATED,
        BOOKING_CONFIRMED,
        BOOKING_CANCELLED,
        BOOKING_COMPLETED,
        BOOKING_PAYMENT_PENDING
    }
    
    public static BookingEvent created(Long ticketId, Long userId, Long trainId, 
            String trainNumber, String departureDate, Integer numberOfSeats, BigDecimal totalPrice) {
        return BookingEvent.builder()
                .eventType(EventType.BOOKING_CREATED.name())
                .ticketId(ticketId)
                .userId(userId)
                .trainId(trainId)
                .trainNumber(trainNumber)
                .departureDate(departureDate)
                .numberOfSeats(numberOfSeats)
                .totalPrice(totalPrice)
                .status("PENDING")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static BookingEvent confirmed(Long ticketId, Long userId) {
        return BookingEvent.builder()
                .eventType(EventType.BOOKING_CONFIRMED.name())
                .ticketId(ticketId)
                .userId(userId)
                .status("CONFIRMED")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static BookingEvent cancelled(Long ticketId, Long userId, String reason) {
        return BookingEvent.builder()
                .eventType(EventType.BOOKING_CANCELLED.name())
                .ticketId(ticketId)
                .userId(userId)
                .status("CANCELLED")
                .cancellationReason(reason)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

