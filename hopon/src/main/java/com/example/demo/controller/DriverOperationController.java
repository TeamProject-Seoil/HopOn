package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.entity.DriverOperation;
import com.example.demo.service.DriverOperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
public class DriverOperationController {

    private final DriverOperationService service;

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
}
