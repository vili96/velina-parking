package com.example.parking.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Response model for a single parking reservation.
 * Contains all the details about a specific reservation.
 */
@Getter
@AllArgsConstructor
public class ReservationResponse {
    private final String reservationId;
    private final int spaceId;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final String licensePlate;
    private final boolean active;

    /**
     * Simplified constructor that assumes the reservation is active.
     */
    public ReservationResponse(
            String reservationId,
            int spaceId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String licensePlate
    ) {
        this(reservationId, spaceId, startTime, endTime, licensePlate, true);
    }
}
