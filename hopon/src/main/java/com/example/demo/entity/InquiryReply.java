package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inquiry_reply")
@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class InquiryReply {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id", nullable = false)
    private Inquiry inquiry;

    @Column(length = 5000, nullable = false)
    private String message;

    @Column(name = "admin_user_num")
    private Long adminUserNum;

    @CreationTimestamp private LocalDateTime createdAt;
}
