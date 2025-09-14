// entity/UserEntity.java
package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name="users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="user_num") private Long userNum;

    @Column(name="userid", nullable=false, unique=true, length=50) private String userid;
    @Column(name="username", length=100) private String username;
    @Column(name="password", nullable=false, length=255) private String password;
    @Column(name="email", length=100) private String email;
    @Column(name="tel", length=20) private String tel;

    @Lob @Column(name="profile_image", columnDefinition="LONGBLOB")
    private byte[] profileImage;

    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20)
    private Role role;
}
