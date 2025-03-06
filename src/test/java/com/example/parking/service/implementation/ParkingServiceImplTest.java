package com.example.parking.service.implementation;

import com.example.parking.entity.ParkingReservation;
import com.example.parking.entity.ParkingSpace;
import com.example.parking.exception.ParkingFullException;
import com.example.parking.exception.ReservationConflictException;
import com.example.parking.exception.ReservationNotFoundException;
import com.example.parking.mapper.ReservationMapper;
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

import static com.example.parking.util.ParkingServiceTestHelper.*;
import static com.example.parking.util.TestConstants.*;
import static com.example.parking.util.TimeUtil.getTimeOneHourLater;
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
    private ParkingReservation mockReservation;
    private ReservationResponse mockResponse;

    @BeforeEach
    void setUp() {
        futureLdtStartTime = getTimeOneHourLater(LocalDateTime.now());
        var futureStartTime = futureLdtStartTime.atZone(ZoneId.systemDefault()).toInstant();
        var spaces = createParkingSpaces();

        mockReservation = createMockReservation(
                TEST_RESERVATION_ID, 1, futureStartTime, getTimeOneHourLater(futureStartTime), LICENSE_PLATE
        );

        mockResponse = createReservationResponse(
                TEST_RESERVATION_ID, 1, LICENSE_PLATE, futureLdtStartTime, getTimeOneHourLater(futureLdtStartTime)
        );

        lenient().when(parkingSpaces.size()).thenReturn(100);
        lenient().when(parkingSpaces.stream()).thenReturn(spaces.stream());
    }

    @Test
    void createReservation_Success() {
        var request = createReservationRequest(futureLdtStartTime, LICENSE_PLATE);

        when(reservationRepository.countByTimeRange(any(Instant.class), any(Instant.class))).thenReturn(0L);
        when(reservationRepository.findAllByTimeRange(any(Instant.class), any(Instant.class)))
                .thenReturn(new ArrayList<>());
        when(reservationRepository.save(any(ParkingReservation.class))).thenReturn(mockReservation);

        doReturn(mockResponse).when(reservationMapper).toResponse(any(ParkingReservation.class));

        var result = parkingService.createReservation(request);
        assertNotNull(result);
        assertEquals(mockReservation.getId(), result.getReservationId());
        assertEquals(mockReservation.getSpaceId(), result.getSpaceId());
        assertEquals(mockReservation.getLicensePlate(), result.getLicensePlate());

        verify(reservationRepository, times(1)).save(any(ParkingReservation.class));
    }

    @Test
    void createReservation_PastTime_ThrowsException() {
        var pastTime = LocalDateTime.now().minusHours(1);
        var request = createReservationRequest(pastTime, LICENSE_PLATE);

        assertThrows(IllegalArgumentException.class, () -> parkingService.createReservation(request));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_ParkingFull_ThrowsException() {
        var request = createReservationRequest(futureLdtStartTime, LICENSE_PLATE);

        when(reservationRepository.countByTimeRange(any(Instant.class), any(Instant.class))).thenReturn(81L);

        assertThrows(ParkingFullException.class, () -> parkingService.createReservation(request));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_SameHour_ThrowsConflict() {
        var request = createReservationRequest(futureLdtStartTime, LICENSE_PLATE);
        var startInstant = futureLdtStartTime.atZone(ZoneId.systemDefault()).toInstant();

        when(reservationRepository.findByLicensePlateAndExactStart(LICENSE_PLATE, startInstant))
                .thenReturn(List.of(new ParkingReservation()));

        assertThrows(ReservationConflictException.class, () -> parkingService.createReservation(request));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_OverlappingTime_ThrowsConflict() {
        var request = createReservationRequest(futureLdtStartTime, LICENSE_PLATE);
        var startInstant = futureLdtStartTime.atZone(ZoneId.systemDefault()).toInstant();
        var endInstant = getTimeOneHourLater(startInstant);

        when(reservationRepository.findByLicensePlateAndExactStart(LICENSE_PLATE, startInstant))
                .thenReturn(Collections.emptyList());
        when(reservationRepository.findOverlappingByLicensePlate(LICENSE_PLATE, startInstant, endInstant))
                .thenReturn(List.of(new ParkingReservation()));

        assertThrows(ReservationConflictException.class, () -> parkingService.createReservation(request));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cancelReservation_Success() {
        var reservationId = TEST_RESERVATION_ID;
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(mockReservation));

        parkingService.cancelReservation(reservationId);
        verify(reservationRepository, times(1)).delete(mockReservation);
    }

    @Test
    void cancelReservation_NotFound_ThrowsException() {
        var reservationId = NON_EXISTENT_RESERVATION_ID;
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class, () -> parkingService.cancelReservation(reservationId));
        verify(reservationRepository, never()).delete(any());
    }

    @Test
    void getReservation_Success() {
        var reservationId = TEST_RESERVATION_ID;
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(mockReservation));
        doReturn(mockResponse).when(reservationMapper).toResponse(mockReservation);

        var result = parkingService.getReservation(reservationId);
        assertNotNull(result);
        assertEquals(mockReservation.getId(), result.getReservationId());
        assertEquals(mockReservation.getSpaceId(), result.getSpaceId());
    }

    @Test
    void getReservation_NotFound_ThrowsException() {
        var reservationId = NON_EXISTENT_RESERVATION_ID;
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class, () -> parkingService.getReservation(reservationId));
    }

    @Test
    void getAllReservations_Success() {
        var reservationList = List.of(mockReservation);
        when(reservationRepository.findAll()).thenReturn(reservationList);
        doReturn(mockResponse).when(reservationMapper).toResponse(mockReservation);

        var result = parkingService.getAllReservations();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(mockReservation.getId(), result.getFirst().getReservationId());
    }
}
