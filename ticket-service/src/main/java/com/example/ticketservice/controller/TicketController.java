package com.example.ticketservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ticketservice.dto.BookTicketRequest;
import com.example.ticketservice.dto.TicketResponse;
import com.example.ticketservice.service.TicketService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/tickets")
public class TicketController {

	@Autowired
	private TicketService ticketService;

	@PostMapping("/book")
	public ResponseEntity<TicketResponse> bookTicket(
			@RequestHeader("X-User-Id") Long userId,
			@Valid @RequestBody BookTicketRequest request) {
		try {
			TicketResponse response = ticketService.bookTicket(userId, request);
			return ResponseEntity.status(HttpStatus.CREATED).body(response);
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
	}

	@GetMapping("/{id}")
	public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long id) {
		return ticketService.getTicketById(id)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<List<TicketResponse>> getUserTickets(@PathVariable Long userId) {
		List<TicketResponse> tickets = ticketService.getUserTickets(userId);
		return ResponseEntity.ok(tickets);
	}

	@PostMapping("/{id}/confirm")
	public ResponseEntity<TicketResponse> confirmTicket(
			@PathVariable Long id,
			@RequestHeader("X-User-Id") Long userId) {
		try {
			TicketResponse response = ticketService.confirmTicket(id, userId);
			return ResponseEntity.ok(response);
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
	}

	@PostMapping("/{id}/cancel")
	public ResponseEntity<TicketResponse> cancelTicket(
			@PathVariable Long id,
			@RequestHeader("X-User-Id") Long userId) {
		try {
			TicketResponse response = ticketService.cancelTicket(id, userId);
			return ResponseEntity.ok(response);
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
	}
}
