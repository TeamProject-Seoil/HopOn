package com.example.demo.controller;

import com.example.demo.dto.UserResponse;
import com.example.demo.dto.UpdateProfileRequest;
import com.example.demo.dto.ChangePasswordRequest;
import com.example.demo.entity.UserEntity;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserSessionRepository;
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
        if (req.getUsername()!=null && !req.getUsername().isBlank()) { u.setUsername(req.getUsername().trim()); changed = true; }
        if (req.getEmail()!=null) {
            String newEmail = req.getEmail().trim().toLowerCase(); // ✅ 소문자
            if (!newEmail.equalsIgnoreCase(u.getEmail()) && userRepository.existsByEmail(newEmail)) {
                return ResponseEntity.status(409).body(Map.of("ok", false, "reason", "DUPLICATE_EMAIL"));
            }
            u.setEmail(newEmail); changed = true;
        }
        if (req.getTel()!=null && !req.getTel().isBlank()) { u.setTel(req.getTel().trim()); changed = true; }

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

    private UserResponse toResponse(UserEntity u) {
        return new UserResponse(
                u.getUserNum(), u.getUserid(), u.getUsername(), u.getEmail(), u.getTel(),
                u.getRole(), u.getProfileImage()!=null
        );
    }
}
