package com.example.parking.service.implementation;

import com.example.parking.entity.ParkingReservation;
import com.example.parking.entity.ParkingSpace;
import com.example.parking.exception.ParkingFullException;
import com.example.parking.mapper.ReservationMapper;
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
import java.util.stream.IntStream;

import static com.example.parking.util.ParkingServiceTestHelper.*;
import static com.example.parking.util.TestConstants.*;
import static com.example.parking.util.TimeUtil.getTimeOneHourLater;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    @BeforeEach
    void setUp() {
        futureLdtTime = LocalDateTime.now().plusHours(2);
        futureTime = futureLdtTime.atZone(ZoneId.systemDefault()).toInstant();

        List<ParkingSpace> mockSpaces = IntStream.rangeClosed(1, TOTAL_SPACES)
                .mapToObj(ParkingSpace::new)
                .toList();

        lenient().when(parkingSpaces.size()).thenReturn(TOTAL_SPACES);
        lenient().when(parkingSpaces.stream()).thenReturn(mockSpaces.stream());
    }

    @Test
    void testCapacityLimit_ExactlyAt80Percent() {
        var request = createReservationRequest(futureLdtTime, TEST_PLATE_001);

        when(reservationRepository.countByTimeRange(any(Instant.class), any(Instant.class)))
                .thenReturn(ALMOST_MAX_CAPACITY);

        when(reservationRepository.findAllByTimeRange(any(Instant.class), any(Instant.class)))
                .thenReturn(new ArrayList<>());

        when(reservationRepository.save(any())).thenAnswer(i -> {
            var res = (ParkingReservation) i.getArgument(0);
            return createMockReservation(res.getSpaceId(), res.getStartTime(), res.getEndTime(), res.getLicensePlate());
        });

        doAnswer(invocation -> {
            var res = (ParkingReservation) invocation.getArgument(0);
            return createReservationResponse(res);
        }).when(reservationMapper).toResponse(any());

        assertDoesNotThrow(() -> parkingService.createReservation(request));
        verify(reservationRepository, times(1)).save(any());
    }

    @Test
    void testCapacityLimit_Exceeds80Percent() {
        var request = createReservationRequest(futureLdtTime, TEST_PLATE_001);

        when(reservationRepository.countByTimeRange(any(Instant.class), any(Instant.class)))
                .thenReturn(MAX_CAPACITY_LIMIT);

        var exception = assertThrows(ParkingFullException.class,
                () -> parkingService.createReservation(request));
        assertTrue(exception.getMessage().contains(ERROR_MAX_CAPACITY));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void testAdjacentTimeSlots_Allowed() {
        var firstRequest = createReservationRequest(futureLdtTime, ADJ_PLATE_001);
        var secondRequest = createReservationRequest(getTimeOneHourLater(futureLdtTime), ADJ_PLATE_002);

        when(reservationRepository.countByTimeRange(any(Instant.class), any(Instant.class)))
                .thenReturn(SAFE_CAPACITY);

        when(reservationRepository.findAllByTimeRange(any(Instant.class), any(Instant.class)))
                .thenReturn(new ArrayList<>());

        when(reservationRepository.save(any())).thenAnswer(i -> {
            var res = (ParkingReservation) i.getArgument(0);
            return createMockReservation(res.getSpaceId(), res.getStartTime(), res.getEndTime(), res.getLicensePlate());
        });

        doAnswer(invocation -> {
            var res = (ParkingReservation) invocation.getArgument(0);
            return createReservationResponse(res);
        }).when(reservationMapper).toResponse(any());

        assertDoesNotThrow(() -> parkingService.createReservation(firstRequest));
        assertDoesNotThrow(() -> parkingService.createReservation(secondRequest));
        verify(reservationRepository, times(2)).save(any());
    }

    @Test
    void testSpaceAllocation_AllSpacesBooked() {
        var request = createReservationRequest(futureLdtTime, FULL_PLATE);

        when(reservationRepository.countByTimeRange(any(Instant.class), any(Instant.class)))
                .thenReturn(ALMOST_MAX_CAPACITY);

        var allConflictingReservations = new ArrayList<ParkingReservation>();
        IntStream.rangeClosed(1, TOTAL_SPACES).forEach(i ->
                allConflictingReservations.add(createMockReservation(i, futureTime,
                        getTimeOneHourLater(futureTime), MOCK_PLATE_PREFIX + i))
        );

        when(reservationRepository.findAllByTimeRange(any(Instant.class), any(Instant.class)))
                .thenReturn(allConflictingReservations);

        var exception = assertThrows(ParkingFullException.class,
                () -> parkingService.createReservation(request));
        assertTrue(exception.getMessage().contains(ERROR_NO_SPACE));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void testSameTimeConflict_DifferentSpace() {
        var request = createReservationRequest(futureLdtTime, CONF_PLATE);

        when(reservationRepository.countByTimeRange(any(Instant.class), any(Instant.class)))
                .thenReturn(SAFE_CAPACITY);

        var existingReservation = createMockReservation(1, futureTime,
                getTimeOneHourLater(futureTime), MOCK_PLATE_PREFIX + 1);
        var existingReservations = List.of(existingReservation);

        when(reservationRepository.findAllByTimeRange(any(Instant.class), any(Instant.class)))
                .thenReturn(existingReservations);

        when(reservationRepository.save(any())).thenAnswer(i -> {
            var res = (ParkingReservation) i.getArgument(0);
            res.setId(TEST_RESERVATION_ID);
            return res;
        });

        doAnswer(invocation -> {
            var res = (ParkingReservation) invocation.getArgument(0);
            return createReservationResponse(res);
        }).when(reservationMapper).toResponse(any());

        var result = parkingService.createReservation(request);

        assertNotEquals(1, result.getSpaceId());
        verify(reservationRepository, times(1)).save(any());
    }
}