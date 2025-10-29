// src/main/java/com/example/demo/service/DriverVehicleRegistrationService.java
package com.example.demo.service;

import com.example.demo.dto.DriverVehicleRegistrationDto;
import com.example.demo.dto.RouteInfoDto;
import com.example.demo.entity.BusVehicleEntity;
import com.example.demo.entity.DriverVehicleRegistration;
import com.example.demo.repository.BusVehicleRepository;
import com.example.demo.repository.DriverVehicleRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverVehicleRegistrationService {

    private final DriverVehicleRegistrationRepository regRepo;
    private final BusVehicleRepository busRepo;
    private final BusStopListService busStopListService;

    @Autowired(required = false)
    private BusRouteInfoService routeInfoService; // 선택적 의존성

    /** (배정 시) 없으면 이력 추가 */
    @Transactional
    public void addIfAbsent(Long userNum, String vehicleId) {
        if (userNum == null || vehicleId == null) return;
        if (!regRepo.existsByUserNumAndVehicleId(userNum, vehicleId)) {
            regRepo.save(DriverVehicleRegistration.builder()
                    .userNum(userNum)
                    .vehicleId(vehicleId)
                    .build());
        }
    }

    /** 이력에서 제거 */
    @Transactional
    public void remove(Long userNum, String vehicleId) {
        if (userNum == null || vehicleId == null) return;
        regRepo.deleteByUserNumAndVehicleId(userNum, vehicleId);
    }

    /** 내 등록 이력 조회 (최근순) + 노선유형 메타 */
    @Transactional(readOnly = true)
    public List<DriverVehicleRegistrationDto> list(Long userNum) {
        var regs = regRepo.findByUserNumOrderByCreatedAtDesc(userNum);
        List<DriverVehicleRegistrationDto> out = new ArrayList<>(regs.size());

        for (var r : regs) {
            BusVehicleEntity v = busRepo.findById(r.getVehicleId()).orElse(null);

            Integer routeTypeCode = resolveRouteTypeCode(v != null ? v.getRouteId() : null);
            String routeTypeLabel = toRouteTypeLabel(routeTypeCode);

            out.add(DriverVehicleRegistrationDto.builder()
                    .vehicleId(r.getVehicleId())
                    .plateNo(v != null ? v.getPlateNo() : null)
                    .routeId(v != null ? v.getRouteId() : null)
                    .routeName(v != null ? v.getRouteName() : null)
                    .routeTypeCode(routeTypeCode)
                    .routeTypeLabel(routeTypeLabel)
                    .createdAtIso(r.getCreatedAt() == null
                            ? null
                            : r.getCreatedAt().toInstant(ZoneOffset.UTC).toString())
                    .build());
        }
        return out;
    }

    // ---------------------- 내부 헬퍼 ----------------------

    /** 노선유형 코드 구하기 (1순위: routeInfoService, 2순위: 정류장 목록) */
    private Integer resolveRouteTypeCode(String routeId) {
        if (routeId == null || routeId.isBlank()) return null;
        // 1) routeInfoService 우선
        if (routeInfoService != null) {
            try {
                RouteInfoDto info = routeInfoService.getRouteInfo(routeId);
                if (info != null && info.getBusRouteType() != null)
                    return info.getBusRouteType();
            } catch (Exception ignore) {}
        }
        // 2) 노선 정류장 목록 routeType
        try {
            var stops = busStopListService.getStaionByRoute(routeId);
            if (stops != null) {
                for (var s : stops) {
                    try {
                        Integer c = Integer.parseInt(String.valueOf(s.getRouteType()).trim());
                        if (c != null) return c;
                    } catch (Exception ignore) {}
                }
            }
        } catch (Exception ignore) {}
        return null;
    }

    private String toRouteTypeLabel(Integer code) {
        if (code == null) return "기타";
        return switch (code) {
            case 1 -> "공항"; case 2 -> "마을"; case 3 -> "간선"; case 4 -> "지선";
            case 5 -> "순환"; case 6 -> "광역"; case 7 -> "인천"; case 8 -> "경기";
            case 9 -> "폐지"; case 0 -> "공용"; default -> "기타";
        };
    }
}
