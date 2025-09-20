package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ResetPasswordAfterVerifyRequest {
    @NotBlank private String userid;
    @Email @NotBlank private String email;

    @NotBlank private String verificationId; // 이메일 인증 성공한 ID

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9]{10,16}$", message = "새 비밀번호는 10~16자 영문/숫자만 가능합니다.")
    private String newPassword;
}
