package com.example.parking.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

    public static final String MSG_RESERVATION_NOT_FOUND = "Reservation not found with ID: ";
    public static final String MSG_RESERVATION_FUTURE = "Reservation time must be in the future";
    public static final String MSG_NO_SPACE_AVAILABLE = "No parking spaces available for this time slot";
    public static final String MSG_MAX_CAPACITY = "Parking has reached maximum capacity for this time slot";

    public static final String ERROR_STATUS = "error";
    public static final String SUCCESS_STATUS = "success";
    public static final String UNEXPECTED_ERROR_OCCURRED = "An unexpected error occurred";
    public static final String VALIDATION_ERROR = "Validation error";
    public static final String MSG_RESERVATION_CONFLICT = "You already have an overlapping reservation in this time range for license plate: %s";
    public static final String MSG_RESERVATION_SAME_HOUR = "You already have a reservation at this exact time for license plate: %s";

    public static final int TOTAL_PARKING_SPACES = 100;

    public static final double MAX_CAPACITY_PERCENTAGE = 0.8;

    public static final int ONE_HOUR_IN_SECONDS = 3600;

}
