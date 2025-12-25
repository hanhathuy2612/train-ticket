package com.example.ticketservice.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.ticketservice.entity.Ticket;
import com.example.ticketservice.entity.Ticket.TicketStatus;

import jakarta.persistence.LockModeType;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

	// Basic queries
	List<Ticket> findByUserId(Long userId);
	
	List<Ticket> findByUserIdAndStatus(Long userId, TicketStatus status);
	
	Optional<Ticket> findByIdAndUserId(Long id, Long userId);

	// Paginated queries
	Page<Ticket> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
	
	Page<Ticket> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, TicketStatus status, Pageable pageable);

	// Pessimistic lock for updates
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT t FROM Ticket t WHERE t.id = :id AND t.userId = :userId")
	Optional<Ticket> findByIdAndUserIdWithLock(@Param("id") Long id, @Param("userId") Long userId);

	// Count queries
	long countByUserId(Long userId);
	
	long countByUserIdAndStatus(Long userId, TicketStatus status);

	// Search with filters
	@Query("SELECT t FROM Ticket t WHERE t.userId = :userId " +
			"AND (:trainId IS NULL OR t.trainId = :trainId) " +
			"AND (:status IS NULL OR t.status = :status) " +
			"ORDER BY t.createdAt DESC")
	Page<Ticket> searchTickets(
			@Param("userId") Long userId,
			@Param("trainId") Long trainId,
			@Param("status") TicketStatus status,
			Pageable pageable
	);

	// Find pending tickets older than specified time (for timeout handling)
	@Query("SELECT t FROM Ticket t WHERE t.status = :status AND t.createdAt < :cutoffTime")
	List<Ticket> findByStatusAndCreatedAtBefore(
			@Param("status") TicketStatus status,
			@Param("cutoffTime") LocalDateTime cutoffTime
	);

	// Find tickets by train and departure date
	List<Ticket> findByTrainIdAndDepartureDateAndStatusIn(
			Long trainId, 
			String departureDate, 
			List<TicketStatus> statuses
	);

	// Check if user has active ticket for same train and date
	@Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Ticket t " +
			"WHERE t.userId = :userId AND t.trainId = :trainId " +
			"AND t.departureDate = :departureDate " +
			"AND t.status IN ('PENDING', 'CONFIRMED')")
	boolean existsActiveTicketForUser(
			@Param("userId") Long userId,
			@Param("trainId") Long trainId,
			@Param("departureDate") String departureDate
	);

	// Statistics
	@Query("SELECT t.status, COUNT(t) FROM Ticket t WHERE t.userId = :userId GROUP BY t.status")
	List<Object[]> getTicketStatsByUser(@Param("userId") Long userId);
}
