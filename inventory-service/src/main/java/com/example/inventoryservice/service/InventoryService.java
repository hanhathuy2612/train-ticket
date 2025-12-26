package com.example.inventoryservice.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.inventoryservice.dto.AvailabilityResponse;
import com.example.inventoryservice.dto.ReserveSeatRequest;
import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.entity.Schedule;
import com.example.inventoryservice.entity.Train;
import com.example.inventoryservice.exception.InsufficientSeatsException;
import com.example.inventoryservice.exception.ScheduleNotFoundException;
import com.example.inventoryservice.exception.TrainNotFoundException;
import com.example.inventoryservice.repository.InventoryRepository;
import com.example.inventoryservice.repository.ScheduleRepository;
import com.example.inventoryservice.repository.TrainRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);
    
    private static final String LOCK_PREFIX = "inventory:lock:";
    private static final String CACHE_PREFIX = "inventory:";
    private static final int LOCK_WAIT_TIME = 5;
    private static final int LOCK_LEASE_TIME = 10;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final InventoryRepository inventoryRepository;
    private final ScheduleRepository scheduleRepository;
    private final TrainRepository trainRepository;
    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Object> redisTemplate;

    @Cacheable(value = "inventory", key = "#trainId + ':' + #departureDate")
    public AvailabilityResponse checkAvailability(Long trainId, String departureDate) {
        logger.debug("Checking availability for train {} on {}", trainId, departureDate);
        
        LocalDate date = parseDate(departureDate);
        
        Optional<Schedule> scheduleOpt = scheduleRepository.findByTrainIdAndDepartureDate(trainId, date);
        if (scheduleOpt.isEmpty()) {
            logger.debug("No schedule found for train {} on {}", trainId, date);
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
                logger.warn("Could not acquire lock for reservation: {}", lockKey);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while acquiring lock for reservation", e);
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
                logger.warn("Could not acquire lock for release: {}", lockKey);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while acquiring lock for release", e);
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
        LocalDate date = parseDate(request.getDepartureDate());
        
        Schedule schedule = scheduleRepository.findByTrainIdAndDepartureDateWithLock(
                request.getTrainId(), date)
                .orElseThrow(() -> new ScheduleNotFoundException(request.getTrainId(), date));

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
            throw new InsufficientSeatsException(seatClass, requested, available);
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
        evictCache(request.getTrainId(), request.getDepartureDate());

        logger.info("Reserved {} {} seats for train {} on {}", 
                requested, seatClass, request.getTrainId(), request.getDepartureDate());
        return true;
    }

    private boolean doReleaseSeats(Long trainId, String departureDate, Integer numberOfSeats, String seatClass) {
        LocalDate date = parseDate(departureDate);
        
        Optional<Schedule> scheduleOpt = scheduleRepository.findByTrainIdAndDepartureDateWithLock(trainId, date);
        if (scheduleOpt.isEmpty()) {
            logger.warn("Schedule not found for release: train={}, date={}", trainId, date);
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

        logger.info("Released {} {} seats for train {} on {}", 
                numberOfSeats, effectiveSeatClass, trainId, departureDate);
        return true;
    }

    private LocalDate parseDate(String dateStr) {
        try {
            if (dateStr.contains(" ")) {
                return LocalDate.parse(dateStr.split(" ")[0], DATE_FORMATTER);
            }
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format. Expected: yyyy-MM-dd", e);
        }
    }

    private void evictCache(Long trainId, String departureDate) {
        String cacheKey = CACHE_PREFIX + trainId + ":" + departureDate;
        redisTemplate.delete(cacheKey);
    }
}
