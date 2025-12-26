package com.example.inventoryservice.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

import com.example.inventoryservice.entity.Train.TrainType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTrainRequest {

    @NotBlank(message = "Train number is required")
    @Size(max = 20, message = "Train number must not exceed 20 characters")
    private String trainNumber;

    @NotBlank(message = "Train name is required")
    @Size(max = 100, message = "Train name must not exceed 100 characters")
    private String trainName;

    @NotNull(message = "Route ID is required")
    private Long routeId;

    private TrainType trainType;

    @NotNull(message = "Departure time is required")
    private LocalTime departureTime;

    @NotNull(message = "Arrival time is required")
    private LocalTime arrivalTime;

    @NotNull(message = "Economy seats count is required")
    @Positive(message = "Economy seats must be positive")
    private Integer economySeats;

    @NotNull(message = "Business seats count is required")
    @Positive(message = "Business seats must be positive")
    private Integer businessSeats;

    @Builder.Default
    private Integer firstClassSeats = 0;

    @NotNull(message = "Economy price is required")
    @Positive(message = "Economy price must be positive")
    private BigDecimal economyPrice;

    @NotNull(message = "Business price is required")
    @Positive(message = "Business price must be positive")
    private BigDecimal businessPrice;

    @Builder.Default
    private BigDecimal firstClassPrice = BigDecimal.ZERO;

    private String amenities;
}

