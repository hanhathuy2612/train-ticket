package com.example.ticketservice.client;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.ticketservice.dto.AvailabilityResponse;
import com.example.ticketservice.dto.ReserveSeatRequest;

@FeignClient(name = "inventory-service", fallback = InventoryServiceClientFallback.class)
public interface InventoryServiceClient {

	@GetMapping("/inventory/availability")
	AvailabilityResponse checkAvailability(
			@RequestParam("trainId") Long trainId,
			@RequestParam("departureDate") String departureDate
	);

	@PostMapping("/inventory/reserve")
	Boolean reserveSeats(@RequestBody ReserveSeatRequest request);

	@PostMapping("/inventory/release")
	Boolean releaseSeats(
			@RequestParam("trainId") Long trainId,
			@RequestParam("departureDate") String departureDate,
			@RequestParam("numberOfSeats") Integer numberOfSeats
	);
}
