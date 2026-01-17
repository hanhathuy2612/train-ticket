package com.example.inventoryservice.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.inventoryservice.entity.Inventory;

import jakarta.persistence.LockModeType;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    
    Optional<Inventory> findByTrainIdAndDepartureDate(Long trainId, Instant departureDate);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.trainId = :trainId AND i.departureDate = :date")
    Optional<Inventory> findByTrainIdAndDepartureDateWithLock(
            @Param("trainId") Long trainId, 
            @Param("date") Instant date);
    
    List<Inventory> findByTrainId(Long trainId);
    
    @Query("SELECT i FROM Inventory i WHERE i.departureDate >= :startOfDay AND i.departureDate < :endOfDay")
    List<Inventory> findByDepartureDate(@Param("startOfDay") Instant startOfDay, @Param("endOfDay") Instant endOfDay);
    
    @Query("SELECT i FROM Inventory i WHERE i.departureDate BETWEEN :startDate AND :endDate")
    List<Inventory> findByDepartureDateBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
    
    @Query("SELECT i FROM Inventory i WHERE i.trainId = :trainId AND i.departureDate >= :startDate")
    List<Inventory> findFutureInventory(@Param("trainId") Long trainId, @Param("startDate") Instant startDate);
    
    boolean existsByTrainIdAndDepartureDate(Long trainId, Instant departureDate);
}
