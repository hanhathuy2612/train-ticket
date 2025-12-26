package com.example.inventoryservice.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.inventoryservice.dto.CreateRouteRequest;
import com.example.inventoryservice.dto.RouteResponse;
import com.example.inventoryservice.entity.Route;
import com.example.inventoryservice.exception.RouteNotFoundException;
import com.example.inventoryservice.repository.RouteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouteService {

    private static final Logger logger = LoggerFactory.getLogger(RouteService.class);

    private final RouteRepository routeRepository;

    @Cacheable(value = "routes", key = "#id")
    public RouteResponse getRouteById(Long id) {
        logger.debug("Fetching route by id: {}", id);
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new RouteNotFoundException(id));
        return RouteResponse.from(route);
    }

    public Page<RouteResponse> getAllRoutes(Pageable pageable) {
        logger.debug("Fetching all active routes");
        return routeRepository.findByActiveTrue(pageable)
                .map(RouteResponse::from);
    }

    public Page<RouteResponse> searchRoutes(String query, Pageable pageable) {
        logger.debug("Searching routes with query: {}", query);
        return routeRepository.search(query, pageable)
                .map(RouteResponse::from);
    }

    public RouteResponse getRouteByOriginAndDestination(String origin, String destination) {
        logger.debug("Fetching route from {} to {}", origin, destination);
        Route route = routeRepository.findByOriginAndDestination(origin, destination)
                .orElseThrow(() -> new RouteNotFoundException(origin, destination));
        return RouteResponse.from(route);
    }

    public List<String> getAllOrigins() {
        return routeRepository.findAllOrigins();
    }

    public List<String> getAllDestinations() {
        return routeRepository.findAllDestinations();
    }

    public List<RouteResponse> getRoutesByOrigin(String origin) {
        return routeRepository.findByOriginIgnoreCase(origin).stream()
                .map(RouteResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public RouteResponse createRoute(CreateRouteRequest request) {
        logger.info("Creating route from {} to {}", request.getOrigin(), request.getDestination());

        if (routeRepository.existsByOriginIgnoreCaseAndDestinationIgnoreCase(
                request.getOrigin(), request.getDestination())) {
            throw new IllegalArgumentException("Route already exists");
        }

        Route route = Route.builder()
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .distanceKm(request.getDistanceKm())
                .durationMinutes(request.getDurationMinutes())
                .basePrice(request.getBasePrice())
                .description(request.getDescription())
                .build();

        route = routeRepository.save(route);
        logger.info("Route created with id: {}", route.getId());

        return RouteResponse.from(route);
    }

    @Transactional
    @CacheEvict(value = "routes", key = "#id")
    public RouteResponse updateRoute(Long id, CreateRouteRequest request) {
        logger.info("Updating route: {}", id);

        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new RouteNotFoundException(id));

        route.setOrigin(request.getOrigin());
        route.setDestination(request.getDestination());
        route.setDistanceKm(request.getDistanceKm());
        route.setDurationMinutes(request.getDurationMinutes());
        route.setBasePrice(request.getBasePrice());
        route.setDescription(request.getDescription());

        route = routeRepository.save(route);
        return RouteResponse.from(route);
    }

    @Transactional
    @CacheEvict(value = "routes", key = "#id")
    public void deleteRoute(Long id) {
        logger.info("Deleting route: {}", id);

        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new RouteNotFoundException(id));

        route.setActive(false);
        routeRepository.save(route);
    }
}

