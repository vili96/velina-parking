package com.example.parking.service.contract;

import com.example.parking.model.ReservationRequest;
import com.example.parking.model.ReservationResponse;

import java.util.List;

public interface ParkingService {
    /**
     * Creates a new parking reservation.
     *
     * @param request The reservation request details
     * @return The created reservation details
     */
    ReservationResponse createReservation(ReservationRequest request);

    /**
     * Cancels an existing reservation.
     *
     * @param reservationId The ID of the reservation to cancel
     */
    void cancelReservation(String reservationId);

    /**
     * Gets details for a specific reservation.
     *
     * @param reservationId The ID of the reservation to retrieve
     * @return The reservation details
     */
    ReservationResponse getReservation(String reservationId);

    /**
     * Gets all current reservations.
     *
     * @return List of all reservation details
     */
    List<ReservationResponse> getAllReservations();

    /**
     * Gets the total number of parking spaces in the lot.
     *
     * @return The total number of parking spaces
     */
    int getTotalSpaces();
}
