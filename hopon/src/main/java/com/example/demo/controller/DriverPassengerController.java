// src/main/java/com/example/demo/controller/DriverOperationController.java
package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
public class DriverPassengerController {

    private final DriverOperationService driverOperationService;
    private final DriverPassengerService driverPassengerService;

    // ... (기존 assign/start/heartbeat/end 등 유지)

    /** 운행 중 기사 화면: 현재 승객 현황 */
    @GetMapping("/passengers/now")
    public ResponseEntity<DriverPassengerListResponse> passengersNow(Authentication auth) {
        var resp = driverPassengerService.listActivePassengersNow(auth);
        return ResponseEntity.ok(resp);
    }
}
