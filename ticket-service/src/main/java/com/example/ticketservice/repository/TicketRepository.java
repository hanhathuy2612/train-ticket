package com.example.ticketservice.repository;

import com.example.ticketservice.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
	List<Ticket> findByUserId(Long userId);
	List<Ticket> findByUserIdAndStatus(Long userId, Ticket.TicketStatus status);
	Optional<Ticket> findByIdAndUserId(Long id, Long userId);
}

