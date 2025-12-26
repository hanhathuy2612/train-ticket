package com.example.paymentservice.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {

    @Positive(message = "Refund amount must be positive")
    private BigDecimal amount; // Optional: partial refund amount

    @NotBlank(message = "Refund reason is required")
    private String reason;

    @Builder.Default
    private Boolean fullRefund = true;
}

