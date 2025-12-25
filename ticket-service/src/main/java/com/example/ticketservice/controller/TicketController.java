package com.example.ticketservice.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.ticketservice.dto.ApiResponse;
import com.example.ticketservice.dto.BookTicketRequest;
import com.example.ticketservice.dto.CancelTicketRequest;
import com.example.ticketservice.dto.PageResponse;
import com.example.ticketservice.dto.TicketResponse;
import com.example.ticketservice.dto.TicketSearchRequest;
import com.example.ticketservice.entity.Ticket.TicketStatus;
import com.example.ticketservice.service.TicketService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

	private static final Logger logger = LoggerFactory.getLogger(TicketController.class);
	private static final int DEFAULT_PAGE_SIZE = 10;
	private static final int MAX_PAGE_SIZE = 100;

	private final TicketService ticketService;

	/**
	 * Book a new ticket
	 * POST /tickets/book
	 */
	@PostMapping("/book")
	public ResponseEntity<ApiResponse<TicketResponse>> bookTicket(
			@RequestHeader("X-User-Id") Long userId,
			@Valid @RequestBody BookTicketRequest request) {
		logger.info("Booking ticket for user: {}", userId);
		TicketResponse ticket = ticketService.bookTicket(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.created(ticket));
	}

	/**
	 * Get ticket by ID
	 * GET /tickets/{id}
	 */
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<TicketResponse>> getTicketById(
			@PathVariable Long id,
			@RequestHeader(value = "X-User-Id", required = false) Long userId) {
		logger.debug("Getting ticket: {} for user: {}", id, userId);
		TicketResponse ticket;
		if (userId != null) {
			ticket = ticketService.getTicketByIdAndUser(id, userId);
		} else {
			ticket = ticketService.getTicketById(id);
		}
		return ResponseEntity.ok(ApiResponse.success(ticket));
	}

	/**
	 * Get all tickets for current user with pagination
	 * GET /tickets/user/{userId}?page=0&size=10
	 */
	@GetMapping("/user/{userId}")
	public ResponseEntity<ApiResponse<PageResponse<TicketResponse>>> getUserTickets(
			@PathVariable Long userId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "createdAt") String sortBy,
			@RequestParam(defaultValue = "desc") String sortDir) {
		logger.debug("Getting tickets for user: {} - page: {}, size: {}", userId, page, size);
		
		size = Math.min(size, MAX_PAGE_SIZE);
		Sort sort = sortDir.equalsIgnoreCase("asc") 
				? Sort.by(sortBy).ascending() 
				: Sort.by(sortBy).descending();
		Pageable pageable = PageRequest.of(page, size, sort);
		
		PageResponse<TicketResponse> tickets = ticketService.getUserTickets(userId, pageable);
		return ResponseEntity.ok(ApiResponse.success(tickets));
	}

	/**
	 * Get all tickets for current user (no pagination)
	 * GET /tickets/my
	 */
	@GetMapping("/my")
	public ResponseEntity<ApiResponse<List<TicketResponse>>> getMyTickets(
			@RequestHeader("X-User-Id") Long userId) {
		logger.debug("Getting all tickets for user: {}", userId);
		List<TicketResponse> tickets = ticketService.getAllUserTickets(userId);
		return ResponseEntity.ok(ApiResponse.success(tickets));
	}

	/**
	 * Get user's tickets by status
	 * GET /tickets/my/status/{status}
	 */
	@GetMapping("/my/status/{status}")
	public ResponseEntity<ApiResponse<List<TicketResponse>>> getMyTicketsByStatus(
			@RequestHeader("X-User-Id") Long userId,
			@PathVariable TicketStatus status) {
		logger.debug("Getting tickets for user: {} with status: {}", userId, status);
		List<TicketResponse> tickets = ticketService.getUserTicketsByStatus(userId, status);
		return ResponseEntity.ok(ApiResponse.success(tickets));
	}

	/**
	 * Search tickets with filters
	 * POST /tickets/search
	 */
	@PostMapping("/search")
	public ResponseEntity<ApiResponse<PageResponse<TicketResponse>>> searchTickets(
			@RequestHeader("X-User-Id") Long userId,
			@RequestBody TicketSearchRequest request,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		logger.debug("Searching tickets for user: {} with filters: {}", userId, request);
		
		size = Math.min(size, MAX_PAGE_SIZE);
		Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
		
		PageResponse<TicketResponse> tickets = ticketService.searchTickets(userId, request, pageable);
		return ResponseEntity.ok(ApiResponse.success(tickets));
	}

	/**
	 * Confirm a pending ticket
	 * POST /tickets/{id}/confirm
	 */
	@PostMapping("/{id}/confirm")
	public ResponseEntity<ApiResponse<TicketResponse>> confirmTicket(
			@PathVariable Long id,
			@RequestHeader("X-User-Id") Long userId) {
		logger.info("Confirming ticket: {} for user: {}", id, userId);
		TicketResponse ticket = ticketService.confirmTicket(id, userId);
		return ResponseEntity.ok(ApiResponse.success("Ticket confirmed successfully", ticket));
	}

	/**
	 * Cancel a ticket with reason
	 * POST /tickets/{id}/cancel
	 */
	@PostMapping("/{id}/cancel")
	public ResponseEntity<ApiResponse<TicketResponse>> cancelTicket(
			@PathVariable Long id,
			@RequestHeader("X-User-Id") Long userId,
			@RequestBody(required = false) CancelTicketRequest request) {
		logger.info("Cancelling ticket: {} for user: {}", id, userId);
		TicketResponse ticket;
		if (request != null) {
			ticket = ticketService.cancelTicket(id, userId, request);
		} else {
			ticket = ticketService.cancelTicket(id, userId);
		}
		return ResponseEntity.ok(ApiResponse.success("Ticket cancelled successfully", ticket));
	}

	/**
	 * Cancel a ticket (simple - DELETE method)
	 * DELETE /tickets/{id}
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<TicketResponse>> deleteTicket(
			@PathVariable Long id,
			@RequestHeader("X-User-Id") Long userId) {
		logger.info("Deleting (cancelling) ticket: {} for user: {}", id, userId);
		TicketResponse ticket = ticketService.cancelTicket(id, userId);
		return ResponseEntity.ok(ApiResponse.success("Ticket cancelled successfully", ticket));
	}

	/**
	 * Mark ticket as completed (internal use / admin)
	 * PUT /tickets/{id}/complete
	 */
	@PutMapping("/{id}/complete")
	public ResponseEntity<ApiResponse<TicketResponse>> completeTicket(
			@PathVariable Long id) {
		logger.info("Completing ticket: {}", id);
		TicketResponse ticket = ticketService.completeTicket(id);
		return ResponseEntity.ok(ApiResponse.success("Ticket completed successfully", ticket));
	}

	/**
	 * Health check endpoint
	 * GET /tickets/health
	 */
	@GetMapping("/health")
	public ResponseEntity<ApiResponse<String>> health() {
		return ResponseEntity.ok(ApiResponse.success("Ticket Service is healthy"));
	}
}
