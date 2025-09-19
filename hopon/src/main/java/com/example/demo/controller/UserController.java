// src/main/java/com/example/demo/controller/UserController.java
package com.example.demo.controller;

import com.example.demo.dto.UserResponse;
import com.example.demo.dto.UpdateProfileRequest;
import com.example.demo.dto.ChangePasswordRequest;
import com.example.demo.dto.DeleteAccountRequest;
import com.example.demo.entity.Role;
import com.example.demo.entity.UserEntity;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserSessionRepository;
import com.example.demo.service.EmailVerificationService; // ✅ 추가

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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserSessionRepository sessionRepository;
    private final EmailVerificationService emailVerificationService; // ✅ 주입

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        String userid = (String) authentication.getPrincipal();
        UserEntity u = userRepository.findByUserid(userid).orElseThrow();
        return ResponseEntity.ok(toResponse(u));
    }

    @GetMapping("/me/profile-image")
    public ResponseEntity<byte[]> getProfileImage(Authentication authentication) throws IOException {
        String userid = (String) authentication.getPrincipal();
        var user = userRepository.findByUserid(userid).orElseThrow();
        byte[] image = user.getProfileImage();
        if (image == null) return ResponseEntity.notFound().build();
        String guessed = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(image));
        MediaType mt = (guessed != null) ? MediaType.parseMediaType(guessed) : MediaType.IMAGE_JPEG;
        HttpHeaders headers = new HttpHeaders(); headers.setContentType(mt);
        return new ResponseEntity<>(image, headers, HttpStatus.OK);
    }

    // ───────── 개인정보 수정 ─────────
    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateMeMultipart(
            Authentication authentication,
            @RequestPart("data") @Validated UpdateProfileRequest req,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) throws IOException {

        String userid = (String) authentication.getPrincipal();
        var u = userRepository.findByUserid(userid).orElseThrow();

        boolean changed = false;

        // ── 텍스트 필드
        if (req.getUsername()!=null && !req.getUsername().isBlank()) {
            u.setUsername(req.getUsername().trim());
            changed = true;
        }

        // ✅ 이메일 변경: 실제 변경 시에만 인증/중복검사
        if (req.getEmail()!=null) {
            String newEmail = req.getEmail().trim().toLowerCase();
            String oldEmail = u.getEmail() == null ? null : u.getEmail().trim().toLowerCase();

            if (!newEmail.equals(oldEmail)) {
                // 1) 중복 검사
                if (userRepository.existsByEmail(newEmail)) {
                    return ResponseEntity.status(409).body(Map.of("ok", false, "reason", "DUPLICATE_EMAIL"));
                }
                // 2) 인증 필수: verificationId 필요
                if (req.getEmailVerificationId() == null || req.getEmailVerificationId().isBlank()) {
                    return ResponseEntity.status(400).body(Map.of("ok", false, "reason", "EMAIL_VERIFICATION_REQUIRED"));
                }
                Long vId;
                try { vId = Long.parseLong(req.getEmailVerificationId()); }
                catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("ok", false, "reason", "INVALID_VERIFICATION_ID")); }

                // 3) 이메일 인증 확인 + 사용처리 (purpose = CHANGE_EMAIL)
                emailVerificationService.ensureVerifiedAndMarkUsed(vId, newEmail, "CHANGE_EMAIL");

                u.setEmail(newEmail);
                changed = true;
            }
        }

        if (req.getTel()!=null && !req.getTel().isBlank()) {
            u.setTel(req.getTel().trim());
            changed = true;
        }

        // ✅ 회사명 변경: 드라이버만 허용
        if (req.getCompany()!=null) {
            if (u.getRole() != Role.ROLE_DRIVER) {
                return ResponseEntity.status(403).body(Map.of("ok", false, "reason", "COMPANY_CHANGE_ONLY_FOR_DRIVER"));
            }
            u.setCompany(req.getCompany().trim());
            changed = true;
        }

        // ── 사진 처리 (상호 배타)
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
        return ResponseEntity.ok(toResponse(u));
    }

    // ───────── 비밀번호 변경 ─────────
    @PostMapping("/me/password")
    public ResponseEntity<?> changePassword(Authentication authentication,
                                            @RequestBody @Validated ChangePasswordRequest req) {
        String userid = (String) authentication.getPrincipal();
        var u = userRepository.findByUserid(userid).orElseThrow();

        if (!passwordEncoder.matches(req.getCurrentPassword(), u.getPassword())) {
            return ResponseEntity.status(400).body(Map.of("ok", false, "reason", "BAD_CURRENT_PASSWORD"));
        }
        if (passwordEncoder.matches(req.getNewPassword(), u.getPassword())) {
            return ResponseEntity.status(400).body(Map.of("ok", false, "reason", "SAME_AS_OLD"));
        }

        u.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(u);

        // 보안상: 모든 활성 세션 무효화
        List<com.example.demo.entity.UserSession> sessions = sessionRepository.findByUserAndRevokedIsFalse(u);
        for (var s : sessions) s.setRevoked(true);
        if (!sessions.isEmpty()) sessionRepository.saveAll(sessions);

        return ResponseEntity.ok(Map.of("ok", true));
    }

    // 헬퍼
    private UserResponse toResponse(UserEntity u) {
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
                .hasDriverLicenseFile(u.getDriverLicenseFile()!=null)
                .build();
    }
    
 // ───────── 계정 탈퇴 (비밀번호만 확인) ─────────
    @DeleteMapping("/me")
    @Transactional
    public ResponseEntity<?> deleteMe(Authentication authentication,
                                      @RequestBody @Validated DeleteAccountRequest req) {
        String userid = (String) authentication.getPrincipal();
        var u = userRepository.findByUserid(userid).orElseThrow();

        // 1) 비밀번호 확인
        if (!passwordEncoder.matches(req.getCurrentPassword(), u.getPassword())) {
            return ResponseEntity.status(400).body(Map.of("ok", false, "reason", "BAD_CURRENT_PASSWORD"));
        }

        // 2) (선택) 활성 세션 revoke — 즉시 리프레시 토큰 무효화
        var active = sessionRepository.findByUserAndRevokedIsFalse(u);
        for (var s : active) s.setRevoked(true);
        if (!active.isEmpty()) sessionRepository.saveAll(active);

        // 3) 계정 삭제 (DB에 FK ON DELETE CASCADE면 세션도 함께 정리됨)
        userRepository.delete(u);

        return ResponseEntity.ok(Map.of("ok", true, "message", "ACCOUNT_DELETED"));
    }
}
