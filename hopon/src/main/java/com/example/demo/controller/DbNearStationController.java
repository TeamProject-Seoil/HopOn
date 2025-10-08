package com.example.demo.controller;

import com.example.demo.dto.DbNearStationDto;
import com.example.demo.service.DbNearStationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DbNearStationController {

    private final DbNearStationService dbNearStationService;

    public DbNearStationController(DbNearStationService dbNearStationService) {
        this.dbNearStationService = dbNearStationService;
    }

    @GetMapping("/stations/nearby") // 엔드포인트 이름을 좀 더 명확하게 수정
    public ResponseEntity<List<DbNearStationDto>> getNearbyStations(
            @RequestParam("lat") double latitude,
            @RequestParam("lon") double longitude,
            @RequestParam(value = "radius", defaultValue = "1000") int radius) {

        List<DbNearStationDto> nearbyStations = dbNearStationService.findNearbyStations(latitude, longitude, radius);
        return ResponseEntity.ok(nearbyStations);
    }
}
