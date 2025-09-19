// src/main/java/com/example/demo/dto/DeleteAccountRequest.java
package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DeleteAccountRequest {
    @NotBlank
    private String currentPassword;   // 본인확인용 (비밀번호만!)
}
