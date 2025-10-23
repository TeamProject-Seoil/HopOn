// src/main/java/com/example/demo/entity/Inquiry.java
package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inquiry")
@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class Inquiry {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(length = 120)
    private String userid;

    @Column(nullable = false, length = 200)
    private String email;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryStatus status;

    /** 비밀글 여부 */
    @Column(name = "is_secret", nullable = false)
    @Builder.Default
    private boolean secret = false;

    /** 비밀글 비밀번호 해시(BCrypt), 비밀글이 아닐 땐 null */
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Builder.Default
    @OneToMany(mappedBy = "inquiry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InquiryAttachment> attachments = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "inquiry", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<InquiryReply> replies = new ArrayList<>();

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp  private LocalDateTime updatedAt;

    public boolean isSecret() {
        return secret;
    }

    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isBlank();
    }
}
