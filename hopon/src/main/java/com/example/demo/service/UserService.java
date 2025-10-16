// src/main/java/com/example/demo/service/UserService.java
package com.example.demo.service;

import com.example.demo.dto.RegisterRequest;
import com.example.demo.entity.ApprovalStatus;
import com.example.demo.entity.DriverLicenseEntity;
import com.example.demo.entity.Role;
import com.example.demo.entity.UserEntity;
import com.example.demo.repository.DriverLicenseRepository;
import com.example.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DriverLicenseRepository driverLicenseRepository;
    private final PasswordEncoder passwordEncoder;

    /** 일반 사용자 회원가입 (프로필만) */
    @Transactional
    public void registerUserWithProfile(RegisterRequest req, MultipartFile profile) {
        validateDup(req);

        String normUserid = req.getUserid().trim();
        String normEmail  = req.getEmail().trim().toLowerCase();

        byte[] imgBytes = readProfileOrDefault(profile);

        var user = UserEntity.builder()
                .userid(normUserid)
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .email(normEmail)
                .tel(req.getTel())
                .profileImage(imgBytes)
                .role(Role.ROLE_USER)
                .approvalStatus(ApprovalStatus.APPROVED) // 일반 유저는 즉시 승인
                .build();

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            // DB 유니크 경합 시 사용자 친화 에러
            throw new IllegalArgumentException("이미 존재하는 사용자 ID/이메일입니다.");
        }
    }

    /** 드라이버 회원가입 (프로필 + 면허 정보/사진 동시 저장) */
    @Transactional
    public void registerDriverWithLicense(RegisterRequest req,
                                          MultipartFile profile,
                                          MultipartFile licensePhoto) {
        validateDup(req);

        String normUserid = req.getUserid().trim();
        String normEmail  = req.getEmail().trim().toLowerCase();

        // 면허번호 정규화/검증/중복
        String licenseNo = normalizeLicenseNo(req.getLicenseNumber());
        if (licenseNo == null || licenseNo.isBlank())
            throw new IllegalArgumentException("자격증 번호를 입력하세요.");
        if (licenseNo.length() > 50)
            throw new IllegalArgumentException("자격증 번호가 너무 깁니다.");
        if (driverLicenseRepository.existsByLicenseNumber(licenseNo))
            throw new IllegalArgumentException("이미 등록된 자격증 번호입니다.");

        LocalDate acquired = parseAcquiredDate(req.getAcquiredDate());

        if (req.getCompany() == null || req.getCompany().isBlank())
            throw new IllegalArgumentException("회사명을 입력하세요.");

        byte[] imgBytes   = readProfileOrDefault(profile);
        byte[] licenseImg = readLicenseImage(licensePhoto);

        var user = UserEntity.builder()
                .userid(normUserid)
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .email(normEmail)
                .tel(req.getTel())
                .company(req.getCompany().trim()) // trim 추가
                .profileImage(imgBytes)
                .role(Role.ROLE_DRIVER)
                .approvalStatus(ApprovalStatus.PENDING) // 관리자 승인 대기
                .build();

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("이미 존재하는 사용자 ID/이메일입니다.");
        }

        var dl = DriverLicenseEntity.builder()
                .user(user)
                .licenseNumber(licenseNo)
                .acquiredDate(acquired)
                .licenseImage(licenseImg)
                .build();

        try {
            driverLicenseRepository.save(dl);
        } catch (DataIntegrityViolationException ex) {
            // license_number UNIQUE 충돌 등
            throw new IllegalArgumentException("이미 등록된 자격증 번호입니다.");
        }
    }

    /** 이름+이메일로 아이디 찾기 (AuthController에서 사용) */
    @Transactional(readOnly = true)
    public String findUseridByNameEmail(String username, String email) {
        return userRepository.findByUsernameAndEmail(username, email)
                .map(UserEntity::getUserid)
                .orElse(null);
    }

    // ===== 내부 유틸 =====
    private void validateDup(RegisterRequest req) {
        String normUserid = req.getUserid().trim();
        String normEmail  = req.getEmail() == null ? null : req.getEmail().trim().toLowerCase();

        // DB 콜레이션(utf8mb4_0900_ai_ci)이 대소문자 무시이므로 서버도 IgnoreCase로 통일
        if (userRepository.existsByUseridIgnoreCase(normUserid))
            throw new IllegalArgumentException("이미 존재하는 사용자 ID입니다.");
        if (normEmail != null && userRepository.existsByEmailIgnoreCase(normEmail))
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
    }

    private String normalizeLicenseNo(String s) {
        return s == null ? null : s.replaceAll("\\s|-", "").trim();
    }

    private LocalDate parseAcquiredDate(String s) {
        if (s == null || s.isBlank())
            throw new IllegalArgumentException("자격취득일을 입력하세요.");
        try {
            LocalDate d = LocalDate.parse(s); // yyyy-MM-dd
            if (d.isAfter(LocalDate.now()))
                throw new IllegalArgumentException("자격취득일은 미래일 수 없습니다.");
            return d;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("자격취득일 형식이 올바르지 않습니다(yyyy-MM-dd).");
        }
    }

    private byte[] readProfileOrDefault(MultipartFile profile) {
        try {
            if (profile != null && !profile.isEmpty()) {
                String ct = profile.getContentType();
                if (ct != null && !ct.startsWith("image/"))
                    throw new IllegalArgumentException("프로필 이미지는 이미지 파일만 허용됩니다.");
                long max = 2L * 1024 * 1024; // 2MB
                if (profile.getSize() > max)
                    throw new IllegalArgumentException("프로필 이미지는 최대 2MB까지 업로드 가능합니다.");
                return profile.getBytes();
            }
            // 기본 이미지
            var res = new ClassPathResource("static/profile_image/default_profile_image.jpg");
            try (InputStream is = res.getInputStream()) { return is.readAllBytes(); }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("프로필 이미지 처리 실패", e);
        }
    }

    private byte[] readLicenseImage(MultipartFile licensePhoto) {
        try {
            if (licensePhoto == null || licensePhoto.isEmpty())
                throw new IllegalArgumentException("면허 사진을 업로드하세요.");
            String ct = licensePhoto.getContentType();
            if (ct == null || !ct.startsWith("image/"))
                throw new IllegalArgumentException("면허 사진은 이미지 파일만 허용됩니다.");
            long max = 10L * 1024 * 1024; // 10MB
            if (licensePhoto.getSize() > max)
                throw new IllegalArgumentException("면허 사진은 최대 10MB까지 업로드 가능합니다.");
            return licensePhoto.getBytes();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("면허 사진 처리 실패", e);
        }
    }
}
