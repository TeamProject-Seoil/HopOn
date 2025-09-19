package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="user_num") private Long userNum;

    @Column(name="userid", nullable=false, unique=true, length=50) private String userid;
    @Column(name="username", length=100) private String username;
    @Column(name="password", nullable=false, length=255) private String password;
    @Column(name="email", nullable=false, unique=true, length=100) private String email;
    @Column(name="tel", length=20) private String tel;

    @Lob @Column(name="profile_image", columnDefinition="LONGBLOB")
    private byte[] profileImage;

    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20)
    private Role role;

    // 👇 추가
    @Column(name="company", length=100)
    private String company;

    @Enumerated(EnumType.STRING) @Column(name="approval_status", nullable=false, length=20)
    private ApprovalStatus approvalStatus;

    @Lob @Column(name="driver_license_file", columnDefinition="LONGBLOB")
    private byte[] driverLicenseFile;
}
