package com.example.parking.model;

import java.time.LocalDateTime;

public record ReservationResponse(
        String reservationId,
        int spaceId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String licensePlate
) {}
