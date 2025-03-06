package com.example.parking.service.implementation;


import com.example.parking.model.ReservationRequest;
import com.example.parking.repository.ParkingReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test to verify concurrency control with real H2 DB.
 */
@SpringBootTest
class ParkingServiceConcurrencyTest {

    @Autowired
    private ParkingServiceImpl parkingService;

    @Autowired
    private ParkingReservationRepository repository;

    @BeforeEach
    void cleanUp() {
        // Clear existing reservations
        repository.deleteAll();
    }

    @Test
    void testConcurrentReservations() throws InterruptedException {
        // We'll attempt to create 85 reservations for the same time slot,
        // but only 80 should succeed due to the 80% capacity rule (80% of 100).
        var startTime = LocalDateTime.now().plusHours(2);

        // Build a list of tasks that call createReservation
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 85; i++) {
            tasks.add(() -> {
                try {
                    parkingService.createReservation(new ReservationRequest(startTime, "PLATE" + Thread.currentThread().getId()));
                    return true;  // success
                } catch (Exception ex) {
                    // If it fails (ParkingFullException, etc.), return false
                    return false;
                }
            });
        }

        var executor = Executors.newFixedThreadPool(20);
        var results = executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        // Count how many succeeded
        long successCount = results.stream().filter(f -> {
            try {
                return f.get();
            } catch (Exception e) {
                return false;
            }
        }).count();

        // We expect that no more than 80 reservations can succeed
        assertTrue(successCount <= 80, "Should not exceed 80 successful reservations");
    }
}
