package com.example.inventoryservice.repository;

import java.time.LocalDate;
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
    
    Optional<Inventory> findByTrainIdAndDepartureDate(Long trainId, LocalDate departureDate);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.trainId = :trainId AND i.departureDate = :date")
    Optional<Inventory> findByTrainIdAndDepartureDateWithLock(
            @Param("trainId") Long trainId, 
            @Param("date") LocalDate date);
    
    List<Inventory> findByTrainId(Long trainId);
    
    List<Inventory> findByDepartureDate(LocalDate date);
    
    List<Inventory> findByDepartureDateBetween(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT i FROM Inventory i WHERE i.trainId = :trainId AND i.departureDate >= :startDate")
    List<Inventory> findFutureInventory(@Param("trainId") Long trainId, @Param("startDate") LocalDate startDate);
    
    boolean existsByTrainIdAndDepartureDate(Long trainId, LocalDate departureDate);
}
