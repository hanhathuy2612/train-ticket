package com.example.paymentservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.paymentservice.entity.Payment;
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
public class PaymentResponse {
    
    private Long id;
    private Long ticketId;
    private Long userId;
    private BigDecimal amount;
    private BigDecimal refundAmount;
    private String status;
    private String statusDescription;
    private String paymentMethod;
    private String transactionId;
    private String gatewayTransactionId;
    private String gatewayProvider;
    private String failureReason;
    private String refundReason;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PaymentResponse(Payment payment) {
        this.id = payment.getId();
        this.ticketId = payment.getTicketId();
        this.userId = payment.getUserId();
        this.amount = payment.getAmount();
        this.refundAmount = payment.getRefundAmount();
        this.status = payment.getStatus().name();
        this.statusDescription = payment.getStatus().getDescription();
        this.paymentMethod = payment.getPaymentMethod().name();
        this.transactionId = payment.getTransactionId();
        this.gatewayTransactionId = payment.getGatewayTransactionId();
        this.gatewayProvider = payment.getGatewayProvider();
        this.failureReason = payment.getFailureReason();
        this.refundReason = payment.getRefundReason();
        this.paidAt = payment.getPaidAt();
        this.refundedAt = payment.getRefundedAt();
        this.createdAt = payment.getCreatedAt();
        this.updatedAt = payment.getUpdatedAt();
    }

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment);
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    public boolean isRefunded() {
        return "REFUNDED".equals(status) || "PARTIALLY_REFUNDED".equals(status);
    }
}
