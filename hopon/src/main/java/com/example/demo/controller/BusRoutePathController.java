package com.example.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.BusRoutePathDto;
import com.example.demo.dto.BusRoutePathRequestDto;
import com.example.demo.service.BusRoutePathService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/busRoutePath")
@RequiredArgsConstructor
public class BusRoutePathController {
    private final BusRoutePathService service;

    @GetMapping
    public ResponseEntity<List<BusRoutePathDto>> findByQuery(
        @RequestParam String busRouteId
        
    ) {
        return ResponseEntity.ok(service.getStaionByRoute(busRouteId));
    }

    @PostMapping
    public ResponseEntity<List<BusRoutePathDto>> findByBody(
        @RequestBody BusRoutePathRequestDto request
    ) {
        return ResponseEntity.ok(
            service.getStaionByRoute(request.getBusRouteId())
        );
    }
}
