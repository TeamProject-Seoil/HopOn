// ReservationController.java
package com.example.demo.controller;

import com.example.demo.dto.ReservationRequestDto;
import com.example.demo.entity.ReservationEntity;
import com.example.demo.entity.UserEntity;
import com.example.demo.service.ReservationService;
import com.example.demo.support.AuthUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")    // 예약 만들기 + 활성 예약 조회
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final AuthUserResolver authUserResolver;   // ✅ 추가

    // 예약 생성
    @PostMapping
    public ResponseEntity<ReservationEntity> createReservation(
            Authentication authentication,
            @RequestBody ReservationRequestDto dto) {

        // ✅ 인증 사용자 1줄 획득 (유효성/예외 처리 내부에서 수행)
        UserEntity user = authUserResolver.requireUser(authentication);

        ReservationEntity saved = reservationService.createReservation(user, dto);
        return ResponseEntity.ok(saved);
    }

    // 활성 예약 1건 조회
    @GetMapping("/active")
    public ResponseEntity<ReservationEntity> getActiveReservation(Authentication authentication) {
        // ✅ 인증 사용자 1줄 획득
        UserEntity user = authUserResolver.requireUser(authentication);

        ReservationEntity active = reservationService.getActiveReservationFromDb(user.getUserNum());
        return (active == null) ? ResponseEntity.noContent().build() : ResponseEntity.ok(active);
    }
}
