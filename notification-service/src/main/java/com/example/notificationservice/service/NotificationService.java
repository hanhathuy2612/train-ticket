package com.example.notificationservice.service;

import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class NotificationService {

	@Autowired
	private NotificationRepository notificationRepository;

	public void sendNotification(Notification notification) {
		try {
			// Simulate sending email/SMS
			// In real implementation, this would call email/SMS service
			boolean sent = sendEmail(notification);
			
			if (sent) {
				notification.setStatus(Notification.NotificationStatus.SENT);
				notification.setSentAt(LocalDateTime.now());
			} else {
				notification.setStatus(Notification.NotificationStatus.FAILED);
			}
			
			notificationRepository.save(notification);
		} catch (Exception e) {
			notification.setStatus(Notification.NotificationStatus.FAILED);
			notificationRepository.save(notification);
			throw new RuntimeException("Failed to send notification", e);
		}
	}

	private boolean sendEmail(Notification notification) {
		// Simulate email sending
		// In real implementation, integrate with email service (SendGrid, AWS SES, etc.)
		System.out.println("Sending " + notification.getChannel() + " to user " + notification.getUserId() + ": " + notification.getMessage());
		return true; // Simulate success
	}
}

