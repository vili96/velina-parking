package com.example.parking.config;

import com.example.parking.entity.ParkingSpace;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

import static com.example.parking.util.Constants.TOTAL_PARKING_SPACES;

@Configuration
public class ParkingConfig {

    @Bean
    public List<ParkingSpace> parkingSpaces() {
        var spaces = new ArrayList<ParkingSpace>();
        for (int i = 1; i <= TOTAL_PARKING_SPACES; i++) {
            spaces.add(new ParkingSpace(i));
        }
        return spaces;
    }
}
