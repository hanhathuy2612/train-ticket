package com.example.inventoryservice.controller;

import com.example.inventoryservice.dto.AvailabilityResponse;
import com.example.inventoryservice.dto.ReserveSeatRequest;
import com.example.inventoryservice.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

	@Autowired
	private InventoryService inventoryService;

	@GetMapping("/availability")
	public ResponseEntity<AvailabilityResponse> checkAvailability(
			@RequestParam Long trainId,
			@RequestParam String departureDate) {
		AvailabilityResponse response = inventoryService.checkAvailability(trainId, departureDate);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/reserve")
	public ResponseEntity<Boolean> reserveSeats(@Valid @RequestBody ReserveSeatRequest request) {
		boolean success = inventoryService.reserveSeats(request);
		return success ? ResponseEntity.ok(true) : ResponseEntity.badRequest().body(false);
	}

	@PostMapping("/release")
	public ResponseEntity<Boolean> releaseSeats(
			@RequestParam Long trainId,
			@RequestParam String departureDate,
			@RequestParam Integer numberOfSeats) {
		boolean success = inventoryService.releaseSeats(trainId, departureDate, numberOfSeats);
		return success ? ResponseEntity.ok(true) : ResponseEntity.badRequest().body(false);
	}
}

