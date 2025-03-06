package com.example.parking.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {
    private String reservationId;
    private int spaceId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String licensePlate;
}
