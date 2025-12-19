package com.example.paymentservice.controller;

import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.dto.ProcessPaymentRequest;
import com.example.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {

	@Autowired
	private PaymentService paymentService;

	@PostMapping("/process")
	public ResponseEntity<PaymentResponse> processPayment(
			@RequestHeader("X-User-Id") Long userId,
			@Valid @RequestBody ProcessPaymentRequest request) {
		try {
			PaymentResponse response = paymentService.processPayment(userId, request);
			return ResponseEntity.status(HttpStatus.CREATED).body(response);
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
	}

	@PostMapping("/{id}/refund")
	public ResponseEntity<PaymentResponse> refundPayment(
			@PathVariable Long id,
			@RequestHeader("X-User-Id") Long userId) {
		try {
			PaymentResponse response = paymentService.refundPayment(id, userId);
			return ResponseEntity.ok(response);
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<List<PaymentResponse>> getUserPayments(@PathVariable Long userId) {
		List<PaymentResponse> payments = paymentService.getUserPayments(userId);
		return ResponseEntity.ok(payments);
	}

	@GetMapping("/ticket/{ticketId}")
	public ResponseEntity<PaymentResponse> getPaymentByTicketId(@PathVariable Long ticketId) {
		return paymentService.getPaymentByTicketId(ticketId)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}
}
