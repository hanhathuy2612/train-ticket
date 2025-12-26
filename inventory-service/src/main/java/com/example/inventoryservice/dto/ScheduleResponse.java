package com.example.inventoryservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.example.inventoryservice.entity.Schedule;
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
public class ScheduleResponse {
    
    private Long id;
    private TrainInfo train;
    private LocalDate departureDate;
    private AvailabilityInfo availability;
    private String status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrainInfo {
        private Long id;
        private String trainNumber;
        private String trainName;
        private String trainType;
        private String origin;
        private String destination;
        private LocalTime departureTime;
        private LocalTime arrivalTime;
        private PriceInfo prices;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailabilityInfo {
        private Integer totalAvailable;
        private Integer economyAvailable;
        private Integer businessAvailable;
        private Integer firstClassAvailable;
        private Integer reserved;
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

    public static ScheduleResponse from(Schedule schedule) {
        ScheduleResponse.ScheduleResponseBuilder builder = ScheduleResponse.builder()
                .id(schedule.getId())
                .departureDate(schedule.getDepartureDate())
                .availability(AvailabilityInfo.builder()
                        .totalAvailable(schedule.getTotalAvailableSeats())
                        .economyAvailable(schedule.getAvailableEconomySeats())
                        .businessAvailable(schedule.getAvailableBusinessSeats())
                        .firstClassAvailable(schedule.getAvailableFirstClassSeats())
                        .reserved(schedule.getReservedSeats())
                        .build())
                .status(schedule.getStatus().name())
                .notes(schedule.getNotes())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt());

        if (schedule.getTrain() != null) {
            TrainInfo.TrainInfoBuilder trainBuilder = TrainInfo.builder()
                    .id(schedule.getTrain().getId())
                    .trainNumber(schedule.getTrain().getTrainNumber())
                    .trainName(schedule.getTrain().getTrainName())
                    .trainType(schedule.getTrain().getTrainType().name())
                    .departureTime(schedule.getTrain().getDepartureTime())
                    .arrivalTime(schedule.getTrain().getArrivalTime())
                    .prices(PriceInfo.builder()
                            .economy(schedule.getTrain().getEconomyPrice())
                            .business(schedule.getTrain().getBusinessPrice())
                            .firstClass(schedule.getTrain().getFirstClassPrice())
                            .build());

            if (schedule.getTrain().getRoute() != null) {
                trainBuilder.origin(schedule.getTrain().getRoute().getOrigin())
                        .destination(schedule.getTrain().getRoute().getDestination());
            }

            builder.train(trainBuilder.build());
        }

        return builder.build();
    }
}

