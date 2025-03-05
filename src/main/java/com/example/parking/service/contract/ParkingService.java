package com.example.parking.service.contract;

import com.example.parking.model.ReservationRequest;
import com.example.parking.model.ReservationResponse;

import java.util.List;

public interface ParkingService {
    ReservationResponse createReservation(ReservationRequest request);

    void cancelReservation(String reservationId);

    ReservationResponse getReservation(String reservationId);

    List<ReservationResponse> getAllReservations();
}
