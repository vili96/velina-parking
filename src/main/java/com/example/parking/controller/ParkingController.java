package com.example.parking.controller;

import com.example.parking.model.ApiResponse;
import com.example.parking.model.ReservationListResponse;
import com.example.parking.model.ReservationRequest;
import com.example.parking.model.ReservationResponse;
import com.example.parking.service.contract.ParkingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/parking")
public class ParkingController {

    private final ParkingService parkingService;

    @PostMapping("/reservations")
    public ResponseEntity<ApiResponse<ReservationResponse>> createReservation(
            @Valid @RequestBody ReservationRequest request
    ) {
        var response = parkingService.createReservation(request);
        return new ResponseEntity<>(ApiResponse.success(response), HttpStatus.CREATED);
    }

    @DeleteMapping("/reservations/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(@PathVariable String id) {
        parkingService.cancelReservation(id);
        return new ResponseEntity<>(ApiResponse.success(null), HttpStatus.OK);
    }

    @GetMapping("/reservations/{id}")
    public ResponseEntity<ApiResponse<ReservationResponse>> getReservation(@PathVariable String id) {
        var response = parkingService.getReservation(id);
        return new ResponseEntity<>(ApiResponse.success(response), HttpStatus.OK);
    }

    @GetMapping("/reservations")
    public ResponseEntity<ApiResponse<ReservationListResponse>> getAllReservations() {
        var reservations = parkingService.getAllReservations();
        var totalSpaces = parkingService.getTotalSpaces();
        var listResponse = ReservationListResponse.from(reservations, totalSpaces);
        return new ResponseEntity<>(ApiResponse.success(listResponse), HttpStatus.OK);
    }
}