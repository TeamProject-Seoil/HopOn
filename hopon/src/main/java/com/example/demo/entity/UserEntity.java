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

    @Column(name="email", nullable=false, length=100, unique=true) // ✅ UNIQUE + NOT NULL
    private String email;

    @Column(name="tel", length=20) private String tel;

    @Lob @Column(name="profile_image", columnDefinition="LONGBLOB")
    private byte[] profileImage;

    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20)
    private Role role;
}
