package com.oshimarket.domain.member.dto;

import com.oshimarket.domain.member.entity.Member;
import java.time.LocalDateTime;

/**
 * 다른 사람에게 보여주는 공개 프로필 (GET /api/members/{id}). 로그인 없이 누구나 볼 수 있어서
 * 이메일 등 개인정보는 절대 넣지 않는다. 신뢰도는 레벨·평균 별점(sellerLevel)으로 보여준다.
 */
public record MemberPublicResponse(
        Long id,
        String nickname,
        LocalDateTime createdAt,
        SellerLevelResponse sellerLevel
) {

    public static MemberPublicResponse from(Member member) {
        return new MemberPublicResponse(
                member.getId(),
                member.getNickname(),
                member.getCreatedAt(),
                SellerLevelResponse.from(member)
        );
    }
}
