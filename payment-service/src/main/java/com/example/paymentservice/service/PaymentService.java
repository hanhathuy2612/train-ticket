package com.example.paymentservice.service;

import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.dto.ProcessPaymentRequest;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.repository.PaymentRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class PaymentService {

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	private static final String PAYMENT_EXCHANGE = "payment.exchange";
	private static final String NOTIFICATION_ROUTING_KEY = "notification.payment";

	public PaymentResponse processPayment(Long userId, ProcessPaymentRequest request) {
		// Check if payment already exists for this ticket
		Optional<Payment> existingPayment = paymentRepository.findByTicketId(request.getTicketId());
		if (existingPayment.isPresent()) {
			throw new RuntimeException("Payment already exists for this ticket");
		}

		Payment payment = new Payment();
		payment.setTicketId(request.getTicketId());
		payment.setUserId(userId);
		payment.setAmount(request.getAmount());
		payment.setPaymentMethod(request.getPaymentMethod());
		payment.setStatus(Payment.PaymentStatus.PENDING);

		// Simulate payment processing
		boolean paymentSuccess = processPaymentWithGateway(request);
		
		if (paymentSuccess) {
			payment.setStatus(Payment.PaymentStatus.COMPLETED);
		} else {
			payment.setStatus(Payment.PaymentStatus.FAILED);
		}

		payment = paymentRepository.save(payment);

		// Send notification event
		Map<String, Object> notificationEvent = new HashMap<>();
		notificationEvent.put("paymentId", payment.getId());
		notificationEvent.put("ticketId", request.getTicketId());
		notificationEvent.put("userId", userId);
		notificationEvent.put("amount", request.getAmount());
		notificationEvent.put("status", payment.getStatus().toString());
		rabbitTemplate.convertAndSend(PAYMENT_EXCHANGE, NOTIFICATION_ROUTING_KEY, notificationEvent);

		return new PaymentResponse(payment);
	}

	public PaymentResponse refundPayment(Long paymentId, Long userId) {
		Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
		if (paymentOpt.isEmpty() || !paymentOpt.get().getUserId().equals(userId)) {
			throw new RuntimeException("Payment not found");
		}

		Payment payment = paymentOpt.get();
		if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
			throw new RuntimeException("Only completed payments can be refunded");
		}

		// Simulate refund processing
		payment.setStatus(Payment.PaymentStatus.REFUNDED);
		payment = paymentRepository.save(payment);

		return new PaymentResponse(payment);
	}

	public List<PaymentResponse> getUserPayments(Long userId) {
		List<Payment> payments = paymentRepository.findByUserId(userId);
		return payments.stream()
				.map(PaymentResponse::new)
				.collect(Collectors.toList());
	}

	public Optional<PaymentResponse> getPaymentByTicketId(Long ticketId) {
		return paymentRepository.findByTicketId(ticketId)
				.map(PaymentResponse::new);
	}

	private boolean processPaymentWithGateway(ProcessPaymentRequest request) {
		// Simulate payment gateway processing
		// In real implementation, this would call actual payment gateway API
		return true; // Simulate success
	}
}

