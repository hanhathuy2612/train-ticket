package com.example.paymentservice.dto;

import java.math.BigDecimal;

import com.example.paymentservice.entity.Payment.PaymentMethod;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentRequest {

    @NotNull(message = "Ticket ID is required")
    private Long ticketId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    // Card payment details (optional)
    private String cardNumber;
    private String cardHolderName;
    private String expiryDate;
    private String cvv;

    // E-wallet details (optional)
    private String walletType;
    private String walletAccountId;

    // Bank transfer details (optional)
    private String bankCode;
    private String accountNumber;

    // Callback URL for async payment processing
    private String callbackUrl;
    private String returnUrl;
}
