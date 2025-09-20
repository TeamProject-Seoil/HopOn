// src/main/java/com/example/demo/service/UserService.java
package com.example.demo.service;

import com.example.demo.dto.RegisterRequest;
import com.example.demo.entity.*;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void registerWithProfile(RegisterRequest req, MultipartFile profile, MultipartFile license) {
        validateDup(req);

        byte[] imgBytes;
        try {
            if (profile != null && !profile.isEmpty()) {
                imgBytes = profile.getBytes();
            } else {
                var res = new ClassPathResource("static/profile_image/default_profile_image.jpg");
                try (InputStream is = res.getInputStream()) { imgBytes = is.readAllBytes(); }
            }
        } catch (Exception e) { throw new RuntimeException("프로필 이미지 처리 실패", e); }

        Role role = switch (req.getClientType()) {
            case "USER_APP"   -> Role.ROLE_USER;
            case "DRIVER_APP" -> Role.ROLE_DRIVER;
            default -> throw new IllegalArgumentException("허용되지 않은 clientType: " + req.getClientType());
        };
        ApprovalStatus approval = "DRIVER_APP".equals(req.getClientType())
                ? ApprovalStatus.PENDING
                : ApprovalStatus.APPROVED;

        byte[] licenseBytes = null;
        try {
            if ("DRIVER_APP".equals(req.getClientType()) && license != null && !license.isEmpty()) {
                licenseBytes = license.getBytes();
            }
        } catch (Exception e) { throw new RuntimeException("면허 파일 처리 실패", e); }

        var user = UserEntity.builder()
                .userid(req.getUserid())
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .email(req.getEmail())
                .tel(req.getTel())
                .company("DRIVER_APP".equals(req.getClientType()) ? req.getCompany() : null)
                .profileImage(imgBytes)
                .driverLicenseFile(licenseBytes)
                .role(role)
                .approvalStatus(approval)
                .build();
        userRepository.save(user);
    }

    private void validateDup(RegisterRequest req) {
        if (userRepository.existsByUserid(req.getUserid()))
            throw new IllegalArgumentException("이미 존재하는 사용자 ID입니다.");
        if (req.getEmail()!=null && userRepository.existsByEmail(req.getEmail()))
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
    }

    /** 이름+이메일로 아이디 찾기 */
    public String findUseridByNameEmail(String username, String email) {
        return userRepository.findByUsernameAndEmail(username, email)
                .map(UserEntity::getUserid)
                .orElse(null);
    }
}
