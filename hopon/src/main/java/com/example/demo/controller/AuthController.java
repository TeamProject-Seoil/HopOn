// src/main/java/com/example/demo/controller/AuthController.java
package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.security.PasswordPolicy;
import com.example.demo.service.EmailVerificationService;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final EmailVerificationService emailVerificationService;

    @Value("${jwt.refresh-exp-days}")
    private long refreshExpDays;

    @Value("${jwt.refresh-absolute-max-days:0}")
    private long refreshAbsoluteMaxDays;

    @GetMapping("/check")
    public ResponseEntity<?> checkDup(
            @RequestParam(required = false) String userid,
            @RequestParam(required = false) String email
    ) {
        boolean useridTaken = userid != null && userRepository.existsByUserid(userid);
        boolean emailTaken  = email  != null && userRepository.existsByEmail(email);
        return ResponseEntity.ok(Map.of("useridTaken", useridTaken, "emailTaken", emailTaken));
    }

    @PostMapping(value = "/register", consumes = {"multipart/form-data"})
    public ResponseEntity<?> register(@RequestPart("data") @Validated RegisterRequest req,
                                      @RequestPart(value = "file", required = false) MultipartFile file,
                                      @RequestPart(value = "license", required = false) MultipartFile license) {
        if (userRepository.existsByUserid(req.getUserid()))
            return ResponseEntity.status(409).body(Map.of("ok", false, "reason", "DUPLICATE_USERID"));
        if (req.getEmail() != null && userRepository.existsByEmail(req.getEmail()))
            return ResponseEntity.status(409).body(Map.of("ok", false, "reason", "DUPLICATE_EMAIL"));

        Long vId = parseVerificationId(req.getVerificationId());
        emailVerificationService.ensureVerifiedAndMarkUsed(vId, req.getEmail(), "REGISTER");

        //▼ 비밀번호 정책 검사
        String reason = PasswordPolicy.validateAndReason(req.getPassword());
        if (reason != null) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "reason", "PASSWORD_POLICY_VIOLATION", "message", reason));
        }
        
        if ("DRIVER_APP".equals(req.getClientType())) {
            if (req.getCompany() == null || req.getCompany().isBlank())
                return ResponseEntity.badRequest().body(Map.of("ok", false, "reason", "COMPANY_REQUIRED_FOR_DRIVER"));
            if (license == null || license.isEmpty())
                return ResponseEntity.badRequest().body(Map.of("ok", false, "reason", "LICENSE_REQUIRED_FOR_DRIVER"));
            if (license.getSize() > 10L * 1024 * 1024)
                return ResponseEntity.status(413).body(Map.of("ok", false, "reason", "LICENSE_TOO_LARGE"));
            String ct = license.getContentType();
            if (ct == null || !(ct.startsWith("image/") || "application/pdf".equals(ct)))
                return ResponseEntity.badRequest().body(Map.of("ok", false, "reason", "INVALID_LICENSE_TYPE"));
        } else {
            license = null;
            req.setCompany(null);
        }

        userService.registerWithProfile(req, file, license);
        return ResponseEntity.status(201).body(Map.of("ok", true, "message", "REGISTERED", "userid", req.getUserid()));
    }

    private Long parseVerificationId(String s) {
        try { return Long.parseLong(s); } catch (Exception e) { throw new IllegalArgumentException("verificationId 형식 오류"); }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Validated @RequestBody AuthRequest req) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUserid(), req.getPassword()));
        } catch (Exception e) {
            log.warn("로그인 실패 - 자격증명 불일치: userid={}", req.getUserid());
            return ResponseEntity.status(401).header("X-Reason", "BAD_CREDENTIALS").build();
        }

        var user = userRepository.findByUserid(req.getUserid())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        String role = user.getRole().name();
        String clientType = req.getClientType();
        String deviceId   = req.getDeviceId();

        if (!isAllowed(clientType, role)) {
            log.warn("로그인 실패 - 앱 권한 불일치: userid={}, role={}, clientType={}", req.getUserid(), role, clientType);
            return ResponseEntity.status(403).header("X-Reason", "ROLE_NOT_ALLOWED_FOR_APP").build();
        }
        if ("DRIVER_APP".equals(clientType) && user.getRole() == Role.ROLE_DRIVER) {
            if (user.getApprovalStatus() != ApprovalStatus.APPROVED) {
                log.warn("로그인 실패 - 드라이버 미승인: userid={}, status={}", req.getUserid(), user.getApprovalStatus());
                return ResponseEntity.status(403).header("X-Reason", "DRIVER_NOT_APPROVED").build();
            }
        }

        var existing = sessionRepository.findByUserAndClientType(user, clientType);
        if (existing.isPresent()) {
            var s = existing.get();
            boolean stillValid = !s.isRevoked() && s.getExpiresAt().isAfter(LocalDateTime.now());
            if (stillValid && !s.getDeviceId().equals(deviceId)) {
                log.warn("로그인 실패 - 다른 기기에서 이미 로그인 중: userid={}, app={}, existingDevice={}, requestDevice={}",
                        req.getUserid(), clientType, s.getDeviceId(), deviceId);
                return ResponseEntity.status(409)
                        .header("X-Reason", "ALREADY_LOGGED_IN_OTHER_DEVICE")
                        .build();
            }
        }

        String access  = tokenProvider.generateAccessToken(user.getUserid(), role, clientType);
        String refresh = tokenProvider.generateRefreshToken(user.getUserid(), role, clientType);
        upsertSession(user, clientType, deviceId, refresh);

        log.info("로그인 성공: userid={}, role={}, app={}, device={}", req.getUserid(), role, clientType, deviceId);
        return ResponseEntity.ok(new AuthResponse(access, refresh, "Bearer", role));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestParam("refreshToken") String refreshToken,
            @RequestParam("clientType") String clientType,
            @RequestParam("deviceId") String deviceId
    ) {
        if (!tokenProvider.validate(refreshToken))
            throw new BadCredentialsException("Refresh token invalid");

        String userid = tokenProvider.getUserid(refreshToken);
        var user = userRepository.findByUserid(userid).orElseThrow(() -> new BadCredentialsException("User not found"));

        var sessionOpt = sessionRepository
                .findByUserAndClientTypeAndDeviceIdAndRevokedIsFalseAndExpiresAtAfter(
                        user, clientType, deviceId, LocalDateTime.now());
        if (sessionOpt.isEmpty())
            throw new BadCredentialsException("Session not found or expired");

        var session = sessionOpt.get();

        if (isAbsoluteCapExceeded(session)) {
            session.setRevoked(true);
            sessionRepository.save(session);
            throw new BadCredentialsException("Session absolute lifetime exceeded");
        }

        String presentedHash = com.example.demo.service.HashUtils.sha256Hex(refreshToken);
        if (!presentedHash.equals(session.getRefreshTokenHash())) {
            log.warn("리프레시 토큰 재사용(또는 위조) 감지: userid={}, app={}, device={}", userid, clientType, deviceId);
            session.setRevoked(true);
            sessionRepository.save(session);
            throw new BadCredentialsException("Refresh token mismatch");
        }

        String role = user.getRole().name();
        String newAccess  = tokenProvider.generateAccessToken(userid, role, clientType);
        String newRefresh = tokenProvider.generateRefreshToken(userid, role, clientType);
        rotateSession(session, newRefresh);

        return ResponseEntity.ok(new AuthResponse(newAccess, newRefresh, "Bearer", role));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody @Validated LogoutRequest req) {
        if (!tokenProvider.validate(req.getRefreshToken())) {
            log.warn("로그아웃 실패 - refreshToken 파싱 불가");
            return ResponseEntity.status(400).body(Map.of("ok", false, "reason", "INVALID_REFRESH_TOKEN"));
        }
        String userid = tokenProvider.getUserid(req.getRefreshToken());
        var user = userRepository.findByUserid(userid).orElse(null);
        if (user == null) {
            log.warn("로그아웃 실패 - 사용자 없음: useridFromToken={}", userid);
            return ResponseEntity.status(400).body(Map.of("ok", false, "reason", "USER_NOT_FOUND"));
        }

        var sessionOpt = sessionRepository.findByUserAndClientTypeAndDeviceId(user, req.getClientType(), req.getDeviceId());
        if (sessionOpt.isEmpty()) {
            log.warn("로그아웃 실패 - 세션 없음: userid={}, app={}, device={}", userid, req.getClientType(), req.getDeviceId());
            return ResponseEntity.status(400).body(Map.of("ok", false, "reason", "SESSION_NOT_FOUND"));
        }

        var session = sessionOpt.get();
        String hash = com.example.demo.service.HashUtils.sha256Hex(req.getRefreshToken());
        if (!hash.equals(session.getRefreshTokenHash())) {
            log.warn("로그아웃 실패 - refreshToken 불일치: userid={}, app={}, device={}", userid, req.getClientType(), req.getDeviceId());
            return ResponseEntity.status(400).body(Map.of("ok", false, "reason", "REFRESH_TOKEN_MISMATCH"));
        }

        session.setRevoked(true);
        sessionRepository.save(session);
        log.info("로그아웃 성공: userid={}, app={}, device={}", userid, req.getClientType(), req.getDeviceId());
        return ResponseEntity.ok(Map.of("ok", true, "message", "LOGGED_OUT"));
    }

    /** 아이디 찾기: 이름+이메일 (+ 이메일 인증 사용처리) */
    @PostMapping("/find-id-after-verify")
    public ResponseEntity<?> findIdAfterVerify(@RequestBody @Validated FindIdAfterVerifyRequest req) {
        Long vId = Long.parseLong(req.getVerificationId());
        emailVerificationService.ensureVerifiedAndMarkUsed(vId, req.getEmail(), "FIND_ID");

        String userid = userService.findUseridByNameEmail(req.getUsername(), req.getEmail());
        if (userid == null) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "reason", "NOT_FOUND"));
        }
        return ResponseEntity.ok(Map.of("ok", true, "userid", userid));
    }

    /** 비밀번호 재설정: 아이디+이메일 (+ 이메일 인증 사용처리) */
    @PostMapping("/reset-password-after-verify")
    public ResponseEntity<?> resetPasswordAfterVerify(@RequestBody @Validated ResetPasswordAfterVerifyRequest req) {
        // ▼ 비밀번호 정책 검사
        String reason = PasswordPolicy.validateAndReason(req.getNewPassword());
        if (reason != null) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "reason", "PASSWORD_POLICY_VIOLATION", "message", reason));
        }

        Long vId = Long.parseLong(req.getVerificationId());
        emailVerificationService.ensureVerifiedAndMarkUsed(vId, req.getEmail(), "RESET_PW");

        var opt = userRepository.findByUseridAndEmail(req.getUserid(), req.getEmail());
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "reason", "NOT_FOUND"));
        }

        var u = opt.get();
        u.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(u);

        // 세션 revoke 유지
        var sessions = sessionRepository.findByUserAndRevokedIsFalse(u);
        for (var s : sessions) s.setRevoked(true);
        if (!sessions.isEmpty()) sessionRepository.saveAll(sessions);

        return ResponseEntity.ok(Map.of("ok", true, "message", "PASSWORD_RESET"));
    }

    // ───────── 내부 헬퍼들 ─────────
    private void upsertSession(UserEntity user, String clientType, String deviceId, String refreshToken) {
        var now = LocalDateTime.now();
        var exp = now.plusDays(refreshExpDays);

        var opt = sessionRepository.findByUserAndClientType(user, clientType);
        if (opt.isPresent()) {
            var s = opt.get();
            s.setDeviceId(deviceId);
            s.setRefreshTokenHash(com.example.demo.service.HashUtils.sha256Hex(refreshToken));
            s.setExpiresAt(applyAbsoluteCapIfNeeded(s, exp));
            s.setRevoked(false);
            sessionRepository.save(s);
        } else {
            var s = UserSession.builder()
                    .user(user)
                    .clientType(clientType)
                    .deviceId(deviceId)
                    .refreshTokenHash(com.example.demo.service.HashUtils.sha256Hex(refreshToken))
                    .expiresAt(exp)
                    .revoked(false)
                    .build();
            sessionRepository.save(s);
            if (refreshAbsoluteMaxDays > 0) {
                s.setExpiresAt(applyAbsoluteCapIfNeeded(s, s.getExpiresAt()));
                sessionRepository.save(s);
            }
        }
    }

    private void rotateSession(UserSession s, String newRefreshToken) {
        var now = LocalDateTime.now();
        if (isAbsoluteCapExceeded(s)) {
            s.setRevoked(true);
            sessionRepository.save(s);
            throw new BadCredentialsException("Session absolute lifetime exceeded");
        }
        s.setRefreshTokenHash(com.example.demo.service.HashUtils.sha256Hex(newRefreshToken));
        var desiredExp = now.plusDays(refreshExpDays);
        s.setExpiresAt(applyAbsoluteCapIfNeeded(s, desiredExp));
        sessionRepository.save(s);
    }

    private boolean isAllowed(String clientType, String role) {
        return switch (clientType) {
            case "USER_APP"   -> role.equals("ROLE_USER")   || role.equals("ROLE_ADMIN");
            case "DRIVER_APP" -> role.equals("ROLE_DRIVER") || role.equals("ROLE_ADMIN");
            case "ADMIN_APP"  -> role.equals("ROLE_ADMIN");
            default -> false;
        };
    }

    private LocalDateTime applyAbsoluteCapIfNeeded(UserSession s, LocalDateTime desiredExp) {
        if (refreshAbsoluteMaxDays <= 0) return desiredExp;
        var capEnd = s.getCreatedAt().plusDays(refreshAbsoluteMaxDays);
        return desiredExp.isAfter(capEnd) ? capEnd : desiredExp;
    }

    private boolean isAbsoluteCapExceeded(UserSession s) {
        if (refreshAbsoluteMaxDays <= 0) return false;
        var capEnd = s.getCreatedAt().plusDays(refreshAbsoluteMaxDays);
        return LocalDateTime.now().isAfter(capEnd);
    }
}
