package com.example.inventoryservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ReserveSeatRequest {
	
	@NotNull(message = "Train ID is required")
	private Long trainId;
	
	@NotNull(message = "Number of seats is required")
	@Min(value = 1, message = "At least 1 seat must be reserved")
	private Integer numberOfSeats;
	
	@NotNull(message = "Departure date is required")
	private String departureDate;

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
}

