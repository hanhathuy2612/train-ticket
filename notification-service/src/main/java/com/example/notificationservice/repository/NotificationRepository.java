package com.example.notificationservice.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.entity.Notification.NotificationChannel;
import com.example.notificationservice.entity.Notification.NotificationStatus;
import com.example.notificationservice.entity.Notification.NotificationType;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // Find by user
    List<Notification> findByUserId(Long userId);
    
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    List<Notification> findByUserIdAndStatus(Long userId, NotificationStatus status);
    
    // Find unread notifications
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.readAt IS NULL ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.readAt IS NULL")
    long countUnreadByUserId(@Param("userId") Long userId);
    
    // Find by type and channel
    List<Notification> findByType(NotificationType type);
    
    List<Notification> findByChannel(NotificationChannel channel);
    
    List<Notification> findByTypeAndChannel(NotificationType type, NotificationChannel channel);
    
    // Find by status
    List<Notification> findByStatus(NotificationStatus status);
    
    Page<Notification> findByStatus(NotificationStatus status, Pageable pageable);
    
    // Find failed notifications for retry
    @Query("SELECT n FROM Notification n WHERE n.status = 'FAILED' AND n.retryCount < 3")
    List<Notification> findFailedForRetry();
    
    // Find by reference
    List<Notification> findByReferenceIdAndReferenceType(Long referenceId, String referenceType);
    
    // Mark as read
    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :readAt, n.status = 'READ' WHERE n.id = :id")
    void markAsRead(@Param("id") Long id, @Param("readAt") LocalDateTime readAt);
    
    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :readAt, n.status = 'READ' WHERE n.userId = :userId AND n.readAt IS NULL")
    void markAllAsRead(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);
    
    // Statistics
    @Query("SELECT n.type, COUNT(n) FROM Notification n WHERE n.userId = :userId GROUP BY n.type")
    List<Object[]> countByTypeForUser(@Param("userId") Long userId);
    
    @Query("SELECT n.status, COUNT(n) FROM Notification n GROUP BY n.status")
    List<Object[]> countByStatus();
    
    // Find by date range
    List<Notification> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    Page<Notification> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
}
