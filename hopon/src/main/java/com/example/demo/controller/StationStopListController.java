package com.example.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.StationStopListDto;
import com.example.demo.dto.StationStopListRequestDto;
import com.example.demo.service.StationStopListService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/stationStop")
@RequiredArgsConstructor
public class StationStopListController {
    private final StationStopListService service;

    @GetMapping
    public ResponseEntity<List<StationStopListDto>> findByQuery(
        @RequestParam String arsId
        
    ) {
        return ResponseEntity.ok(service.getStationByUid(arsId));
    }

    @PostMapping
    public ResponseEntity<List<StationStopListDto>> findByBody(
        @RequestBody StationStopListRequestDto request
    ) {
        return ResponseEntity.ok(
            service.getStationByUid(request.getArsId())
        );
    }
}
