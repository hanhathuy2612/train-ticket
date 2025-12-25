package com.example.ticketservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.ticketservice.entity.Ticket;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TicketResponse {
	
	private Long id;
	private Long userId;
	private Long trainId;
	private String departureDate;
	private Integer numberOfSeats;
	private BigDecimal totalPrice;
	private String status;
	private String statusDescription;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	
	// Additional info
	private TrainInfo trainInfo;
	private PaymentInfo paymentInfo;
	private List<String> seatNumbers;

	public TicketResponse(Ticket ticket) {
		this.id = ticket.getId();
		this.userId = ticket.getUserId();
		this.trainId = ticket.getTrainId();
		this.departureDate = ticket.getDepartureDate();
		this.numberOfSeats = ticket.getNumberOfSeats();
		this.totalPrice = ticket.getTotalPrice();
		this.status = ticket.getStatus().name();
		this.statusDescription = getStatusDescription(ticket.getStatus());
		this.createdAt = ticket.getCreatedAt();
		this.updatedAt = ticket.getUpdatedAt();
	}
	
	public static TicketResponse from(Ticket ticket) {
		return new TicketResponse(ticket);
	}
	
	public static TicketResponse withTrainInfo(Ticket ticket, TrainInfo trainInfo) {
		TicketResponse response = new TicketResponse(ticket);
		response.setTrainInfo(trainInfo);
		return response;
	}
	
	private String getStatusDescription(Ticket.TicketStatus status) {
		return switch (status) {
			case PENDING -> "Waiting for payment";
			case CONFIRMED -> "Payment confirmed, ticket is valid";
			case CANCELLED -> "Ticket has been cancelled";
			case COMPLETED -> "Trip completed";
		};
	}
	
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class TrainInfo {
		private String trainNumber;
		private String origin;
		private String destination;
		private LocalDateTime departureTime;
		private LocalDateTime arrivalTime;
	}
	
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class PaymentInfo {
		private Long paymentId;
		private String paymentStatus;
		private BigDecimal amount;
		private LocalDateTime paidAt;
	}
}
