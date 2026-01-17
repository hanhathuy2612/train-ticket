package com.example.inventoryservice.service;

import com.example.inventoryservice.dto.AvailabilityResponse;
import com.example.inventoryservice.dto.ReserveSeatRequest;
import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.entity.Schedule;
import com.example.inventoryservice.entity.Train;
import com.example.inventoryservice.exception.InventoryErrorCode;
import com.example.inventoryservice.repository.InventoryRepository;
import com.example.inventoryservice.repository.ScheduleRepository;
import com.example.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {
    private static final String LOCK_PREFIX = "inventory:lock:";
    private static final String CACHE_PREFIX = "inventory:";
    private static final int LOCK_WAIT_TIME = 5;
    private static final int LOCK_LEASE_TIME = 10;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final InventoryRepository inventoryRepository;
    private final ScheduleRepository scheduleRepository;
    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Object> redisTemplate;

    @Cacheable(value = "inventory", key = "#trainId + ':' + #departureDate")
    public AvailabilityResponse checkAvailability(Long trainId, String departureDate) {
        log.debug("Checking availability for train {} on {}", trainId, departureDate);

        Instant date = parseDate(departureDate);

        Optional<Schedule> scheduleOpt = scheduleRepository.findByTrainIdAndDepartureDate(trainId, date);
        if (scheduleOpt.isEmpty()) {
            log.debug("No schedule found for train {} on {}", trainId, date);
            return AvailabilityResponse.builder()
                    .trainId(trainId)
                    .totalSeats(0)
                    .availableSeats(0)
                    .reservedSeats(0)
                    .status("NOT_AVAILABLE")
                    .build();
        }

        Schedule schedule = scheduleOpt.get();
        Train train = schedule.getTrain();

        return AvailabilityResponse.builder()
                .trainId(trainId)
                .trainNumber(train.getTrainNumber())
                .trainName(train.getTrainName())
                .origin(train.getRoute().getOrigin())
                .destination(train.getRoute().getDestination())
                .departureDate(date)
                .departureTime(train.getDepartureTime())
                .arrivalTime(train.getArrivalTime())
                .totalSeats(train.getTotalSeats())
                .availableSeats(schedule.getTotalAvailableSeats())
                .reservedSeats(schedule.getReservedSeats())
                .seatAvailability(AvailabilityResponse.SeatAvailability.builder()
                        .economy(schedule.getAvailableEconomySeats())
                        .business(schedule.getAvailableBusinessSeats())
                        .firstClass(schedule.getAvailableFirstClassSeats())
                        .build())
                .prices(AvailabilityResponse.PriceInfo.builder()
                        .economy(train.getEconomyPrice())
                        .business(train.getBusinessPrice())
                        .firstClass(train.getFirstClassPrice())
                        .build())
                .status(schedule.getStatus().name())
                .build();
    }

    @Transactional
    public boolean reserveSeats(ReserveSeatRequest request) {
        String lockKey = LOCK_PREFIX + request.getTrainId() + ":" + request.getDepartureDate();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                try {
                    return doReserveSeats(request);
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("Could not acquire lock for reservation: {}", lockKey);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while acquiring lock for reservation", e);
            return false;
        }
    }

    @Transactional
    public boolean releaseSeats(Long trainId, String departureDate, Integer numberOfSeats) {
        String lockKey = LOCK_PREFIX + trainId + ":" + departureDate;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                try {
                    return doReleaseSeats(trainId, departureDate, numberOfSeats, null);
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("Could not acquire lock for release: {}", lockKey);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while acquiring lock for release", e);
            return false;
        }
    }

    @Transactional
    public boolean releaseSeats(Long trainId, String departureDate, Integer numberOfSeats, String seatClass) {
        String lockKey = LOCK_PREFIX + trainId + ":" + departureDate;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                try {
                    return doReleaseSeats(trainId, departureDate, numberOfSeats, seatClass);
                } finally {
                    lock.unlock();
                }
            } else {
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean doReserveSeats(ReserveSeatRequest request) {
        Instant date = request.getDepartureDate();

        Schedule schedule = scheduleRepository.findByTrainIdAndDepartureDateWithLock(
                        request.getTrainId(), date)
                .orElseThrow(() -> new BusinessException(InventoryErrorCode.SCHEDULE_NOT_FOUND));

        String seatClass = request.getSeatClass() != null ? request.getSeatClass().toUpperCase() : "ECONOMY";
        int requested = request.getNumberOfSeats();

        // Check availability based on seat class
        boolean hasSeats = switch (seatClass) {
            case "ECONOMY" -> schedule.getAvailableEconomySeats() >= requested;
            case "BUSINESS" -> schedule.getAvailableBusinessSeats() >= requested;
            case "FIRST" -> schedule.getAvailableFirstClassSeats() >= requested;
            default -> schedule.getTotalAvailableSeats() >= requested;
        };

        if (!hasSeats) {
            int available = switch (seatClass) {
                case "ECONOMY" -> schedule.getAvailableEconomySeats();
                case "BUSINESS" -> schedule.getAvailableBusinessSeats();
                case "FIRST" -> schedule.getAvailableFirstClassSeats();
                default -> schedule.getTotalAvailableSeats();
            };
            throw new BusinessException(InventoryErrorCode.INSUFFICIENT_SEATS);
        }

        // Update schedule
        switch (seatClass) {
            case "ECONOMY" -> schedule.setAvailableEconomySeats(
                    schedule.getAvailableEconomySeats() - requested);
            case "BUSINESS" -> schedule.setAvailableBusinessSeats(
                    schedule.getAvailableBusinessSeats() - requested);
            case "FIRST" -> schedule.setAvailableFirstClassSeats(
                    schedule.getAvailableFirstClassSeats() - requested);
            default -> schedule.setAvailableEconomySeats(
                    schedule.getAvailableEconomySeats() - requested);
        }
        schedule.setReservedSeats(schedule.getReservedSeats() + requested);
        scheduleRepository.save(schedule);

        // Update inventory
        Inventory inventory = inventoryRepository.findByTrainIdAndDepartureDateWithLock(
                        request.getTrainId(), date)
                .orElse(null);

        if (inventory != null) {
            inventory.setAvailableSeats(inventory.getAvailableSeats() - requested);
            inventory.setReservedSeats(inventory.getReservedSeats() + requested);
            switch (seatClass) {
                case "ECONOMY" -> inventory.setEconomyAvailable(
                        inventory.getEconomyAvailable() - requested);
                case "BUSINESS" -> inventory.setBusinessAvailable(
                        inventory.getBusinessAvailable() - requested);
                case "FIRST" -> inventory.setFirstClassAvailable(
                        inventory.getFirstClassAvailable() - requested);
            }
            inventoryRepository.save(inventory);
        }

        // Evict cache
        evictCache(request.getTrainId(), request.getDepartureDate().toString());

        log.info("Reserved {} {} seats for train {} on {}",
                requested, seatClass, request.getTrainId(), request.getDepartureDate());
        return true;
    }

    private boolean doReleaseSeats(Long trainId, String departureDate, Integer numberOfSeats, String seatClass) {
        Instant date = parseDate(departureDate);

        Optional<Schedule> scheduleOpt = scheduleRepository.findByTrainIdAndDepartureDateWithLock(trainId, date);
        if (scheduleOpt.isEmpty()) {
            log.warn("Schedule not found for release: train={}, date={}", trainId, date);
            return false;
        }

        Schedule schedule = scheduleOpt.get();
        String effectiveSeatClass = seatClass != null ? seatClass.toUpperCase() : "ECONOMY";

        // Update schedule
        switch (effectiveSeatClass) {
            case "ECONOMY" -> schedule.setAvailableEconomySeats(
                    schedule.getAvailableEconomySeats() + numberOfSeats);
            case "BUSINESS" -> schedule.setAvailableBusinessSeats(
                    schedule.getAvailableBusinessSeats() + numberOfSeats);
            case "FIRST" -> schedule.setAvailableFirstClassSeats(
                    schedule.getAvailableFirstClassSeats() + numberOfSeats);
            default -> schedule.setAvailableEconomySeats(
                    schedule.getAvailableEconomySeats() + numberOfSeats);
        }
        schedule.setReservedSeats(Math.max(0, schedule.getReservedSeats() - numberOfSeats));
        scheduleRepository.save(schedule);

        // Update inventory
        Optional<Inventory> inventoryOpt = inventoryRepository.findByTrainIdAndDepartureDateWithLock(trainId, date);
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            inventory.setAvailableSeats(inventory.getAvailableSeats() + numberOfSeats);
            inventory.setReservedSeats(Math.max(0, inventory.getReservedSeats() - numberOfSeats));
            switch (effectiveSeatClass) {
                case "ECONOMY" -> inventory.setEconomyAvailable(
                        inventory.getEconomyAvailable() + numberOfSeats);
                case "BUSINESS" -> inventory.setBusinessAvailable(
                        inventory.getBusinessAvailable() + numberOfSeats);
                case "FIRST" -> inventory.setFirstClassAvailable(
                        inventory.getFirstClassAvailable() + numberOfSeats);
            }
            inventoryRepository.save(inventory);
        }

        // Evict cache
        evictCache(trainId, departureDate);

        log.info("Released {} {} seats for train {} on {}",
                numberOfSeats, effectiveSeatClass, trainId, departureDate);
        return true;
    }

    private Instant parseDate(String dateStr) {
        try {
            LocalDate localDate;
            if (dateStr.contains(" ")) {
                localDate = LocalDate.parse(dateStr.split(" ")[0], DATE_FORMATTER);
            } else {
                localDate = LocalDate.parse(dateStr, DATE_FORMATTER);
            }
            // Convert LocalDate to Instant at start of day in system timezone
            return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format. Expected: yyyy-MM-dd or ISO-8601", e);
        }
    }

    private void evictCache(Long trainId, String departureDate) {
        String cacheKey = CACHE_PREFIX + trainId + ":" + departureDate;
        redisTemplate.delete(cacheKey);
    }

    private void evictCache(Long trainId, Instant departureDate) {
        evictCache(trainId, departureDate.toString());
    }
}
