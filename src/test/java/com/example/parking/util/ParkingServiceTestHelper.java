package com.example.parking.util;

import com.example.parking.entity.ParkingReservation;
import com.example.parking.entity.ParkingSpace;
import com.example.parking.model.ReservationRequest;
import com.example.parking.model.ReservationResponse;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;

@UtilityClass
public class ParkingServiceTestHelper {

    public static ArrayList<ParkingSpace> createParkingSpaces() {
        var spaces = new ArrayList<ParkingSpace>();
        for (var i = 1; i <= 100; i++) {
            spaces.add(new ParkingSpace(i));
        }
        return spaces;
    }

    public static ReservationRequest createReservationRequest(LocalDateTime start, String licensePlate) {
        return new ReservationRequest(start, licensePlate);
    }

    public static ParkingReservation createMockReservation(int spaceId,
                                                           Instant startTime,
                                                           Instant endTime,
                                                           String licensePlate) {
        return new ParkingReservation(
                java.util.UUID.randomUUID().toString(),
                spaceId,
                startTime,
                endTime,
                licensePlate
        );
    }

    public static ReservationResponse createReservationResponse(ParkingReservation res) {
        var response = new ReservationResponse();
        response.setReservationId(res.getId());
        response.setSpaceId(res.getSpaceId());
        response.setLicensePlate(res.getLicensePlate());
        response.setStartTime(LocalDateTime.ofInstant(res.getStartTime(), ZoneId.systemDefault()));
        response.setEndTime(LocalDateTime.ofInstant(res.getEndTime(), ZoneId.systemDefault()));
        return response;
    }

    public static ParkingReservation createMockReservation(String id, int spaceId,
                                                           Instant startTime, Instant endTime,
                                                           String licensePlate) {
        return new ParkingReservation(id, spaceId, startTime, endTime, licensePlate);
    }

    public static ReservationResponse createReservationResponse(String id, int spaceId, String plate,
                                                                LocalDateTime start, LocalDateTime end) {
        var response = new ReservationResponse();
        response.setReservationId(id);
        response.setSpaceId(spaceId);
        response.setLicensePlate(plate);
        response.setStartTime(start);
        response.setEndTime(end);
        return response;
    }
}
