package com.example.parking.service.implementation;

import com.example.parking.entity.ParkingReservation;
import com.example.parking.entity.ParkingSpace;
import com.example.parking.exception.ParkingFullException;
import com.example.parking.exception.ReservationConflictException;
import com.example.parking.exception.ReservationNotFoundException;
import com.example.parking.mapper.ReservationMapper;
import com.example.parking.model.ReservationRequest;
import com.example.parking.model.ReservationResponse;
import com.example.parking.repository.ParkingReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.example.parking.util.TestConstants.FUTURE_RESERVATION_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingServiceImplTest {

    @Mock
    private ParkingReservationRepository reservationRepository;

    @Mock
    private List<ParkingSpace> parkingSpaces;

    @Spy
    private ReservationMapper reservationMapper = Mappers.getMapper(ReservationMapper.class);

    @InjectMocks
    private ParkingServiceImpl parkingService;

    private LocalDateTime futureLdtStartTime;
    private Instant futureStartTime;
    private ParkingSpace availableSpace;
    private ParkingReservation mockReservation;
    private ReservationResponse mockResponse;

    @BeforeEach
    void setUp() {
        // Set up future time for valid reservation
        futureLdtStartTime = LocalDateTime.now().plusHours(1);
        futureStartTime = futureLdtStartTime.atZone(ZoneId.systemDefault()).toInstant();
        Instant futureEndTime = futureLdtStartTime.plusHours(1).atZone(ZoneId.systemDefault()).toInstant();

        // Create a test parking space
        availableSpace = new ParkingSpace(1);

        // Create a mock list of parking spaces
        var spaces = new ArrayList<ParkingSpace>();
        for (int i = 1; i <= 100; i++) {
            spaces.add(new ParkingSpace(i));
        }

        // Setup mockReservation
        mockReservation = new ParkingReservation(
                FUTURE_RESERVATION_ID, // Use the ID constructor
                1,
                futureStartTime,
                futureStartTime.plusSeconds(3600), // 1 hour in seconds
                "ABC123"
        );

        // Create mock response
        mockResponse = new ReservationResponse();
        mockResponse.setReservationId(FUTURE_RESERVATION_ID);
        mockResponse.setSpaceId(1);
        mockResponse.setLicensePlate("ABC123");
        mockResponse.setStartTime(futureLdtStartTime);
        mockResponse.setEndTime(futureLdtStartTime.plusHours(1));

        // Mock parkingSpaces behavior with lenient stubs
        // (these won't be used in all test methods)
        lenient().when(parkingSpaces.size()).thenReturn(100);
        lenient().when(parkingSpaces.stream()).thenReturn(spaces.stream());
    }

    @Test
    void createReservation_Success() {
        // Arrange
        var request = new ReservationRequest(futureLdtStartTime, "ABC123");

        when(reservationRepository.countByTimeRange(any(Instant.class), any(Instant.class))).thenReturn(0L);
        when(reservationRepository.findAllByTimeRange(any(Instant.class), any(Instant.class))).thenReturn(new ArrayList<>());
        when(reservationRepository.save(any(ParkingReservation.class))).thenReturn(mockReservation);

        // Mock the mapper to return our mockResponse
        doReturn(mockResponse).when(reservationMapper).toResponse(any(ParkingReservation.class));

        // Act
        var result = parkingService.createReservation(request);

        // Assert
        assertNotNull(result);
        assertEquals(mockReservation.getId(), result.getReservationId());
        assertEquals(mockReservation.getSpaceId(), result.getSpaceId());
        assertEquals(mockReservation.getLicensePlate(), result.getLicensePlate());

        verify(reservationRepository, times(1)).save(any(ParkingReservation.class));
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
        var request = new ReservationRequest(futureLdtStartTime, "ABC123");

        // Mock the capacity check to indicate it's full (81 is >80% of 100)
        when(reservationRepository.countByTimeRange(any(Instant.class), any(Instant.class))).thenReturn(81L);

        // Act & Assert
        assertThrows(ParkingFullException.class, () -> {
            parkingService.createReservation(request);
        });

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_SameHour_ThrowsConflict() {
        var request = new ReservationRequest(futureLdtStartTime, "ABC123");
        var startInstant = futureLdtStartTime.atZone(ZoneId.systemDefault()).toInstant();

        // Mock that same license plate and exact start time already exist
        when(reservationRepository.findByLicensePlateAndExactStart("ABC123", startInstant))
                .thenReturn(List.of(new ParkingReservation()));

        assertThrows(ReservationConflictException.class, () -> parkingService.createReservation(request));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_OverlappingTime_ThrowsConflict() {
        var request = new ReservationRequest(futureLdtStartTime, "ABC123");
        var startInstant = futureLdtStartTime.atZone(ZoneId.systemDefault()).toInstant();
        var endInstant = startInstant.plusSeconds(3600);

        // Mock that there is an overlapping reservation for same plate
        when(reservationRepository.findByLicensePlateAndExactStart("ABC123", startInstant))
                .thenReturn(Collections.emptyList());
        when(reservationRepository.findOverlappingByLicensePlate("ABC123", startInstant, endInstant))
                .thenReturn(List.of(new ParkingReservation()));

        assertThrows(ReservationConflictException.class, () -> parkingService.createReservation(request));
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
        var reservationId = FUTURE_RESERVATION_ID;
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(mockReservation));
        doReturn(mockResponse).when(reservationMapper).toResponse(mockReservation);

        // Act
        var result = parkingService.getReservation(reservationId);

        // Assert
        assertNotNull(result);
        assertEquals(mockReservation.getId(), result.getReservationId());
        assertEquals(mockReservation.getSpaceId(), result.getSpaceId());
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
        doReturn(mockResponse).when(reservationMapper).toResponse(mockReservation);

        // Act
        var result = parkingService.getAllReservations();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(mockReservation.getId(), result.getFirst().getReservationId());
    }
}