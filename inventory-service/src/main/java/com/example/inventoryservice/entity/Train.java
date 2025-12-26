package com.example.inventoryservice.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trains", indexes = {
    @Index(name = "idx_train_number", columnList = "trainNumber"),
    @Index(name = "idx_train_route", columnList = "route_id"),
    @Index(name = "idx_train_active", columnList = "active"),
    @Index(name = "idx_train_type", columnList = "trainType")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Train {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String trainNumber;

    @Column(nullable = false, length = 100)
    private String trainName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TrainType trainType = TrainType.EXPRESS;

    @Column(nullable = false)
    private LocalTime departureTime;

    @Column(nullable = false)
    private LocalTime arrivalTime;

    @Column(nullable = false)
    private Integer totalSeats;

    @Column(nullable = false)
    private Integer economySeats;

    @Column(nullable = false)
    private Integer businessSeats;

    @Column(nullable = false)
    @Builder.Default
    private Integer firstClassSeats = 0;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal economyPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal businessPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal firstClassPrice = BigDecimal.ZERO;

    @OneToMany(mappedBy = "train", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Seat> seats = new ArrayList<>();

    @OneToMany(mappedBy = "train", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Schedule> schedules = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(length = 500)
    private String amenities; // JSON string of amenities

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
        if (totalSeats == null) {
            totalSeats = economySeats + businessSeats + (firstClassSeats != null ? firstClassSeats : 0);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        totalSeats = economySeats + businessSeats + (firstClassSeats != null ? firstClassSeats : 0);
    }

    public enum TrainType {
        LOCAL("Local Train"),
        EXPRESS("Express Train"),
        BULLET("Bullet Train");

        private final String description;

        TrainType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Helper method
    public BigDecimal getPriceForClass(String seatClass) {
        return switch (seatClass.toUpperCase()) {
            case "ECONOMY" -> economyPrice;
            case "BUSINESS" -> businessPrice;
            case "FIRST" -> firstClassPrice;
            default -> economyPrice;
        };
    }
}
