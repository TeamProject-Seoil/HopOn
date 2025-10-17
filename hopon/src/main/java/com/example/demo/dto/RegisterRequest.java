// src/main/java/com/example/demo/dto/RegisterRequest.java
package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegisterRequest {

    @NotBlank private String userid;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9]{10,16}$", message = "비밀번호는 10~16자 영문/숫자만 가능합니다.")
    private String password;

    @Email @NotBlank private String email;

    @NotBlank private String username;

    private String tel;

    /** USER_APP | DRIVER_APP */
    @NotBlank private String clientType;

    /** 이메일 인증 식별자 */
    @NotBlank private String verificationId;

    /** DRIVER_APP일 때만 의미 있음 */
    @Size(max = 100)
    private String company;

    // ▼ 새로 추가: DRIVER_APP일 때만 필수
    /** 자격증(면허) 번호 */
    private String licenseNumber;

    /** 취득일(yyyy-MM-dd) */
    private String acquiredDate;
    
    private String birthDate;     // yyyy-MM-dd
    
    private String licenseName;
}
