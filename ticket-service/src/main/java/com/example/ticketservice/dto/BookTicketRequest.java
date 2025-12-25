package com.example.ticketservice.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookTicketRequest {

	@NotNull(message = "Train ID is required")
	private Long trainId;

	@NotNull(message = "Number of seats is required")
	@Min(value = 1, message = "At least 1 seat must be booked")
	private Integer numberOfSeats;

	@NotBlank(message = "Departure date is required")
	@Pattern(regexp = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}", 
			message = "Departure date must be in format yyyy-MM-dd HH:mm")
	private String departureDate;

	@NotNull(message = "Total price is required")
	@Positive(message = "Total price must be positive")
	private BigDecimal totalPrice;
	
	// Optional: specific seat numbers if user wants to choose seats
	private List<String> seatNumbers;
	
	// Optional: passenger information
	private List<PassengerInfo> passengers;
	
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class PassengerInfo {
		@NotBlank(message = "Passenger name is required")
		private String name;
		
		private String idNumber;
		
		private String phoneNumber;
	}
}
