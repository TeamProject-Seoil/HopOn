package com.example.demo.controller;

import com.example.demo.dto.CancelResult;
import com.example.demo.entity.UserEntity;
import com.example.demo.service.ReservationCommandService;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationCancelController {

    private final ReservationCommandService service;
    private final UserRepository userRepository; // ✅ 주입 추가 (RequiredArgsConstructor가 생성자 만들어줌)

    @DeleteMapping("/{id}")
    public ResponseEntity<CancelResult> cancel(Authentication authentication,
                                               @PathVariable Long id) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("UNAUTHORIZED");
        }

        Object principal = authentication.getPrincipal();
        UserEntity user;

        if (principal instanceof UserEntity u) {
            user = u; // 커스텀 UserDetails가 UserEntity인 경우
        } else if (principal instanceof UserDetails ud) {
            user = userRepository.findByUserid(ud.getUsername())
                    .orElseThrow(() -> new IllegalStateException("USER_NOT_FOUND"));
        } else if (principal instanceof String username) {
            user = userRepository.findByUserid(username)
                    .orElseThrow(() -> new IllegalStateException("USER_NOT_FOUND"));
        } else {
            throw new IllegalStateException("UNSUPPORTED_PRINCIPAL");
        }

        CancelResult res = service.cancel(id, user); // 서비스는 UserEntity를 받도록 구현
        return ResponseEntity.ok(res);
    }
}