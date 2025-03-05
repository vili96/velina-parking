package com.example.parking.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request model for creating a new parking reservation.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {
    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotBlank(message = "License plate is required")
    @Pattern(regexp = "^[A-Z0-9]{1,10}$", message = "License plate format is invalid")
    private String licensePlate;
}
