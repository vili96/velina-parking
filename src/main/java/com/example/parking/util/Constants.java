package com.example.parking.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

    public static final String MSG_RESERVATION_NOT_FOUND = "Reservation not found with ID: ";
    public static final String MSG_RESERVATION_FUTURE = "Reservation time must be in the future";
    public static final String MSG_NO_SPACE_AVAILABLE = "No parking spaces available for this time slot";
    public static final String MSG_MAX_CAPACITY = "Parking has reached maximum capacity for this time slot";

    public static final double MAX_CAPACITY_PERCENTAGE = 0.8;
}
