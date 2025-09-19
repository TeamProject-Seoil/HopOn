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
import java.security.SecureRandom;

@Service @RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository; 
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void registerWithProfile(RegisterRequest req, MultipartFile file) {
        validateDup(req);

        // 기본/업로드 프로필
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
            case "DRIVER_APP" -> Role.ROLE_DRIVER;
            default -> throw new IllegalArgumentException("허용되지 않은 clientType: " + req.getClientType());
        };

        var normalizedEmail = req.getEmail().trim().toLowerCase(); // ✅ 소문자 저장
        var user = UserEntity.builder()
                .userid(req.getUserid())
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .email(normalizedEmail)
                .tel(req.getTel())
                .profileImage(imgBytes)
                .role(role)
                .build();
        userRepository.save(user);
    }

    private void validateDup(RegisterRequest req) {
        if (userRepository.existsByUserid(req.getUserid()))
            throw new IllegalArgumentException("이미 존재하는 사용자 ID입니다.");
        if (req.getEmail()!=null && userRepository.existsByEmail(req.getEmail().trim().toLowerCase()))
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
    }
    
    public String findUseridExact(String username, String tel, String email) {
        return userRepository.findByUsernameAndTelAndEmail(username, tel, email.toLowerCase())
                .map(UserEntity::getUserid)
                .orElse(null);
    }

    @Transactional
    public String resetPasswordWithTemp(String userid, String username, String tel, String email) {
        var opt = userRepository.findByUseridAndUsernameAndTelAndEmail(userid, username, tel, email.toLowerCase());
        if (opt.isEmpty()) return null;

        var user = opt.get();
        String temp = generateTempPassword(10);
        user.setPassword(passwordEncoder.encode(temp));
        userRepository.save(user);
        return temp;
    }

    private String generateTempPassword(int len) {
        final char[] pool = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(pool[r.nextInt(pool.length)]);
        return sb.toString();
    }
}
