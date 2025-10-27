package com.example.demo.service;

import com.example.demo.dto.BusLocationDto;
import com.example.demo.dto.*;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.support.AuthUserResolver;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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

        // ✅ 업서트: 있으면 UPDATE, 없으면 INSERT
        var existing = driverVehicleRepository.findByUserNum(user.getUserNum());
        if (existing.isPresent()) {
            var dv = existing.get();
            dv.setVehicleId(vehicle.getVehicleId());   // **UPDATE**
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

        // 기존 RUNNING 있으면 막기(운행은 동시에 한 개만)
        driverOperationRepository.findFirstByUserNumAndStatus(user.getUserNum(), DriverOperationStatus.RUNNING)
                .ifPresent(op -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "ALREADY_RUNNING"); });

        // vehicleId 명시가 없으면 배정된 차량 사용
        String vehicleId = StringUtils.hasText(req.getVehicleId())
                ? req.getVehicleId()
                : driverVehicleRepository.findByUserNum(user.getUserNum())
                    .map(DriverVehicle::getVehicleId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "NO_ASSIGNED_VEHICLE"));

        var vehicle = busVehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "VEHICLE_NOT_FOUND"));

        // 공공API에서 해당 노선의 버스 목록 조회 → 번호판 일치하는 차량 찾기
        List<BusLocationDto> apiVehicles = busLocationService.getBusPosByRtid(vehicle.getRouteId());

        // 번호판 정규화 후 매칭 (공공 API plainNo vs DB plate_no)
        String dbPlateNorm = normalizePlate(vehicle.getPlateNo());
        BusLocationDto matched = apiVehicles.stream()
                .filter(v -> dbPlateNorm.equals(normalizePlate(v.getPlainNo())))
                .findFirst()
                // 일치가 없으면 "가장 가까운 차량"을 근사 선택(기사 GPS 기준)
                .orElseGet(() -> apiVehicles.stream()
                        .min(Comparator.comparingDouble(v ->
                                distance2(req.getLat(), req.getLon(), v.getGpsY(), v.getGpsX())
                        )).orElse(null));

        if (matched == null) {
            // 공공 API상 해당 노선에 차량이 없거나 일시 오류
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "NO_MATCHING_BUS_FROM_PUBLIC_API");
        }

        var now = LocalDateTime.now();
        var op = DriverOperation.builder()
                .userNum(user.getUserNum())
                .vehicleId(vehicle.getVehicleId())
                .routeId(vehicle.getRouteId())
                .routeName(vehicle.getRouteName())
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

        // 하트비트 수신 시 SSE 브로드캐스트
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

        // ✅ 운행 종료 시 현재 배정 해제 (정책)
        driverVehicleRepository.deleteByUserNum(user.getUserNum());
    }

    /* ===============================
       5) 조회
       =============================== */
    @Transactional
    public Optional<DriverOperation> findActive(Authentication auth) {
        var user = authUserResolver.requireUser(auth);
        return driverOperationRepository.findFirstByUserNumAndStatus(user.getUserNum(), DriverOperationStatus.RUNNING);
    }

    private String normalizePlate(String s) {
        if (s == null) return "";
        return s.replaceAll("[^0-9가-힣A-Za-z]", "").toUpperCase();
    }

    // 간단 근사: 위경도 차의 제곱거리
    private double distance2(double lat1, double lon1, double lat2, double lon2) {
        double dy = lat1 - lat2;
        double dx = lon1 - lon2;
        return dx * dx + dy * dy;
    }
}
