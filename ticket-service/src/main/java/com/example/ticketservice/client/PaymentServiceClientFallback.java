package com.example.ticketservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.ticketservice.dto.PaymentRequest;
import com.example.ticketservice.dto.PaymentResponse;

@Component
public class PaymentServiceClientFallback implements PaymentServiceClient {
	
	private static final Logger logger = LoggerFactory.getLogger(PaymentServiceClientFallback.class);

	@Override
	public PaymentResponse processPayment(Long userId, PaymentRequest request) {
		logger.error("Fallback: Payment service is unavailable for processPayment. TicketId: {}", request.getTicketId());
		return null;
	}

	@Override
	public PaymentResponse refundPayment(Long paymentId, Long userId) {
		logger.error("Fallback: Payment service is unavailable for refundPayment. PaymentId: {}", paymentId);
		return null;
	}

	@Override
	public PaymentResponse getPaymentByTicketId(Long ticketId) {
		logger.error("Fallback: Payment service is unavailable for getPaymentByTicketId. TicketId: {}", ticketId);
		return null;
	}
}

