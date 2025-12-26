package com.example.paymentservice.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCallbackRequest {

    private String transactionId;
    private String gatewayTransactionId;
    private String status; // SUCCESS, FAILED, CANCELLED
    private String responseCode;
    private String responseMessage;
    private String checksum;
    private Map<String, String> additionalData;
}

