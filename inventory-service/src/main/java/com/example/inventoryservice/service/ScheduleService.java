package com.example.inventoryservice.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.inventoryservice.dto.AvailabilityResponse;
import com.example.inventoryservice.dto.CreateScheduleRequest;
import com.example.inventoryservice.dto.ScheduleResponse;
import com.example.inventoryservice.dto.SearchTrainRequest;
import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.entity.Schedule;
import com.example.inventoryservice.entity.Train;
import com.example.inventoryservice.exception.InventoryErrorCode;
import com.example.inventoryservice.repository.InventoryRepository;
import com.example.inventoryservice.repository.ScheduleRepository;
import com.example.inventoryservice.repository.TrainRepository;
import com.example.shared.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

        private static final Logger logger = LoggerFactory.getLogger(ScheduleService.class);

        private final ScheduleRepository scheduleRepository;
        private final TrainRepository trainRepository;
        private final InventoryRepository inventoryRepository;

        public ScheduleResponse getScheduleById(Long id) {
                logger.debug("Fetching schedule by id: {}", id);
                Schedule schedule = scheduleRepository.findById(id)
                                .orElseThrow(() -> new BusinessException(InventoryErrorCode.SCHEDULE_NOT_FOUND));
                return ScheduleResponse.from(schedule);
        }

        public ScheduleResponse getScheduleByTrainAndDate(Long trainId, Instant date) {
                logger.debug("Fetching schedule for train {} on {}", trainId, date);
                Schedule schedule = scheduleRepository.findByTrainIdAndDepartureDate(trainId, date)
                                .orElseThrow(() -> new BusinessException(InventoryErrorCode.SCHEDULE_NOT_FOUND));
                return ScheduleResponse.from(schedule);
        }

        public Page<ScheduleResponse> getSchedulesByDate(Instant date, Pageable pageable) {
                logger.debug("Fetching schedules for date: {}", date);
                LocalDate localDate = date.atZone(ZoneId.systemDefault()).toLocalDate();
                Instant startOfDay = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
                Instant endOfDay = localDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
                return scheduleRepository.findByDepartureDate(startOfDay, endOfDay, pageable)
                                .map(ScheduleResponse::from);
        }

        public List<ScheduleResponse> searchSchedules(SearchTrainRequest request) {
                logger.debug("Searching schedules from {} to {} on {}",
                                request.getOrigin(), request.getDestination(), request.getDepartureDate());

                LocalDate localDate = request.getDepartureDate().atZone(ZoneId.systemDefault()).toLocalDate();
                Instant startOfDay = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
                Instant endOfDay = localDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

                List<Schedule> schedules;
                if (request.getPassengers() > 1) {
                        schedules = scheduleRepository.findAvailableSchedulesWithSeats(
                                        request.getOrigin(),
                                        request.getDestination(),
                                        startOfDay,
                                        endOfDay,
                                        request.getPassengers());
                } else {
                        schedules = scheduleRepository.findAvailableSchedules(
                                        request.getOrigin(),
                                        request.getDestination(),
                                        startOfDay,
                                        endOfDay);
                }

                return schedules.stream()
                                .map(ScheduleResponse::from)
                                .collect(Collectors.toList());
        }

        public List<AvailabilityResponse> getAvailability(SearchTrainRequest request) {
                logger.debug("Getting availability from {} to {} on {}",
                                request.getOrigin(), request.getDestination(), request.getDepartureDate());

                LocalDate localDate = request.getDepartureDate().atZone(ZoneId.systemDefault()).toLocalDate();
                Instant startOfDay = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
                Instant endOfDay = localDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

                List<Schedule> schedules = scheduleRepository.findAvailableSchedules(
                                request.getOrigin(),
                                request.getDestination(),
                                startOfDay,
                                endOfDay);

                return schedules.stream()
                                .map(this::toAvailabilityResponse)
                                .collect(Collectors.toList());
        }

        @Transactional
        public ScheduleResponse createSchedule(CreateScheduleRequest request) {
                logger.info("Creating schedule for train {} on {}", request.getTrainId(), request.getDepartureDate());

                if (scheduleRepository.existsByTrainIdAndDepartureDate(request.getTrainId(),
                                request.getDepartureDate())) {
                        throw new IllegalArgumentException("Schedule already exists for this train and date");
                }

                Train train = trainRepository.findById(request.getTrainId())
                                .orElseThrow(() -> new BusinessException(InventoryErrorCode.TRAIN_NOT_FOUND));

                Schedule schedule = Schedule.builder()
                                .train(train)
                                .departureDate(request.getDepartureDate())
                                .availableEconomySeats(train.getEconomySeats())
                                .availableBusinessSeats(train.getBusinessSeats())
                                .availableFirstClassSeats(train.getFirstClassSeats())
                                .reservedSeats(0)
                                .notes(request.getNotes())
                                .build();

                schedule = scheduleRepository.save(schedule);

                // Create inventory record
                Inventory inventory = Inventory.builder()
                                .trainId(train.getId())
                                .departureDate(request.getDepartureDate())
                                .totalSeats(train.getTotalSeats())
                                .availableSeats(train.getTotalSeats())
                                .reservedSeats(0)
                                .economyAvailable(train.getEconomySeats())
                                .businessAvailable(train.getBusinessSeats())
                                .firstClassAvailable(train.getFirstClassSeats())
                                .build();
                inventoryRepository.save(inventory);

                logger.info("Schedule created with id: {}", schedule.getId());
                return ScheduleResponse.from(schedule);
        }

        @Transactional
        public List<ScheduleResponse> createBulkSchedules(Long trainId, Instant startDate, Instant endDate) {
                logger.info("Creating bulk schedules for train {} from {} to {}", trainId, startDate, endDate);

                LocalDate startLocalDate = startDate.atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate endLocalDate = endDate.atZone(ZoneId.systemDefault()).toLocalDate();
                long daysBetween = ChronoUnit.DAYS.between(startLocalDate, endLocalDate) + 1;

                return IntStream.range(0, (int) daysBetween)
                                .mapToObj(startLocalDate::plusDays)
                                .map(localDate -> localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
                                .filter(date -> !scheduleRepository.existsByTrainIdAndDepartureDate(trainId, date))
                                .map(date -> {
                                        CreateScheduleRequest request = CreateScheduleRequest.builder()
                                                        .trainId(trainId)
                                                        .departureDate(date)
                                                        .build();
                                        return createSchedule(request);
                                })
                                .collect(Collectors.toList());
        }

        @Transactional
        public void cancelSchedule(Long id, String reason) {
                logger.info("Cancelling schedule: {}", id);

                Schedule schedule = scheduleRepository.findById(id)
                                .orElseThrow(() -> new BusinessException(InventoryErrorCode.SCHEDULE_NOT_FOUND));

                schedule.setStatus(Schedule.ScheduleStatus.CANCELLED);
                schedule.setNotes(reason);
                scheduleRepository.save(schedule);
        }

        @Transactional
        public void updateScheduleStatus(Long id, Schedule.ScheduleStatus status) {
                logger.info("Updating schedule {} status to {}", id, status);
                scheduleRepository.updateStatus(id, status);
        }

        private AvailabilityResponse toAvailabilityResponse(Schedule schedule) {
                Train train = schedule.getTrain();
                return AvailabilityResponse.builder()
                                .trainId(train.getId())
                                .trainNumber(train.getTrainNumber())
                                .trainName(train.getTrainName())
                                .origin(train.getRoute().getOrigin())
                                .destination(train.getRoute().getDestination())
                                .departureDate(schedule.getDepartureDate())
                                .departureTime(train.getDepartureTime())
                                .arrivalTime(train.getArrivalTime())
                                .totalSeats(train.getTotalSeats())
                                .availableSeats(schedule.getTotalAvailableSeats())
                                .reservedSeats(schedule.getReservedSeats())
                                .seatAvailability(AvailabilityResponse.SeatAvailability.builder()
                                                .economy(schedule.getAvailableEconomySeats())
                                                .business(schedule.getAvailableBusinessSeats())
                                                .firstClass(schedule.getAvailableFirstClassSeats())
                                                .build())
                                .prices(AvailabilityResponse.PriceInfo.builder()
                                                .economy(train.getEconomyPrice())
                                                .business(train.getBusinessPrice())
                                                .firstClass(train.getFirstClassPrice())
                                                .build())
                                .status(schedule.getStatus().name())
                                .build();
        }
}
