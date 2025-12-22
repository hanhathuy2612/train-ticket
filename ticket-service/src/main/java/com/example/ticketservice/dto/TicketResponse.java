package com.example.ticketservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.ticketservice.entity.Ticket;

public class TicketResponse {
	private Long id;
	private Long userId;
	private Long trainId;
	private String departureDate;
	private Integer numberOfSeats;
	private BigDecimal totalPrice;
	private Ticket.TicketStatus status;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public TicketResponse(Ticket ticket) {
		this.id = ticket.getId();
		this.userId = ticket.getUserId();
		this.trainId = ticket.getTrainId();
		this.departureDate = ticket.getDepartureDate();
		this.numberOfSeats = ticket.getNumberOfSeats();
		this.totalPrice = ticket.getTotalPrice();
		this.status = ticket.getStatus();
		this.createdAt = ticket.getCreatedAt();
		this.updatedAt = ticket.getUpdatedAt();
	}

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Long getTrainId() {
		return trainId;
	}

	public void setTrainId(Long trainId) {
		this.trainId = trainId;
	}

	public String getDepartureDate() {
		return departureDate;
	}

	public void setDepartureDate(String departureDate) {
		this.departureDate = departureDate;
	}

	public Integer getNumberOfSeats() {
		return numberOfSeats;
	}

	public void setNumberOfSeats(Integer numberOfSeats) {
		this.numberOfSeats = numberOfSeats;
	}

	public BigDecimal getTotalPrice() {
		return totalPrice;
	}

	public void setTotalPrice(BigDecimal totalPrice) {
		this.totalPrice = totalPrice;
	}

	public Ticket.TicketStatus getStatus() {
		return status;
	}

	public void setStatus(Ticket.TicketStatus status) {
		this.status = status;
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
