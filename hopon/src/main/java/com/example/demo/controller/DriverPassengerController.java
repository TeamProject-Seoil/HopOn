// src/main/java/com/example/demo/controller/DriverPassengerController.java
package com.example.demo.controller;

import com.example.demo.dto.DriverPassengerListResponse;
import com.example.demo.service.DriverOperationService;
import com.example.demo.service.DriverPassengerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 기사 승객 현황 조회 전담 컨트롤러
 * - 기존 로그 기준으로 404가 났던 /api/driver/passengers 를 그대로 지원
 * - operations 컨벤션도 함께 지원(/api/driver/operations/passengers)
 */
@RestController
@RequiredArgsConstructor
public class DriverPassengerController {

    private final DriverOperationService driverOperationService;
    private final DriverPassengerService driverPassengerService;

    /** (권장) operations 컨벤션 */
    @GetMapping("/api/driver/operations/passengers")
    public ResponseEntity<DriverPassengerListResponse> passengersV2(Authentication auth) {
        var resp = driverPassengerService.listActivePassengersNow(auth);
        return ResponseEntity.ok(resp);
    }

    /** (호환) 예전 경로도 함께 열어둠 → 클라/문서 어디가 먼저 고쳐져도 안전 */
    @GetMapping({"/api/driver/passengers", "/api/driver/passengers/now"})
    public ResponseEntity<DriverPassengerListResponse> passengersCompat(Authentication auth) {
        var resp = driverPassengerService.listActivePassengersNow(auth);
        return ResponseEntity.ok(resp);
    }
}
