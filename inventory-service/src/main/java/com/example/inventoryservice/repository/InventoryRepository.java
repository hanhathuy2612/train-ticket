package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
	
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT i FROM Inventory i WHERE i.trainId = :trainId AND DATE(i.departureDate) = DATE(:departureDate)")
	Optional<Inventory> findByTrainIdAndDepartureDateWithLock(
			@Param("trainId") Long trainId, 
			@Param("departureDate") LocalDateTime departureDate
	);
	
	Optional<Inventory> findByTrainIdAndDepartureDate(Long trainId, LocalDateTime departureDate);
}

