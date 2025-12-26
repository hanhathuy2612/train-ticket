package com.example.inventoryservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.inventoryservice.entity.Seat;
import com.example.inventoryservice.entity.Seat.SeatClass;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    
    List<Seat> findByTrainId(Long trainId);
    
    List<Seat> findByTrainIdAndSeatClass(Long trainId, SeatClass seatClass);
    
    List<Seat> findByTrainIdAndAvailableTrue(Long trainId);
    
    List<Seat> findByTrainIdAndSeatClassAndAvailableTrue(Long trainId, SeatClass seatClass);
    
    Optional<Seat> findByTrainIdAndSeatNumber(Long trainId, String seatNumber);
    
    @Query("SELECT s FROM Seat s WHERE s.train.id = :trainId AND s.seatNumber IN :seatNumbers")
    List<Seat> findByTrainIdAndSeatNumbers(
            @Param("trainId") Long trainId, 
            @Param("seatNumbers") List<String> seatNumbers);
    
    @Query("SELECT COUNT(s) FROM Seat s WHERE s.train.id = :trainId AND s.seatClass = :seatClass AND s.available = true")
    int countAvailableByTrainIdAndSeatClass(
            @Param("trainId") Long trainId, 
            @Param("seatClass") SeatClass seatClass);
    
    @Modifying
    @Query("UPDATE Seat s SET s.available = :available WHERE s.train.id = :trainId AND s.seatNumber IN :seatNumbers")
    int updateAvailability(
            @Param("trainId") Long trainId, 
            @Param("seatNumbers") List<String> seatNumbers, 
            @Param("available") boolean available);
    
    boolean existsByTrainIdAndSeatNumber(Long trainId, String seatNumber);
}
