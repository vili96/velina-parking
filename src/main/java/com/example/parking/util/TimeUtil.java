package com.example.parking.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static com.example.parking.util.Constants.ONE_HOUR_IN_SECONDS;

public class TimeUtil {

    public static Instant getTimeOneHourLater(Instant futureTime) {
        return futureTime.plusSeconds(ONE_HOUR_IN_SECONDS);
    }

    public static LocalDateTime getTimeOneHourLater(LocalDateTime time) {
        return time.plusHours(1);
    }

    public static Instant getInstant(LocalDateTime requestStartTime) {
        return requestStartTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
