package com.example.ticketservice.event;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import com.example.ticketservice.entity.Ticket;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TicketEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(TicketEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${kafka.topic.booking-events:booking-events}")
    private String bookingEventsTopic;

    public void publishBookingCreated(Ticket ticket) {
        Map<String, Object> event = createBookingEvent(ticket, "BOOKING_CREATED");
        publish(bookingEventsTopic, event);
        logger.info("Published booking created event for ticket: {}", ticket.getId());
    }

    public void publishBookingConfirmed(Ticket ticket) {
        Map<String, Object> event = createBookingEvent(ticket, "BOOKING_CONFIRMED");
        publish(bookingEventsTopic, event);
        logger.info("Published booking confirmed event for ticket: {}", ticket.getId());
    }

    public void publishBookingCancelled(Ticket ticket) {
        Map<String, Object> event = createBookingEvent(ticket, "BOOKING_CANCELLED");
        event.put("cancellationReason", ticket.getCancellationReason());
        event.put("cancelledAt", ticket.getCancelledAt());
        publish(bookingEventsTopic, event);
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

    private void publish(String topic, Map<String, Object> event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            String eventType = (String) event.get("eventType");
            
            kafkaTemplate.send(topic, eventType, eventJson)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.debug("Successfully sent event to topic {}: {}", topic, eventType);
                        } else {
                            logger.error("Failed to send event to topic {}: {}", topic, eventType, ex);
                        }
                    });
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize event to JSON for topic: {}", topic, e);
        } catch (Exception e) {
            logger.error("Failed to publish event to Kafka: topic={}", topic, e);
            // Don't throw exception - event publishing failure shouldn't affect main flow
        }
    }
}

