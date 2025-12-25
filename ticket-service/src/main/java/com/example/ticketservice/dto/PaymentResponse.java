package com.example.ticketservice.dto;

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
public class PaymentResponse {
	
	private Long id;
	private Long ticketId;
	private Long userId;
	private BigDecimal amount;
	private String status; // PENDING, COMPLETED, FAILED, REFUNDED
	private String paymentMethod;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	
	public boolean isCompleted() {
		return "COMPLETED".equals(status);
	}
	
	public boolean isRefunded() {
		return "REFUNDED".equals(status);
	}
}

