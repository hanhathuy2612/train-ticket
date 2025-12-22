package com.example.ticketservice.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
}
