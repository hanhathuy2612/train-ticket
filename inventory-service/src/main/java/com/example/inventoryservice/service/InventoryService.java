package com.example.inventoryservice.service;

import com.example.inventoryservice.dto.AvailabilityResponse;
import com.example.inventoryservice.dto.ReserveSeatRequest;
import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.repository.InventoryRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class InventoryService {

	@Autowired
	private InventoryRepository inventoryRepository;

	@Autowired
	private RedissonClient redissonClient;

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	private static final String LOCK_PREFIX = "inventory:lock:";
	private static final String CACHE_PREFIX = "inventory:";
	private static final int LOCK_WAIT_TIME = 5;
	private static final int LOCK_LEASE_TIME = 10;

	@Cacheable(value = "inventory", key = "#trainId + ':' + #departureDate")
	public AvailabilityResponse checkAvailability(Long trainId, String departureDate) {
		LocalDateTime date = parseDate(departureDate);
		Optional<Inventory> inventoryOpt = inventoryRepository.findByTrainIdAndDepartureDate(trainId, date);

		if (inventoryOpt.isEmpty()) {
			// Return default if inventory doesn't exist
			return new AvailabilityResponse(trainId, 0, 0, 0);
		}

		Inventory inventory = inventoryOpt.get();
		return new AvailabilityResponse(
				inventory.getTrainId(),
				inventory.getTotalSeats(),
				inventory.getAvailableSeats(),
				inventory.getReservedSeats()
		);
	}

	public boolean reserveSeats(ReserveSeatRequest request) {
		String lockKey = LOCK_PREFIX + request.getTrainId() + ":" + request.getDepartureDate();
		RLock lock = redissonClient.getLock(lockKey);

		try {
			// Try to acquire lock with timeout
			if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
				try {
					LocalDateTime departureDate = parseDate(request.getDepartureDate());
					
					// Use pessimistic lock for database update
					Optional<Inventory> inventoryOpt = inventoryRepository
							.findByTrainIdAndDepartureDateWithLock(request.getTrainId(), departureDate);

					if (inventoryOpt.isEmpty()) {
						return false;
					}

					Inventory inventory = inventoryOpt.get();

					if (inventory.getAvailableSeats() < request.getNumberOfSeats()) {
						return false;
					}

					// Update inventory
					inventory.setAvailableSeats(inventory.getAvailableSeats() - request.getNumberOfSeats());
					inventory.setReservedSeats(inventory.getReservedSeats() + request.getNumberOfSeats());
					inventoryRepository.save(inventory);

					// Evict cache
					String cacheKey = CACHE_PREFIX + request.getTrainId() + ":" + request.getDepartureDate();
					redisTemplate.delete(cacheKey);

					return true;
				} finally {
					lock.unlock();
				}
			} else {
				return false; // Could not acquire lock
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	public boolean releaseSeats(Long trainId, String departureDate, Integer numberOfSeats) {
		String lockKey = LOCK_PREFIX + trainId + ":" + departureDate;
		RLock lock = redissonClient.getLock(lockKey);

		try {
			if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
				try {
					LocalDateTime date = parseDate(departureDate);
					Optional<Inventory> inventoryOpt = inventoryRepository
							.findByTrainIdAndDepartureDateWithLock(trainId, date);

					if (inventoryOpt.isEmpty()) {
						return false;
					}

					Inventory inventory = inventoryOpt.get();
					
					// Release seats
					inventory.setAvailableSeats(inventory.getAvailableSeats() + numberOfSeats);
					inventory.setReservedSeats(Math.max(0, inventory.getReservedSeats() - numberOfSeats));
					inventoryRepository.save(inventory);

					// Evict cache
					String cacheKey = CACHE_PREFIX + trainId + ":" + departureDate;
					redisTemplate.delete(cacheKey);

					return true;
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

	private LocalDateTime parseDate(String dateStr) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
		return LocalDateTime.parse(dateStr, formatter);
	}
}

