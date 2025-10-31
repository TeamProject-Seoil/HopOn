// src/main/java/com/example/demo/controller/ReservationBusController.java
package com.example.demo.controller;

import com.example.demo.dto.DriverLocationDto;
import com.example.demo.repository.DriverOperationRepository;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.service.ArrivalNowService;
import com.example.demo.service.BusLocationService;
import com.example.demo.support.AuthUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/reservations")
public class ReservationBusController {

    private final ReservationRepository reservationRepository;
    private final DriverOperationRepository driverOperationRepository;
    private final BusLocationService busLocationService;
    private final ArrivalNowService arrivalNowService;   // ✅ 노선유형 조회용
    private final AuthUserResolver auth;

    @GetMapping("/{id}/location")
    public ResponseEntity<DriverLocationDto> currentLocation(Authentication a, @PathVariable Long id){
        var user = auth.requireUser(a);
        var r = reservationRepository.findById(id).orElse(null);
        if (r == null || !r.getUser().getUserNum().equals(user.getUserNum())) {
            return ResponseEntity.notFound().build();
        }

        // 노선유형(BLUE/GREEN/RED/YELLOW/null) 미리 계산
        String routeType = resolveRouteType(r.getRouteId());

        // 1) HopOn 운행과 연결된 경우 → 운영 DB 좌표 우선
        if (r.getOperationId() != null) {
            var op = driverOperationRepository.findById(r.getOperationId()).orElse(null);
            if (op != null && op.getLastLat() != null && op.getLastLon() != null) {
                var dto = DriverLocationDto.builder()
                        .operationId(op.getId())
                        .lat(op.getLastLat())
                        .lon(op.getLastLon()) // JSON 키는 "lng"
                        .updatedAtIso(op.getUpdatedAt() == null ? null :
                                op.getUpdatedAt().atOffset(java.time.ZoneOffset.UTC).toString())
                        .stale(false)
                        .plainNo(r.getApiPlainNo())
                        .routeType(routeType)                 // ✅ 추가
                        .build();
                return ResponseEntity.ok(dto);
            }
            // 좌표 없으면 공공데이터로 폴백
        }

        // 2) 공공데이터 폴백
        var list = busLocationService.getBusPosByRtid(r.getRouteId());
        if (list == null || list.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        var targetPlain = r.getApiPlainNo();
        var targetVehId = r.getApiVehId();

        var match = list.stream().filter(b -> {
            boolean byPlate = targetPlain != null && !targetPlain.isBlank()
                    && norm(b.getPlainNo()).equals(norm(targetPlain));
            boolean byVehId = targetVehId != null && targetVehId.equals(b.getVehId());
            return byPlate || byVehId;
        }).findFirst().orElse(null);

        if (match == null) return ResponseEntity.noContent().build();

        var dto = DriverLocationDto.builder()
                .operationId(r.getOperationId())
                .lat(match.getGpsY())      // 위도
                .lon(match.getGpsX())      // 경도 (JSON은 "lng"로 나감)
                .updatedAtIso(match.getDataTm())
                .stale(false)
                .plainNo(match.getPlainNo())
                .routeType(routeType)      // ✅ 추가
                .build();
        return ResponseEntity.ok(dto);
    }

    private String norm(String s){
        return s==null?"":s.replaceAll("[^0-9가-힣A-Za-z]","").toUpperCase();
    }

    /** routeId → 서울 버스 routeType 코드 조회 후 색 이름으로 매핑 */
    private String resolveRouteType(String routeId) {
        try {
            Integer code = arrivalNowService.getRouteTypeCode(routeId); // 네 서비스에 맞게 구현돼 있어야 함
            return toRouteTypeName(code);
        } catch (Exception ignore) { return null; }
    }

    /** 서울시 코드 → BLUE/GREEN/RED/YELLOW */
    private String toRouteTypeName(Integer code) {
        if (code == null) return null;
        return switch (code) {
            case 3 -> "BLUE";            // 간선
            case 4, 2 -> "GREEN";        // 지선/마을
            case 5 -> "YELLOW";          // 순환
            case 6 -> "RED";             // 광역
            default -> null;
        };
    }
}
