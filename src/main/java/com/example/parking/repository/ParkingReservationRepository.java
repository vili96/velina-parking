package com.example.parking.repository;

import com.example.parking.entity.ParkingReservation;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ParkingReservationRepository {

    private final List<ParkingReservation> reservations = new ArrayList<>();

    public ParkingReservation save(ParkingReservation reservation) {
        reservations.add(reservation);
        return reservation;
    }

    public Optional<ParkingReservation> findById(String id) {
        return reservations.stream()
                .filter(reservation -> reservation.getId().equals(id))
                .findFirst();
    }

    public void delete(ParkingReservation reservation) {
        reservations.remove(reservation);
    }

    public List<ParkingReservation> findAllByTimeRange(LocalDateTime start, LocalDateTime end) {
        return reservations.stream()
                .filter(reservation ->
                        (reservation.getStartTime().isBefore(end) &&
                                reservation.getEndTime().isAfter(start)))
                .collect(Collectors.toList());
    }

    public List<ParkingReservation> findAll() {
        return new ArrayList<>(reservations);
    }
}