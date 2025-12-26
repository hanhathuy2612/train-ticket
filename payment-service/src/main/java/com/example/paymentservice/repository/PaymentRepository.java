package com.example.paymentservice.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.Payment.PaymentMethod;
import com.example.paymentservice.entity.Payment.PaymentStatus;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    // Find by ticket
    Optional<Payment> findByTicketId(Long ticketId);
    
    Optional<Payment> findByTicketIdAndStatus(Long ticketId, PaymentStatus status);
    
    // Find by transaction
    Optional<Payment> findByTransactionId(String transactionId);
    
    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);
    
    // Find by user
    List<Payment> findByUserId(Long userId);
    
    Page<Payment> findByUserId(Long userId, Pageable pageable);
    
    List<Payment> findByUserIdAndStatus(Long userId, PaymentStatus status);
    
    Page<Payment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    // Find by status
    List<Payment> findByStatus(PaymentStatus status);
    
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);
    
    // Find by payment method
    List<Payment> findByPaymentMethod(PaymentMethod paymentMethod);
    
    // Find by date range
    List<Payment> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    Page<Payment> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    // Statistics
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status")
    long countByStatus(@Param("status") PaymentStatus status);
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED'")
    BigDecimal sumCompletedPayments();
    
    @Query("SELECT SUM(p.refundAmount) FROM Payment p WHERE p.status IN ('REFUNDED', 'PARTIALLY_REFUNDED')")
    BigDecimal sumRefundedAmount();
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.userId = :userId AND p.status = 'COMPLETED'")
    BigDecimal sumCompletedPaymentsByUser(@Param("userId") Long userId);
    
    // Search
    @Query("SELECT p FROM Payment p WHERE " +
           "p.userId = :userId AND " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:paymentMethod IS NULL OR p.paymentMethod = :paymentMethod) " +
           "ORDER BY p.createdAt DESC")
    Page<Payment> searchPayments(
            @Param("userId") Long userId,
            @Param("status") PaymentStatus status,
            @Param("paymentMethod") PaymentMethod paymentMethod,
            Pageable pageable);
    
    // Check existence
    boolean existsByTicketId(Long ticketId);
    
    boolean existsByTransactionId(String transactionId);
    
    // Find pending payments older than specified time
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.createdAt < :cutoffTime")
    List<Payment> findPendingPaymentsOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);
}
