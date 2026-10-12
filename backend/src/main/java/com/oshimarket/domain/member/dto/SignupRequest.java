package com.oshimarket.domain.member.dto;

import com.oshimarket.domain.member.entity.Member;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(

        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하로 입력해주세요.")
        String password,

        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하로 입력해주세요.")
        String nickname
) {

    /** 요청을 받는 순간(검증보다 먼저) 이메일을 정규화한다 — 앞뒤 공백이 있어도 형식 오류가 나지 않게. */
    public SignupRequest {
        email = Member.normalizeEmail(email);
    }
}
