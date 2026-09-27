package com.oshimarket.domain.member.dto;

import com.oshimarket.domain.member.entity.Member;
import java.time.LocalDateTime;

public record MemberResponse(
        Long id,
        String email,
        String nickname,
        int trustScore,
        LocalDateTime createdAt
) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getTrustScore(),
                member.getCreatedAt()
        );
    }
}
