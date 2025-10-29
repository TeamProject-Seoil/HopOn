// src/main/java/com/example/demo/controller/DriverOperationController.java
package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.entity.DriverOperation;
import com.example.demo.entity.BusVehicleEntity;
import com.example.demo.repository.BusVehicleRepository;
import com.example.demo.service.ArrivalNowService;
import com.example.demo.service.DriverOperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
public class DriverOperationController {

    private final DriverOperationService service;
    private final ArrivalNowService arrivalNowService;     // 노선 메타 계산
    private final BusVehicleRepository busVehicleRepository; // plate 보강용(선택)

    /** 1) 기사-차량 배정(등록) */
    @PostMapping("/assign")
    public ResponseEntity<AssignVehicleResponse> assign(Authentication auth,
                                                        @RequestBody @Validated AssignVehicleRequest req) {
        return ResponseEntity.ok(service.assignVehicle(auth, req));
    }

    /** 2) 운행 시작 (기사 GPS + 공공API 매칭) */
    @PostMapping("/operations/start")
    public ResponseEntity<StartOperationResponse> start(Authentication auth,
                                                        @RequestBody @Validated StartOperationRequest req) {
        return ResponseEntity.ok(service.startOperation(auth, req));
    }

    /** 3) 하트비트(위치 업데이트) */
    @PostMapping("/operations/heartbeat")
    public ResponseEntity<Map<String, Object>> heartbeat(Authentication auth,
                                                         @RequestBody @Validated HeartbeatRequest req) {
        service.heartbeat(auth, req);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** 4) 운행 종료 */
    @PostMapping("/operations/end")
    public ResponseEntity<Map<String, Object>> end(Authentication auth,
                                                   @RequestBody(required = false) EndOperationRequest req) {
        service.endOperation(auth, req == null ? new EndOperationRequest() : req);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** 5) 현재 운행 조회 */
    @GetMapping("/operations/active")
    public ResponseEntity<?> active(Authentication auth) {
        Optional<DriverOperation> op = service.findActive(auth);
        return ResponseEntity.ok(op.orElse(null));
    }

    /** 6) 현재 차량의 이번 / 다음 정류장 조회 */
    @GetMapping("/operations/arrival-now")
    public ResponseEntity<ArrivalNowResponse> arrivalNow(Authentication auth) {
        return ResponseEntity.ok(service.getArrivalNow(auth));
    }

    // -------------------------
    // 신규: 페이징 운행 기록 API
    // -------------------------

    /**
     * 7) 운행 목록(상태별) 페이징 조회
     *   - 프론트: GET /api/driver/operations?status=ENDED&page=0&size=20&sort=endedAt,desc
     *   - 반환: PageResponse<DriverOperationListItem>
     */
    @GetMapping("/operations")
    public ResponseEntity<PageResponse<DriverOperationListItem>> listOperations(
            Authentication auth,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "endedAt,desc") String sortExpr
    ) {
        Sort sort = parseSort(sortExpr);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DriverOperation> p = service.findOperations(auth, status, pageable);

        List<DriverOperationListItem> dtoList = p.getContent()
                .stream()
                .map(this::toListItem)
                .toList();

        PageResponse<DriverOperationListItem> body = PageResponse.<DriverOperationListItem>builder()
                .content(dtoList)
                .page(p.getNumber())                 // ✅ 필드명 일치 (number → page)
                .size(p.getSize())
                .totalPages(p.getTotalPages())
                .totalElements(p.getTotalElements())
                .first(p.isFirst())
                .last(p.isLast())
                .build();

        return ResponseEntity.ok(body);
    }

    /** 8) 종료된 운행 단건 조회(상세) */
    @GetMapping("/operations/ended/{operationId}")
    public ResponseEntity<DriverOperationListItem> getEnded(Authentication auth,
                                                            @PathVariable Long operationId) {
        DriverOperation op = service.getEnded(auth, operationId);
        return ResponseEntity.ok(toListItem(op));
    }

    // --------------------------------
    // (하위) 정렬 파서 & DTO 매핑 유틸
    // --------------------------------

    private Sort parseSort(String sortExpr) {
        if (sortExpr == null || sortExpr.isBlank()) return Sort.by(Sort.Order.desc("endedAt"));
        String[] parts = sortExpr.split(",");
        String field = parts[0].trim();
        boolean desc = parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc");
        return Sort.by(new Sort.Order(desc ? Sort.Direction.DESC : Sort.Direction.ASC, field));
    }

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private String toIsoOrNull(java.time.LocalDateTime t) {
        if (t == null) return null;
        return t.atOffset(ZoneOffset.UTC).format(ISO);
    }

    /** ✅ 여기서 라우트 메타(유형코드/라벨) 계산 + plate 보강 */
    private DriverOperationListItem toListItem(DriverOperation op) {
        var meta = arrivalNowService.resolveRouteType(op.getRouteId());

        // plate 보강: apiPlainNo 없으면 vehicle 테이블에서 조회
        String plate = op.getApiPlainNo();
        if (!StringUtils.hasText(plate) && StringUtils.hasText(op.getVehicleId())) {
            plate = busVehicleRepository.findById(op.getVehicleId())
                    .map(BusVehicleEntity::getPlateNo)
                    .orElse(null);
        }

        return DriverOperationListItem.builder()
                .id(op.getId())
                .routeId(op.getRouteId())
                .routeName(op.getRouteName())
                .vehicleId(op.getVehicleId())
                .plateNo(plate)                             // ✅ 안정적인 번호판
                .startedAt(toIsoOrNull(op.getStartedAt()))
                .endedAt(toIsoOrNull(op.getEndedAt()))
                .routeTypeCode(meta == null ? null : meta.code)
                .routeTypeLabel(meta == null ? null : meta.label)
                .build();
    }
}
