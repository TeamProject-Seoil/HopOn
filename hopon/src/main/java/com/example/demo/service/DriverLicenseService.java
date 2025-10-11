// src/main/java/com/example/demo/service/DriverLicenseService.java
package com.example.demo.service;

import com.example.demo.entity.DriverLicenseEntity;
import com.example.demo.entity.UserEntity;
import com.example.demo.repository.DriverLicenseRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DriverLicenseService {

    private final DriverLicenseRepository driverLicenseRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public View getByUser(Long userNum) {
        DriverLicenseEntity e = driverLicenseRepository.findByUser_UserNum(userNum)
                .orElseThrow(() -> new IllegalArgumentException("면허 정보가 없습니다."));
        return View.of(e);
    }

    @Transactional(readOnly = true)
    public byte[] getImageByUser(Long userNum) {
        DriverLicenseEntity e = driverLicenseRepository.findByUser_UserNum(userNum)
                .orElseThrow(() -> new IllegalArgumentException("면허 정보가 없습니다."));
        if (e.getLicenseImage() == null || e.getLicenseImage().length == 0) {
            throw new IllegalStateException("등록된 면허 사진이 없습니다.");
        }
        return e.getLicenseImage();
    }

    @Transactional
    public View upsert(Long userNum,
                       String licenseNumber,
                       LocalDate acquiredDate,
                       MultipartFile photo) {

        UserEntity user = userRepository.findById(userNum)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String normalizedNo = normalizeLicenseNo(licenseNumber);
        validateLicenseNumber(normalizedNo);
        validateAcquiredDate(acquiredDate);

        byte[] photoBytes = readImageOrNull(photo); // null 가능

        // 다른 사용자가 동일 면허번호 사용 중인지 검사
        if (driverLicenseRepository.existsByLicenseNumberAndUser_UserNumNot(normalizedNo, userNum)) {
            throw new IllegalStateException("이미 다른 계정에 등록된 자격증 번호입니다.");
        }

        DriverLicenseEntity entity = driverLicenseRepository.findByUser_UserNum(userNum)
                .orElseGet(() -> DriverLicenseEntity.builder()
                        .user(user)
                        .licenseNumber(normalizedNo)
                        .acquiredDate(acquiredDate)
                        .licenseImage(photoBytes)
                        .build());

        // 업데이트
        entity.setLicenseNumber(normalizedNo);
        entity.setAcquiredDate(acquiredDate);
        if (photoBytes != null) {
            entity.setLicenseImage(photoBytes);
        }

        DriverLicenseEntity saved = driverLicenseRepository.save(entity);
        return View.of(saved);
    }

    @Transactional
    public void deleteByUser(Long userNum) {
        driverLicenseRepository.deleteByUser_UserNum(userNum);
    }

    // ===== 내부 유틸 =====
    private String normalizeLicenseNo(String s) {
        if (s == null) return null;
        // 공백/하이픈 제거 + trim
        return s.replaceAll("\\s|-", "").trim();
    }

    private void validateLicenseNumber(String no) {
        if (no == null || no.isBlank()) {
            throw new IllegalArgumentException("자격증 번호를 입력하세요.");
        }
        if (no.length() > 50) {
            throw new IllegalArgumentException("자격증 번호가 너무 깁니다.");
        }
    }

    private void validateAcquiredDate(LocalDate acquired) {
        if (acquired == null) {
            throw new IllegalArgumentException("자격취득일을 입력하세요.");
        }
        if (acquired.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("자격취득일은 미래일 수 없습니다.");
        }
    }

    private byte[] readImageOrNull(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) return null;
            String contentType = file.getContentType();
            if (contentType != null && !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
            }
            long max = 5L * 1024 * 1024; // 5MB
            if (file.getSize() > max) {
                throw new IllegalArgumentException("이미지 파일은 최대 5MB까지 업로드 가능합니다.");
            }
            return file.getBytes();
        } catch (Exception e) {
            throw new IllegalStateException("이미지 파일을 처리하는 중 오류가 발생했습니다.", e);
        }
    }

    // 응답용 뷰(요약 DTO)
    public record View(
            Long id,
            String licenseNumber,
            LocalDate acquiredDate,
            boolean hasPhoto
    ) {
        public static View of(DriverLicenseEntity e) {
            return new View(
                    e.getLicenseId(),
                    e.getLicenseNumber(),
                    e.getAcquiredDate(),
                    e.getLicenseImage() != null && e.getLicenseImage().length > 0
            );
        }
    }
}
