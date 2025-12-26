package com.example.inventoryservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.inventoryservice.entity.Train;
import com.example.inventoryservice.entity.Train.TrainType;

@Repository
public interface TrainRepository extends JpaRepository<Train, Long> {
    
    Optional<Train> findByTrainNumber(String trainNumber);
    
    List<Train> findByRouteId(Long routeId);
    
    List<Train> findByActiveTrue();
    
    Page<Train> findByActiveTrue(Pageable pageable);
    
    List<Train> findByTrainType(TrainType trainType);
    
    @Query("SELECT t FROM Train t JOIN t.route r WHERE " +
           "LOWER(r.origin) = LOWER(:origin) AND " +
           "LOWER(r.destination) = LOWER(:destination) AND " +
           "t.active = true ORDER BY t.departureTime")
    List<Train> findByRouteOriginAndDestination(
            @Param("origin") String origin, 
            @Param("destination") String destination);
    
    @Query("SELECT t FROM Train t WHERE t.active = true AND " +
           "(LOWER(t.trainNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(t.trainName) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Train> search(@Param("query") String query, Pageable pageable);
    
    boolean existsByTrainNumber(String trainNumber);
    
    @Query("SELECT COUNT(t) FROM Train t WHERE t.route.id = :routeId AND t.active = true")
    long countActiveByRouteId(@Param("routeId") Long routeId);
}
