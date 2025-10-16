// src/main/java/com/example/demo/controller/UserController.java
package com.example.demo.controller;

import com.example.demo.dto.UserResponse;
import com.example.demo.dto.UpdateProfileRequest;
import com.example.demo.dto.ChangePasswordRequest;
import com.example.demo.dto.DeleteAccountRequest;
import com.example.demo.entity.Role;
import com.example.demo.entity.UserEntity;
import com.example.demo.repository.DriverLicenseRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserSessionRepository;
import com.example.demo.security.PasswordPolicy;
import com.example.demo.service.EmailVerificationService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserSessionRepository sessionRepository;
    private final EmailVerificationService emailVerificationService;
    private final DriverLicenseRepository driverLicenseRepository;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        String userid = (String) authentication.getPrincipal();
        UserEntity u = userRepository.findByUserid(userid).orElseThrow();

        // ⭐ 정책: 최근 접속 = lastLoginAt과 lastRefreshAt 중 최신값
        LocalDateTime last = u.getLastLoginAt();
        if (u.getLastRefreshAt() != null && (last == null || u.getLastRefreshAt().isAfter(last))) {
            last = u.getLastRefreshAt();
        }

        return ResponseEntity.ok(toResponse(u, last));
    }

    @GetMapping("/me/profile-image")
    public ResponseEntity<byte[]> getProfileImage(Authentication authentication) throws IOException {
        String userid = (String) authentication.getPrincipal();
        var user = userRepository.findByUserid(userid).orElseThrow();
        byte[] image = user.getProfileImage();
        if (image == null) return ResponseEntity.notFound().build();
        String guessed = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(image));
        MediaType mt = (guessed != null) ? MediaType.parseMediaType(guessed) : MediaType.IMAGE_JPEG;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mt);
        return new ResponseEntity<>(image, headers, HttpStatus.OK);
    }

    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateMeMultipart(
            Authentication authentication,
            @RequestPart("data") @Validated UpdateProfileRequest req,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) throws IOException {

        String userid = (String) authentication.getPrincipal();
        var u = userRepository.findByUserid(userid).orElseThrow();

        boolean changed = false;

        if (req.getUsername()!=null && !req.getUsername().isBlank()) {
            u.setUsername(req.getUsername().trim());
            changed = true;
        }

        if (req.getEmail()!=null) {
            String newEmail = req.getEmail().trim().toLowerCase();
            String oldEmail = u.getEmail() == null ? null : u.getEmail().trim().toLowerCase();

            if (!newEmail.equals(oldEmail)) {
                if (userRepository.existsByEmail(newEmail)) {
                    return ResponseEntity.status(409).body(Map.of("ok", false, "reason", "DUPLICATE_EMAIL"));
                }
                if (req.getEmailVerificationId() == null || req.getEmailVerificationId().isBlank()) {
                    return ResponseEntity.status(400).body(Map.of("ok", false, "reason", "EMAIL_VERIFICATION_REQUIRED"));
                }
                Long vId;
                try { vId = Long.parseLong(req.getEmailVerificationId()); }
                catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("ok", false, "reason", "INVALID_VERIFICATION_ID")); }

                emailVerificationService.ensureVerifiedAndMarkUsed(vId, newEmail, "CHANGE_EMAIL");

                u.setEmail(newEmail);
                changed = true;
            }
        }

        if (req.getTel()!=null && !req.getTel().isBlank()) {
            u.setTel(req.getTel().trim());
            changed = true;
        }

        if (req.getCompany()!=null) {
            if (u.getRole() != Role.ROLE_DRIVER) {
                return ResponseEntity.status(403).body(Map.of("ok", false, "reason", "COMPANY_CHANGE_ONLY_FOR_DRIVER"));
            }
            u.setCompany(req.getCompany().trim());
            changed = true;
        }

        final long MAX_IMAGE_BYTES = 2L * 1024 * 1024; // 2MB
        boolean remove = Boolean.TRUE.equals(req.getRemoveProfileImage());
        boolean hasFile = (file != null && !file.isEmpty());

        if (remove && hasFile) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "reason", "MUTUALLY_EXCLUSIVE"));
        }
        if (remove) {
            u.setProfileImage(null); changed = true;
        } else if (hasFile) {
            if (file.getSize() > MAX_IMAGE_BYTES)
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("ok", false, "reason", "IMAGE_TOO_LARGE"));
            String ct = file.getContentType();
            if (ct == null || !ct.startsWith("image/"))
                return ResponseEntity.badRequest().body(Map.of("ok", false, "reason", "INVALID_IMAGE_TYPE"));
            u.setProfileImage(file.getBytes()); changed = true;
        }

        if (!changed) return ResponseEntity.badRequest().body(Map.of("ok", false, "reason", "NO_FIELDS"));

        userRepository.save(u);

        // 업데이트 후에도 최신 last 계산해서 내려줌
        LocalDateTime last = u.getLastLoginAt();
        if (u.getLastRefreshAt() != null && (last == null || u.getLastRefreshAt().isAfter(last))) {
            last = u.getLastRefreshAt();
        }
        return ResponseEntity.ok(toResponse(u, last));
    }

    @PostMapping("/me/password")
    public ResponseEntity<?> changePassword(Authentication authentication,
                                            @RequestBody @Validated ChangePasswordRequest req) {
        String userid = (String) authentication.getPrincipal();
        var u = userRepository.findByUserid(userid).orElseThrow();

        if (!passwordEncoder.matches(req.getCurrentPassword(), u.getPassword())) {
            return ResponseEntity.status(400).body(Map.of("ok", false, "reason", "BAD_CURRENT_PASSWORD"));
        }

        String reason = PasswordPolicy.validateAndReason(req.getNewPassword());
        if (reason != null) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "reason", "PASSWORD_POLICY_VIOLATION", "message", reason));
        }
        if (passwordEncoder.matches(req.getNewPassword(), u.getPassword())) {
            return ResponseEntity.status(400).body(Map.of("ok", false, "reason", "SAME_AS_OLD"));
        }

        u.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(u);

        var sessions = sessionRepository.findByUserAndRevokedIsFalse(u);
        for (var s : sessions) s.setRevoked(true);
        if (!sessions.isEmpty()) sessionRepository.saveAll(sessions);

        return ResponseEntity.ok(Map.of("ok", true));
    }

    @DeleteMapping("/me")
    @Transactional
    public ResponseEntity<?> deleteMe(Authentication authentication,
                                      @RequestBody @Validated DeleteAccountRequest req) {
        String userid = (String) authentication.getPrincipal();
        var u = userRepository.findByUserid(userid).orElseThrow();

        if (!passwordEncoder.matches(req.getCurrentPassword(), u.getPassword())) {
            return ResponseEntity.status(400).body(Map.of("ok", false, "reason", "BAD_CURRENT_PASSWORD"));
        }

        try {
            // 세션 revoke/save 불필요 — CASCADE가 모두 제거
            // 하드 삭제 (둘 중 편한 것 사용)
            // int affected = userRepository.hardDeleteByUserNum(u.getUserNum());
            int affected = userRepository.hardDeleteByUserid(userid);

            if (affected == 0) {
                return ResponseEntity.status(404).body(Map.of("ok", false, "reason", "NOT_FOUND"));
            }
            return ResponseEntity.ok(Map.of("ok", true, "message", "ACCOUNT_DELETED"));

        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            var cause = org.springframework.core.NestedExceptionUtils.getMostSpecificCause(ex);
            return ResponseEntity.status(409).body(Map.of(
                "ok", false,
                "reason", "FK_CONSTRAINT",
                "detail", (cause != null ? cause.getMessage() : "Data integrity violation")
            ));
        }
    }


    // ▼ last(최근 접속/활동 시각)을 받아 응답으로 포함
    private UserResponse toResponse(UserEntity u, LocalDateTime last) {
        boolean hasDriverLicense = driverLicenseRepository.findByUser_UserNum(u.getUserNum()).isPresent();
        return UserResponse.builder()
                .userNum(u.getUserNum())
                .userid(u.getUserid())
                .username(u.getUsername())
                .email(u.getEmail())
                .tel(u.getTel())
                .role(u.getRole())
                .hasProfileImage(u.getProfileImage()!=null)
                .company(u.getCompany())
                .approvalStatus(u.getApprovalStatus())
                .hasDriverLicenseFile(hasDriverLicense) // 클라 호환용
                .lastLoginAtIso(UserResponse.toIsoOrNull(last)) // ⭐ 추가
                .build();
    }
}
