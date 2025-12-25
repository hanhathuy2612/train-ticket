package com.example.ticketservice.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ticketservice.client.InventoryServiceClient;
import com.example.ticketservice.dto.BookTicketRequest;
import com.example.ticketservice.dto.TicketResponse;
import com.example.ticketservice.entity.Ticket;
import com.example.ticketservice.repository.TicketRepository;

@Service
@Transactional
public class TicketService {

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private InventoryServiceClient inventoryServiceClient;

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	private static final String TICKET_CACHE_PREFIX = "ticket:";
	private static final String USER_TICKETS_CACHE_PREFIX = "user:tickets:";
	private static final String BOOKING_TOPIC = "booking-events";
	private static final String NOTIFICATION_ROUTING_KEY = "notification.booking";

	@Cacheable(value = "tickets", key = "#id")
	public Optional<TicketResponse> getTicketById(Long id) {
		Optional<Ticket> ticket = ticketRepository.findById(id);
		return ticket.map(TicketResponse::new);
	}

	@Cacheable(value = "userTickets", key = "#userId")
	public List<TicketResponse> getUserTickets(Long userId) {
		List<Ticket> tickets = ticketRepository.findByUserId(userId);
		return tickets.stream()
				.map(TicketResponse::new)
				.collect(Collectors.toList());
	}

	public TicketResponse bookTicket(Long userId, BookTicketRequest request) {
		// Check availability
		Map<String, Object> availability = inventoryServiceClient.checkAvailability(
				request.getTrainId(),
				request.getDepartureDate());

		Integer availableSeats = (Integer) availability.get("availableSeats");
		if (availableSeats == null || availableSeats < request.getNumberOfSeats()) {
			throw new RuntimeException("Not enough seats available");
		}

		// Reserve seats in inventory
		Map<String, Object> reserveRequest = new HashMap<>();
		reserveRequest.put("trainId", request.getTrainId());
		reserveRequest.put("numberOfSeats", request.getNumberOfSeats());
		reserveRequest.put("departureDate", request.getDepartureDate());

		Boolean reserved = inventoryServiceClient.reserveSeats(reserveRequest);
		if (!reserved) {
			throw new RuntimeException("Failed to reserve seats");
		}

		// Create ticket
		Ticket ticket = new Ticket();
		ticket.setUserId(userId);
		ticket.setTrainId(request.getTrainId());
		ticket.setDepartureDate(request.getDepartureDate());
		ticket.setNumberOfSeats(request.getNumberOfSeats());
		ticket.setTotalPrice(request.getTotalPrice());
		ticket.setStatus(Ticket.TicketStatus.PENDING);

		ticket = ticketRepository.save(ticket);

		// Send notification event
		Map<String, Object> notificationEvent = new HashMap<>();
		notificationEvent.put("ticketId", ticket.getId());
		notificationEvent.put("userId", userId);
		notificationEvent.put("trainId", request.getTrainId());
		notificationEvent.put("numberOfSeats", request.getNumberOfSeats());
		kafkaTemplate.send(BOOKING_TOPIC, notificationEvent);

		// Evict cache
		evictUserTicketsCache(userId);

		return new TicketResponse(ticket);
	}

	public TicketResponse confirmTicket(Long ticketId, Long userId) {
		Optional<Ticket> ticketOpt = ticketRepository.findByIdAndUserId(ticketId, userId);
		if (ticketOpt.isEmpty()) {
			throw new RuntimeException("Ticket not found");
		}

		Ticket ticket = ticketOpt.get();
		if (ticket.getStatus() != Ticket.TicketStatus.PENDING) {
			throw new RuntimeException("Ticket cannot be confirmed");
		}

		ticket.setStatus(Ticket.TicketStatus.CONFIRMED);
		ticket = ticketRepository.save(ticket);

		evictCache(ticketId);
		evictUserTicketsCache(userId);

		return new TicketResponse(ticket);
	}

	public TicketResponse cancelTicket(Long ticketId, Long userId) {
		Optional<Ticket> ticketOpt = ticketRepository.findByIdAndUserId(ticketId, userId);
		if (ticketOpt.isEmpty()) {
			throw new RuntimeException("Ticket not found");
		}

		Ticket ticket = ticketOpt.get();
		if (ticket.getStatus() == Ticket.TicketStatus.CANCELLED) {
			throw new RuntimeException("Ticket already cancelled");
		}

		// Release seats in inventory
		Boolean released = inventoryServiceClient.releaseSeats(
				ticket.getTrainId(),
				ticket.getDepartureDate(),
				ticket.getNumberOfSeats());

		if (!released) {
			throw new RuntimeException("Failed to release seats");
		}

		ticket.setStatus(Ticket.TicketStatus.CANCELLED);
		ticket = ticketRepository.save(ticket);

		evictCache(ticketId);
		evictUserTicketsCache(userId);

		return new TicketResponse(ticket);
	}

	@CacheEvict(value = "tickets", key = "#ticketId")
	private void evictCache(Long ticketId) {
		redisTemplate.delete(TICKET_CACHE_PREFIX + ticketId);
	}

	@CacheEvict(value = "userTickets", key = "#userId")
	private void evictUserTicketsCache(Long userId) {
		redisTemplate.delete(USER_TICKETS_CACHE_PREFIX + userId);
	}
}
