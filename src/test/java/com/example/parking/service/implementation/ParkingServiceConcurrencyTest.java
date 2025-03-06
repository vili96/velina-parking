package com.example.parking.service.implementation;

import com.example.parking.repository.ParkingReservationRepository;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.example.parking.util.ParkingServiceTestHelper.createReservationRequest;
import static com.example.parking.util.TestConstants.SHOULD_NOT_EXCEED_80_SUCCESSFUL_RESERVATIONS;
import static com.example.parking.util.TestConstants.THREAD_POOL_SIZE;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ParkingServiceConcurrencyTest {

    private final ParkingServiceImpl parkingService;

    private final ParkingReservationRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void testConcurrentReservations() throws InterruptedException {
        var startTime = LocalDateTime.now().plusHours(2);
        var tasks = createReservationTasks(startTime, 85);

        try (var executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE)) {
            var results = executor.invokeAll(tasks);

            executor.shutdown();
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                System.err.println("Warning: Some reservation tasks did not complete in time. Forcing shutdown...");
                executor.shutdownNow();
            }

            long successCount = results.stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception e) {
                            System.err.println("Error processing reservation task: " + e.getMessage());
                            return false;
                        }
                    })
                    .filter(success -> success)
                    .count();

            assertTrue(successCount <= 80, SHOULD_NOT_EXCEED_80_SUCCESSFUL_RESERVATIONS);
        }
    }

    private List<Callable<Boolean>> createReservationTasks(LocalDateTime startTime, int numberOfTasks) {
        var tasks = new ArrayList<Callable<Boolean>>();
        for (var i = 0; i < numberOfTasks; i++) {
            tasks.add(() -> {
                try {
                    var plate = "PLATE" + Thread.currentThread().threadId();
                    parkingService.createReservation(createReservationRequest(startTime, plate));
                    return true;
                } catch (Exception ex) {
                    return false;
                }
            });
        }
        return tasks;
    }
}
