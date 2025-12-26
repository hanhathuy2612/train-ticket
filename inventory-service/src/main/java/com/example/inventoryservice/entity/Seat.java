package com.example.inventoryservice.entity;

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
@Table(name = "seats", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"train_id", "seatNumber"}),
    indexes = {
        @Index(name = "idx_seat_train", columnList = "train_id"),
        @Index(name = "idx_seat_class", columnList = "seatClass"),
        @Index(name = "idx_seat_available", columnList = "available")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @Column(nullable = false, length = 10)
    private String seatNumber; // e.g., "1A", "2B"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatClass seatClass;

    @Column(nullable = false)
    @Builder.Default
    private Boolean available = true;

    @Column(length = 50)
    private String position; // WINDOW, AISLE, MIDDLE

    @Column(nullable = false)
    private Integer carNumber;

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

    public enum SeatClass {
        ECONOMY("Economy Class"),
        BUSINESS("Business Class"),
        FIRST("First Class");

        private final String description;

        SeatClass(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
