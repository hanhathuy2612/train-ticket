package com.example.paymentservice.dto;

import com.example.paymentservice.entity.Payment;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentResponse {
	private Long id;
	private Long ticketId;
	private Long userId;
	private BigDecimal amount;
	private Payment.PaymentStatus status;
	private String paymentMethod;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public PaymentResponse(Payment payment) {
		this.id = payment.getId();
		this.ticketId = payment.getTicketId();
		this.userId = payment.getUserId();
		this.amount = payment.getAmount();
		this.status = payment.getStatus();
		this.paymentMethod = payment.getPaymentMethod();
		this.createdAt = payment.getCreatedAt();
		this.updatedAt = payment.getUpdatedAt();
	}

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getTicketId() {
		return ticketId;
	}

	public void setTicketId(Long ticketId) {
		this.ticketId = ticketId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public Payment.PaymentStatus getStatus() {
		return status;
	}

	public void setStatus(Payment.PaymentStatus status) {
		this.status = status;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}

