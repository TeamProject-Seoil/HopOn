package com.example.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.BusStopListDto;
import com.example.demo.dto.BusStopListRequestDto;
import com.example.demo.service.BusStopListService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/busStopList")
@RequiredArgsConstructor
public class BusStopListController {
    private final BusStopListService service;

    @GetMapping
    public ResponseEntity<List<BusStopListDto>> findByQuery(
        @RequestParam String busRouteId
        
    ) {
        return ResponseEntity.ok(service.getStaionByRoute(busRouteId));
    }

    @PostMapping
    public ResponseEntity<List<BusStopListDto>> findByBody(
        @RequestBody BusStopListRequestDto request
    ) {
        return ResponseEntity.ok(
            service.getStaionByRoute(request.getBusRouteId())
        );
    }
}
