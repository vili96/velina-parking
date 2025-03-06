package com.example.parking.mapper;

import com.example.parking.entity.ParkingReservation;
import com.example.parking.model.ReservationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    @Mapping(source = "id",           target = "reservationId")
    @Mapping(source = "spaceId",      target = "spaceId")
    @Mapping(source = "licensePlate", target = "licensePlate")
    @Mapping(source = "startTime",    target = "startTime", qualifiedByName = "instantToLocalDateTime")
    @Mapping(source = "endTime",      target = "endTime",   qualifiedByName = "instantToLocalDateTime")
    ReservationResponse toResponse(ParkingReservation reservation);

    @Named("instantToLocalDateTime")
    default LocalDateTime instantToLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
