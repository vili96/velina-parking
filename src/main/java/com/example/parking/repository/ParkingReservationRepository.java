package com.example.parking.repository;

import com.example.parking.entity.ParkingReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ParkingReservationRepository extends JpaRepository<ParkingReservation, String> {

    @Query("""
            SELECT r FROM ParkingReservation r
             WHERE (r.startTime < :endTime AND r.endTime > :startTime)
             """)
    List<ParkingReservation> findAllByTimeRange(@Param("startTime") Instant startTime,
                                                @Param("endTime") Instant endTime);

    @Query("""
            SELECT r FROM ParkingReservation r
            WHERE r.spaceId = :spaceId
            AND (r.startTime < :endTime AND r.endTime > :startTime)
            """)
    List<ParkingReservation> findAllBySpaceIdAndTimeRange(@Param("spaceId") int spaceId,
                                                          @Param("startTime") Instant startTime,
                                                          @Param("endTime") Instant endTime);

    @Query("""
            SELECT COUNT(r) FROM ParkingReservation r
            WHERE (r.startTime < :endTime AND r.endTime > :startTime)
            """)
    long countByTimeRange(@Param("startTime") Instant startTime,
                          @Param("endTime") Instant endTime);

    @Query("""
       SELECT r FROM ParkingReservation r
       WHERE r.licensePlate = :licensePlate
         AND r.startTime = :startTime
       """)
    List<ParkingReservation> findByLicensePlateAndExactStart(
            @Param("licensePlate") String licensePlate,
            @Param("startTime") Instant startTime);

    @Query("""
       SELECT r FROM ParkingReservation r
       WHERE r.licensePlate = :licensePlate
         AND (r.startTime < :endTime AND r.endTime > :startTime)
       """)
    List<ParkingReservation> findOverlappingByLicensePlate(
            @Param("licensePlate") String licensePlate,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

}
