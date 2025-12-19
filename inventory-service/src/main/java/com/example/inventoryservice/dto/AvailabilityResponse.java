package com.example.inventoryservice.dto;

public class AvailabilityResponse {
	private Long trainId;
	private Integer totalSeats;
	private Integer availableSeats;
	private Integer reservedSeats;
	private Boolean available;

	public AvailabilityResponse(Long trainId, Integer totalSeats, Integer availableSeats, Integer reservedSeats) {
		this.trainId = trainId;
		this.totalSeats = totalSeats;
		this.availableSeats = availableSeats;
		this.reservedSeats = reservedSeats;
		this.available = availableSeats > 0;
	}

	public Long getTrainId() {
		return trainId;
	}

	public void setTrainId(Long trainId) {
		this.trainId = trainId;
	}

	public Integer getTotalSeats() {
		return totalSeats;
	}

	public void setTotalSeats(Integer totalSeats) {
		this.totalSeats = totalSeats;
	}

	public Integer getAvailableSeats() {
		return availableSeats;
	}

	public void setAvailableSeats(Integer availableSeats) {
		this.availableSeats = availableSeats;
	}

	public Integer getReservedSeats() {
		return reservedSeats;
	}

	public void setReservedSeats(Integer reservedSeats) {
		this.reservedSeats = reservedSeats;
	}

	public Boolean getAvailable() {
		return available;
	}

	public void setAvailable(Boolean available) {
		this.available = available;
	}
}

