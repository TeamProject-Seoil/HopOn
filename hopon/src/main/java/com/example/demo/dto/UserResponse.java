package com.example.demo.dto;

import com.example.demo.entity.ApprovalStatus;
import com.example.demo.entity.Role;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserResponse {
    private Long userNum;
    private String userid;
    private String username;
    private String email;
    private String tel;
    private Role role;
    private boolean hasProfileImage;

    // 👇 추가
    private String company;
    private ApprovalStatus approvalStatus;
    private boolean hasDriverLicenseFile;
}
