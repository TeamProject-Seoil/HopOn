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
    private final ArrivalNowService arrivalNowService;   // 노선유형 조회
    private final AuthUserResolver auth;

    @GetMapping("/{id}/location")
    public ResponseEntity<DriverLocationDto> currentLocation(Authentication a, @PathVariable Long id) {
        var user = auth.requireUser(a);

        var r = reservationRepository.findById(id).orElse(null);
        if (r == null || !r.getUser().getUserNum().equals(user.getUserNum())) {
            return ResponseEntity.notFound().build();
        }

        // ---- 노선유형 (코드/라벨/토큰) 일괄 계산: 항상 값이 내려가도록 보장 ----
        Integer routeTypeCode = null;
        try {
            var meta = arrivalNowService.resolveRouteType(r.getRouteId());
            routeTypeCode = (meta != null ? meta.code : null);
            if (routeTypeCode == null) {
                // 폴백: 개별 코드 조회
                routeTypeCode = arrivalNowService.resolveRouteTypeCode(r.getRouteId());
            }
        } catch (Exception ignore) { /* 폴백 아래에서 처리 */ }

        // 라벨은 null이어도 "기타"로 보장됨
        String routeTypeLabel = arrivalNowService.toRouteTypeLabel(routeTypeCode);
        // 마커 색상용 토큰(BLUE/GREEN/RED/YELLOW...), 없으면 라벨을 대신 사용
        String routeTypeToken = toRouteTypeName(routeTypeCode);
        String routeTypeForClient = (routeTypeToken != null) ? routeTypeToken
                                 : (routeTypeLabel != null ? routeTypeLabel : "기타");

        // 1) HopOn 자체 운행 좌표 우선
        if (r.getOperationId() != null) {
            var op = driverOperationRepository.findById(r.getOperationId()).orElse(null);
            if (op != null && op.getLastLat() != null && op.getLastLon() != null) {
                var dto = DriverLocationDto.builder()
                        .operationId(op.getId())
                        .lat(op.getLastLat())
                        .lon(op.getLastLon()) // JSON 키는 "lng"로 직렬화됨(@JsonProperty 필요)
                        .updatedAtIso(op.getUpdatedAt() == null ? null
                                : op.getUpdatedAt().atOffset(java.time.ZoneOffset.UTC).toString())
                        .stale(false)
                        .plainNo(r.getApiPlainNo())
                        .routeType(routeTypeForClient)     // ✅ 항상 값 보장
                        .routeTypeLabel(routeTypeLabel)    // ✅ 항상 값 보장("기타" 포함)
                        .routeTypeCode(routeTypeCode)      // null일 수 있음
                        .build();
                return ResponseEntity.ok(dto);
            }
            // 좌표 없으면 공공데이터 폴백
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
                .lon(match.getGpsX())      // 경도(JSON은 "lng")
                .updatedAtIso(match.getDataTm())
                .stale(false)
                .plainNo(match.getPlainNo())
                .routeType(routeTypeForClient)   // ✅ 동일한 규칙 적용
                .routeTypeLabel(routeTypeLabel)
                .routeTypeCode(routeTypeCode)
                .build();

        return ResponseEntity.ok(dto);
    }

    /** 서울시 번호판 등 비교용 정규화 */
    private String norm(String s) {
        return s == null ? "" : s.replaceAll("[^0-9가-힣A-Za-z]", "").toUpperCase();
    }

    private String toRouteTypeName(Integer code) {
        if (code == null) return null;
        return switch (code) {
            case 3 -> "BLUE";     // 간선
            case 4, 2 -> "GREEN"; // 지선/마을
            case 5 -> "YELLOW";   // 순환
            case 6 -> "RED";      // 광역
            // 정책상 묶을 항목들
            case 1 -> "RED";      // 공항
            case 7 -> "RED";      // 급행/좌석 등
            default -> null;
        };
    }
}
