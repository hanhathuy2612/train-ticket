package com.example.inventoryservice.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveSeatRequest {

    @NotNull(message = "Train ID is required")
    private Long trainId;

    @NotNull(message = "Number of seats is required")
    @Positive(message = "Number of seats must be positive")
    private Integer numberOfSeats;

    @NotNull(message = "Departure date is required")
    private String departureDate;

    private String seatClass; // ECONOMY, BUSINESS, FIRST

    private List<String> seatNumbers; // Optional: specific seat numbers
}
