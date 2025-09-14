// entity/UserSession.java
package com.example.demo.entity;


import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="user_sessions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_num","client_type"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserSession {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="user_num", nullable=false)
    private UserEntity user;

    @Column(name="client_type", nullable=false, length=20)
    private String clientType; // USER_APP | DRIVER_APP | ADMIN_APP

    @Column(name="device_id", nullable=false, length=100)
    private String deviceId;

    @Column(name="refresh_token_hash", nullable=false, length=64)
    private String refreshTokenHash;

    @Column(name="expires_at", nullable=false)
    private LocalDateTime expiresAt;

    @Column(name="revoked", nullable=false)
    private boolean revoked;

    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", nullable=false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}
