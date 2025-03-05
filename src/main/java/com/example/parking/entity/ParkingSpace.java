package com.example.parking.entity;

import java.util.Objects;

public class ParkingSpace {
    private final int id;
    private boolean available;

    public ParkingSpace(int id) {
        this.id = id;
        this.available = true;
    }

    public int getId() {
        return id;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
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