// src/main/java/com/example/demo/entity/InquiryAttachment.java
package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inquiry_attachment")
@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class InquiryAttachment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id", nullable = false)
    private Inquiry inquiry;

    @Column(nullable = false, length = 200)
    private String filename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Lob
    private byte[] bytes;

    @Column(nullable = false)
    private long size;

    @CreationTimestamp private LocalDateTime createdAt;
}
