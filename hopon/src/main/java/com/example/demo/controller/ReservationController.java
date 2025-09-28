package com.example.demo.controller;

import com.example.demo.dto.ReservationRequestDto;
import com.example.demo.entity.Reservation;
import com.example.demo.entity.UserEntity;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ReservationService;
import io.jsonwebtoken.Jwt;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor

public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<Reservation> createReservation(
            @AuthenticationPrincipal String userid, //
            @RequestBody ReservationRequestDto dto) {

        Reservation saved = reservationService.createReservation(userid, dto);
        return ResponseEntity.ok(saved);
    }
}