package com.example.demo.controller;

import com.example.demo.dto.NearStationDto;
import com.example.demo.dto.NearStationRequestDto;
import com.example.demo.service.NearStationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nearstations")
@RequiredArgsConstructor
public class NearStationController {
    private final NearStationService service;

    @GetMapping
    public ResponseEntity<List<NearStationDto>> findByQuery(
        @RequestParam double x,
        @RequestParam double y,
        @RequestParam int radius
    ) {
        return ResponseEntity.ok(service.getStationsByPos(x, y, radius));
    }

    @PostMapping
    public ResponseEntity<List<NearStationDto>> findByBody(
        @RequestBody NearStationRequestDto request
    ) {
        return ResponseEntity.ok(
            service.getStationsByPos(request.getX(), request.getY(), request.getRadius())
        );
    }
}
