package com.example.ticketservice.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveSeatRequest {
	
	private Long trainId;
	private Integer numberOfSeats;
	private String departureDate;
	private List<String> seatNumbers; // Optional: specific seats to reserve
}

