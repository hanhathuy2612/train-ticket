package com.example.ticketservice.dto;

import java.time.LocalDate;

import com.example.ticketservice.entity.Ticket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketSearchRequest {
	
	private Long trainId;
	private Ticket.TicketStatus status;
	private LocalDate departureDateFrom;
	private LocalDate departureDateTo;
	private LocalDate createdDateFrom;
	private LocalDate createdDateTo;
}

