package com.example.demo.service;

import com.example.demo.entity.EmailVerification;
import com.example.demo.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private final EmailVerificationRepository repo;

    private String generate6() {
        SecureRandom r = new SecureRandom();
        int n = r.nextInt(1_000_000);
        return String.format("%06d", n);
    }

    @Transactional
    public EmailVerification createAndSend(String email, String purpose, EmailSender sender) {
        String norm = email.trim().toLowerCase(); // ✅ 통일
        String code = generate6();
        String hash = HashUtils.sha256Hex(code);
        var ev = EmailVerification.builder()
                .email(norm)
                .purpose(purpose)
                .codeHash(hash)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .verified(false)
                .used(false)
                .build();
        var saved = repo.save(ev);

        sender.send(norm, purpose, code);
        return saved;
    }

    @Transactional
    public EmailVerification verify(Long verificationId, String email, String purpose, String code) {
        String norm = email.trim().toLowerCase(); // ✅ 통일
        var ev = repo.findByIdAndEmailAndPurpose(verificationId, norm, purpose)
                .orElseThrow(() -> new IllegalArgumentException("인증 요청을 찾을 수 없습니다."));
        if (ev.isUsed()) throw new IllegalStateException("이미 사용된 인증입니다.");
        if (ev.isVerified()) return ev;
        if (LocalDateTime.now().isAfter(ev.getExpiresAt()))
            throw new IllegalStateException("인증 코드가 만료되었습니다.");

        String hash = HashUtils.sha256Hex(code);
        if (!hash.equals(ev.getCodeHash()))
            throw new IllegalArgumentException("인증 코드가 일치하지 않습니다.");

        ev.setVerified(true);
        ev.setVerifiedAt(LocalDateTime.now());
        return repo.save(ev);
    }

    @Transactional
    public void ensureVerifiedAndMarkUsed(Long verificationId, String email, String purpose) {
        String norm = email.trim().toLowerCase(); // ✅ 통일
        var ev = repo.findByIdAndEmailAndPurpose(verificationId, norm, purpose)
                .orElseThrow(() -> new IllegalArgumentException("유효한 이메일 인증이 없습니다."));
        if (!ev.isVerified()) throw new IllegalStateException("이메일 인증이 완료되지 않았습니다.");
        if (LocalDateTime.now().isAfter(ev.getExpiresAt()))
            throw new IllegalStateException("이메일 인증이 만료되었습니다.");
        if (ev.isUsed()) throw new IllegalStateException("이메일 인증이 이미 사용되었습니다.");
        ev.setUsed(true);
        ev.setUsedAt(LocalDateTime.now());
        repo.save(ev);
    }

    @FunctionalInterface
    public interface EmailSender {
        void send(String to, String purpose, String code);
    }
}
