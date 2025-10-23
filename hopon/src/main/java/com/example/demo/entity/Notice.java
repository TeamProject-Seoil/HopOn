package com.example.demo.entity;

import com.example.demo.entity.NoticeType;
import com.example.demo.entity.TargetRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name="notice")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Notice {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false,length=200)
    private String title;

    @Lob
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name="notice_type", nullable=false,length=30)
    private NoticeType noticeType;

    @Enumerated(EnumType.STRING)
    @Column(name="target_role",nullable=false,length=20)
    private TargetRole targetRole;

    @Column(name="view_count",nullable=false)
    private long viewCount;

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp  private LocalDateTime updatedAt;
}
