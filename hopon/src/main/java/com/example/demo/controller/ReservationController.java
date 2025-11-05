// src/main/java/com/example/demo/controller/ReservationController.java
package com.example.demo.controller;

import com.example.demo.dto.ReservationDto;
import com.example.demo.dto.ReservationRequestDto;
import com.example.demo.entity.DriverOperation;
import com.example.demo.entity.ReservationEntity;
import com.example.demo.entity.UserEntity;
import com.example.demo.repository.DriverOperationRepository;
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
    private final AuthUserResolver authUserResolver;
    private final DriverOperationRepository driverOperationRepository;   // ✅ 지연 여부 확인용

    // 예약 생성
    @PostMapping
    public ResponseEntity<ReservationDto> createReservation(
            Authentication authentication,
            @RequestBody ReservationRequestDto dto) {

        UserEntity user = authUserResolver.requireUser(authentication);
        ReservationEntity saved = reservationService.createReservation(user, dto);

        // 막 만든 예약은 아직 운행과 연결/지연 개념이 없으므로 delayed=false 로 내려줌
        return ResponseEntity.ok(ReservationDto.from(saved, false));
    }

    // 활성 예약 1건 조회 (+ 지연 여부 포함)
    @GetMapping("/active")
    public ResponseEntity<ReservationDto> getActiveReservation(Authentication authentication) {
        UserEntity user = authUserResolver.requireUser(authentication);
        ReservationEntity active = reservationService.getActiveReservationFromDb(user.getUserNum());
        if (active == null) {
            return ResponseEntity.noContent().build();
        }

        boolean delayed = false;
        if (active.getOperationId() != null) {
            delayed = driverOperationRepository.findById(active.getOperationId())
                    .map(DriverOperation::isDelayed)
                    .orElse(false);
        }

        return ResponseEntity.ok(ReservationDto.from(active, delayed));
    }

    // 최근 예약 조회
    @GetMapping
    public ResponseEntity<List<ReservationDto>> listAll(Authentication authentication) {
        UserEntity user = authUserResolver.requireUser(authentication);

        // 여기서도 필요하다면 예약마다 operation 찾아서 delayed 계산할 수 있지만,
        // 일단은 '지연 여부는 활성 예약 화면에서만 중요하다'고 보고 false 로 둬도 됨.
        var list = reservationService.getAll(user).stream()
                .map(e -> ReservationDto.from(e, false))
                .toList();

        return ResponseEntity.ok(list);
    }
}
