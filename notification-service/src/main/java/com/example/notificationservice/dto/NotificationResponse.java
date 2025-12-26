package com.example.notificationservice.dto;

import java.time.LocalDateTime;

import com.example.notificationservice.entity.Notification;
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
public class NotificationResponse {
    
    private Long id;
    private Long userId;
    private String type;
    private String typeDescription;
    private String title;
    private String message;
    private String channel;
    private String status;
    private String statusDescription;
    private Long referenceId;
    private String referenceType;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private Boolean isRead;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType().name())
                .typeDescription(notification.getType().getDescription())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .channel(notification.getChannel().name())
                .status(notification.getStatus().name())
                .statusDescription(notification.getStatus().getDescription())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .createdAt(notification.getCreatedAt())
                .sentAt(notification.getSentAt())
                .readAt(notification.getReadAt())
                .isRead(notification.getReadAt() != null)
                .build();
    }
}

