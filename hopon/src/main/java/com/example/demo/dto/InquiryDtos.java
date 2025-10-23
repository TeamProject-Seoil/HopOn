// src/main/java/com/example/demo/dto/InquiryDtos.java
package com.example.demo.dto;

import com.example.demo.entity.Inquiry;
import com.example.demo.entity.InquiryStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class InquiryDtos {

    /** 비밀글이면 password 필수 (컨트롤러에서 검증), 서비스에서는 해시로 저장 */
    public record Create(String name, String email, String userid, String title, String content, boolean secret, String password) {}

    public record Att(Long id, String filename, String contentType, long size) {}

    public record Rep(Long id, String message, LocalDateTime createdAt) {}

    public record Resp(
            Long id,
            String name,
            String email,
            String userid,
            String title,
            String content,
            InquiryStatus status,
            boolean secret,
            boolean hasPassword,
            List<Att> attachments,
            List<Rep> replies,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static Resp from(Inquiry i) {
            List<Att> aa = Optional.ofNullable(i.getAttachments())
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(a -> new Att(a.getId(), a.getFilename(), a.getContentType(), a.getSize()))
                    .toList();

            List<Rep> rr = Optional.ofNullable(i.getReplies())
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(r -> new Rep(r.getId(), r.getMessage(), r.getCreatedAt()))
                    .toList();

            return new Resp(
                    i.getId(),
                    i.getName(),
                    i.getEmail(),
                    i.getUserid(),
                    i.getTitle(),
                    i.getContent(),
                    i.getStatus(),
                    i.isSecret(),
                    i.hasPassword(),
                    aa,
                    rr,
                    i.getCreatedAt(),
                    i.getUpdatedAt()
            );
        }

        /** 공개/타인 뷰에서 마스킹 */
        public static Resp fromRedactedIfSecret(Inquiry i, boolean isOwnerOrAdmin) {
            boolean redacted = i.isSecret() && !isOwnerOrAdmin;
            List<Att> aa = Optional.ofNullable(i.getAttachments()).orElse(Collections.emptyList())
                    .stream()
                    .map(a -> new Att(a.getId(), a.getFilename(), a.getContentType(), a.getSize()))
                    .toList();
            List<Rep> rr = redacted ? List.of()
                    : Optional.ofNullable(i.getReplies()).orElse(Collections.emptyList())
                      .stream().map(r -> new Rep(r.getId(), r.getMessage(), r.getCreatedAt())).toList();

            return new Resp(
                    i.getId(),
                    i.getName(),
                    i.getEmail(),
                    i.getUserid(),
                    i.getTitle(),
                    redacted ? "비밀글입니다." : i.getContent(),
                    i.getStatus(),
                    i.isSecret(),
                    i.hasPassword(),
                    aa,
                    rr,
                    i.getCreatedAt(),
                    i.getUpdatedAt()
            );
        }
    }
}
