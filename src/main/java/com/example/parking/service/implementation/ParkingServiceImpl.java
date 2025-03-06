package com.example.parking.service.implementation;

import com.example.parking.entity.ParkingReservation;
import com.example.parking.entity.ParkingSpace;
import com.example.parking.exception.ParkingFullException;
import com.example.parking.exception.ReservationConflictException;
import com.example.parking.exception.ReservationNotFoundException;
import com.example.parking.mapper.ReservationMapper;
import com.example.parking.model.ReservationRequest;
import com.example.parking.model.ReservationResponse;
import com.example.parking.repository.ParkingReservationRepository;
import com.example.parking.service.contract.ParkingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.example.parking.util.Constants.*;
import static com.example.parking.util.TimeUtil.getInstant;
import static com.example.parking.util.TimeUtil.getTimeOneHourLater;

@Service
@RequiredArgsConstructor
public class ParkingServiceImpl implements ParkingService {

    private final ParkingReservationRepository reservationRepository;
    private final ReservationMapper reservationMapper;
    private final List<ParkingSpace> parkingSpaces;
    private final Random random = new Random();
    private final Object reservationLock = new Object();

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ReservationResponse createReservation(ReservationRequest request) {
        synchronized (reservationLock) {
            var requestStartTime = request.getStartTime();
            var now = LocalDateTime.now();

            if (requestStartTime.isBefore(now)) {
                throw new IllegalArgumentException(MSG_RESERVATION_FUTURE);
            }

            var startTime = getInstant(requestStartTime);
            var endTime = getTimeOneHourLater(startTime);
            var sameHourList =
                    reservationRepository.findByLicensePlateAndExactStart(request.getLicensePlate(), startTime);

            if (!sameHourList.isEmpty()) {
                throw new ReservationConflictException(String.format(MSG_RESERVATION_SAME_HOUR, request.getLicensePlate()));
            }

            var overlappingList = reservationRepository.findOverlappingByLicensePlate(
                    request.getLicensePlate(),
                    startTime,
                    endTime
            );

            if (!overlappingList.isEmpty()) {
                throw new ReservationConflictException(String.format(MSG_RESERVATION_CONFLICT, request.getLicensePlate()));
            }

            checkCapacity(startTime, endTime);

            var spaceId = findAvailableSpace(startTime, endTime);
            var reservation = new ParkingReservation(spaceId, startTime, endTime, request.getLicensePlate());
            var saved = reservationRepository.save(reservation);

            return reservationMapper.toResponse(saved);
        }
    }

    @Override
    @Transactional
    public void cancelReservation(String reservationId) {
        synchronized (reservationLock) {
            var reservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new ReservationNotFoundException(
                            MSG_RESERVATION_NOT_FOUND + reservationId));

            reservationRepository.delete(reservation);
        }
    }

    @Override
    public ReservationResponse getReservation(String reservationId) {
        var reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(
                        MSG_RESERVATION_NOT_FOUND + reservationId));
        return reservationMapper.toResponse(reservation);
    }

    @Override
    public List<ReservationResponse> getAllReservations() {
        var reservations = reservationRepository.findAll();
        return reservations.stream()
                .map(reservationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public int getTotalSpaces() {
        return parkingSpaces.size();
    }

    private void checkCapacity(Instant startTime, Instant endTime) {
        var reservationCount = reservationRepository.countByTimeRange(startTime, endTime);
        var maxReservations = (int) (parkingSpaces.size() * MAX_CAPACITY_PERCENTAGE);
        if (reservationCount >= maxReservations) {
            throw new ParkingFullException(MSG_MAX_CAPACITY);
        }
    }

    private int findAvailableSpace(Instant startTime, Instant endTime) {
        var conflictingReservations = reservationRepository.findAllByTimeRange(startTime, endTime);
        var occupiedSpaces = new HashSet<Integer>();

        conflictingReservations.forEach(r -> occupiedSpaces.add(r.getSpaceId()));

        var totalSpaces = parkingSpaces.size();
        var availableSpaces = IntStream.rangeClosed(1, totalSpaces)
                .filter(spaceId -> !occupiedSpaces.contains(spaceId))
                .boxed()
                .toList();

        if (availableSpaces.isEmpty()) {
            throw new ParkingFullException(MSG_NO_SPACE_AVAILABLE);
        }

        return availableSpaces.get(random.nextInt(availableSpaces.size()));
    }
}