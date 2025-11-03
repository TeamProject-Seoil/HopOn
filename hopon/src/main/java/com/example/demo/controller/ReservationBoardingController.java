// src/main/java/com/example/demo/controller/ReservationBoardingController.java
package com.example.demo.controller;

import com.example.demo.entity.UserEntity;
import com.example.demo.service.ReservationBoardingService;
import com.example.demo.support.AuthUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationBoardingController {

    private final AuthUserResolver auth;
    private final ReservationBoardingService service;

    /** 출발역 도착 → 유저가 "탑승 완료" 버튼 누름 */
    @PostMapping("/{id}/board/confirm")
    public ResponseEntity<?> confirmBoard(Authentication a, @PathVariable Long id) {
        UserEntity user = auth.requireUser(a);
        service.confirmBoard(id, user);
        return ResponseEntity.ok().build();
    }

    /** 출발역 도착 후 일정 시간 내 확인 X → 앱이 타임아웃 호출 */
    @PostMapping("/{id}/board/timeout")
    public ResponseEntity<?> boardTimeout(Authentication a, @PathVariable Long id) {
        UserEntity user = auth.requireUser(a);
        service.boardTimeout(id, user);
        return ResponseEntity.ok().build();
    }

    /** 하차역 근처 → 유저가 "하차 완료" 버튼 누름 */
    @PostMapping("/{id}/alight/confirm")
    public ResponseEntity<?> confirmAlight(Authentication a, @PathVariable Long id) {
        UserEntity user = auth.requireUser(a);
        service.confirmAlight(id, user);
        return ResponseEntity.ok().build();
    }

    /** 하차역 근처 + 일정 시간 지나도 버튼 안 눌림 → 앱이 자동 호출 */
    @PostMapping("/{id}/alight/timeout")
    public ResponseEntity<?> alightTimeout(Authentication a, @PathVariable Long id) {
        UserEntity user = auth.requireUser(a);
        service.alightTimeout(id, user);
        return ResponseEntity.ok().build();
    }
}
