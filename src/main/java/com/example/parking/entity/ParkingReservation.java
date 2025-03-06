package com.example.parking.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@Table(name = "parking_reservations")
public class ParkingReservation {
    @Id
    private String id;
    private int spaceId;
    private Instant startTime;
    private Instant endTime;
    private String licensePlate;

    public ParkingReservation(int spaceId, Instant startTime, Instant endTime, String licensePlate) {
        this.id = UUID.randomUUID().toString();
        this.spaceId = spaceId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.licensePlate = licensePlate;
    }

    public ParkingReservation(String id, int spaceId, Instant startTime, Instant endTime, String licensePlate) {
        this.id = id;
        this.spaceId = spaceId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.licensePlate = licensePlate;
    }
}