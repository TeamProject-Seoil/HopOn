// src/main/java/com/example/demo/service/InquiryService.java
package com.example.demo.service;

import com.example.demo.config.SecurityBeans.PasswordHasher;
import com.example.demo.dto.InquiryDtos;
import com.example.demo.entity.Inquiry;
import com.example.demo.entity.InquiryAttachment;
import com.example.demo.entity.InquiryStatus;
import com.example.demo.repository.InquiryAttachmentRepository;
import com.example.demo.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class InquiryService {
    private final InquiryRepository repo;
    private final InquiryAttachmentRepository attRepo;
    private final PasswordHasher hasher;

    /** (신규) 공개 목록 */
    @Transactional(readOnly = true)
    public Page<Inquiry> listPublic(String q, InquiryStatus status, Pageable pageable) {
        return repo.findPublic(q, status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Inquiry> list(String email, String q, InquiryStatus status, Pageable pageable) {
        return repo.findForClient(email, q, status, pageable);
    }

    @Transactional(readOnly = true)
    public Inquiry getMine(Long id, String email) {
        return repo.findByIdAndEmail(id, email)
                .orElseThrow(() -> new IllegalArgumentException("문의가 없거나 권한이 없습니다."));
    }

    @Transactional(readOnly = true)
    public Inquiry get(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문의입니다."));
    }

    @Transactional
    public Inquiry create(InquiryDtos.Create dto, MultipartFile[] files) throws Exception {
        String passwordHash = null;
        if (dto.secret()) {
            if (!StringUtils.hasText(dto.password()) || dto.password().trim().length() < 4 || dto.password().length() > 64)
                throw new IllegalArgumentException("비밀글 비밀번호는 4~64자여야 합니다.");
            passwordHash = hasher.hash(dto.password().trim());
        }

        Inquiry i = Inquiry.builder()
                .name(dto.name())
                .email(dto.email())
                .userid(dto.userid())
                .title(dto.title())
                .content(dto.content())
                .status(InquiryStatus.OPEN)
                .secret(dto.secret())
                .passwordHash(passwordHash)
                .build();

        if (files != null) {
            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) continue;
                InquiryAttachment a = InquiryAttachment.builder()
                        .inquiry(i)
                        .filename(f.getOriginalFilename())
                        .contentType(f.getContentType() == null ? "application/octet-stream" : f.getContentType())
                        .bytes(f.getBytes())
                        .size(f.getSize())
                        .build();
                i.getAttachments().add(a);
            }
        }
        return repo.save(i);
    }

    @Transactional(readOnly = true)
    public boolean canViewPublic(Inquiry i, String rawPassword, boolean isOwner /*, boolean isAdmin */) {
        if (!i.isSecret()) return true;
        if (isOwner) return true;
        if (!i.hasPassword()) return false;
        if (!StringUtils.hasText(rawPassword)) return false;
        return hasher.matches(rawPassword.trim(), i.getPasswordHash());
    }

    @Transactional(readOnly = true)
    public InquiryAttachment getAttachment(Long inquiryId, Long attId) {
        return attRepo.findByIdAndInquiry_Id(attId, inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("첨부가 없습니다."));
    }

    @Transactional(readOnly = true)
    public InquiryAttachment getAttachmentMine(Long inquiryId, Long attId, String email) {
        Inquiry i = getMine(inquiryId, email);
        return attRepo.findByIdAndInquiry_Id(attId, i.getId())
                .orElseThrow(() -> new IllegalArgumentException("첨부가 없거나 권한이 없습니다."));
    }
}
