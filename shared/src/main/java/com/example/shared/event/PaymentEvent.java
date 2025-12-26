package com.example.shared.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    
    private String eventType;
    private Long paymentId;
    private Long ticketId;
    private Long userId;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private String transactionId;
    private String failureReason;
    private LocalDateTime timestamp;
    
    public enum EventType {
        PAYMENT_INITIATED,
        PAYMENT_COMPLETED,
        PAYMENT_FAILED,
        PAYMENT_REFUNDED
    }
    
    public static PaymentEvent completed(Long paymentId, Long ticketId, Long userId, 
            BigDecimal amount, String paymentMethod, String transactionId) {
        return PaymentEvent.builder()
                .eventType(EventType.PAYMENT_COMPLETED.name())
                .paymentId(paymentId)
                .ticketId(ticketId)
                .userId(userId)
                .amount(amount)
                .paymentMethod(paymentMethod)
                .transactionId(transactionId)
                .status("COMPLETED")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static PaymentEvent failed(Long paymentId, Long ticketId, Long userId, 
            BigDecimal amount, String reason) {
        return PaymentEvent.builder()
                .eventType(EventType.PAYMENT_FAILED.name())
                .paymentId(paymentId)
                .ticketId(ticketId)
                .userId(userId)
                .amount(amount)
                .status("FAILED")
                .failureReason(reason)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static PaymentEvent refunded(Long paymentId, Long ticketId, Long userId, BigDecimal amount) {
        return PaymentEvent.builder()
                .eventType(EventType.PAYMENT_REFUNDED.name())
                .paymentId(paymentId)
                .ticketId(ticketId)
                .userId(userId)
                .amount(amount)
                .status("REFUNDED")
                .timestamp(LocalDateTime.now())
                .build();
    }
}

