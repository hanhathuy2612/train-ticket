package com.example.ticketservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponse {
	
	private Long trainId;
	private Integer totalSeats;
	private Integer availableSeats;
	private Integer reservedSeats;
	private boolean available;
	
	public boolean hasEnoughSeats(int requestedSeats) {
		return available && availableSeats != null && availableSeats >= requestedSeats;
	}
}

