package com.example.paymentservice.repository;

import com.example.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
	List<Payment> findByUserId(Long userId);
	Optional<Payment> findByTicketId(Long ticketId);
	List<Payment> findByUserIdAndStatus(Long userId, Payment.PaymentStatus status);
}

