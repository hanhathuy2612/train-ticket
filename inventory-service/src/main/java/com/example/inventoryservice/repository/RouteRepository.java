package com.example.inventoryservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.inventoryservice.entity.Route;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {
    
    Optional<Route> findByOriginAndDestination(String origin, String destination);
    
    List<Route> findByOriginIgnoreCase(String origin);
    
    List<Route> findByDestinationIgnoreCase(String destination);
    
    List<Route> findByActiveTrue();
    
    Page<Route> findByActiveTrue(Pageable pageable);
    
    @Query("SELECT r FROM Route r WHERE r.active = true AND " +
           "(LOWER(r.origin) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(r.destination) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Route> search(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT DISTINCT r.origin FROM Route r WHERE r.active = true ORDER BY r.origin")
    List<String> findAllOrigins();
    
    @Query("SELECT DISTINCT r.destination FROM Route r WHERE r.active = true ORDER BY r.destination")
    List<String> findAllDestinations();
    
    boolean existsByOriginIgnoreCaseAndDestinationIgnoreCase(String origin, String destination);
}

