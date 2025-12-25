package com.example.ticketservice.event;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.ticketservice.entity.Ticket;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TicketEventPublisher {
	
	private static final Logger logger = LoggerFactory.getLogger(TicketEventPublisher.class);
	
	private final RabbitTemplate rabbitTemplate;
	
	@Value("${rabbitmq.exchange.booking:booking-exchange}")
	private String bookingExchange;
	
	@Value("${rabbitmq.routing-key.booking-created:booking.created}")
	private String bookingCreatedRoutingKey;
	
	@Value("${rabbitmq.routing-key.booking-confirmed:booking.confirmed}")
	private String bookingConfirmedRoutingKey;
	
	@Value("${rabbitmq.routing-key.booking-cancelled:booking.cancelled}")
	private String bookingCancelledRoutingKey;

	public void publishBookingCreated(Ticket ticket) {
		Map<String, Object> event = createBookingEvent(ticket, "BOOKING_CREATED");
		publish(bookingExchange, bookingCreatedRoutingKey, event);
		logger.info("Published booking created event for ticket: {}", ticket.getId());
	}

	public void publishBookingConfirmed(Ticket ticket) {
		Map<String, Object> event = createBookingEvent(ticket, "BOOKING_CONFIRMED");
		publish(bookingExchange, bookingConfirmedRoutingKey, event);
		logger.info("Published booking confirmed event for ticket: {}", ticket.getId());
	}

	public void publishBookingCancelled(Ticket ticket) {
		Map<String, Object> event = createBookingEvent(ticket, "BOOKING_CANCELLED");
		event.put("cancellationReason", ticket.getCancellationReason());
		event.put("cancelledAt", ticket.getCancelledAt());
		publish(bookingExchange, bookingCancelledRoutingKey, event);
		logger.info("Published booking cancelled event for ticket: {}", ticket.getId());
	}

	private Map<String, Object> createBookingEvent(Ticket ticket, String eventType) {
		Map<String, Object> event = new HashMap<>();
		event.put("eventType", eventType);
		event.put("ticketId", ticket.getId());
		event.put("userId", ticket.getUserId());
		event.put("trainId", ticket.getTrainId());
		event.put("departureDate", ticket.getDepartureDate());
		event.put("numberOfSeats", ticket.getNumberOfSeats());
		event.put("totalPrice", ticket.getTotalPrice());
		event.put("status", ticket.getStatus().name());
		event.put("timestamp", LocalDateTime.now().toString());
		return event;
	}

	private void publish(String exchange, String routingKey, Map<String, Object> event) {
		try {
			rabbitTemplate.convertAndSend(exchange, routingKey, event);
		} catch (Exception e) {
			logger.error("Failed to publish event to RabbitMQ: exchange={}, routingKey={}", exchange, routingKey, e);
			// Don't throw exception - event publishing failure shouldn't affect main flow
		}
	}
}

