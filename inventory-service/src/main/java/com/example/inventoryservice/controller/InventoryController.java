package com.example.inventoryservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.inventoryservice.dto.ApiResponse;
import com.example.inventoryservice.dto.AvailabilityResponse;
import com.example.inventoryservice.dto.ReserveSeatRequest;
import com.example.inventoryservice.service.InventoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);

    private final InventoryService inventoryService;

    /**
     * Check seat availability for a train on a specific date
     * GET /inventory/availability?trainId=1&departureDate=2024-01-15
     */
    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> checkAvailability(
            @RequestParam Long trainId,
            @RequestParam String departureDate) {
        logger.debug("Check availability for train {} on {}", trainId, departureDate);
        AvailabilityResponse response = inventoryService.checkAvailability(trainId, departureDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Reserve seats for a booking
     * POST /inventory/reserve
     */
    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<Boolean>> reserveSeats(
            @Valid @RequestBody ReserveSeatRequest request) {
        logger.info("Reserve {} seats for train {} on {}", 
                request.getNumberOfSeats(), request.getTrainId(), request.getDepartureDate());
        boolean success = inventoryService.reserveSeats(request);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("Seats reserved successfully", true));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to reserve seats", 400));
        }
    }

    /**
     * Release seats (for cancellation)
     * POST /inventory/release
     */
    @PostMapping("/release")
    public ResponseEntity<ApiResponse<Boolean>> releaseSeats(
            @RequestParam Long trainId,
            @RequestParam String departureDate,
            @RequestParam Integer numberOfSeats,
            @RequestParam(required = false) String seatClass) {
        logger.info("Release {} seats for train {} on {}", numberOfSeats, trainId, departureDate);
        boolean success;
        if (seatClass != null) {
            success = inventoryService.releaseSeats(trainId, departureDate, numberOfSeats, seatClass);
        } else {
            success = inventoryService.releaseSeats(trainId, departureDate, numberOfSeats);
        }
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("Seats released successfully", true));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to release seats", 400));
        }
    }

    /**
     * Health check
     * GET /inventory/health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Inventory Service is healthy"));
    }
}
