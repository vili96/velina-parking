package com.example.parking.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class ParkingReservation {
    private final String id;
    private final int spaceId;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final String licensePlate;

    public ParkingReservation(int spaceId, LocalDateTime startTime, LocalDateTime endTime, String licensePlate) {
        this.id = UUID.randomUUID().toString();
        this.spaceId = spaceId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.licensePlate = licensePlate;
    }

    public String getId() {
        return id;
    }

    public int getSpaceId() {
        return spaceId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (ParkingReservation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
