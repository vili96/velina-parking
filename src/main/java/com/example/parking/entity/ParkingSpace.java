package com.example.parking.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class ParkingSpace {
    private final int id;
    private boolean available;

    public ParkingSpace(int id) {
        this.id = id;
        this.available = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var parkingSpace = (ParkingSpace) o;
        return id == parkingSpace.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}