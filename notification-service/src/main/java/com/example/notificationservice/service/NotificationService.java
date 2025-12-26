package com.example.notificationservice.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.notificationservice.dto.NotificationPreferenceResponse;
import com.example.notificationservice.dto.NotificationResponse;
import com.example.notificationservice.dto.SendNotificationRequest;
import com.example.notificationservice.dto.UpdatePreferenceRequest;
import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.entity.Notification.NotificationChannel;
import com.example.notificationservice.entity.Notification.NotificationStatus;
import com.example.notificationservice.entity.Notification.NotificationType;
import com.example.notificationservice.entity.NotificationPreference;
import com.example.notificationservice.repository.NotificationPreferenceRepository;
import com.example.notificationservice.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;

    // ============ Send Notifications ============

    @Transactional
    public NotificationResponse sendNotification(SendNotificationRequest request) {
        logger.info("Sending {} notification to user: {}", request.getType(), request.getUserId());

        // Check user preferences
        if (!shouldSendNotification(request.getUserId(), request.getType(), request.getChannel())) {
            logger.info("Notification skipped due to user preferences");
            return null;
        }

        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .userEmail(request.getUserEmail())
                .userPhone(request.getUserPhone())
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .channel(request.getChannel())
                .templateCode(request.getTemplateCode())
                .referenceId(request.getReferenceId())
                .referenceType(request.getReferenceType())
                .status(NotificationStatus.PENDING)
                .build();

        notification = notificationRepository.save(notification);

        // Send the notification
        boolean sent = doSendNotification(notification);

        if (sent) {
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            logger.info("Notification {} sent successfully", notification.getId());
        } else {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage("Failed to send notification");
            notification.incrementRetry();
            logger.warn("Notification {} failed to send", notification.getId());
        }

        notification = notificationRepository.save(notification);
        return NotificationResponse.from(notification);
    }

    @Transactional
    public void sendBookingConfirmation(Long userId, String email, Long ticketId, 
            String trainNumber, String departureDate, int seats, String totalPrice) {
        String title = "Booking Confirmation";
        String message = String.format(
                "Your booking #%d has been confirmed!\n\n" +
                "Train: %s\nDeparture: %s\nSeats: %d\nTotal: %s\n\n" +
                "Thank you for choosing our service!",
                ticketId, trainNumber, departureDate, seats, totalPrice);

        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId(userId)
                .userEmail(email)
                .type(NotificationType.BOOKING_CONFIRMATION)
                .title(title)
                .message(message)
                .channel(NotificationChannel.EMAIL)
                .referenceId(ticketId)
                .referenceType("TICKET")
                .build();

        sendNotification(request);

        // Also send in-app notification
        request.setChannel(NotificationChannel.IN_APP);
        sendNotification(request);
    }

    @Transactional
    public void sendPaymentNotification(Long userId, String email, Long paymentId, 
            Long ticketId, String amount, boolean success) {
        NotificationType type = success ? NotificationType.PAYMENT_SUCCESS : NotificationType.PAYMENT_FAILED;
        String title = success ? "Payment Successful" : "Payment Failed";
        String message = success
                ? String.format("Payment of %s for ticket #%d was successful.", amount, ticketId)
                : String.format("Payment of %s for ticket #%d failed. Please try again.", amount, ticketId);

        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId(userId)
                .userEmail(email)
                .type(type)
                .title(title)
                .message(message)
                .channel(NotificationChannel.EMAIL)
                .referenceId(paymentId)
                .referenceType("PAYMENT")
                .build();

        sendNotification(request);
    }

    // ============ Query Notifications ============

    public List<NotificationResponse> getUserNotifications(Long userId) {
        return notificationRepository.findByUserId(userId).stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }

    public Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::from);
    }

    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        return notificationRepository.findUnreadByUserId(userId).stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    public NotificationResponse getNotificationById(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        return NotificationResponse.from(notification);
    }

    // ============ Mark as Read ============

    @Transactional
    public void markAsRead(Long notificationId) {
        logger.debug("Marking notification {} as read", notificationId);
        notificationRepository.markAsRead(notificationId, LocalDateTime.now());
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        logger.debug("Marking all notifications as read for user: {}", userId);
        notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }

    // ============ Preferences ============

    public NotificationPreferenceResponse getUserPreferences(Long userId) {
        NotificationPreference pref = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));
        return NotificationPreferenceResponse.from(pref);
    }

    @Transactional
    public NotificationPreferenceResponse updatePreferences(Long userId, UpdatePreferenceRequest request) {
        logger.info("Updating notification preferences for user: {}", userId);

        NotificationPreference pref = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));

        // Update email preferences
        if (request.getEmailEnabled() != null) pref.setEmailEnabled(request.getEmailEnabled());
        if (request.getEmailBookingConfirmation() != null) 
            pref.setEmailBookingConfirmation(request.getEmailBookingConfirmation());
        if (request.getEmailPaymentNotification() != null) 
            pref.setEmailPaymentNotification(request.getEmailPaymentNotification());
        if (request.getEmailTripReminder() != null) pref.setEmailTripReminder(request.getEmailTripReminder());
        if (request.getEmailPromotional() != null) pref.setEmailPromotional(request.getEmailPromotional());

        // Update SMS preferences
        if (request.getSmsEnabled() != null) pref.setSmsEnabled(request.getSmsEnabled());
        if (request.getSmsBookingConfirmation() != null) 
            pref.setSmsBookingConfirmation(request.getSmsBookingConfirmation());
        if (request.getSmsTripReminder() != null) pref.setSmsTripReminder(request.getSmsTripReminder());

        // Update push preferences
        if (request.getPushEnabled() != null) pref.setPushEnabled(request.getPushEnabled());
        if (request.getPushBookingUpdate() != null) pref.setPushBookingUpdate(request.getPushBookingUpdate());
        if (request.getPushScheduleChange() != null) pref.setPushScheduleChange(request.getPushScheduleChange());
        if (request.getPushPromotional() != null) pref.setPushPromotional(request.getPushPromotional());

        // Update quiet hours
        if (request.getQuietHoursStart() != null) pref.setQuietHoursStart(request.getQuietHoursStart());
        if (request.getQuietHoursEnd() != null) pref.setQuietHoursEnd(request.getQuietHoursEnd());

        pref = preferenceRepository.save(pref);
        return NotificationPreferenceResponse.from(pref);
    }

    // ============ Retry Failed Notifications ============

    @Transactional
    public void retryFailedNotifications() {
        logger.info("Retrying failed notifications");
        List<Notification> failed = notificationRepository.findFailedForRetry();

        for (Notification notification : failed) {
            notification.setStatus(NotificationStatus.PENDING);
            notification.incrementRetry();
            notificationRepository.save(notification);

            boolean sent = doSendNotification(notification);
            if (sent) {
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
            } else {
                notification.setStatus(NotificationStatus.FAILED);
            }
            notificationRepository.save(notification);
        }
    }

    // ============ Helper Methods ============

    private NotificationPreference createDefaultPreferences(Long userId) {
        NotificationPreference pref = NotificationPreference.builder()
                .userId(userId)
                .build();
        return preferenceRepository.save(pref);
    }

    private boolean shouldSendNotification(Long userId, NotificationType type, NotificationChannel channel) {
        NotificationPreference pref = preferenceRepository.findByUserId(userId).orElse(null);
        if (pref == null) {
            return true; // Default: send all notifications
        }

        // Check channel-specific preferences
        return switch (channel) {
            case EMAIL -> pref.getEmailEnabled() && isEmailTypeEnabled(pref, type);
            case SMS -> pref.getSmsEnabled() && isSmsTypeEnabled(pref, type);
            case PUSH -> pref.getPushEnabled() && isPushTypeEnabled(pref, type);
            case IN_APP -> true; // Always send in-app notifications
        };
    }

    private boolean isEmailTypeEnabled(NotificationPreference pref, NotificationType type) {
        return switch (type) {
            case BOOKING_CONFIRMATION, BOOKING_CANCELLATION -> pref.getEmailBookingConfirmation();
            case PAYMENT_SUCCESS, PAYMENT_FAILED, PAYMENT_REFUND -> pref.getEmailPaymentNotification();
            case TRIP_REMINDER -> pref.getEmailTripReminder();
            case PROMOTIONAL -> pref.getEmailPromotional();
            default -> true;
        };
    }

    private boolean isSmsTypeEnabled(NotificationPreference pref, NotificationType type) {
        return switch (type) {
            case BOOKING_CONFIRMATION -> pref.getSmsBookingConfirmation();
            case TRIP_REMINDER -> pref.getSmsTripReminder();
            default -> false;
        };
    }

    private boolean isPushTypeEnabled(NotificationPreference pref, NotificationType type) {
        return switch (type) {
            case BOOKING_CONFIRMATION, BOOKING_CANCELLATION -> pref.getPushBookingUpdate();
            case SCHEDULE_CHANGE -> pref.getPushScheduleChange();
            case PROMOTIONAL -> pref.getPushPromotional();
            default -> true;
        };
    }

    private boolean doSendNotification(Notification notification) {
        // Simulate sending notification based on channel
        logger.debug("Sending {} notification via {}: {}", 
                notification.getType(), notification.getChannel(), notification.getTitle());

        try {
            Thread.sleep(50); // Simulate network delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // In real implementation, this would call actual email/SMS/push services
        return switch (notification.getChannel()) {
            case EMAIL -> sendEmail(notification);
            case SMS -> sendSms(notification);
            case PUSH -> sendPushNotification(notification);
            case IN_APP -> true; // Always succeeds for in-app
        };
    }

    private boolean sendEmail(Notification notification) {
        // Integrate with email service (SendGrid, AWS SES, etc.)
        logger.info("Sending email to {}: {}", notification.getUserEmail(), notification.getTitle());
        return true;
    }

    private boolean sendSms(Notification notification) {
        // Integrate with SMS service (Twilio, etc.)
        logger.info("Sending SMS to {}: {}", notification.getUserPhone(), notification.getTitle());
        return true;
    }

    private boolean sendPushNotification(Notification notification) {
        // Integrate with push notification service (Firebase, etc.)
        logger.info("Sending push notification to user {}: {}", notification.getUserId(), notification.getTitle());
        return true;
    }
}
