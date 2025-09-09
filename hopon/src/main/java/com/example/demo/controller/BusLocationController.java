package com.example.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.BusLocationDto;
import com.example.demo.dto.BusLocationRequestDto;
import com.example.demo.service.BusLocationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/busLocation")
@RequiredArgsConstructor
public class BusLocationController {
    private final BusLocationService service;

    @GetMapping
    public ResponseEntity<List<BusLocationDto>> findByQuery(
        @RequestParam String busRouteId
        
    ) {
        return ResponseEntity.ok(service.getBusPosByRtid(busRouteId));
    }

    @PostMapping
    public ResponseEntity<List<BusLocationDto>> findByBody(
        @RequestBody BusLocationRequestDto request
    ) {
        return ResponseEntity.ok(
            service.getBusPosByRtid(request.getBusRouteId())
        );
    }
}
