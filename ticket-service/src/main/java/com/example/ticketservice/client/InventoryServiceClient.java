package com.example.ticketservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "inventory-service")
public interface InventoryServiceClient {

	@GetMapping("/inventory/availability")
	Map<String, Object> checkAvailability(
			@RequestParam Long trainId,
			@RequestParam String departureDate
	);

	@PostMapping("/inventory/reserve")
	Boolean reserveSeats(@RequestBody Map<String, Object> request);

	@PostMapping("/inventory/release")
	Boolean releaseSeats(
			@RequestParam Long trainId,
			@RequestParam String departureDate,
			@RequestParam Integer numberOfSeats
	);
}

