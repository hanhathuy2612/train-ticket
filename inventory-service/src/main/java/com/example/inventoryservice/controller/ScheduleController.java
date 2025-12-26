package com.example.inventoryservice.controller;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.inventoryservice.dto.ApiResponse;
import com.example.inventoryservice.dto.AvailabilityResponse;
import com.example.inventoryservice.dto.CreateScheduleRequest;
import com.example.inventoryservice.dto.ScheduleResponse;
import com.example.inventoryservice.dto.SearchTrainRequest;
import com.example.inventoryservice.entity.Schedule.ScheduleStatus;
import com.example.inventoryservice.service.ScheduleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleController.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final ScheduleService scheduleService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduleResponse>> getScheduleById(@PathVariable Long id) {
        logger.debug("Get schedule by id: {}", id);
        ScheduleResponse response = scheduleService.getScheduleById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/train/{trainId}/date/{date}")
    public ResponseEntity<ApiResponse<ScheduleResponse>> getScheduleByTrainAndDate(
            @PathVariable Long trainId,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        logger.debug("Get schedule for train {} on {}", trainId, date);
        ScheduleResponse response = scheduleService.getScheduleByTrainAndDate(trainId, date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<ApiResponse<Page<ScheduleResponse>>> getSchedulesByDate(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        logger.debug("Get schedules for date: {}", date);
        
        size = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);
        
        Page<ScheduleResponse> schedules = scheduleService.getSchedulesByDate(date, pageable);
        return ResponseEntity.ok(ApiResponse.success(schedules));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> searchSchedules(
            @Valid @RequestBody SearchTrainRequest request) {
        logger.debug("Search schedules from {} to {} on {}", 
                request.getOrigin(), request.getDestination(), request.getDepartureDate());
        List<ScheduleResponse> schedules = scheduleService.searchSchedules(request);
        return ResponseEntity.ok(ApiResponse.success(schedules));
    }

    @PostMapping("/availability")
    public ResponseEntity<ApiResponse<List<AvailabilityResponse>>> getAvailability(
            @Valid @RequestBody SearchTrainRequest request) {
        logger.debug("Get availability from {} to {} on {}", 
                request.getOrigin(), request.getDestination(), request.getDepartureDate());
        List<AvailabilityResponse> availability = scheduleService.getAvailability(request);
        return ResponseEntity.ok(ApiResponse.success(availability));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleResponse>> createSchedule(
            @Valid @RequestBody CreateScheduleRequest request) {
        logger.info("Create schedule for train {} on {}", request.getTrainId(), request.getDepartureDate());
        ScheduleResponse response = scheduleService.createSchedule(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> createBulkSchedules(
            @RequestParam Long trainId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        logger.info("Create bulk schedules for train {} from {} to {}", trainId, startDate, endDate);
        List<ScheduleResponse> schedules = scheduleService.createBulkSchedules(trainId, startDate, endDate);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(schedules));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateScheduleStatus(
            @PathVariable Long id,
            @RequestParam ScheduleStatus status) {
        logger.info("Update schedule {} status to {}", id, status);
        scheduleService.updateScheduleStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Schedule status updated", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelSchedule(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Cancelled by admin") String reason) {
        logger.info("Cancel schedule: {}", id);
        scheduleService.cancelSchedule(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Schedule cancelled", null));
    }
}

