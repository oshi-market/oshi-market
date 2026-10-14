package com.oshimarket.domain.member.dto;

import com.oshimarket.domain.member.entity.Member;
import java.time.LocalDateTime;

/**
 * 다른 사람에게 보여주는 공개 프로필 (GET /api/members/{id}). 로그인 없이 누구나 볼 수 있어서
 * 이메일 등 개인정보는 절대 넣지 않는다. 신뢰도는 리뷰 기능과 함께 계산 방식이 정해지면 추가.
 */
public record MemberPublicResponse(
        Long id,
        String nickname,
        LocalDateTime createdAt
) {

    public static MemberPublicResponse from(Member member) {
        return new MemberPublicResponse(member.getId(), member.getNickname(), member.getCreatedAt());
    }
}
