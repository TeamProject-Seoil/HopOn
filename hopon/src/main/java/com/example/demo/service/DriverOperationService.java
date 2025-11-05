// src/main/java/com/example/demo/service/DriverOperationService.java
package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.support.AuthUserResolver;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;              // ✅ 올바른 Pageable/Page 임포트
import org.springframework.data.domain.Pageable;      // ✅
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.PageImpl;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DriverOperationService {

    private final AuthUserResolver authUserResolver;
    private final BusVehicleRepository busVehicleRepository;
    private final DriverVehicleRepository driverVehicleRepository;
    private final DriverOperationRepository driverOperationRepository;
    private final BusLocationService busLocationService;
    private final DriverLocationStreamService streamService;
    private final DriverVehicleRegistrationService registrationService;
    private final ReservationRepository reservationRepository;
    private final NotificationService notificationService;
    private final ArrivalNowService arrivalNowService;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    /* ===============================
       1) 기사-차량 배정(등록)
       =============================== */
    @Transactional
    public AssignVehicleResponse assignVehicle(Authentication auth, AssignVehicleRequest req) {
        var user = authUserResolver.requireUser(auth);

        driverOperationRepository.findFirstByUserNumAndStatus(user.getUserNum(), DriverOperationStatus.RUNNING)
                .ifPresent(op -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "CANNOT_ASSIGN_WHILE_RUNNING"); });

        if (!StringUtils.hasText(req.getVehicleId()) && !StringUtils.hasText(req.getPlateNo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VEHICLE_ID_OR_PLATE_REQUIRED");
        }

        var vehicle = resolveVehicle(req.getVehicleId(), req.getPlateNo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "VEHICLE_NOT_FOUND"));

        // 업서트: 있으면 UPDATE, 없으면 INSERT
        var existing = driverVehicleRepository.findByUserNum(user.getUserNum());
        if (existing.isPresent()) {
            var dv = existing.get();
            dv.setVehicleId(vehicle.getVehicleId());
            driverVehicleRepository.save(dv);
        } else {
            driverVehicleRepository.save(
                    DriverVehicle.builder()
                            .userNum(user.getUserNum())
                            .vehicleId(vehicle.getVehicleId())
                            .build()
            );
        }

        registrationService.addIfAbsent(user.getUserNum(), vehicle.getVehicleId());

        return AssignVehicleResponse.builder()
                .vehicleId(vehicle.getVehicleId())
                .plateNo(vehicle.getPlateNo())
                .routeId(vehicle.getRouteId())
                .routeName(vehicle.getRouteName())
                .build();
    }

    private Optional<BusVehicleEntity> resolveVehicle(String vehicleId, String plateNo) {
        if (StringUtils.hasText(vehicleId)) {
            return busVehicleRepository.findById(vehicleId);
        }
        if (StringUtils.hasText(plateNo)) {
            return busVehicleRepository.findByPlateNo(plateNo);
        }
        return Optional.empty();
    }

    /* ===============================
       2) 운행 시작
       =============================== */
    @Transactional
    public StartOperationResponse startOperation(Authentication auth, StartOperationRequest req) {
        var user = authUserResolver.requireUser(auth);
        driverOperationRepository.findFirstByUserNumAndStatus(user.getUserNum(), DriverOperationStatus.RUNNING)
                .ifPresent(op -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "ALREADY_RUNNING"); });

        String vehicleId = StringUtils.hasText(req.getVehicleId())
                ? req.getVehicleId()
                : driverVehicleRepository.findByUserNum(user.getUserNum())
                .map(DriverVehicle::getVehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "NO_ASSIGNED_VEHICLE"));

        var vehicle = busVehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "VEHICLE_NOT_FOUND"));

        // 공공 API 매칭
        List<BusLocationDto> apiVehicles = busLocationService.getBusPosByRtid(vehicle.getRouteId());
        String dbPlateNorm = normalizePlate(vehicle.getPlateNo());
        BusLocationDto matched = apiVehicles.stream()
                .filter(v -> dbPlateNorm.equals(normalizePlate(v.getPlainNo())))
                .findFirst()
                .orElse(null);
        if (matched == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PLATE_NOT_FOUND_ON_ROUTE");

        // ★ 노선유형 메타 계산 → 운행에 스냅샷 저장
        ArrivalNowService.RouteTypeMeta meta = arrivalNowService.resolveRouteType(vehicle.getRouteId());

        var now = LocalDateTime.now();
        var op = DriverOperation.builder()
                .userNum(user.getUserNum())
                .vehicleId(vehicle.getVehicleId())
                .routeId(vehicle.getRouteId())
                .routeName(vehicle.getRouteName())
                .routeTypeCode(meta.code)          // ★ 저장
                .routeTypeLabel(meta.label)        // ★ 저장
                .apiVehId(matched.getVehId())
                .apiPlainNo(matched.getPlainNo())
                .status(DriverOperationStatus.RUNNING)
                .startedAt(now)
                .updatedAt(now)
                .lastLat(req.getLat())
                .lastLon(req.getLon())
                .build();

        var saved = driverOperationRepository.save(op);

        streamService.publish(DriverLocationDto.builder()
                .operationId(saved.getId())
                .lat(saved.getLastLat())
                .lon(saved.getLastLon())
                .updatedAtIso(saved.getUpdatedAt().toInstant(ZoneOffset.UTC).toString())
                .stale(false)
                .build());

        return StartOperationResponse.builder()
                .operationId(saved.getId())
                .vehicleId(vehicle.getVehicleId())
                .plateNo(vehicle.getPlateNo())
                .routeId(vehicle.getRouteId())
                .routeName(vehicle.getRouteName())
                .apiVehId(matched.getVehId())
                .apiPlainNo(matched.getPlainNo())
                .build();
    }

    /* ===============================
       3) 운행 중 위치 업데이트(하트비트)
       =============================== */
    @Transactional
    public void heartbeat(Authentication auth, HeartbeatRequest req) {
        var user = authUserResolver.requireUser(auth);
        var op = driverOperationRepository.findFirstByUserNumAndStatus(user.getUserNum(), DriverOperationStatus.RUNNING)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NO_ACTIVE_OPERATION"));

        op.setLastLat(req.getLat());
        op.setLastLon(req.getLon());
        op.setUpdatedAt(LocalDateTime.now());
        driverOperationRepository.save(op);

        // SSE 브로드캐스트
        streamService.publish(DriverLocationDto.builder()
                .operationId(op.getId())
                .lat(op.getLastLat())
                .lon(op.getLastLon())
                .updatedAtIso(op.getUpdatedAt().toInstant(ZoneOffset.UTC).toString())
                .stale(false)
                .build());
    }

    /* ===============================
       4) 운행 종료
       =============================== */
    @Transactional
    public void endOperation(Authentication auth, EndOperationRequest req) {
        var user = authUserResolver.requireUser(auth);
        var op = driverOperationRepository.findFirstByUserNumAndStatus(user.getUserNum(), DriverOperationStatus.RUNNING)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NO_ACTIVE_OPERATION"));

        op.setStatus(DriverOperationStatus.ENDED);
        op.setEndedAt(LocalDateTime.now());
        op.setUpdatedAt(LocalDateTime.now());
        driverOperationRepository.save(op);

        // 정책: 종료 시 배정 해제
        driverVehicleRepository.deleteByUserNum(user.getUserNum());
    }

    /* ===============================
       5) 단건/현재 조회
       =============================== */
    @Transactional
    public Optional<DriverOperation> findActive(Authentication auth) {
        var user = authUserResolver.requireUser(auth);
        return driverOperationRepository.findFirstByUserNumAndStatus(user.getUserNum(), DriverOperationStatus.RUNNING);
    }

    // 간단 근사: 위경도 차의 제곱거리
    private double distance2(double lat1, double lon1, double lat2, double lon2) {
        double dy = lat1 - lat2;
        double dx = lon1 - lon2;
        return dx * dx + dy * dy;
    }
    
    /* ===============================
       6) “이번/다음 정류장” 계산
       =============================== */
    @Transactional
    public ArrivalNowResponse getArrivalNow(Authentication auth) {
        var user = authUserResolver.requireUser(auth);

        var op = driverOperationRepository
                .findFirstByUserNumAndStatus(user.getUserNum(), DriverOperationStatus.RUNNING)
                .orElse(null);

        if (op == null) {
            return ArrivalNowResponse.builder()
                    .currentStopName("-")
                    .nextStopName("-")
                    .etaSec(null)
                    .build();
        }

        List<BusLocationDto> buses = busLocationService.getBusPosByRtid(op.getRouteId());
        if (buses == null || buses.isEmpty()) {
            return ArrivalNowResponse.builder()
                    .currentStopName("-").nextStopName("-").etaSec(null).build();
        }

        BusLocationDto matched = null;
        if (StringUtils.hasText(op.getApiVehId())) {
            matched = buses.stream()
                    .filter(b -> op.getApiVehId().equals(b.getVehId()))
                    .findFirst().orElse(null);
        }
        if (matched == null && StringUtils.hasText(op.getApiPlainNo())) {
            String norm = normalizePlate(op.getApiPlainNo());
            matched = buses.stream()
                    .filter(b -> norm.equals(normalizePlate(b.getPlainNo())))
                    .findFirst().orElse(null);
        }
        if (matched == null) {
            return ArrivalNowResponse.builder()
                    .currentStopName("-").nextStopName("-").etaSec(null).build();
        }

        // 노선 정류장 + 도착정보 합성 계산
        ArrivalNowResponse resp = arrivalNowService.build(op.getRouteId(), matched, op.getApiPlainNo());
        resp.setRouteTypeCode(op.getRouteTypeCode());
        resp.setRouteTypeLabel(op.getRouteTypeLabel());
        return resp;
    }

    /* ===============================
       7) 운행 목록 페이징 조회 (status 필터)
       =============================== */
    @Transactional
    public Page<DriverOperation> findOperations(Authentication auth, String status, Pageable pageable) {
        var user = authUserResolver.requireUser(auth);

        if (!StringUtils.hasText(status)) {
            // 전체(내 모든 운행)
            return driverOperationRepository.findByUserNum(user.getUserNum(), pageable);
        }

        DriverOperationStatus st;
        try {
            st = DriverOperationStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_STATUS");
        }

        return driverOperationRepository.findByUserNumAndStatus(user.getUserNum(), st, pageable);
    }

    /* ===============================
       8) 종료된 운행 단건 조회 (소유자 검증)
       =============================== */
    @Transactional
    public DriverOperation getEnded(Authentication auth, Long operationId) {
        var user = authUserResolver.requireUser(auth);

        return driverOperationRepository
                .findByIdAndUserNumAndStatus(operationId, user.getUserNum(), DriverOperationStatus.ENDED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ENDED_OPERATION_NOT_FOUND"));
    }

    /* ===============================
       utilities
       =============================== */
    private String normalizePlate(String s) {
        if (s == null) return "";
        return s.replaceAll("[^0-9가-힣A-Za-z]", "").toUpperCase();
    }
    
    // UTC ISO 문자열 변환 유틸
    private static String toIsoOrNull(LocalDateTime t) {
        if (t == null) return null;
        return t.atZone(ZoneId.systemDefault()).toInstant().toString();
    }
    
    @Transactional
    public PageResponse<DriverOperationListItem> findOperationList(Authentication auth, String status, Pageable pageable) {
        var page = findOperations(auth, status, pageable); // <- 네가 이미 만든 메서드(엔티티 페이지)
        var dtoList = page.getContent().stream()
                .map(this::toListItemWithRouteType) // ★ 여기서 노선유형 계산
                .collect(Collectors.toList());

        return PageResponse.<DriverOperationListItem>builder()
                .content(dtoList)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    /** 엔티티 → DTO 매핑하면서 routeType 계산 */
    private DriverOperationListItem toListItemWithRouteType(DriverOperation op) {
        Integer code  = op.getRouteTypeCode();
        String  label = op.getRouteTypeLabel();

        if (code == null || (label == null || label.isBlank())) {
            var meta = arrivalNowService.resolveRouteType(op.getRouteId());
            code  = (code  != null) ? code  : meta.code;
            label = (label != null && !label.isBlank()) ? label : meta.label;
        }

        return DriverOperationListItem.builder()
                .id(op.getId())
                .routeId(op.getRouteId())
                .routeName(op.getRouteName())
                .vehicleId(op.getVehicleId())
                .plateNo(op.getApiPlainNo())
                .startedAt(toIsoOrNull(op.getStartedAt()))
                .endedAt(toIsoOrNull(op.getEndedAt()))
                .routeTypeCode(code)
                .routeTypeLabel(label)
                .build();
    }
    
    

    @Transactional
    public void markDelayed(Authentication auth) {
        var user = authUserResolver.requireUser(auth);

        var op = driverOperationRepository
                .findFirstByUserNumAndStatus(user.getUserNum(), DriverOperationStatus.RUNNING)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NO_ACTIVE_OPERATION"));

        boolean next = !op.isDelayed();
        op.setDelayed(next);
        op.setUpdatedAt(LocalDateTime.now());
        driverOperationRepository.save(op);

        // ✅ 지연 ON일 때만 예약자에게 알림
        if (next) {
            var reservations = reservationRepository.findActiveNoshowByOperation(
                    op.getId(),
                    ReservationStatus.CONFIRMED,
                    BoardingStage.NOSHOW
            );
            for (var r : reservations) {
                if (r.getUser() != null) {
                    notificationService.sendDelayNotification(r.getUser(), r);
                }
            }
        }
    }

}
