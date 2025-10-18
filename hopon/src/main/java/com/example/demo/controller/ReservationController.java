// ReservationController.java
package com.example.demo.controller;

import com.example.demo.dto.ReservationDto;
import com.example.demo.dto.ReservationRequestDto;
import com.example.demo.entity.ReservationEntity;
import com.example.demo.entity.UserEntity;
import com.example.demo.service.ReservationService;
import com.example.demo.support.AuthUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")    // 예약 만들기 + 활성 예약 조회
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final AuthUserResolver authUserResolver;   // ✅ 추가

    // 예약 생성
    @PostMapping
    public ResponseEntity<ReservationDto> createReservation(
            Authentication authentication,
            @RequestBody ReservationRequestDto dto) {

        UserEntity user = authUserResolver.requireUser(authentication);
        ReservationEntity saved = reservationService.createReservation(user, dto);
        return ResponseEntity.ok(ReservationDto.from(saved));
    }

    // 활성 예약 1건 조회
    @GetMapping("/active")
    public ResponseEntity<ReservationDto> getActiveReservation(Authentication authentication) {
        UserEntity user = authUserResolver.requireUser(authentication);
        ReservationEntity active = reservationService.getActiveReservationFromDb(user.getUserNum());
        return (active == null)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(ReservationDto.from(active));
    }

    //최근 예약 조회
    @GetMapping
    public ResponseEntity<List<ReservationDto>> listAll(Authentication authentication) {
        UserEntity user = authUserResolver.requireUser(authentication);
        var list = reservationService.getAll(user).stream()
                .map(ReservationDto::from)
                .toList();
        return ResponseEntity.ok(list);
    }
}
