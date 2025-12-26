package com.example.notificationservice.dto;

import java.util.Map;

import com.example.notificationservice.entity.Notification.NotificationChannel;
import com.example.notificationservice.entity.Notification.NotificationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    private String userEmail;

    private String userPhone;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    @NotNull(message = "Channel is required")
    private NotificationChannel channel;

    private String templateCode;

    private Long referenceId;

    private String referenceType;

    private Map<String, Object> templateData;
}

