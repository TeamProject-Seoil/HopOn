// src/main/java/com/example/demo/service/DriverVehicleRegistrationService.java
package com.example.demo.service;

import com.example.demo.dto.DriverVehicleRegistrationDto;
import com.example.demo.entity.BusVehicleEntity;
import com.example.demo.entity.DriverVehicleRegistration;
import com.example.demo.repository.BusVehicleRepository;
import com.example.demo.repository.DriverVehicleRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverVehicleRegistrationService {

    private final DriverVehicleRegistrationRepository regRepo;
    private final BusVehicleRepository busRepo;

    /** (배정 시) 없으면 이력 추가 */
    public void addIfAbsent(Long userNum, String vehicleId) {
        if (!regRepo.existsByUserNumAndVehicleId(userNum, vehicleId)) {
            regRepo.save(DriverVehicleRegistration.builder()
                    .userNum(userNum)
                    .vehicleId(vehicleId)
                    .build());
        }
    }

    /** 내 등록 이력 목록 (최근순) */
    public List<DriverVehicleRegistrationDto> list(Long userNum) {
        var regs = regRepo.findByUserNumOrderByCreatedAtDesc(userNum);
        List<DriverVehicleRegistrationDto> out = new ArrayList<>(regs.size());
        for (var r : regs) {
            BusVehicleEntity v = busRepo.findById(r.getVehicleId()).orElse(null);
            out.add(DriverVehicleRegistrationDto.builder()
                    .vehicleId(r.getVehicleId())
                    .plateNo(v != null ? v.getPlateNo() : null)
                    .routeId(v != null ? v.getRouteId() : null)
                    .routeName(v != null ? v.getRouteName() : null)
                    .createdAtIso(r.getCreatedAt() == null ? null : r.getCreatedAt().toInstant(ZoneOffset.UTC).toString())
                    .build());
        }
        return out;
    }

    /** 이력에서 제거 (선택 기능) */
    public void remove(Long userNum, String vehicleId) {
        regRepo.deleteByUserNumAndVehicleId(userNum, vehicleId);
    }
}
