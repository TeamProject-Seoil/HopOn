// src/main/java/com/example/demo/controller/ReservationBusController.java
package com.example.demo.controller;

import com.example.demo.dto.BusLocationDto;
import com.example.demo.dto.DriverLocationDto;
import com.example.demo.entity.ReservationEntity;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.service.BusLocationService;
import com.example.demo.support.AuthUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationBusController {

    private final ReservationRepository reservationRepository;
    private final BusLocationService busLocationService;
    private final AuthUserResolver auth;

    @GetMapping("/{id}/location")
    public ResponseEntity<DriverLocationDto> currentLocation(Authentication a, @PathVariable Long id){
        var user = auth.requireUser(a);
        ReservationEntity r = reservationRepository.findById(id)
                .orElse(null);
        if (r == null || !r.getUser().getUserNum().equals(user.getUserNum())) {
            return ResponseEntity.notFound().build();
        }

        // 1) HopOn 운행과 연결된 경우 → 공용 DTO로 리턴(프론트는 SSE도 가능)
        if (r.getOperationId() != null) {
            // 프론트에선 SSE로 /api/stream/driver/operations/{operationId}/location 구독 권장
            // 여기서는 즉시 스냅샷만 간단히 리턴하려면 DriverLocationController를 호출해도 되고,
            // or 별도 Repo로 op 조회 + lastLat/lastLon 리턴 구현 가능(간단화를 위해 공공API로 fallback).
        }

        // 2) 공공데이터 fallback: routeId 목록 중 plate/vehId 일치하는 차량 찾아 리턴
        var list = busLocationService.getBusPosByRtid(r.getRouteId());
        var match = list.stream().filter(b ->
                norm(b.getPlainNo()).equals(norm(r.getApiPlainNo()))
             || (r.getApiVehId()!=null && r.getApiVehId().equals(b.getVehId()))
        ).findFirst().orElse(null);

        if (match == null) return ResponseEntity.noContent().build();

        var dto = DriverLocationDto.builder()
                .operationId(r.getOperationId()) // null일 수 있음
                .lat(match.getGpsY())
                .lon(match.getGpsX())
                .updatedAtIso(match.getDataTm()) // 공공데이터 제공시간 문자열
                .stale(false)
                .build();
        return ResponseEntity.ok(dto);
    }

    private String norm(String s){ return s==null?"":s.replaceAll("[^0-9가-힣A-Za-z]","").toUpperCase(); }
}
