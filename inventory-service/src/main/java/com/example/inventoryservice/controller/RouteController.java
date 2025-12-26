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
import com.example.inventoryservice.dto.CreateRouteRequest;
import com.example.inventoryservice.dto.RouteResponse;
import com.example.inventoryservice.service.RouteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
public class RouteController {

    private static final Logger logger = LoggerFactory.getLogger(RouteController.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final RouteService routeService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RouteResponse>> getRouteById(@PathVariable Long id) {
        logger.debug("Get route by id: {}", id);
        RouteResponse response = routeService.getRouteById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RouteResponse>>> getAllRoutes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "origin") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        logger.debug("Get all routes - page: {}, size: {}", page, size);
        
        size = Math.min(size, MAX_PAGE_SIZE);
        Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<RouteResponse> routes = routeService.getAllRoutes(pageable);
        return ResponseEntity.ok(ApiResponse.success(routes));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<RouteResponse>>> searchRoutes(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        logger.debug("Search routes with query: {}", q);
        
        size = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);
        
        Page<RouteResponse> routes = routeService.searchRoutes(q, pageable);
        return ResponseEntity.ok(ApiResponse.success(routes));
    }

    @GetMapping("/origins")
    public ResponseEntity<ApiResponse<List<String>>> getAllOrigins() {
        logger.debug("Get all origins");
        List<String> origins = routeService.getAllOrigins();
        return ResponseEntity.ok(ApiResponse.success(origins));
    }

    @GetMapping("/destinations")
    public ResponseEntity<ApiResponse<List<String>>> getAllDestinations() {
        logger.debug("Get all destinations");
        List<String> destinations = routeService.getAllDestinations();
        return ResponseEntity.ok(ApiResponse.success(destinations));
    }

    @GetMapping("/from/{origin}")
    public ResponseEntity<ApiResponse<List<RouteResponse>>> getRoutesByOrigin(
            @PathVariable String origin) {
        logger.debug("Get routes from origin: {}", origin);
        List<RouteResponse> routes = routeService.getRoutesByOrigin(origin);
        return ResponseEntity.ok(ApiResponse.success(routes));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RouteResponse>> createRoute(
            @Valid @RequestBody CreateRouteRequest request) {
        logger.info("Create route from {} to {}", request.getOrigin(), request.getDestination());
        RouteResponse response = routeService.createRoute(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RouteResponse>> updateRoute(
            @PathVariable Long id,
            @Valid @RequestBody CreateRouteRequest request) {
        logger.info("Update route: {}", id);
        RouteResponse response = routeService.updateRoute(id, request);
        return ResponseEntity.ok(ApiResponse.success("Route updated", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoute(@PathVariable Long id) {
        logger.info("Delete route: {}", id);
        routeService.deleteRoute(id);
        return ResponseEntity.ok(ApiResponse.success("Route deleted", null));
    }
}

