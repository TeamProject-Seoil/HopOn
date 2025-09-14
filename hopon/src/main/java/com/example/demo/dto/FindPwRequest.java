package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class FindPwRequest {
    @NotBlank private String userid;
    @NotBlank private String username;
    @NotBlank private String tel;
    @Email @NotBlank private String email;
}
