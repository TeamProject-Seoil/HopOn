package com.example.demo.dto;

import com.example.demo.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class UserResponse {
    private Long userNum;
    private String userid;
    private String username;
    private String email;
    private String tel;
    private Role role;
    private boolean hasProfileImage;
}