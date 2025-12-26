package com.example.inventoryservice.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import com.example.inventoryservice.dto.CreateTrainRequest;
import com.example.inventoryservice.dto.TrainResponse;
import com.example.inventoryservice.service.TrainService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/trains")
@RequiredArgsConstructor
public class TrainController {

    private static final Logger logger = LoggerFactory.getLogger(TrainController.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final TrainService trainService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TrainResponse>> getTrainById(@PathVariable Long id) {
        logger.debug("Get train by id: {}", id);
        TrainResponse response = trainService.getTrainById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/number/{trainNumber}")
    public ResponseEntity<ApiResponse<TrainResponse>> getTrainByNumber(
            @PathVariable String trainNumber) {
        logger.debug("Get train by number: {}", trainNumber);
        TrainResponse response = trainService.getTrainByNumber(trainNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TrainResponse>>> getAllTrains(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "trainNumber") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        logger.debug("Get all trains - page: {}, size: {}", page, size);
        
        size = Math.min(size, MAX_PAGE_SIZE);
        Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<TrainResponse> trains = trainService.getAllTrains(pageable);
        return ResponseEntity.ok(ApiResponse.success(trains));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<TrainResponse>>> searchTrains(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        logger.debug("Search trains with query: {}", q);
        
        size = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);
        
        Page<TrainResponse> trains = trainService.searchTrains(q, pageable);
        return ResponseEntity.ok(ApiResponse.success(trains));
    }

    @GetMapping("/route/{routeId}")
    public ResponseEntity<ApiResponse<List<TrainResponse>>> getTrainsByRoute(
            @PathVariable Long routeId) {
        logger.debug("Get trains for route: {}", routeId);
        List<TrainResponse> trains = trainService.getTrainsByRoute(routeId);
        return ResponseEntity.ok(ApiResponse.success(trains));
    }

    @GetMapping("/from/{origin}/to/{destination}")
    public ResponseEntity<ApiResponse<List<TrainResponse>>> getTrainsByRoute(
            @PathVariable String origin,
            @PathVariable String destination) {
        logger.debug("Get trains from {} to {}", origin, destination);
        List<TrainResponse> trains = trainService.getTrainsByRouteOriginAndDestination(origin, destination);
        return ResponseEntity.ok(ApiResponse.success(trains));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TrainResponse>> createTrain(
            @Valid @RequestBody CreateTrainRequest request) {
        logger.info("Create train: {}", request.getTrainNumber());
        TrainResponse response = trainService.createTrain(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TrainResponse>> updateTrain(
            @PathVariable Long id,
            @Valid @RequestBody CreateTrainRequest request) {
        logger.info("Update train: {}", id);
        TrainResponse response = trainService.updateTrain(id, request);
        return ResponseEntity.ok(ApiResponse.success("Train updated", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTrain(@PathVariable Long id) {
        logger.info("Delete train: {}", id);
        trainService.deleteTrain(id);
        return ResponseEntity.ok(ApiResponse.success("Train deleted", null));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Train Service is healthy"));
    }
}

