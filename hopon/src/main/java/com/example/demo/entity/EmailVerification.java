package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_verifications",
       indexes = {
           @Index(name="ix_email_purpose", columnList = "email,purpose"),
           @Index(name="ix_expires", columnList = "expiresAt")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailVerification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=120)
    private String email;

    @Column(nullable=false, length=30)
    private String purpose; // REGISTER | FIND_ID | RESET_PW

    @Column(nullable=false, length=64)
    private String codeHash; // 6자리 코드의 SHA-256

    @Column(nullable=false)
    private LocalDateTime expiresAt;

    @Column(nullable=false)
    private boolean verified;

    @Column(nullable=false)
    private boolean used;

    @Column(nullable=false)
    private LocalDateTime createdAt;

    private LocalDateTime verifiedAt;
    private LocalDateTime usedAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}
