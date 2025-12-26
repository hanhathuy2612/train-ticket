package com.example.paymentservice.dto;

import java.math.BigDecimal;

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
public class PaymentStatsResponse {
    
    private Long totalPayments;
    private Long completedPayments;
    private Long failedPayments;
    private Long refundedPayments;
    private Long pendingPayments;
    private BigDecimal totalAmount;
    private BigDecimal totalRefunded;
    private BigDecimal netAmount;
}

