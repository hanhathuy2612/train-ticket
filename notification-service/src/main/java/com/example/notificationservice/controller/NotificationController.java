package com.example.notificationservice.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.notificationservice.dto.ApiResponse;
import com.example.notificationservice.dto.NotificationPreferenceResponse;
import com.example.notificationservice.dto.NotificationResponse;
import com.example.notificationservice.dto.SendNotificationRequest;
import com.example.notificationservice.dto.UpdatePreferenceRequest;
import com.example.notificationservice.service.NotificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationService notificationService;

    // ============ User Notification Endpoints ============

    /**
     * Get current user's notifications
     * GET /notifications/my
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getMyNotifications(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        logger.debug("Get notifications for user: {}", userId);
        
        size = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<NotificationResponse> notifications = notificationService.getUserNotifications(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    /**
     * Get unread notifications
     * GET /notifications/my/unread
     */
    @GetMapping("/my/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnreadNotifications(
            @RequestHeader("X-User-Id") Long userId) {
        logger.debug("Get unread notifications for user: {}", userId);
        List<NotificationResponse> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    /**
     * Get unread notification count
     * GET /notifications/my/unread/count
     */
    @GetMapping("/my/unread/count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @RequestHeader("X-User-Id") Long userId) {
        logger.debug("Get unread count for user: {}", userId);
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * Get notification by ID
     * GET /notifications/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotificationById(
            @PathVariable Long id) {
        logger.debug("Get notification by id: {}", id);
        NotificationResponse response = notificationService.getNotificationById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Mark notification as read
     * POST /notifications/{id}/read
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        logger.debug("Mark notification {} as read", id);
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    /**
     * Mark all notifications as read
     * POST /notifications/my/read-all
     */
    @PostMapping("/my/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @RequestHeader("X-User-Id") Long userId) {
        logger.debug("Mark all notifications as read for user: {}", userId);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    // ============ Preference Endpoints ============

    /**
     * Get user's notification preferences
     * GET /notifications/preferences
     */
    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> getPreferences(
            @RequestHeader("X-User-Id") Long userId) {
        logger.debug("Get preferences for user: {}", userId);
        NotificationPreferenceResponse preferences = notificationService.getUserPreferences(userId);
        return ResponseEntity.ok(ApiResponse.success(preferences));
    }

    /**
     * Update notification preferences
     * PUT /notifications/preferences
     */
    @PutMapping("/preferences")
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> updatePreferences(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody UpdatePreferenceRequest request) {
        logger.info("Update preferences for user: {}", userId);
        NotificationPreferenceResponse preferences = notificationService.updatePreferences(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Preferences updated", preferences));
    }

    // ============ Admin/Internal Endpoints ============

    /**
     * Send a notification (internal use)
     * POST /notifications/send
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendNotification(
            @Valid @RequestBody SendNotificationRequest request) {
        logger.info("Send notification to user: {}", request.getUserId());
        NotificationResponse response = notificationService.sendNotification(request);
        if (response == null) {
            return ResponseEntity.ok(ApiResponse.success("Notification skipped due to user preferences", null));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    /**
     * Get notifications for a specific user (admin)
     * GET /notifications/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getUserNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        logger.debug("Get notifications for user: {}", userId);
        
        size = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<NotificationResponse> notifications = notificationService.getUserNotifications(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    /**
     * Retry failed notifications (admin)
     * POST /notifications/retry-failed
     */
    @PostMapping("/retry-failed")
    public ResponseEntity<ApiResponse<Void>> retryFailedNotifications() {
        logger.info("Retry failed notifications");
        notificationService.retryFailedNotifications();
        return ResponseEntity.ok(ApiResponse.success("Failed notifications retried", null));
    }

    /**
     * Health check
     * GET /notifications/health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Notification Service is healthy"));
    }
}

