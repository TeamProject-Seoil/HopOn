package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class FindIdAfterVerifyRequest {
    @NotBlank private String username;
    @NotBlank private String tel;
    @Email @NotBlank private String email;

    @NotBlank private String verificationId; // 이메일 인증 성공한 ID
}
