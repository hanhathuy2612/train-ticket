package com.example.inventoryservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.inventoryservice.entity.Route;
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
public class RouteResponse {
    
    private Long id;
    private String origin;
    private String destination;
    private Integer distanceKm;
    private Integer durationMinutes;
    private BigDecimal basePrice;
    private String description;
    private Boolean active;
    private Integer trainCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RouteResponse from(Route route) {
        return RouteResponse.builder()
                .id(route.getId())
                .origin(route.getOrigin())
                .destination(route.getDestination())
                .distanceKm(route.getDistanceKm())
                .durationMinutes(route.getDurationMinutes())
                .basePrice(route.getBasePrice())
                .description(route.getDescription())
                .active(route.getActive())
                .trainCount(route.getTrains() != null ? route.getTrains().size() : 0)
                .createdAt(route.getCreatedAt())
                .updatedAt(route.getUpdatedAt())
                .build();
    }

    public static RouteResponse basic(Route route) {
        return RouteResponse.builder()
                .id(route.getId())
                .origin(route.getOrigin())
                .destination(route.getDestination())
                .distanceKm(route.getDistanceKm())
                .durationMinutes(route.getDurationMinutes())
                .basePrice(route.getBasePrice())
                .build();
    }
}

