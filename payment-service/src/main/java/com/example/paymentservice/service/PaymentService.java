package com.example.paymentservice.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.paymentservice.dto.PaymentCallbackRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.dto.PaymentStatsResponse;
import com.example.paymentservice.dto.ProcessPaymentRequest;
import com.example.paymentservice.dto.RefundRequest;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.Payment.PaymentMethod;
import com.example.paymentservice.entity.Payment.PaymentStatus;
import com.example.paymentservice.exception.DuplicatePaymentException;
import com.example.paymentservice.exception.PaymentNotFoundException;
import com.example.paymentservice.exception.PaymentProcessingException;
import com.example.paymentservice.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private static final String PAYMENT_TOPIC = "payment-events";

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ============ Payment Processing ============

    @Transactional
    public PaymentResponse processPayment(Long userId, ProcessPaymentRequest request) {
        logger.info("Processing payment for user: {}, ticket: {}, amount: {}", 
                userId, request.getTicketId(), request.getAmount());

        // Check for duplicate payment
        if (paymentRepository.existsByTicketId(request.getTicketId())) {
            Payment existing = paymentRepository.findByTicketId(request.getTicketId())
                    .orElseThrow(() -> new PaymentNotFoundException(request.getTicketId()));
            if (existing.getStatus() == PaymentStatus.COMPLETED) {
                throw new DuplicatePaymentException(request.getTicketId());
            }
            // If previous payment failed, allow retry
            if (existing.getStatus() != PaymentStatus.FAILED) {
                throw new PaymentProcessingException("Payment is already in progress");
            }
            // Delete failed payment and allow retry
            paymentRepository.delete(existing);
        }

        // Create payment record
        Payment payment = Payment.builder()
                .ticketId(request.getTicketId())
                .userId(userId)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PROCESSING)
                .build();

        payment = paymentRepository.save(payment);
        logger.info("Payment created with id: {}, transaction: {}", payment.getId(), payment.getTransactionId());

        // Process payment based on method
        try {
            boolean success = processWithGateway(payment, request);
            
            if (success) {
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setPaidAt(LocalDateTime.now());
                payment.setGatewayTransactionId(generateGatewayTransactionId());
                payment.setGatewayProvider(getGatewayProvider(request.getPaymentMethod()));
                logger.info("Payment {} completed successfully", payment.getId());
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Payment declined by gateway");
                logger.warn("Payment {} failed", payment.getId());
            }
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            logger.error("Payment {} failed with error: {}", payment.getId(), e.getMessage());
        }

        payment = paymentRepository.save(payment);

        // Publish event
        publishPaymentEvent(payment);

        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse handleCallback(PaymentCallbackRequest callback) {
        logger.info("Handling payment callback for transaction: {}", callback.getTransactionId());

        Payment payment = paymentRepository.findByTransactionId(callback.getTransactionId())
                .orElseThrow(() -> new PaymentNotFoundException(callback.getTransactionId()));

        if ("SUCCESS".equalsIgnoreCase(callback.getStatus())) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaidAt(LocalDateTime.now());
            payment.setGatewayTransactionId(callback.getGatewayTransactionId());
        } else if ("FAILED".equalsIgnoreCase(callback.getStatus())) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(callback.getResponseMessage());
        } else if ("CANCELLED".equalsIgnoreCase(callback.getStatus())) {
            payment.setStatus(PaymentStatus.CANCELLED);
        }

        payment = paymentRepository.save(payment);
        publishPaymentEvent(payment);

        return PaymentResponse.from(payment);
    }

    // ============ Refund Processing ============

    @Transactional
    public PaymentResponse refundPayment(Long paymentId, Long userId, RefundRequest request) {
        logger.info("Processing refund for payment: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        // Verify ownership
        if (!payment.getUserId().equals(userId)) {
            throw new PaymentProcessingException("Unauthorized to refund this payment");
        }

        if (!payment.canBeRefunded()) {
            throw new PaymentProcessingException("Payment cannot be refunded. Current status: " + payment.getStatus());
        }

        BigDecimal refundAmount;
        if (request.getFullRefund()) {
            refundAmount = payment.getAmount();
            payment.setStatus(PaymentStatus.REFUNDED);
        } else {
            refundAmount = request.getAmount();
            if (refundAmount.compareTo(payment.getAmount()) > 0) {
                throw new PaymentProcessingException("Refund amount exceeds payment amount");
            }
            if (refundAmount.compareTo(payment.getAmount()) == 0) {
                payment.setStatus(PaymentStatus.REFUNDED);
            } else {
                payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
            }
        }

        payment.setRefundAmount(refundAmount);
        payment.setRefundReason(request.getReason());
        payment.setRefundedAt(LocalDateTime.now());

        payment = paymentRepository.save(payment);
        logger.info("Refund processed for payment {}: {}", paymentId, refundAmount);

        // Publish refund event
        publishPaymentEvent(payment);

        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse refundPayment(Long paymentId, Long userId) {
        return refundPayment(paymentId, userId, RefundRequest.builder()
                .fullRefund(true)
                .reason("User requested refund")
                .build());
    }

    // ============ Query Methods ============

    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        return PaymentResponse.from(payment);
    }

    public PaymentResponse getPaymentByTransactionId(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new PaymentNotFoundException(transactionId));
        return PaymentResponse.from(payment);
    }

    public PaymentResponse getPaymentByTicketId(Long ticketId) {
        Payment payment = paymentRepository.findByTicketId(ticketId)
                .orElseThrow(() -> new PaymentNotFoundException(ticketId));
        return PaymentResponse.from(payment);
    }

    public List<PaymentResponse> getUserPayments(Long userId) {
        return paymentRepository.findByUserId(userId).stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }

    public Page<PaymentResponse> getUserPayments(Long userId, Pageable pageable) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(PaymentResponse::from);
    }

    public Page<PaymentResponse> searchPayments(Long userId, PaymentStatus status, 
            PaymentMethod method, Pageable pageable) {
        return paymentRepository.searchPayments(userId, status, method, pageable)
                .map(PaymentResponse::from);
    }

    public PaymentStatsResponse getUserPaymentStats(Long userId) {
        List<Payment> payments = paymentRepository.findByUserId(userId);
        
        long completed = payments.stream().filter(p -> p.getStatus() == PaymentStatus.COMPLETED).count();
        long failed = payments.stream().filter(p -> p.getStatus() == PaymentStatus.FAILED).count();
        long refunded = payments.stream().filter(p -> 
                p.getStatus() == PaymentStatus.REFUNDED || p.getStatus() == PaymentStatus.PARTIALLY_REFUNDED).count();
        long pending = payments.stream().filter(p -> 
                p.getStatus() == PaymentStatus.PENDING || p.getStatus() == PaymentStatus.PROCESSING).count();
        
        BigDecimal totalAmount = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalRefunded = payments.stream()
                .filter(p -> p.getRefundAmount() != null)
                .map(Payment::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PaymentStatsResponse.builder()
                .totalPayments((long) payments.size())
                .completedPayments(completed)
                .failedPayments(failed)
                .refundedPayments(refunded)
                .pendingPayments(pending)
                .totalAmount(totalAmount)
                .totalRefunded(totalRefunded)
                .netAmount(totalAmount.subtract(totalRefunded))
                .build();
    }

    // ============ Admin Methods ============

    public Page<PaymentResponse> getAllPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable)
                .map(PaymentResponse::from);
    }

    public Page<PaymentResponse> getPaymentsByStatus(PaymentStatus status, Pageable pageable) {
        return paymentRepository.findByStatus(status, pageable)
                .map(PaymentResponse::from);
    }

    public PaymentStatsResponse getGlobalPaymentStats() {
        long total = paymentRepository.count();
        long completed = paymentRepository.countByStatus(PaymentStatus.COMPLETED);
        long failed = paymentRepository.countByStatus(PaymentStatus.FAILED);
        long pending = paymentRepository.countByStatus(PaymentStatus.PENDING);
        long refunded = paymentRepository.countByStatus(PaymentStatus.REFUNDED) + 
                       paymentRepository.countByStatus(PaymentStatus.PARTIALLY_REFUNDED);
        
        BigDecimal totalAmount = paymentRepository.sumCompletedPayments();
        BigDecimal totalRefunded = paymentRepository.sumRefundedAmount();

        return PaymentStatsResponse.builder()
                .totalPayments(total)
                .completedPayments(completed)
                .failedPayments(failed)
                .refundedPayments(refunded)
                .pendingPayments(pending)
                .totalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO)
                .totalRefunded(totalRefunded != null ? totalRefunded : BigDecimal.ZERO)
                .netAmount(totalAmount != null && totalRefunded != null 
                        ? totalAmount.subtract(totalRefunded) : BigDecimal.ZERO)
                .build();
    }

    // ============ Helper Methods ============

    private boolean processWithGateway(Payment payment, ProcessPaymentRequest request) {
        // Simulate payment gateway processing
        // In real implementation, this would call actual payment gateway API
        logger.debug("Processing payment with gateway: {}", request.getPaymentMethod());
        
        try {
            Thread.sleep(100); // Simulate network delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simulate 95% success rate
        return Math.random() > 0.05;
    }

    private String generateGatewayTransactionId() {
        return "GW" + System.currentTimeMillis() + (int) (Math.random() * 10000);
    }

    private String getGatewayProvider(PaymentMethod method) {
        return switch (method) {
            case VNPAY -> "VNPay";
            case MOMO -> "MoMo";
            case ZALOPAY -> "ZaloPay";
            case CREDIT_CARD, DEBIT_CARD -> "Stripe";
            case BANK_TRANSFER -> "Local Bank";
            default -> "Internal";
        };
    }

    private void publishPaymentEvent(Payment payment) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "PAYMENT_" + payment.getStatus().name());
            event.put("paymentId", payment.getId());
            event.put("ticketId", payment.getTicketId());
            event.put("userId", payment.getUserId());
            event.put("amount", payment.getAmount());
            event.put("status", payment.getStatus().name());
            event.put("transactionId", payment.getTransactionId());
            event.put("timestamp", LocalDateTime.now().toString());

            kafkaTemplate.send(PAYMENT_TOPIC, event);
            logger.debug("Published payment event for payment: {}", payment.getId());
        } catch (Exception e) {
            logger.error("Failed to publish payment event for payment: {}", payment.getId(), e);
        }
    }
}
