package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegisterRequest {
    @NotBlank private String userid;
    @NotBlank private String password;
    @Email @NotBlank private String email;
    @NotBlank private String username;
    private String tel;

    // 어떤 앱에서 가입하는지: USER_APP | DRIVER_APP
    @NotBlank private String clientType;
    
    @NotBlank
    private String verificationId; // 이메일 인증 성공한 ID
}
