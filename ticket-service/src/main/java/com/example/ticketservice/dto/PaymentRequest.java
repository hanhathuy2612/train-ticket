package com.example.ticketservice.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
	
	private Long ticketId;
	private BigDecimal amount;
	private String paymentMethod; // CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, E_WALLET
}

