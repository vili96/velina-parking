package com.example.parking.service.implementation;

import com.example.parking.model.ReservationRequest;
import com.example.parking.exception.ParkingFullException;
import com.example.parking.exception.ReservationNotFoundException;
import com.example.parking.entity.ParkingReservation;
import com.example.parking.entity.ParkingSpace;
import com.example.parking.repository.ParkingReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ParkingServiceImplTest {

    @Mock
    private ParkingReservationRepository reservationRepository;

    @Mock
    private List<ParkingSpace> parkingSpaces;

    @InjectMocks
    private ParkingServiceImpl parkingService;

    private LocalDateTime futureStartTime;
    private ParkingSpace availableSpace;
    private ParkingReservation mockReservation;

    @BeforeEach
    void setUp() {
        // Set up future time for valid reservation
        futureStartTime = LocalDateTime.now().plusHours(1);

        // Create a test parking space
        availableSpace = new ParkingSpace(1);

        // Create a mock list of parking spaces
        var spaces = new ArrayList<ParkingSpace>();
        for (int i = 1; i <= 100; i++) {
            spaces.add(new ParkingSpace(i));
        }

        // Setup mockReservation
        mockReservation = new ParkingReservation(
                1,
                futureStartTime,
                futureStartTime.plusHours(1),
                "ABC123"
        );

        // Mock parkingSpaces behavior with lenient stubs
        // (these won't be used in all test methods)
        lenient().when(parkingSpaces.size()).thenReturn(100);
        lenient().when(parkingSpaces.stream()).thenReturn(spaces.stream());
    }

    @Test
    void createReservation_Success() {
        // Arrange
        var request = new ReservationRequest(futureStartTime, "ABC123");

        when(reservationRepository.findAllByTimeRange(any(), any())).thenReturn(new ArrayList<>());
        when(reservationRepository.save(any())).thenReturn(mockReservation);

        // Act
        var result = parkingService.createReservation(request);

        // Assert
        assertNotNull(result);
        assertEquals(mockReservation.getId(), result.reservationId());
        assertEquals(mockReservation.getSpaceId(), result.spaceId());
        assertEquals(mockReservation.getLicensePlate(), result.licensePlate());

        verify(reservationRepository, times(1)).save(any());
    }

    @Test
    void createReservation_PastTime_ThrowsException() {
        // Arrange
        var pastTime = LocalDateTime.now().minusHours(1);
        var request = new ReservationRequest(pastTime, "ABC123");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            parkingService.createReservation(request);
        });

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_ParkingFull_ThrowsException() {
        // Arrange
        var request = new ReservationRequest(futureStartTime, "ABC123");

        // Create list with 81 reservations (>80% of 100)
        var fullReservations = new ArrayList<ParkingReservation>();
        for (int i = 0; i < 81; i++) {
            fullReservations.add(new ParkingReservation(
                    i + 1,
                    futureStartTime,
                    futureStartTime.plusHours(1),
                    "CAR" + i
            ));
        }

        when(reservationRepository.findAllByTimeRange(any(), any())).thenReturn(fullReservations);

        // Act & Assert
        assertThrows(ParkingFullException.class, () -> {
            parkingService.createReservation(request);
        });

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cancelReservation_Success() {
        // Arrange
        var reservationId = "test-id";
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(mockReservation));

        // Act
        parkingService.cancelReservation(reservationId);

        // Assert
        verify(reservationRepository, times(1)).delete(mockReservation);
    }

    @Test
    void cancelReservation_NotFound_ThrowsException() {
        // Arrange
        var reservationId = "non-existent-id";
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ReservationNotFoundException.class, () -> {
            parkingService.cancelReservation(reservationId);
        });

        verify(reservationRepository, never()).delete(any());
    }

    @Test
    void getReservation_Success() {
        // Arrange
        var reservationId = "test-id";
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(mockReservation));

        // Act
        var result = parkingService.getReservation(reservationId);

        // Assert
        assertNotNull(result);
        assertEquals(mockReservation.getId(), result.reservationId());
        assertEquals(mockReservation.getSpaceId(), result.spaceId());
    }

    @Test
    void getReservation_NotFound_ThrowsException() {
        // Arrange
        var reservationId = "non-existent-id";
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ReservationNotFoundException.class, () -> {
            parkingService.getReservation(reservationId);
        });
    }

    @Test
    void getAllReservations_Success() {
        // Arrange
        var reservationList = List.of(mockReservation);
        when(reservationRepository.findAll()).thenReturn(reservationList);

        // Act
        var result = parkingService.getAllReservations();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(mockReservation.getId(), result.get(0).reservationId());
    }
}