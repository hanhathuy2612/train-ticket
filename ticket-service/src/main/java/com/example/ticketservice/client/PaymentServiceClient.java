package com.example.ticketservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.example.ticketservice.dto.PaymentRequest;
import com.example.ticketservice.dto.PaymentResponse;

@FeignClient(name = "payment-service", fallback = PaymentServiceClientFallback.class)
public interface PaymentServiceClient {

	@PostMapping("/payments/process")
	PaymentResponse processPayment(
			@RequestHeader("X-User-Id") Long userId,
			@RequestBody PaymentRequest request
	);

	@PostMapping("/payments/{paymentId}/refund")
	PaymentResponse refundPayment(
			@PathVariable("paymentId") Long paymentId,
			@RequestHeader("X-User-Id") Long userId
	);

	@GetMapping("/payments/ticket/{ticketId}")
	PaymentResponse getPaymentByTicketId(@PathVariable("ticketId") Long ticketId);
}

