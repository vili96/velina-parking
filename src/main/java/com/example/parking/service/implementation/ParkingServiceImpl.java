package com.example.parking.service.implementation;

import com.example.parking.model.ReservationRequest;
import com.example.parking.model.ReservationResponse;
import com.example.parking.exception.ParkingFullException;
import com.example.parking.exception.ReservationNotFoundException;
import com.example.parking.entity.ParkingReservation;
import com.example.parking.entity.ParkingSpace;
import com.example.parking.repository.ParkingReservationRepository;
import com.example.parking.service.contract.ParkingService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ParkingServiceImpl implements ParkingService {

    private final List<ParkingSpace> parkingSpaces;
    private final ParkingReservationRepository reservationRepository;
    private static final int MAX_CAPACITY_PERCENT = 80;

    public ParkingServiceImpl(List<ParkingSpace> parkingSpaces, ParkingReservationRepository reservationRepository) {
        this.parkingSpaces = parkingSpaces;
        this.reservationRepository = reservationRepository;
    }

    @Override
    public ReservationResponse createReservation(ReservationRequest request) {
        // Validate that start time is in the future
        if (request.startTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Reservation start time must be in the future");
        }

        // Calculate end time (1 hour slot)
        var endTime = request.startTime().plusHours(1);

        // Find active reservations for the requested time slot
        var activeReservations = reservationRepository.findAllByTimeRange(
                request.startTime(), endTime);

        // Check if parking lot utilization would exceed 80%
        var maxSpaces = parkingSpaces.size() * MAX_CAPACITY_PERCENT / 100;
        if (activeReservations.size() >= maxSpaces) {
            throw new ParkingFullException("Parking lot is at maximum capacity for the requested time slot");
        }

        // Find an available space
        var availableSpaceOpt = parkingSpaces.stream()
                .filter(space -> isSpaceAvailable(space, request.startTime(), endTime))
                .findFirst();

        if (availableSpaceOpt.isEmpty()) {
            throw new ParkingFullException("No available parking spaces for the requested time slot");
        }

        var availableSpace = availableSpaceOpt.get();

        // Create and save reservation
        var reservation = new ParkingReservation(
                availableSpace.getId(),
                request.startTime(),
                endTime,
                request.licensePlate()
        );

        var savedReservation = reservationRepository.save(reservation);

        return mapToResponse(savedReservation);
    }

    @Override
    public void cancelReservation(String reservationId) {
        var reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found with id: " + reservationId));

        reservationRepository.delete(reservation);
    }

    @Override
    public ReservationResponse getReservation(String reservationId) {
        var reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found with id: " + reservationId));

        return mapToResponse(reservation);
    }

    @Override
    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private boolean isSpaceAvailable(ParkingSpace space, LocalDateTime start, LocalDateTime end) {
        // Check if there are any reservations for this space during the requested time slot
        var conflictingReservations = reservationRepository.findAllByTimeRange(start, end).stream()
                .filter(reservation -> reservation.getSpaceId() == space.getId())
                .findAny();

        return conflictingReservations.isEmpty();
    }

    private ReservationResponse mapToResponse(ParkingReservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getSpaceId(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getLicensePlate()
        );
    }
}