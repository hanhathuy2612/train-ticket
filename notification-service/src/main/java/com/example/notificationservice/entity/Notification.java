package com.example.notificationservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_user", columnList = "userId"),
    @Index(name = "idx_notification_type", columnList = "type"),
    @Index(name = "idx_notification_status", columnList = "status"),
    @Index(name = "idx_notification_created", columnList = "createdAt"),
    @Index(name = "idx_notification_read", columnList = "readAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(length = 100)
    private String userEmail;

    @Column(length = 20)
    private String userPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(length = 50)
    private String templateCode;

    private Long referenceId; // ticketId, paymentId, etc.

    @Column(length = 50)
    private String referenceType; // TICKET, PAYMENT, etc.

    @Column(length = 500)
    private String errorMessage;

    private Integer retryCount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime sentAt;

    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    public enum NotificationType {
        BOOKING_CONFIRMATION("Booking Confirmation"),
        BOOKING_CANCELLATION("Booking Cancellation"),
        PAYMENT_SUCCESS("Payment Success"),
        PAYMENT_FAILED("Payment Failed"),
        PAYMENT_REFUND("Payment Refund"),
        TRIP_REMINDER("Trip Reminder"),
        SCHEDULE_CHANGE("Schedule Change"),
        PROMOTIONAL("Promotional"),
        WELCOME("Welcome"),
        PASSWORD_RESET("Password Reset"),
        ACCOUNT_UPDATE("Account Update"),
        SYSTEM("System Notification");

        private final String description;

        NotificationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum NotificationChannel {
        EMAIL("Email"),
        SMS("SMS"),
        PUSH("Push Notification"),
        IN_APP("In-App Notification");

        private final String description;

        NotificationChannel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum NotificationStatus {
        PENDING("Pending"),
        SENDING("Sending"),
        SENT("Sent"),
        DELIVERED("Delivered"),
        READ("Read"),
        FAILED("Failed");

        private final String description;

        NotificationStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Helper methods
    public boolean canRetry() {
        return status == NotificationStatus.FAILED && retryCount < 3;
    }

    public void incrementRetry() {
        this.retryCount++;
    }
}
