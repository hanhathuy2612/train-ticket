package com.example.ticketservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.ticketservice.dto.AvailabilityResponse;
import com.example.ticketservice.dto.ReserveSeatRequest;

@Component
public class InventoryServiceClientFallback implements InventoryServiceClient {
	
	private static final Logger logger = LoggerFactory.getLogger(InventoryServiceClientFallback.class);

	@Override
	public AvailabilityResponse checkAvailability(Long trainId, String departureDate) {
		logger.error("Fallback: Inventory service is unavailable for checkAvailability. TrainId: {}", trainId);
		// Return empty availability to prevent booking when service is down
		return AvailabilityResponse.builder()
				.trainId(trainId)
				.totalSeats(0)
				.availableSeats(0)
				.reservedSeats(0)
				.available(false)
				.build();
	}

	@Override
	public Boolean reserveSeats(ReserveSeatRequest request) {
		logger.error("Fallback: Inventory service is unavailable for reserveSeats. TrainId: {}", request.getTrainId());
		return false;
	}

	@Override
	public Boolean releaseSeats(Long trainId, String departureDate, Integer numberOfSeats) {
		logger.error("Fallback: Inventory service is unavailable for releaseSeats. TrainId: {}", trainId);
		// Return true to allow ticket cancellation to proceed, seats will be released later
		return true;
	}
}

