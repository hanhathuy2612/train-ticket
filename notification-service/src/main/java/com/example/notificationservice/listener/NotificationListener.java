package com.example.notificationservice.listener;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.notificationservice.dto.SendNotificationRequest;
import com.example.notificationservice.entity.Notification.NotificationChannel;
import com.example.notificationservice.entity.Notification.NotificationType;
import com.example.notificationservice.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationListener.class);

    private final NotificationService notificationService;

    @KafkaListener(topics = "booking-events", groupId = "notification-service-group")
    public void handleBookingEvent(Map<String, Object> message) {
        try {
            logger.info("Received booking event: {}", message.get("eventType"));
            
            String eventType = String.valueOf(message.get("eventType"));
            Long userId = Long.valueOf(message.get("userId").toString());
            Long ticketId = Long.valueOf(message.get("ticketId").toString());

            switch (eventType) {
                case "BOOKING_CREATED" -> handleBookingCreated(message, userId, ticketId);
                case "BOOKING_CONFIRMED" -> handleBookingConfirmed(message, userId, ticketId);
                case "BOOKING_CANCELLED" -> handleBookingCancelled(message, userId, ticketId);
                default -> logger.debug("Unhandled booking event type: {}", eventType);
            }
        } catch (Exception e) {
            logger.error("Error processing booking event", e);
        }
    }

    @KafkaListener(topics = "payment-events", groupId = "notification-service-group")
    public void handlePaymentEvent(Map<String, Object> message) {
        try {
            logger.info("Received payment event: {}", message.get("eventType"));
            
            String eventType = String.valueOf(message.get("eventType"));
            Long userId = Long.valueOf(message.get("userId").toString());
            Long ticketId = Long.valueOf(message.get("ticketId").toString());
            Long paymentId = Long.valueOf(message.get("paymentId").toString());
            Object amountObj = message.get("amount");

            switch (eventType) {
                case "PAYMENT_COMPLETED" -> handlePaymentCompleted(userId, ticketId, paymentId, amountObj);
                case "PAYMENT_FAILED" -> handlePaymentFailed(userId, ticketId, paymentId, amountObj);
                case "PAYMENT_REFUNDED" -> handlePaymentRefunded(userId, ticketId, paymentId, amountObj);
                default -> logger.debug("Unhandled payment event type: {}", eventType);
            }
        } catch (Exception e) {
            logger.error("Error processing payment event", e);
        }
    }

    private void handleBookingCreated(Map<String, Object> message, Long userId, Long ticketId) {
        Integer numberOfSeats = Integer.valueOf(message.get("numberOfSeats").toString());
        String trainNumber = message.get("trainNumber") != null ? message.get("trainNumber").toString() : "N/A";
        String departureDate = message.get("departureDate") != null ? message.get("departureDate").toString() : "N/A";
        Object totalPrice = message.get("totalPrice");

        String title = "Booking Created - Pending Payment";
        String messageText = String.format(
                "Your booking #%d has been created!\n\n" +
                "Train: %s\nDeparture: %s\nSeats: %d\nTotal: %s\n\n" +
                "Please complete your payment within 15 minutes to confirm your booking.",
                ticketId, trainNumber, departureDate, numberOfSeats, totalPrice);

        sendNotification(userId, NotificationType.BOOKING_CONFIRMATION, title, messageText, 
                ticketId, "TICKET");
    }

    private void handleBookingConfirmed(Map<String, Object> message, Long userId, Long ticketId) {
        String title = "Booking Confirmed";
        String messageText = String.format(
                "Your booking #%d has been confirmed! Thank you for choosing our service.",
                ticketId);

        sendNotification(userId, NotificationType.BOOKING_CONFIRMATION, title, messageText, 
                ticketId, "TICKET");
    }

    private void handleBookingCancelled(Map<String, Object> message, Long userId, Long ticketId) {
        String reason = message.get("cancellationReason") != null 
                ? message.get("cancellationReason").toString() 
                : "User requested";

        String title = "Booking Cancelled";
        String messageText = String.format(
                "Your booking #%d has been cancelled.\n\nReason: %s\n\n" +
                "If you have any questions, please contact our support.",
                ticketId, reason);

        sendNotification(userId, NotificationType.BOOKING_CANCELLATION, title, messageText, 
                ticketId, "TICKET");
    }

    private void handlePaymentCompleted(Long userId, Long ticketId, Long paymentId, Object amount) {
        String title = "Payment Successful";
        String messageText = String.format(
                "Payment of %s for ticket #%d was successful. Your booking is now confirmed!",
                amount, ticketId);

        sendNotification(userId, NotificationType.PAYMENT_SUCCESS, title, messageText, 
                paymentId, "PAYMENT");
    }

    private void handlePaymentFailed(Long userId, Long ticketId, Long paymentId, Object amount) {
        String title = "Payment Failed";
        String messageText = String.format(
                "Payment of %s for ticket #%d failed. Please try again or use a different payment method.",
                amount, ticketId);

        sendNotification(userId, NotificationType.PAYMENT_FAILED, title, messageText, 
                paymentId, "PAYMENT");
    }

    private void handlePaymentRefunded(Long userId, Long ticketId, Long paymentId, Object amount) {
        String title = "Payment Refunded";
        String messageText = String.format(
                "A refund of %s for ticket #%d has been processed. " +
                "It may take 3-5 business days to reflect in your account.",
                amount, ticketId);

        sendNotification(userId, NotificationType.PAYMENT_REFUND, title, messageText, 
                paymentId, "PAYMENT");
    }

    private void sendNotification(Long userId, NotificationType type, String title, 
            String message, Long referenceId, String referenceType) {
        // Send email notification
        SendNotificationRequest emailRequest = SendNotificationRequest.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .channel(NotificationChannel.EMAIL)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build();
        notificationService.sendNotification(emailRequest);

        // Send in-app notification
        SendNotificationRequest inAppRequest = SendNotificationRequest.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .channel(NotificationChannel.IN_APP)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build();
        notificationService.sendNotification(inAppRequest);
    }
}
