package com.example.inventoryservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.example.inventoryservice.entity.Train;
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
public class TrainResponse {
    
    private Long id;
    private String trainNumber;
    private String trainName;
    private String trainType;
    private RouteInfo route;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private Integer totalSeats;
    private SeatInfo seatInfo;
    private PriceInfo priceInfo;
    private String amenities;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteInfo {
        private Long id;
        private String origin;
        private String destination;
        private Integer distanceKm;
        private Integer durationMinutes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatInfo {
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

    public static TrainResponse from(Train train) {
        TrainResponse.TrainResponseBuilder builder = TrainResponse.builder()
                .id(train.getId())
                .trainNumber(train.getTrainNumber())
                .trainName(train.getTrainName())
                .trainType(train.getTrainType().name())
                .departureTime(train.getDepartureTime())
                .arrivalTime(train.getArrivalTime())
                .totalSeats(train.getTotalSeats())
                .seatInfo(SeatInfo.builder()
                        .economy(train.getEconomySeats())
                        .business(train.getBusinessSeats())
                        .firstClass(train.getFirstClassSeats())
                        .build())
                .priceInfo(PriceInfo.builder()
                        .economy(train.getEconomyPrice())
                        .business(train.getBusinessPrice())
                        .firstClass(train.getFirstClassPrice())
                        .build())
                .amenities(train.getAmenities())
                .active(train.getActive())
                .createdAt(train.getCreatedAt())
                .updatedAt(train.getUpdatedAt());

        if (train.getRoute() != null) {
            builder.route(RouteInfo.builder()
                    .id(train.getRoute().getId())
                    .origin(train.getRoute().getOrigin())
                    .destination(train.getRoute().getDestination())
                    .distanceKm(train.getRoute().getDistanceKm())
                    .durationMinutes(train.getRoute().getDurationMinutes())
                    .build());
        }

        return builder.build();
    }

    public static TrainResponse basic(Train train) {
        return TrainResponse.builder()
                .id(train.getId())
                .trainNumber(train.getTrainNumber())
                .trainName(train.getTrainName())
                .trainType(train.getTrainType().name())
                .departureTime(train.getDepartureTime())
                .arrivalTime(train.getArrivalTime())
                .build();
    }
}

