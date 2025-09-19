package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendVerificationCode(String to, String purpose, String code) {
        String subject = "[HopOn] " + purpose + " 이메일 인증 코드";
        String text = """
                안녕하세요. HopOn 입니다.

                요청하신 이메일 인증 코드는 아래와 같습니다.
                인증코드: %s

                * 유효기간: 10분
                * 다른 사람에게 공유하지 마세요.
                """.formatted(code);

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom("hopon2025@naver.com"); 
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(text);
        mailSender.send(msg);
    }
}
