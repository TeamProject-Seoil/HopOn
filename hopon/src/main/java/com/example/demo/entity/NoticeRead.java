package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "notice_read",
    uniqueConstraints = @UniqueConstraint(name = "uk_notice_read_user_notice", columnNames = {"user_id", "notice_id"})
)
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class NoticeRead {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 읽은 사용자 식별자 (로그인 주체) */
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    /** 어떤 공지를 읽었는지 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notice_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Notice notice;

    @CreationTimestamp
    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;
}
