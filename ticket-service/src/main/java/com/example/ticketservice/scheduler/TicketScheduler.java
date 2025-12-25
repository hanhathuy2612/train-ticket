package com.example.ticketservice.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.ticketservice.client.InventoryServiceClient;
import com.example.ticketservice.entity.Ticket;
import com.example.ticketservice.entity.Ticket.TicketStatus;
import com.example.ticketservice.event.TicketEventPublisher;
import com.example.ticketservice.repository.TicketRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TicketScheduler {
	
	private static final Logger logger = LoggerFactory.getLogger(TicketScheduler.class);
	
	private final TicketRepository ticketRepository;
	private final InventoryServiceClient inventoryServiceClient;
	private final TicketEventPublisher eventPublisher;
	
	// Timeout for pending tickets (15 minutes)
	private static final int PENDING_TIMEOUT_MINUTES = 15;

	/**
	 * Cancel expired pending tickets every 5 minutes
	 * Tickets that remain PENDING for more than 15 minutes will be auto-cancelled
	 */
	@Scheduled(fixedRate = 300000) // 5 minutes
	@Transactional
	public void cancelExpiredPendingTickets() {
		logger.info("Running scheduled job: Cancel expired pending tickets");
		
		LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(PENDING_TIMEOUT_MINUTES);
		List<Ticket> expiredTickets = ticketRepository.findByStatusAndCreatedAtBefore(
				TicketStatus.PENDING, cutoffTime);
		
		if (expiredTickets.isEmpty()) {
			logger.debug("No expired pending tickets found");
			return;
		}
		
		logger.info("Found {} expired pending tickets to cancel", expiredTickets.size());
		
		for (Ticket ticket : expiredTickets) {
			try {
				cancelExpiredTicket(ticket);
			} catch (Exception e) {
				logger.error("Failed to cancel expired ticket: {}", ticket.getId(), e);
			}
		}
	}
	
	private void cancelExpiredTicket(Ticket ticket) {
		logger.info("Auto-cancelling expired ticket: {}", ticket.getId());
		
		// Release seats
		try {
			inventoryServiceClient.releaseSeats(
					ticket.getTrainId(),
					ticket.getDepartureDate(),
					ticket.getNumberOfSeats()
			);
		} catch (Exception e) {
			logger.warn("Failed to release seats for expired ticket: {}", ticket.getId(), e);
			// Continue with cancellation even if seat release fails
		}
		
		// Update ticket status
		ticket.setStatus(TicketStatus.CANCELLED);
		ticket.setCancellationReason("Auto-cancelled due to payment timeout");
		ticket.setCancelledAt(LocalDateTime.now());
		ticketRepository.save(ticket);
		
		// Publish event
		eventPublisher.publishBookingCancelled(ticket);
		
		logger.info("Successfully auto-cancelled expired ticket: {}", ticket.getId());
	}

	/**
	 * Log ticket statistics every hour
	 */
	@Scheduled(cron = "0 0 * * * *") // Every hour
	public void logTicketStatistics() {
		long totalTickets = ticketRepository.count();
		logger.info("Ticket statistics - Total tickets: {}", totalTickets);
	}
}

