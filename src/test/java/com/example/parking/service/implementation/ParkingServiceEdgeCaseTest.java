package com.example.parking.service.implementation;

import com.example.parking.entity.ParkingReservation;
import com.example.parking.entity.ParkingSpace;
import com.example.parking.exception.ParkingFullException;
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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingServiceEdgeCaseTest {

    @Mock
    private ParkingReservationRepository reservationRepository;

    @Mock
    private List<ParkingSpace> parkingSpaces;

    @Spy
    private ReservationMapper reservationMapper = Mappers.getMapper(ReservationMapper.class);

    @InjectMocks
    private ParkingServiceImpl parkingService;

    private LocalDateTime futureLdtTime;
    private Instant futureTime;
    private List<ParkingSpace> mockSpaces;

    @BeforeEach
    void setUp() {
        // Setup future time for valid reservations
        futureLdtTime = LocalDateTime.now().plusHours(2);
        futureTime = futureLdtTime.atZone(ZoneId.systemDefault()).toInstant();

        // Create a list of 100 mock parking spaces
        mockSpaces = IntStream.rangeClosed(1, 100)
                .mapToObj(ParkingSpace::new)
                .collect(Collectors.toList());

        // Setup common mock behaviors
        lenient().when(parkingSpaces.size()).thenReturn(100);
        lenient().when(parkingSpaces.stream()).thenReturn(mockSpaces.stream());
    }

    @Test
    void testCapacityLimit_ExactlyAt80Percent() {
        // Arrange
        var request = new ReservationRequest(futureLdtTime, "TEST001");

        // Mock that there are 79 existing reservations (79% capacity)
        when(reservationRepository.countByTimeRange(any(Instant.class), any(Instant.class))).thenReturn(79L);

        // Mock findAllByTimeRange to return empty list so no spaces are considered occupied
        when(reservationRepository.findAllByTimeRange(any(Instant.class), any(Instant.class)))
                .thenReturn(new ArrayList<>());

        // Mock reservation saved successfully
        when(reservationRepository.save(any())).thenAnswer(i -> {
            ParkingReservation res = (ParkingReservation) i.getArgument(0);
            // Create a new reservation with an ID since we can't modify the existing one
            return new ParkingReservation(
                    java.util.UUID.randomUUID().toString(),
                    res.getSpaceId(),
                    res.getStartTime(),
                    res.getEndTime(),
                    res.getLicensePlate()
            );
        });

        // Mock the response mapping
        doAnswer(invocation -> {
            ParkingReservation res = invocation.getArgument(0);
            ReservationResponse response = new ReservationResponse();
            response.setReservationId(res.getId());
            response.setSpaceId(res.getSpaceId());
            response.setLicensePlate(res.getLicensePlate());
            response.setStartTime(LocalDateTime.ofInstant(res.getStartTime(), ZoneId.systemDefault()));
            response.setEndTime(LocalDateTime.ofInstant(res.getEndTime(), ZoneId.systemDefault()));
            return response;
        }).when(reservationMapper).toResponse(any());

        // Act & Assert
        // The 80th reservation should be allowed (exactly 80%)
        assertDoesNotThrow(() -> parkingService.createReservation(request));

        // Verify the reservation was saved
        verify(reservationRepository, times(1)).save(any());
    }

    @Test
    void testCapacityLimit_Exceeds80Percent() {
        // Arrange
        var request = new ReservationRequest(futureLdtTime, "TEST001");

        // Mock that there are 80 existing reservations (80% capacity)
        when(reservationRepository.countByTimeRange(any(Instant.class), any(Instant.class))).thenReturn(80L);

        // Act & Assert
        // The 81st reservation should throw an exception (exceeds 80%)
        var exception = assertThrows(ParkingFullException.class,
                () -> parkingService.createReservation(request));
        assertTrue(exception.getMessage().contains("maximum capacity"));

        // Verify no reservation was saved
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void testAdjacentTimeSlots_Allowed() {
        // Arrange
        var firstSlotTime = futureLdtTime;
        var secondSlotTime = futureLdtTime.plusHours(1); // Adjacent, not overlapping

        var firstRequest = new ReservationRequest(firstSlotTime, "ADJ001");
        var secondRequest = new ReservationRequest(secondSlotTime, "ADJ002");

        // Mock for capacity checks - under capacity for both time slots
        when(reservationRepository.countByTimeRange(any(Instant.class), any(Instant.class)))
                .thenReturn(10L); // Well under 80% capacity

        // For the first slot - mock no conflicts
        when(reservationRepository.findAllByTimeRange(
                any(Instant.class), any(Instant.class)))
                .thenReturn(new ArrayList<>());

        // Mock successful save for both reservations
        when(reservationRepository.save(any())).thenAnswer(i -> {
            ParkingReservation res = (ParkingReservation) i.getArgument(0);
            // Create a new reservation with an ID since we can't modify the existing one
            return new ParkingReservation(
                    java.util.UUID.randomUUID().toString(),
                    res.getSpaceId(),
                    res.getStartTime(),
                    res.getEndTime(),
                    res.getLicensePlate()
            );
        });

        // Mock the response mapping
        doAnswer(invocation -> {
            ParkingReservation res = invocation.getArgument(0);
            ReservationResponse response = new ReservationResponse();
            response.setReservationId(res.getId());
            response.setSpaceId(res.getSpaceId());
            response.setLicensePlate(res.getLicensePlate());
            response.setStartTime(LocalDateTime.ofInstant(res.getStartTime(), ZoneId.systemDefault()));
            response.setEndTime(LocalDateTime.ofInstant(res.getEndTime(), ZoneId.systemDefault()));
            return response;
        }).when(reservationMapper).toResponse(any());

        // Act & Assert
        // First reservation should succeed
        assertDoesNotThrow(() -> parkingService.createReservation(firstRequest));

        // Second reservation should also succeed
        assertDoesNotThrow(() -> parkingService.createReservation(secondRequest));

        // Verify both reservations were saved
        verify(reservationRepository, times(2)).save(any());
    }

    @Test
    void testSpaceAllocation_AllSpacesBooked() {
        // Arrange
        var request = new ReservationRequest(futureLdtTime, "FULL001");

        // Mock capacity check to pass (under 80%)
        when(reservationRepository.countByTimeRange(any(Instant.class), any(Instant.class)))
                .thenReturn(79L);

        // Create a list with all 100 spaces already reserved
        var allConflictingReservations = new ArrayList<ParkingReservation>();
        for (int i = 1; i <= 100; i++) {
            allConflictingReservations.add(createMockReservation(i));
        }

        // Mock that all spaces are already booked for this time slot
        when(reservationRepository.findAllByTimeRange(any(Instant.class), any(Instant.class)))
                .thenReturn(allConflictingReservations);

        // Act & Assert
        // Should throw exception because no space is available
        // even though we're under 80% capacity
        var exception = assertThrows(ParkingFullException.class,
                () -> parkingService.createReservation(request));

        // Verify the exception message
        assertTrue(exception.getMessage().contains("No parking spaces available"));

        // Verify no reservation was saved
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void testSameTimeConflict_DifferentSpace() {
        // Arrange
        var request = new ReservationRequest(futureLdtTime, "CONF001");

        // Mock capacity check to pass
        when(reservationRepository.countByTimeRange(any(Instant.class), any(Instant.class)))
                .thenReturn(10L);

        // Create a reservation for space #1 at the same time
        var existingReservation = createMockReservation(1);
        List<ParkingReservation> existingReservations = List.of(existingReservation);

        when(reservationRepository.findAllByTimeRange(any(Instant.class), any(Instant.class)))
                .thenReturn(existingReservations);

        when(reservationRepository.save(any())).thenAnswer(i -> {
            ParkingReservation res = (ParkingReservation) i.getArgument(0);
            res.setId("test-id");
            return res;
        });

        // Mock the response mapping
        doAnswer(invocation -> {
            ParkingReservation res = invocation.getArgument(0);
            ReservationResponse response = new ReservationResponse();
            response.setReservationId(res.getId());
            response.setSpaceId(res.getSpaceId());
            response.setLicensePlate(res.getLicensePlate());
            response.setStartTime(LocalDateTime.ofInstant(res.getStartTime(), ZoneId.systemDefault()));
            response.setEndTime(LocalDateTime.ofInstant(res.getEndTime(), ZoneId.systemDefault()));
            return response;
        }).when(reservationMapper).toResponse(any());

        // Act
        var result = parkingService.createReservation(request);

        // Assert
        assertNotEquals(1, result.getSpaceId()); // Should not be space #1
        verify(reservationRepository, times(1)).save(any());
    }

    // Helper method to create mock reservations
    private ParkingReservation createMockReservation(int spaceId) {
        // Use the constructor that takes an ID parameter
        return new ParkingReservation(
                java.util.UUID.randomUUID().toString(),
                spaceId,
                futureTime,
                futureTime.plusSeconds(3600), // 1 hour in seconds
                "MOCK" + spaceId
        );
    }
}