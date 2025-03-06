package com.example.parking.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ReservationListResponse {

    private final List<ReservationResponse> reservations;
    private final int total;
    private final int availableSpaces;

    public static ReservationListResponse from(List<ReservationResponse> reservations, int totalSpaces) {
        return new ReservationListResponse(
                reservations,
                reservations.size(),
                totalSpaces - reservations.size()
        );
    }
}
