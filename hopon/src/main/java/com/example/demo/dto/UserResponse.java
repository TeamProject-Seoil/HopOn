// src/main/java/com/example/demo/dto/UserResponse.java
package com.example.demo.dto;

import com.example.demo.entity.ApprovalStatus;
import com.example.demo.entity.Role;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.*;
import java.time.format.DateTimeFormatter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserResponse {
    private Long userNum;
    private String userid;
    private String username;
    private String email;
    private String tel;
    private Role role;                        // 기존 코드 그대로 유지
    private boolean hasProfileImage;

    private String company;
    private ApprovalStatus approvalStatus;
    private boolean hasDriverLicenseFile;

    // ⭐ 클라가 받는 필드 이름을 lastLoginAt 으로 직렬화
    @JsonProperty("lastLoginAt")
    private String lastLoginAtIso;            // ISO8601 "…Z"

    // 유틸: LocalDateTime -> ISO_INSTANT (UTC) 문자열
    public static String toIsoOrNull(LocalDateTime t) {
        if (t == null) return null;
        return t.atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT);
    }
}
