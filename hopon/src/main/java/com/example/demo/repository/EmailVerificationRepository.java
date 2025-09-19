package com.example.demo.repository;

import com.example.demo.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findTopByEmailAndPurposeOrderByIdDesc(String email, String purpose);
    Optional<EmailVerification> findByIdAndEmailAndPurpose(Long id, String email, String purpose);
}
