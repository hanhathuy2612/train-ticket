package com.example.notificationservice.listener;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.repository.NotificationRepository;
import com.example.notificationservice.service.NotificationService;

@Component
public class NotificationListener {

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private NotificationRepository notificationRepository;

	@KafkaListener(topics = "booking-events", groupId = "notification-service-group")
	public void handleBookingNotification(Map<String, Object> message) {
		try {
			Long userId = Long.valueOf(message.get("userId").toString());
			Long ticketId = Long.valueOf(message.get("ticketId").toString());
			Integer numberOfSeats = Integer.valueOf(message.get("numberOfSeats").toString());

			String notificationMessage = String.format(
					"Your ticket booking #%d for %d seat(s) has been confirmed. Thank you for choosing our service!",
					ticketId, numberOfSeats);

			Notification notification = new Notification();
			notification.setUserId(userId);
			notification.setType("BOOKING_CONFIRMATION");
			notification.setMessage(notificationMessage);
			notification.setChannel("EMAIL");
			notification = notificationRepository.save(notification);

			notificationService.sendNotification(notification);
		} catch (Exception e) {
			// Log error and handle retry logic
			e.printStackTrace();
		}
	}

	@KafkaListener(topics = "payment-events", groupId = "notification-service-group")
	public void handlePaymentNotification(Map<String, Object> message) {
		try {
			Long userId = Long.valueOf(message.get("userId").toString());
			Long ticketId = Long.valueOf(message.get("ticketId").toString());
			String status = message.get("status").toString();
			Object amountObj = message.get("amount");

			String notificationMessage;
			if ("COMPLETED".equals(status)) {
				notificationMessage = String.format(
						"Payment for ticket #%d has been completed successfully. Amount: %s",
						ticketId, amountObj);
			} else {
				notificationMessage = String.format(
						"Payment for ticket #%d has failed. Please try again.",
						ticketId);
			}

			Notification notification = new Notification();
			notification.setUserId(userId);
			notification.setType("PAYMENT_" + status);
			notification.setMessage(notificationMessage);
			notification.setChannel("EMAIL");
			notification = notificationRepository.save(notification);

			notificationService.sendNotification(notification);
		} catch (Exception e) {
			// Log error and handle retry logic
			e.printStackTrace();
		}
	}
}
