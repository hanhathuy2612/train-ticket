package com.example.inventoryservice.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "schedules", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"train_id", "departureDate"}),
    indexes = {
        @Index(name = "idx_schedule_train", columnList = "train_id"),
        @Index(name = "idx_schedule_date", columnList = "departureDate"),
        @Index(name = "idx_schedule_status", columnList = "status")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @Column(nullable = false)
    private LocalDate departureDate;

    @Column(nullable = false)
    private Integer availableEconomySeats;

    @Column(nullable = false)
    private Integer availableBusinessSeats;

    @Column(nullable = false)
    @Builder.Default
    private Integer availableFirstClassSeats = 0;

    @Column(nullable = false)
    private Integer reservedSeats;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ScheduleStatus status = ScheduleStatus.SCHEDULED;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ScheduleStatus {
        SCHEDULED("Scheduled"),
        BOARDING("Boarding"),
        DEPARTED("Departed"),
        ARRIVED("Arrived"),
        CANCELLED("Cancelled"),
        DELAYED("Delayed");

        private final String description;

        ScheduleStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Helper methods
    public int getTotalAvailableSeats() {
        return availableEconomySeats + availableBusinessSeats + 
               (availableFirstClassSeats != null ? availableFirstClassSeats : 0);
    }

    public boolean hasAvailableSeats(int count, String seatClass) {
        return switch (seatClass.toUpperCase()) {
            case "ECONOMY" -> availableEconomySeats >= count;
            case "BUSINESS" -> availableBusinessSeats >= count;
            case "FIRST" -> availableFirstClassSeats >= count;
            default -> getTotalAvailableSeats() >= count;
        };
    }

    public boolean canBeBooked() {
        return status == ScheduleStatus.SCHEDULED && getTotalAvailableSeats() > 0;
    }
}

