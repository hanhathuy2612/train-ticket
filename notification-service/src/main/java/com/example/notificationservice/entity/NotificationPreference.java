package com.example.notificationservice.entity;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_preferences", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"userId"}),
    indexes = {
        @Index(name = "idx_pref_user", columnList = "userId")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    // Email preferences
    @Column(nullable = false)
    @Builder.Default
    private Boolean emailEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean emailBookingConfirmation = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean emailPaymentNotification = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean emailTripReminder = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean emailPromotional = false;

    // SMS preferences
    @Column(nullable = false)
    @Builder.Default
    private Boolean smsEnabled = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean smsBookingConfirmation = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean smsTripReminder = true;

    // Push notification preferences
    @Column(nullable = false)
    @Builder.Default
    private Boolean pushEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean pushBookingUpdate = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean pushScheduleChange = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean pushPromotional = false;

    // Quiet hours
    private Integer quietHoursStart; // 0-23

    private Integer quietHoursEnd; // 0-23

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

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
}

