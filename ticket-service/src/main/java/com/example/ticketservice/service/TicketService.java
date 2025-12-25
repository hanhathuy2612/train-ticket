package com.example.ticketservice.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.example.ticketservice.client.InventoryServiceClient;
import com.example.ticketservice.client.PaymentServiceClient;
import com.example.ticketservice.dto.AvailabilityResponse;
import com.example.ticketservice.dto.BookTicketRequest;
import com.example.ticketservice.dto.CancelTicketRequest;
import com.example.ticketservice.dto.PageResponse;
import com.example.ticketservice.dto.PaymentResponse;
import com.example.ticketservice.dto.ReserveSeatRequest;
import com.example.ticketservice.dto.TicketResponse;
import com.example.ticketservice.dto.TicketSearchRequest;
import com.example.ticketservice.entity.Ticket;
import com.example.ticketservice.entity.Ticket.TicketStatus;
import com.example.ticketservice.event.TicketEventPublisher;
import com.example.ticketservice.exception.InsufficientSeatsException;
import com.example.ticketservice.exception.TicketNotFoundException;
import com.example.ticketservice.exception.TicketOperationException;
import com.example.ticketservice.exception.UnauthorizedAccessException;
import com.example.ticketservice.repository.TicketRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketService {

	private static final Logger logger = LoggerFactory.getLogger(TicketService.class);

	private final TicketRepository ticketRepository;
	private final InventoryServiceClient inventoryServiceClient;
	private final PaymentServiceClient paymentServiceClient;
	private final TicketEventPublisher eventPublisher;

	/**
	 * Get ticket by ID with caching
	 */
	@Cacheable(value = "tickets", key = "#id", unless = "#result == null")
	public TicketResponse getTicketById(Long id) {
		logger.debug("Fetching ticket by id: {}", id);
		return ticketRepository.findById(id)
				.map(this::enrichTicketResponse)
				.orElseThrow(() -> new TicketNotFoundException(id));
	}

	/**
	 * Get ticket by ID and verify ownership
	 */
	public TicketResponse getTicketByIdAndUser(Long id, Long userId) {
		logger.debug("Fetching ticket {} for user {}", id, userId);
		Ticket ticket = ticketRepository.findByIdAndUserId(id, userId)
				.orElseThrow(() -> new TicketNotFoundException(id));
		return enrichTicketResponse(ticket);
	}

	/**
	 * Get all tickets for a user with pagination
	 */
	@Cacheable(value = "userTickets", key = "#userId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
	public PageResponse<TicketResponse> getUserTickets(Long userId, Pageable pageable) {
		logger.debug("Fetching tickets for user: {} with pagination: {}", userId, pageable);
		Page<Ticket> ticketPage = ticketRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
		Page<TicketResponse> responsePage = ticketPage.map(TicketResponse::from);
		return PageResponse.from(responsePage);
	}

	/**
	 * Get all tickets for a user (no pagination)
	 */
	public List<TicketResponse> getAllUserTickets(Long userId) {
		logger.debug("Fetching all tickets for user: {}", userId);
		return ticketRepository.findByUserId(userId).stream()
				.map(TicketResponse::from)
				.toList();
	}

	/**
	 * Get tickets by status for a user
	 */
	public List<TicketResponse> getUserTicketsByStatus(Long userId, TicketStatus status) {
		logger.debug("Fetching tickets for user: {} with status: {}", userId, status);
		return ticketRepository.findByUserIdAndStatus(userId, status).stream()
				.map(TicketResponse::from)
				.toList();
	}

	/**
	 * Search tickets with filters
	 */
	public PageResponse<TicketResponse> searchTickets(Long userId, TicketSearchRequest request, Pageable pageable) {
		logger.debug("Searching tickets for user: {} with filters: {}", userId, request);
		Page<Ticket> ticketPage = ticketRepository.searchTickets(
				userId,
				request.getTrainId(),
				request.getStatus(),
				pageable
		);
		Page<TicketResponse> responsePage = ticketPage.map(TicketResponse::from);
		return PageResponse.from(responsePage);
	}

	/**
	 * Book a new ticket with retry mechanism
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	@Retryable(
			retryFor = {TicketOperationException.class},
			maxAttempts = 3,
			backoff = @Backoff(delay = 1000, multiplier = 2)
	)
	@Caching(evict = {
			@CacheEvict(value = "userTickets", allEntries = true)
	})
	public TicketResponse bookTicket(Long userId, BookTicketRequest request) {
		logger.info("Booking ticket for user: {}, train: {}, seats: {}", 
				userId, request.getTrainId(), request.getNumberOfSeats());

		// Step 1: Check availability
		AvailabilityResponse availability = checkAndValidateAvailability(request);
		
		// Step 2: Reserve seats
		reserveSeats(request);

		// Step 3: Create ticket
		Ticket ticket = createTicket(userId, request);
		ticket = ticketRepository.save(ticket);
		logger.info("Ticket created successfully with id: {}", ticket.getId());

		// Step 4: Publish event for notification
		eventPublisher.publishBookingCreated(ticket);

		return TicketResponse.from(ticket);
	}

	/**
	 * Confirm a pending ticket
	 */
	@Transactional
	@Caching(evict = {
			@CacheEvict(value = "tickets", key = "#ticketId"),
			@CacheEvict(value = "userTickets", allEntries = true)
	})
	public TicketResponse confirmTicket(Long ticketId, Long userId) {
		logger.info("Confirming ticket: {} for user: {}", ticketId, userId);

		Ticket ticket = getTicketForUpdate(ticketId, userId);
		validateTicketForConfirmation(ticket);

		ticket.setStatus(TicketStatus.CONFIRMED);
		ticket = ticketRepository.save(ticket);
		logger.info("Ticket {} confirmed successfully", ticketId);

		// Publish event
		eventPublisher.publishBookingConfirmed(ticket);

		return TicketResponse.from(ticket);
	}

	/**
	 * Cancel a ticket with optional refund
	 */
	@Transactional
	@Caching(evict = {
			@CacheEvict(value = "tickets", key = "#ticketId"),
			@CacheEvict(value = "userTickets", allEntries = true)
	})
	public TicketResponse cancelTicket(Long ticketId, Long userId, CancelTicketRequest request) {
		logger.info("Cancelling ticket: {} for user: {}, reason: {}", ticketId, userId, request.getReason());

		Ticket ticket = getTicketForUpdate(ticketId, userId);
		validateTicketForCancellation(ticket);

		// Step 1: Release seats in inventory
		releaseSeats(ticket);

		// Step 2: Process refund if requested and payment was made
		if (request.isRequestRefund() && ticket.getStatus() == TicketStatus.CONFIRMED) {
			processRefund(ticket, userId);
		}

		// Step 3: Update ticket status
		ticket.setStatus(TicketStatus.CANCELLED);
		ticket.setCancellationReason(request.getReason());
		ticket.setCancelledAt(LocalDateTime.now());
		ticket = ticketRepository.save(ticket);
		logger.info("Ticket {} cancelled successfully", ticketId);

		// Step 4: Publish event
		eventPublisher.publishBookingCancelled(ticket);

		return TicketResponse.from(ticket);
	}

	/**
	 * Cancel a ticket (simple version without request body)
	 */
	@Transactional
	@Caching(evict = {
			@CacheEvict(value = "tickets", key = "#ticketId"),
			@CacheEvict(value = "userTickets", allEntries = true)
	})
	public TicketResponse cancelTicket(Long ticketId, Long userId) {
		CancelTicketRequest request = CancelTicketRequest.builder()
				.reason("User requested cancellation")
				.requestRefund(true)
				.build();
		return cancelTicket(ticketId, userId, request);
	}

	/**
	 * Mark ticket as completed (after trip is done)
	 */
	@Transactional
	@Caching(evict = {
			@CacheEvict(value = "tickets", key = "#ticketId"),
			@CacheEvict(value = "userTickets", allEntries = true)
	})
	public TicketResponse completeTicket(Long ticketId) {
		logger.info("Completing ticket: {}", ticketId);

		Ticket ticket = ticketRepository.findById(ticketId)
				.orElseThrow(() -> new TicketNotFoundException(ticketId));

		if (ticket.getStatus() != TicketStatus.CONFIRMED) {
			throw new TicketOperationException("Only confirmed tickets can be completed");
		}

		ticket.setStatus(TicketStatus.COMPLETED);
		ticket = ticketRepository.save(ticket);
		logger.info("Ticket {} completed successfully", ticketId);

		return TicketResponse.from(ticket);
	}

	// ============ Private Helper Methods ============

	private AvailabilityResponse checkAndValidateAvailability(BookTicketRequest request) {
		logger.debug("Checking availability for train: {} on {}", request.getTrainId(), request.getDepartureDate());
		
		AvailabilityResponse availability = inventoryServiceClient.checkAvailability(
				request.getTrainId(),
				request.getDepartureDate()
		);

		if (!availability.hasEnoughSeats(request.getNumberOfSeats())) {
			logger.warn("Insufficient seats for train: {}. Requested: {}, Available: {}", 
					request.getTrainId(), request.getNumberOfSeats(), availability.getAvailableSeats());
			throw new InsufficientSeatsException(
					request.getTrainId(), 
					request.getNumberOfSeats(), 
					availability.getAvailableSeats() != null ? availability.getAvailableSeats() : 0
			);
		}

		return availability;
	}

	private void reserveSeats(BookTicketRequest request) {
		logger.debug("Reserving {} seats for train: {}", request.getNumberOfSeats(), request.getTrainId());
		
		ReserveSeatRequest reserveRequest = ReserveSeatRequest.builder()
				.trainId(request.getTrainId())
				.numberOfSeats(request.getNumberOfSeats())
				.departureDate(request.getDepartureDate())
				.seatNumbers(request.getSeatNumbers())
				.build();

		Boolean reserved = inventoryServiceClient.reserveSeats(reserveRequest);
		if (reserved == null || !reserved) {
			logger.error("Failed to reserve seats for train: {}", request.getTrainId());
			throw new TicketOperationException("Failed to reserve seats. Please try again.");
		}
	}

	private Ticket createTicket(Long userId, BookTicketRequest request) {
		return Ticket.builder()
				.userId(userId)
				.trainId(request.getTrainId())
				.departureDate(request.getDepartureDate())
				.numberOfSeats(request.getNumberOfSeats())
				.totalPrice(request.getTotalPrice())
				.status(TicketStatus.PENDING)
				.build();
	}

	private Ticket getTicketForUpdate(Long ticketId, Long userId) {
		Ticket ticket = ticketRepository.findByIdAndUserId(ticketId, userId)
				.orElseThrow(() -> new TicketNotFoundException(ticketId));

		if (!ticket.getUserId().equals(userId)) {
			throw new UnauthorizedAccessException(userId, ticketId);
		}

		return ticket;
	}

	private void validateTicketForConfirmation(Ticket ticket) {
		if (ticket.getStatus() != TicketStatus.PENDING) {
			throw new TicketOperationException(
					String.format("Ticket cannot be confirmed. Current status: %s", ticket.getStatus())
			);
		}
	}

	private void validateTicketForCancellation(Ticket ticket) {
		if (ticket.getStatus() == TicketStatus.CANCELLED) {
			throw new TicketOperationException("Ticket is already cancelled");
		}
		if (ticket.getStatus() == TicketStatus.COMPLETED) {
			throw new TicketOperationException("Cannot cancel a completed ticket");
		}
	}

	private void releaseSeats(Ticket ticket) {
		logger.debug("Releasing {} seats for train: {}", ticket.getNumberOfSeats(), ticket.getTrainId());
		
		Boolean released = inventoryServiceClient.releaseSeats(
				ticket.getTrainId(),
				ticket.getDepartureDate(),
				ticket.getNumberOfSeats()
		);

		if (released == null || !released) {
			logger.warn("Failed to release seats for ticket: {}. Will be handled by compensation job.", ticket.getId());
			// Don't fail the cancellation, seats will be released by a compensation job
		}
	}

	private void processRefund(Ticket ticket, Long userId) {
		logger.debug("Processing refund for ticket: {}", ticket.getId());
		
		try {
			PaymentResponse payment = paymentServiceClient.getPaymentByTicketId(ticket.getId());
			if (payment != null && payment.isCompleted()) {
				PaymentResponse refund = paymentServiceClient.refundPayment(payment.getId(), userId);
				if (refund == null || !refund.isRefunded()) {
					logger.warn("Refund processing failed for ticket: {}. Will be handled manually.", ticket.getId());
				}
			}
		} catch (Exception e) {
			logger.error("Error processing refund for ticket: {}", ticket.getId(), e);
			// Don't fail cancellation due to refund failure
		}
	}

	private TicketResponse enrichTicketResponse(Ticket ticket) {
		TicketResponse response = TicketResponse.from(ticket);
		
		// Try to fetch payment info
		try {
			PaymentResponse payment = paymentServiceClient.getPaymentByTicketId(ticket.getId());
			if (payment != null) {
				response.setPaymentInfo(TicketResponse.PaymentInfo.builder()
						.paymentId(payment.getId())
						.paymentStatus(payment.getStatus())
						.amount(payment.getAmount())
						.paidAt(payment.getCreatedAt())
						.build());
			}
		} catch (Exception e) {
			logger.debug("Could not fetch payment info for ticket: {}", ticket.getId());
		}

		return response;
	}
}
