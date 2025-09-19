package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class VerifyEmailCodeRequest {
    @NotBlank private String verificationId;
    @Email @NotBlank private String email;
    @NotBlank private String purpose;
    @NotBlank private String code;
}
