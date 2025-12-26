package com.example.shared.event;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    
    private String eventType;
    private Long userId;
    private String userEmail;
    private String userPhone;
    private String channel; // EMAIL, SMS, PUSH
    private String templateCode;
    private String subject;
    private String message;
    private Map<String, Object> templateData;
    private LocalDateTime timestamp;
    
    public enum Channel {
        EMAIL, SMS, PUSH, ALL
    }
    
    public enum TemplateCode {
        BOOKING_CONFIRMATION,
        BOOKING_CANCELLATION,
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        PAYMENT_REFUND,
        WELCOME,
        PASSWORD_RESET,
        TRIP_REMINDER
    }
    
    public static NotificationEvent email(Long userId, String email, String templateCode, 
            String subject, Map<String, Object> data) {
        return NotificationEvent.builder()
                .eventType("SEND_NOTIFICATION")
                .userId(userId)
                .userEmail(email)
                .channel(Channel.EMAIL.name())
                .templateCode(templateCode)
                .subject(subject)
                .templateData(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static NotificationEvent sms(Long userId, String phone, String templateCode, 
            String message) {
        return NotificationEvent.builder()
                .eventType("SEND_NOTIFICATION")
                .userId(userId)
                .userPhone(phone)
                .channel(Channel.SMS.name())
                .templateCode(templateCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

