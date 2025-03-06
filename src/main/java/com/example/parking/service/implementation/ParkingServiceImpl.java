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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.example.parking.util.Constants.*;

@Service
public class ParkingServiceImpl implements ParkingService {

    private final ParkingReservationRepository reservationRepository;
    private final ReservationMapper reservationMapper;
    private final List<ParkingSpace> parkingSpaces;
    private final Random random = new Random();

    // Lock object for concurrency
    private final Object reservationLock = new Object();

    @Autowired
    public ParkingServiceImpl(
            ParkingReservationRepository reservationRepository,
            ReservationMapper reservationMapper,
            List<ParkingSpace> parkingSpaces
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationMapper = reservationMapper;
        this.parkingSpaces = parkingSpaces;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ReservationResponse createReservation(ReservationRequest request) {
        // We only lock around the logic that checks and updates reservations
        synchronized (reservationLock) {
            var requestStartTime = request.getStartTime();
            var now = LocalDateTime.now();

            if (requestStartTime.isBefore(now)) {
                throw new IllegalArgumentException(MSG_RESERVATION_FUTURE);
            }

            // Convert to Instant
            var startTime = requestStartTime.atZone(ZoneId.systemDefault()).toInstant();
            var endTime = startTime.plusSeconds(3600); // exactly 1 hour

            var sameHourList = reservationRepository.findByLicensePlateAndExactStart(
                    request.getLicensePlate(),
                    startTime
            );
            if (!sameHourList.isEmpty()) {
                // e.g. "You already have a reservation at 9AM for license plate ABC123."
                throw new ReservationConflictException(
                        "You already have a reservation at this exact time for license plate: "
                                + request.getLicensePlate()
                );
            }

            // 2) Check if the user has *any overlapping reservation* for the same plate.
            var overlappingList = reservationRepository.findOverlappingByLicensePlate(
                    request.getLicensePlate(),
                    startTime,
                    endTime
            );
            if (!overlappingList.isEmpty()) {
                throw new ReservationConflictException(
                        "You already have an overlapping reservation in this time range for license plate: "
                                + request.getLicensePlate()
                );
            }

            // 1) Check capacity
            checkCapacity(startTime, endTime);

            // 2) Find a free space
            var spaceId = findAvailableSpace(startTime, endTime);

            // 3) Create & save
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
        var occupiedSpaces = new BitSet(parkingSpaces.size() + 1);

        // Mark all reserved spaces
        for (var r : conflictingReservations) {
            occupiedSpaces.set(r.getSpaceId());
        }

        var totalSpaces = parkingSpaces.size();
        var availableSpaces = IntStream.rangeClosed(1, totalSpaces)
                .filter(spaceId -> !occupiedSpaces.get(spaceId))
                .boxed()
                .toList();

        if (availableSpaces.isEmpty()) {
            throw new ParkingFullException(MSG_NO_SPACE_AVAILABLE);
        }

        // Pick random to distribute usage
        return availableSpaces.get(random.nextInt(availableSpaces.size()));
    }
}