// src/main/java/com/example/demo/dto/UpdateProfileRequest.java
package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 부분 수정용: null이면 해당 필드 미변경 */
@Getter @Setter
public class UpdateProfileRequest {
    @Size(min = 1, max = 100, message = "이름은 1~100자")
    private String username;

    @Email(message = "이메일 형식이 올바르지 않습니다")
    private String email;

    // ✅ 이메일 변경용 인증 ID(메일 인증 완료 후 받은 값). 이메일을 바꿀 때만 필수.
    private String emailVerificationId;

    @Pattern(regexp = "^[0-9\\-]{9,20}$", message = "전화번호 형식이 올바르지 않습니다")
    private String tel;

    // 사진 제거 요청 (true면 기존 이미지 삭제)
    private Boolean removeProfileImage;

    // ✅ 드라이버만 수정 가능
    @Size(max = 100, message = "회사명은 최대 100자")
    private String company;
}
