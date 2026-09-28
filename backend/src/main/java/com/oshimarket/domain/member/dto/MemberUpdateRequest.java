package com.oshimarket.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 프로필 수정 요청. MVP 범위는 닉네임만 (1_MVP기획서.md "최소 필드만 구현"). */
public record MemberUpdateRequest(

        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하로 입력해주세요.")
        String nickname
) {
}
