package com.example.demo.controller;

import com.example.demo.dto.SendEmailCodeRequest;
import com.example.demo.dto.VerifyEmailCodeRequest;
import com.example.demo.service.EmailService;
import com.example.demo.service.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth/email")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService verificationService;
    private final EmailService emailService;

    // 1) 코드 발송
    @PostMapping("/send-code")
    public ResponseEntity<?> send(@RequestBody @Valid SendEmailCodeRequest req) {
        var ev = verificationService.createAndSend(
                req.getEmail(),
                req.getPurpose(),
                (to, purpose, code) -> emailService.sendVerificationCode(to, purpose, code)
        );
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "verificationId", String.valueOf(ev.getId()),
                "message", "CODE_SENT"
        ));
    }

    // 2) 코드 검증
    @PostMapping("/verify-code")
    public ResponseEntity<?> verify(@RequestBody @Valid VerifyEmailCodeRequest req) {
        var id = Long.parseLong(req.getVerificationId());
        var ev = verificationService.verify(id, req.getEmail(), req.getPurpose(), req.getCode());
        return ResponseEntity.ok(Map.of("ok", true, "verificationId", String.valueOf(ev.getId())));
    }
}
