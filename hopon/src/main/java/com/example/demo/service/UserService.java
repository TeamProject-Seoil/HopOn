package com.example.demo.service;

import com.example.demo.dto.RegisterRequest;
import com.example.demo.entity.*; import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.security.SecureRandom;

@Service @RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository; 
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void registerWithProfile(RegisterRequest req, MultipartFile file) {
        validateDup(req);

        // 프로필 이미지(업로드 없으면 기본 이미지)
        byte[] imgBytes;
        try {
            if (file != null && !file.isEmpty()) {
                imgBytes = file.getBytes();
            } else {
                var res = new ClassPathResource("static/profile_image/default_profile_image.jpg");
                try (InputStream is = res.getInputStream()) { imgBytes = is.readAllBytes(); }
            }
        } catch (Exception e) { throw new RuntimeException("프로필 이미지 처리 실패", e); }

        // 앱별 기본 역할
        Role role = switch (req.getClientType()) {
            case "USER_APP"   -> Role.ROLE_USER;
            case "DRIVER_APP" -> Role.ROLE_DRIVER; // 승인제가 필요하면 ROLE_USER로 두고 관리자 승격
            default -> throw new IllegalArgumentException("허용되지 않은 clientType: " + req.getClientType());
        };

        var user = UserEntity.builder()
                .userid(req.getUserid())
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .email(req.getEmail())
                .tel(req.getTel())
                .profileImage(imgBytes)
                .role(role)
                .build();
        userRepository.save(user);
    }

    private void validateDup(RegisterRequest req) {
        if (userRepository.existsByUserid(req.getUserid())) throw new IllegalArgumentException("이미 존재하는 사용자 ID입니다.");
        if (req.getEmail()!=null && userRepository.existsByEmail(req.getEmail())) throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
    }
    
    /** 조건 일치 시 userid 반환 (없으면 null) */
    public String findUseridExact(String username, String tel, String email) {
        return userRepository.findByUsernameAndTelAndEmail(username, tel, email)
                .map(UserEntity::getUserid)
                .orElse(null);
    }

    /** 조건 일치 시 임시 비번 발급/저장 후 임시비번(평문) 반환, 없으면 null */
    @Transactional
    public String resetPasswordWithTemp(String userid, String username, String tel, String email) {
        var opt = userRepository.findByUseridAndUsernameAndTelAndEmail(userid, username, tel, email);
        if (opt.isEmpty()) return null;

        var user = opt.get();
        String temp = generateTempPassword(10);              // 길이 10
        user.setPassword(passwordEncoder.encode(temp));      // BCrypt 저장
        userRepository.save(user);
        return temp;
    }

    // 임시 비밀번호 생성기 (영문+숫자)
    private String generateTempPassword(int len) {
        final char[] pool = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(pool[r.nextInt(pool.length)]);
        return sb.toString();
    }
}