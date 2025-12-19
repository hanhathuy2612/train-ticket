package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
	List<Seat> findByTrainId(Long trainId);
	List<Seat> findByTrainIdAndAvailable(Long trainId, Boolean available);
	Optional<Seat> findByTrainIdAndSeatNumber(Long trainId, String seatNumber);
	
	@Query("SELECT COUNT(s) FROM Seat s WHERE s.train.id = :trainId AND s.available = true")
	Long countAvailableSeatsByTrainId(@Param("trainId") Long trainId);
}

