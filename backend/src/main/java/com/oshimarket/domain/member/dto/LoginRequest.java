package com.oshimarket.domain.member.dto;

import com.oshimarket.domain.member.entity.Member;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password
) {

    /** 요청을 받는 순간(검증보다 먼저) 이메일을 정규화한다 — 앞뒤 공백이 있어도 형식 오류가 나지 않게. */
    public LoginRequest {
        email = Member.normalizeEmail(email);
    }
}
