package com.example.paymentservice.controller;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.paymentservice.dto.ApiResponse;
import com.example.paymentservice.dto.PaymentCallbackRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.dto.PaymentStatsResponse;
import com.example.paymentservice.dto.ProcessPaymentRequest;
import com.example.paymentservice.dto.RefundRequest;
import com.example.paymentservice.entity.Payment.PaymentMethod;
import com.example.paymentservice.entity.Payment.PaymentStatus;
import com.example.paymentservice.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final PaymentService paymentService;

    // ============ Payment Processing Endpoints ============

    /**
     * Process a new payment
     * POST /payments/process
     */
    @PostMapping("/process")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ProcessPaymentRequest request) {
        logger.info("Process payment for user: {}, ticket: {}", userId, request.getTicketId());
        PaymentResponse response = paymentService.processPayment(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    /**
     * Handle payment gateway callback
     * POST /payments/callback
     */
    @PostMapping("/callback")
    public ResponseEntity<ApiResponse<PaymentResponse>> handleCallback(
            @RequestBody PaymentCallbackRequest callback) {
        logger.info("Payment callback received for transaction: {}", callback.getTransactionId());
        PaymentResponse response = paymentService.handleCallback(callback);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Refund a payment
     * POST /payments/{id}/refund
     */
    @PostMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody(required = false) RefundRequest request) {
        logger.info("Refund payment: {} for user: {}", id, userId);
        PaymentResponse response;
        if (request != null) {
            response = paymentService.refundPayment(id, userId, request);
        } else {
            response = paymentService.refundPayment(id, userId);
        }
        return ResponseEntity.ok(ApiResponse.success("Refund processed successfully", response));
    }

    // ============ Query Endpoints ============

    /**
     * Get payment by ID
     * GET /payments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable Long id) {
        logger.debug("Get payment by id: {}", id);
        PaymentResponse response = paymentService.getPaymentById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get payment by transaction ID
     * GET /payments/transaction/{transactionId}
     */
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByTransactionId(
            @PathVariable String transactionId) {
        logger.debug("Get payment by transaction: {}", transactionId);
        PaymentResponse response = paymentService.getPaymentByTransactionId(transactionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get payment by ticket ID
     * GET /payments/ticket/{ticketId}
     */
    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByTicketId(
            @PathVariable Long ticketId) {
        logger.debug("Get payment for ticket: {}", ticketId);
        PaymentResponse response = paymentService.getPaymentByTicketId(ticketId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get current user's payments
     * GET /payments/my
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getMyPayments(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        logger.debug("Get payments for user: {}", userId);
        
        size = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<PaymentResponse> payments = paymentService.getUserPayments(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    /**
     * Search user's payments
     * GET /payments/my/search
     */
    @GetMapping("/my/search")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> searchMyPayments(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) PaymentMethod method,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        logger.debug("Search payments for user: {}", userId);
        
        size = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);
        
        Page<PaymentResponse> payments = paymentService.searchPayments(userId, status, method, pageable);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    /**
     * Get current user's payment statistics
     * GET /payments/my/stats
     */
    @GetMapping("/my/stats")
    public ResponseEntity<ApiResponse<PaymentStatsResponse>> getMyPaymentStats(
            @RequestHeader("X-User-Id") Long userId) {
        logger.debug("Get payment stats for user: {}", userId);
        PaymentStatsResponse stats = paymentService.getUserPaymentStats(userId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Get payments for a specific user (admin/internal)
     * GET /payments/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getUserPayments(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        logger.debug("Get payments for user: {}", userId);
        
        size = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<PaymentResponse> payments = paymentService.getUserPayments(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    // ============ Admin Endpoints ============

    /**
     * Get all payments (admin only)
     * GET /payments
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        logger.debug("Get all payments - page: {}, size: {}", page, size);
        
        size = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<PaymentResponse> payments = paymentService.getAllPayments(pageable);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    /**
     * Get payments by status (admin only)
     * GET /payments/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getPaymentsByStatus(
            @PathVariable PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        logger.debug("Get payments by status: {}", status);
        
        size = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<PaymentResponse> payments = paymentService.getPaymentsByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    /**
     * Get global payment statistics (admin only)
     * GET /payments/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<PaymentStatsResponse>> getGlobalPaymentStats() {
        logger.debug("Get global payment stats");
        PaymentStatsResponse stats = paymentService.getGlobalPaymentStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Health check
     * GET /payments/health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Payment Service is healthy"));
    }
}
