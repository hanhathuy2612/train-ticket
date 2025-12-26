package com.example.inventoryservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AvailabilityResponse {
    
    private Long trainId;
    private String trainNumber;
    private String trainName;
    private String origin;
    private String destination;
    private LocalDate departureDate;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private Integer totalSeats;
    private Integer availableSeats;
    private Integer reservedSeats;
    private SeatAvailability seatAvailability;
    private PriceInfo prices;
    private String status;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatAvailability {
        private Integer economy;
        private Integer business;
        private Integer firstClass;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceInfo {
        private BigDecimal economy;
        private BigDecimal business;
        private BigDecimal firstClass;
    }

    // Simple constructor for backward compatibility
    public AvailabilityResponse(Long trainId, Integer totalSeats, Integer availableSeats, Integer reservedSeats) {
        this.trainId = trainId;
        this.totalSeats = totalSeats;
        this.availableSeats = availableSeats;
        this.reservedSeats = reservedSeats;
    }

    public boolean hasEnoughSeats(int count) {
        return availableSeats != null && availableSeats >= count;
    }

    public boolean hasEnoughSeats(int count, String seatClass) {
        if (seatAvailability == null) {
            return hasEnoughSeats(count);
        }
        return switch (seatClass.toUpperCase()) {
            case "ECONOMY" -> seatAvailability.getEconomy() >= count;
            case "BUSINESS" -> seatAvailability.getBusiness() >= count;
            case "FIRST" -> seatAvailability.getFirstClass() >= count;
            default -> hasEnoughSeats(count);
        };
    }
}
