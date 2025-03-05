package com.example.parking.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Response model specifically for lists of parking reservations.
 * This provides a clear contract for what the client expects when requesting multiple reservations.
 */
@Getter
@AllArgsConstructor
public class ReservationListResponse {
    private final List<ReservationResponse> reservations;
    private final int total;
    private final int availableSpaces;

    /**
     * Factory method to create a reservation list response from a list of reservations.
     *
     * @param reservations The list of reservation responses
     * @param totalSpaces  The total number of parking spaces
     * @return A formatted reservation list response
     */
    public static ReservationListResponse from(List<ReservationResponse> reservations, int totalSpaces) {
        return new ReservationListResponse(
                reservations,
                reservations.size(),
                totalSpaces - reservations.size()
        );
    }
}
