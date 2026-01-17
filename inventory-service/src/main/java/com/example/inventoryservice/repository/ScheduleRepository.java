package com.example.inventoryservice.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.inventoryservice.entity.Schedule;
import com.example.inventoryservice.entity.Schedule.ScheduleStatus;

import jakarta.persistence.LockModeType;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

        Optional<Schedule> findByTrainIdAndDepartureDate(Long trainId, Instant departureDate);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT s FROM Schedule s WHERE s.train.id = :trainId AND s.departureDate = :date")
        Optional<Schedule> findByTrainIdAndDepartureDateWithLock(
                        @Param("trainId") Long trainId,
                        @Param("date") Instant date);

        List<Schedule> findByTrainId(Long trainId);

        @Query("SELECT s FROM Schedule s WHERE s.departureDate >= :startOfDay AND s.departureDate < :endOfDay")
        List<Schedule> findByDepartureDate(@Param("startOfDay") Instant startOfDay,
                        @Param("endOfDay") Instant endOfDay);

        @Query("SELECT s FROM Schedule s WHERE s.departureDate >= :startOfDay AND s.departureDate < :endOfDay")
        Page<Schedule> findByDepartureDate(@Param("startOfDay") Instant startOfDay, @Param("endOfDay") Instant endOfDay,
                        Pageable pageable);

        @Query("SELECT s FROM Schedule s WHERE s.departureDate BETWEEN :startDate AND :endDate")
        List<Schedule> findByDepartureDateBetween(@Param("startDate") Instant startDate,
                        @Param("endDate") Instant endDate);

        @Query("SELECT s FROM Schedule s JOIN s.train t JOIN t.route r WHERE " +
                        "LOWER(r.origin) = LOWER(:origin) AND " +
                        "LOWER(r.destination) = LOWER(:destination) AND " +
                        "s.departureDate >= :startOfDay AND s.departureDate < :endOfDay AND " +
                        "s.status = 'SCHEDULED' " +
                        "ORDER BY t.departureTime")
        List<Schedule> findAvailableSchedules(
                        @Param("origin") String origin,
                        @Param("destination") String destination,
                        @Param("startOfDay") Instant startOfDay,
                        @Param("endOfDay") Instant endOfDay);

        @Query("SELECT s FROM Schedule s JOIN s.train t JOIN t.route r WHERE " +
                        "LOWER(r.origin) = LOWER(:origin) AND " +
                        "LOWER(r.destination) = LOWER(:destination) AND " +
                        "s.departureDate >= :startOfDay AND s.departureDate < :endOfDay AND " +
                        "s.status = 'SCHEDULED' AND " +
                        "(s.availableEconomySeats + s.availableBusinessSeats + s.availableFirstClassSeats) >= :seats " +
                        "ORDER BY t.departureTime")
        List<Schedule> findAvailableSchedulesWithSeats(
                        @Param("origin") String origin,
                        @Param("destination") String destination,
                        @Param("startOfDay") Instant startOfDay,
                        @Param("endOfDay") Instant endOfDay,
                        @Param("seats") int seats);

        @Modifying
        @Query("UPDATE Schedule s SET s.status = :status WHERE s.id = :id")
        void updateStatus(@Param("id") Long id, @Param("status") ScheduleStatus status);

        @Query("SELECT s FROM Schedule s WHERE s.departureDate < :date AND s.status = 'SCHEDULED'")
        List<Schedule> findPastScheduledSchedules(@Param("date") Instant date);

        boolean existsByTrainIdAndDepartureDate(Long trainId, Instant departureDate);
}
