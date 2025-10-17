// src/main/java/com/example/demo/controller/DriverLicenseController.java
package com.example.demo.controller;

import com.example.demo.repository.UserRepository;
import com.example.demo.service.DriverLicenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/me/driver-license")
public class DriverLicenseController {

    private final DriverLicenseService driverLicenseService;
    private final UserRepository userRepository;

    @GetMapping
    public DriverLicenseService.View getMine(Authentication authentication) {
        Long userNum = currentUserNum(authentication);
        return driverLicenseService.getByUser(userNum);
    }

    @GetMapping("/image")
    public ResponseEntity<byte[]> getMineImage(Authentication authentication) {
        Long userNum = currentUserNum(authentication);
        byte[] bytes = driverLicenseService.getImageByUser(userNum);

        MediaType mt = detectImageMediaType(bytes);
        String ext = mediaTypeToExt(mt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mt);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"driver_license" + ext + "\"");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DriverLicenseService.View upsertMine(
            Authentication authentication,
            @RequestPart("licenseNumber") String licenseNumber,
            @RequestPart("acquiredDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate acquiredDate,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @RequestPart(value = "birthDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate,     // ✅
            @RequestPart(value = "name", required = false) String holderName        // ✅
    ) {
        Long userNum = currentUserNum(authentication);
        return driverLicenseService.upsert(userNum, licenseNumber, acquiredDate, photo, birthDate, holderName);
    }

    @DeleteMapping
    public Map<String, Object> deleteMine(Authentication authentication) {
        Long userNum = currentUserNum(authentication);
        driverLicenseService.deleteByUser(userNum);
        Map<String, Object> resp = new HashMap<>();
        resp.put("ok", true);
        return resp;
    }

    // ===== 예외 변환 =====
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("ok", false, "message", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("ok", false, "message", e.getMessage()));
    }

    // ===== 내부 헬퍼 =====
    private Long currentUserNum(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        Object p = authentication.getPrincipal();
        String userid = (p instanceof String)
                ? (String) p
                : (p instanceof UserDetails) ? ((UserDetails) p).getUsername() : null;
        if (userid == null) throw new IllegalStateException("인증 사용자 식별에 실패했습니다.");

        return userRepository.findByUserid(userid)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."))
                .getUserNum();
    }

    private MediaType detectImageMediaType(byte[] b) {
        if (b == null || b.length < 4) return MediaType.APPLICATION_OCTET_STREAM;
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return MediaType.IMAGE_JPEG;
        if ((b[0] & 0xFF) == 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47) return MediaType.IMAGE_PNG;
        if (b[0] == 0x47 && b[1] == 0x49 && b[2] == 0x46 && b[3] == 0x38) return MediaType.IMAGE_GIF;
        return MediaType.APPLICATION_OCTET_STREAM;
    }
    private String mediaTypeToExt(MediaType mt) {
        if (MediaType.IMAGE_JPEG.equals(mt)) return ".jpg";
        if (MediaType.IMAGE_PNG.equals(mt))  return ".png";
        if (MediaType.IMAGE_GIF.equals(mt))  return ".gif";
        return "";
    }
}
