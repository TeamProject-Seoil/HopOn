package com.example.demo.repository;

import com.example.demo.entity.InquiryAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InquiryAttachmentRepository extends JpaRepository<InquiryAttachment, Long> {
    Optional<InquiryAttachment> findByIdAndInquiry_Id(Long id, Long inquiryId);
}
