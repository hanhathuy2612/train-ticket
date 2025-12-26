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

import com.example.inventoryservice.dto.CreateTrainRequest;
import com.example.inventoryservice.dto.TrainResponse;
import com.example.inventoryservice.entity.Route;
import com.example.inventoryservice.entity.Seat;
import com.example.inventoryservice.entity.Train;
import com.example.inventoryservice.entity.Train.TrainType;
import com.example.inventoryservice.exception.RouteNotFoundException;
import com.example.inventoryservice.exception.TrainNotFoundException;
import com.example.inventoryservice.repository.RouteRepository;
import com.example.inventoryservice.repository.SeatRepository;
import com.example.inventoryservice.repository.TrainRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainService {

    private static final Logger logger = LoggerFactory.getLogger(TrainService.class);

    private final TrainRepository trainRepository;
    private final RouteRepository routeRepository;
    private final SeatRepository seatRepository;

    @Cacheable(value = "trains", key = "#id")
    public TrainResponse getTrainById(Long id) {
        logger.debug("Fetching train by id: {}", id);
        Train train = trainRepository.findById(id)
                .orElseThrow(() -> new TrainNotFoundException(id));
        return TrainResponse.from(train);
    }

    public TrainResponse getTrainByNumber(String trainNumber) {
        logger.debug("Fetching train by number: {}", trainNumber);
        Train train = trainRepository.findByTrainNumber(trainNumber)
                .orElseThrow(() -> new TrainNotFoundException(trainNumber));
        return TrainResponse.from(train);
    }

    public Page<TrainResponse> getAllTrains(Pageable pageable) {
        logger.debug("Fetching all active trains");
        return trainRepository.findByActiveTrue(pageable)
                .map(TrainResponse::from);
    }

    public Page<TrainResponse> searchTrains(String query, Pageable pageable) {
        logger.debug("Searching trains with query: {}", query);
        return trainRepository.search(query, pageable)
                .map(TrainResponse::from);
    }

    public List<TrainResponse> getTrainsByRoute(Long routeId) {
        logger.debug("Fetching trains for route: {}", routeId);
        return trainRepository.findByRouteId(routeId).stream()
                .map(TrainResponse::from)
                .collect(Collectors.toList());
    }

    public List<TrainResponse> getTrainsByRouteOriginAndDestination(String origin, String destination) {
        logger.debug("Fetching trains from {} to {}", origin, destination);
        return trainRepository.findByRouteOriginAndDestination(origin, destination).stream()
                .map(TrainResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public TrainResponse createTrain(CreateTrainRequest request) {
        logger.info("Creating train: {}", request.getTrainNumber());

        if (trainRepository.existsByTrainNumber(request.getTrainNumber())) {
            throw new IllegalArgumentException("Train number already exists");
        }

        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new RouteNotFoundException(request.getRouteId()));

        Train train = Train.builder()
                .trainNumber(request.getTrainNumber())
                .trainName(request.getTrainName())
                .route(route)
                .trainType(request.getTrainType() != null ? request.getTrainType() : TrainType.EXPRESS)
                .departureTime(request.getDepartureTime())
                .arrivalTime(request.getArrivalTime())
                .economySeats(request.getEconomySeats())
                .businessSeats(request.getBusinessSeats())
                .firstClassSeats(request.getFirstClassSeats() != null ? request.getFirstClassSeats() : 0)
                .economyPrice(request.getEconomyPrice())
                .businessPrice(request.getBusinessPrice())
                .firstClassPrice(request.getFirstClassPrice())
                .amenities(request.getAmenities())
                .build();

        train = trainRepository.save(train);
        logger.info("Train created with id: {}", train.getId());

        // Generate seats for the train
        generateSeats(train);

        return TrainResponse.from(train);
    }

    @Transactional
    @CacheEvict(value = "trains", key = "#id")
    public TrainResponse updateTrain(Long id, CreateTrainRequest request) {
        logger.info("Updating train: {}", id);

        Train train = trainRepository.findById(id)
                .orElseThrow(() -> new TrainNotFoundException(id));

        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new RouteNotFoundException(request.getRouteId()));

        train.setTrainName(request.getTrainName());
        train.setRoute(route);
        if (request.getTrainType() != null) {
            train.setTrainType(request.getTrainType());
        }
        train.setDepartureTime(request.getDepartureTime());
        train.setArrivalTime(request.getArrivalTime());
        train.setEconomySeats(request.getEconomySeats());
        train.setBusinessSeats(request.getBusinessSeats());
        train.setFirstClassSeats(request.getFirstClassSeats());
        train.setEconomyPrice(request.getEconomyPrice());
        train.setBusinessPrice(request.getBusinessPrice());
        train.setFirstClassPrice(request.getFirstClassPrice());
        train.setAmenities(request.getAmenities());

        train = trainRepository.save(train);
        return TrainResponse.from(train);
    }

    @Transactional
    @CacheEvict(value = "trains", key = "#id")
    public void deleteTrain(Long id) {
        logger.info("Deleting train: {}", id);

        Train train = trainRepository.findById(id)
                .orElseThrow(() -> new TrainNotFoundException(id));

        train.setActive(false);
        trainRepository.save(train);
    }

    private void generateSeats(Train train) {
        logger.debug("Generating seats for train: {}", train.getId());
        
        int seatCounter = 1;
        int carNumber = 1;
        
        // Generate economy seats
        for (int i = 0; i < train.getEconomySeats(); i++) {
            String seatNumber = String.valueOf(seatCounter) + (char) ('A' + (i % 4));
            Seat seat = Seat.builder()
                    .train(train)
                    .seatNumber(seatNumber)
                    .seatClass(Seat.SeatClass.ECONOMY)
                    .carNumber(carNumber)
                    .position(i % 4 == 0 || i % 4 == 3 ? "WINDOW" : "AISLE")
                    .build();
            seatRepository.save(seat);
            if ((i + 1) % 4 == 0) seatCounter++;
            if ((i + 1) % 40 == 0) carNumber++;
        }
        
        carNumber++;
        seatCounter = 1;
        
        // Generate business seats
        for (int i = 0; i < train.getBusinessSeats(); i++) {
            String seatNumber = "B" + seatCounter + (char) ('A' + (i % 3));
            Seat seat = Seat.builder()
                    .train(train)
                    .seatNumber(seatNumber)
                    .seatClass(Seat.SeatClass.BUSINESS)
                    .carNumber(carNumber)
                    .position(i % 3 == 0 || i % 3 == 2 ? "WINDOW" : "AISLE")
                    .build();
            seatRepository.save(seat);
            if ((i + 1) % 3 == 0) seatCounter++;
            if ((i + 1) % 24 == 0) carNumber++;
        }
        
        // Generate first class seats
        if (train.getFirstClassSeats() > 0) {
            carNumber++;
            seatCounter = 1;
            for (int i = 0; i < train.getFirstClassSeats(); i++) {
                String seatNumber = "F" + seatCounter + (char) ('A' + (i % 2));
                Seat seat = Seat.builder()
                        .train(train)
                        .seatNumber(seatNumber)
                        .seatClass(Seat.SeatClass.FIRST)
                        .carNumber(carNumber)
                        .position(i % 2 == 0 ? "WINDOW" : "AISLE")
                        .build();
                seatRepository.save(seat);
                if ((i + 1) % 2 == 0) seatCounter++;
                if ((i + 1) % 12 == 0) carNumber++;
            }
        }
        
        logger.debug("Generated {} seats for train: {}", 
                train.getEconomySeats() + train.getBusinessSeats() + train.getFirstClassSeats(), 
                train.getId());
    }
}

