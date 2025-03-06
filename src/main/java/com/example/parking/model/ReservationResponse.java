package com.example.parking.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

//@Getter
//@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {
    private String reservationId;
    private int spaceId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String licensePlate;

    // Remove all manually written getters/setters
}
