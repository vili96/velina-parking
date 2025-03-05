package com.example.parking.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

public record ReservationRequest(
        @NotNull(message = "Start time is required")
        LocalDateTime startTime,

        @NotBlank(message = "License plate is required")
        @Pattern(regexp = "^[A-Z0-9]{1,10}$", message = "License plate format is invalid")
        String licensePlate
) {}
