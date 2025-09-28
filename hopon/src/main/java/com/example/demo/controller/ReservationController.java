package com.example.demo.controller;

import com.example.demo.dto.ReservationRequestDto;
import com.example.demo.entity.ReservationEntity;
import com.example.demo.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor

public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationEntity> createReservation(
            @AuthenticationPrincipal String userid, //
            @RequestBody ReservationRequestDto dto) {

        ReservationEntity saved = reservationService.createReservation(userid, dto);
        return ResponseEntity.ok(saved);
    }
}