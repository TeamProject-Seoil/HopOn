// src/main/java/com/example/demo/controller/DriverVehicleRegistrationController.java
package com.example.demo.controller;

import com.example.demo.dto.DriverVehicleRegistrationDto;
import com.example.demo.service.DriverVehicleRegistrationService;
import com.example.demo.support.AuthUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/driver/registrations")
@RequiredArgsConstructor
public class DriverVehicleRegistrationController {

    private final AuthUserResolver authUserResolver;
    private final DriverVehicleRegistrationService service;

    /** 내 등록 이력 조회 (최근순) */
    @GetMapping
    public ResponseEntity<List<DriverVehicleRegistrationDto>> list(Authentication auth) {
        var user = authUserResolver.requireUser(auth);
        return ResponseEntity.ok(service.list(user.getUserNum()));
    }

    /** 이력에서 제거 (선택) */
    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<?> remove(Authentication auth, @PathVariable String vehicleId) {
        var user = authUserResolver.requireUser(auth);
        service.remove(user.getUserNum(), vehicleId);
        return ResponseEntity.ok().build();
    }
}
