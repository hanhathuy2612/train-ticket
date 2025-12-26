package com.example.inventoryservice.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(name = "inventory", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"trainId", "departureDate"}),
    indexes = {
        @Index(name = "idx_inventory_train", columnList = "trainId"),
        @Index(name = "idx_inventory_date", columnList = "departureDate")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long trainId;

    @Column(nullable = false)
    private LocalDate departureDate;

    @Column(nullable = false)
    private Integer totalSeats;

    @Column(nullable = false)
    private Integer availableSeats;

    @Column(nullable = false)
    @Builder.Default
    private Integer reservedSeats = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer economyAvailable = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer businessAvailable = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer firstClassAvailable = 0;

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

    // Helper methods
    public boolean hasAvailableSeats(int count) {
        return availableSeats >= count;
    }

    public boolean reserveSeats(int count) {
        if (availableSeats >= count) {
            availableSeats -= count;
            reservedSeats += count;
            return true;
        }
        return false;
    }

    public void releaseSeats(int count) {
        availableSeats += count;
        reservedSeats = Math.max(0, reservedSeats - count);
    }
}
