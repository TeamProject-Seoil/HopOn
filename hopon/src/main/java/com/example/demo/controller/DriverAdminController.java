// src/main/java/com/example/demo/controller/DriverAdminController.java
package com.example.demo.controller;

import com.example.demo.dto.UserResponse;
import com.example.demo.entity.*;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/drivers")
@RequiredArgsConstructor
public class DriverAdminController {

    private final UserRepository userRepository;

    // 대기중 목록
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> listPending() {
        var list = userRepository.findByRoleAndApprovalStatus(Role.ROLE_DRIVER, ApprovalStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    // 면허 파일 보기/다운로드
    @GetMapping("/{userid}/license")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> getLicense(@PathVariable String userid) throws Exception {
        var u = userRepository.findByUserid(userid).orElseThrow();
        byte[] file = u.getDriverLicenseFile();
        if (file == null) return ResponseEntity.notFound().build();

        String guessed = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(file));
        MediaType mt = (guessed != null) ? MediaType.parseMediaType(guessed) : MediaType.APPLICATION_OCTET_STREAM;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mt);
        headers.setContentDisposition(ContentDisposition.inline().filename("license-" + userid).build());
        return new ResponseEntity<>(file, headers, HttpStatus.OK);
    }

    // 승인
    @PostMapping("/{userid}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approve(@PathVariable String userid) {
        var u = userRepository.findByUserid(userid).orElseThrow();
        if (u.getRole() != Role.ROLE_DRIVER) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "reason", "NOT_A_DRIVER"));
        }
        u.setApprovalStatus(ApprovalStatus.APPROVED);
        userRepository.save(u);
        return ResponseEntity.ok(Map.of("ok", true, "status", "APPROVED"));
    }

    // 거절
    @PostMapping("/{userid}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> reject(@PathVariable String userid) {
        var u = userRepository.findByUserid(userid).orElseThrow();
        if (u.getRole() != Role.ROLE_DRIVER) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "reason", "NOT_A_DRIVER"));
        }
        u.setApprovalStatus(ApprovalStatus.REJECTED);
        userRepository.save(u);
        return ResponseEntity.ok(Map.of("ok", true, "status", "REJECTED"));
    }

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
}
