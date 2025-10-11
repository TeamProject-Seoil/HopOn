// src/main/java/com/example/demo/controller/DriverAdminController.java
package com.example.demo.controller;

import com.example.demo.dto.UserResponse;
import com.example.demo.entity.*;
import com.example.demo.repository.DriverLicenseRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/drivers")
@RequiredArgsConstructor
public class DriverAdminController {

    private final UserRepository userRepository;
    private final DriverLicenseRepository driverLicenseRepository;

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> listPending() {
        var list = userRepository.findByRoleAndApprovalStatus(Role.ROLE_DRIVER, ApprovalStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{userid}/license")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> getLicense(@PathVariable String userid) {
        var u = userRepository.findByUserid(userid).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();

        var dl = driverLicenseRepository.findByUser_UserNum(u.getUserNum()).orElse(null);
        if (dl == null || dl.getLicenseImage() == null) return ResponseEntity.notFound().build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.inline().filename("license-" + userid + ".bin").build());
        return new ResponseEntity<>(dl.getLicenseImage(), headers, HttpStatus.OK);
    }

    @PostMapping("/{userid}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approve(@PathVariable String userid) {
        var u = userRepository.findByUserid(userid).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();
        if (u.getRole() != Role.ROLE_DRIVER) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "reason", "NOT_A_DRIVER"));
        }
        u.setApprovalStatus(ApprovalStatus.APPROVED);
        userRepository.save(u);
        return ResponseEntity.ok(Map.of("ok", true, "status", "APPROVED"));
    }

    @PostMapping("/{userid}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> reject(@PathVariable String userid) {
        var u = userRepository.findByUserid(userid).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();
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
                .hasDriverLicenseFile(false) // 파일 컬럼은 제거, 별도 테이블 존재 여부는 필요 시 조회
                .build();
    }
}
