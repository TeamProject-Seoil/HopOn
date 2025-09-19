// src/main/java/com/example/demo/dto/RegisterRequest.java
package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegisterRequest {
    @NotBlank private String userid;
    @NotBlank private String password;
    @Email @NotBlank private String email;
    @NotBlank private String username;
    private String tel;

    @NotBlank private String clientType;   // USER_APP | DRIVER_APP
    @NotBlank private String verificationId;

    // ▼ DRIVER_APP일 때만 의미 있음
    @Size(max = 100)
    private String company;
}
