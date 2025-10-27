package com.example.demo.controller;

import com.example.demo.dto.DriverLocationDto;
import com.example.demo.entity.DriverOperationStatus;
import com.example.demo.repository.DriverOperationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DriverLocationController {

    private final DriverOperationRepository driverOperationRepository;

    // 특정 운행의 현재 위치 (폴링)
    @GetMapping("/driver/operations/{operationId}/location")
    public ResponseEntity<DriverLocationDto> getOperationLocation(@PathVariable Long operationId) {
        var op = driverOperationRepository.findById(operationId).orElse(null);
        if (op == null || op.getLastLat() == null || op.getLastLon() == null) {
            return ResponseEntity.ok(null); // 또는 404로 바꿔도 됨
        }
        boolean stale = op.getUpdatedAt() == null ||
                Duration.between(op.getUpdatedAt().atZone(ZoneOffset.systemDefault()),
                        java.time.ZonedDateTime.now()).abs().getSeconds() > 15;

        var dto = DriverLocationDto.builder()
                .operationId(op.getId())
                .lat(op.getLastLat())
                .lon(op.getLastLon())
                .updatedAtIso(op.getUpdatedAt() == null ? null : op.getUpdatedAt().toInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).toString())
                .stale(stale)
                .build();
        return ResponseEntity.ok(dto);
    }

    // 특정 노선의 활성 차량들 위치 (폴링)
    @GetMapping("/routes/{routeId}/locations")
    public ResponseEntity<List<DriverLocationDto>> getRouteLocations(@PathVariable String routeId) {
        var ops = driverOperationRepository.findByRouteIdAndStatusOrderByUpdatedAtDesc(routeId, DriverOperationStatus.RUNNING);
        var list = ops.stream().filter(op -> op.getLastLat() != null && op.getLastLon() != null)
                .map(op -> DriverLocationDto.builder()
                        .operationId(op.getId())
                        .lat(op.getLastLat())
                        .lon(op.getLastLon())
                        .updatedAtIso(op.getUpdatedAt() == null ? null : op.getUpdatedAt().toInstant(ZoneOffset.UTC).toString())
                        .stale(false)
                        .build())
                .toList();
        return ResponseEntity.ok(list);
    }
}
