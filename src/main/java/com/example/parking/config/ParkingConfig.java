package com.example.parking.config;

import com.example.parking.entity.ParkingSpace;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class ParkingConfig {

    private static final int TOTAL_PARKING_SPACES = 100;

    @Bean
    public List<ParkingSpace> parkingSpaces() {
        var spaces = new ArrayList<ParkingSpace>();
        for (int i = 1; i <= TOTAL_PARKING_SPACES; i++) {
            spaces.add(new ParkingSpace(i));
        }
        return spaces;
    }
}
