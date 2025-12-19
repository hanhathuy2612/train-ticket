package com.example.ticketservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class BookTicketRequest {
	
	@NotNull(message = "Train ID is required")
	private Long trainId;
	
	@NotNull(message = "Number of seats is required")
	@Min(value = 1, message = "At least 1 seat must be booked")
	private Integer numberOfSeats;
	
	@NotNull(message = "Departure date is required")
	private String departureDate;
	
	@NotNull(message = "Total price is required")
	private BigDecimal totalPrice;

	public Long getTrainId() {
		return trainId;
	}

	public void setTrainId(Long trainId) {
		this.trainId = trainId;
	}

	public Integer getNumberOfSeats() {
		return numberOfSeats;
	}

	public void setNumberOfSeats(Integer numberOfSeats) {
		this.numberOfSeats = numberOfSeats;
	}

	public String getDepartureDate() {
		return departureDate;
	}

	public void setDepartureDate(String departureDate) {
		this.departureDate = departureDate;
	}

	public BigDecimal getTotalPrice() {
		return totalPrice;
	}

	public void setTotalPrice(BigDecimal totalPrice) {
		this.totalPrice = totalPrice;
	}
}

