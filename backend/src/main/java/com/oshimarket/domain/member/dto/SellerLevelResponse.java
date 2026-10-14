package com.oshimarket.domain.member.dto;

import com.oshimarket.domain.member.entity.Member;
import com.oshimarket.domain.member.entity.SellerLevel;

/**
 * 판매자 레벨·별점 요약. 프로필의 경험치 바를 그릴 수 있게 현재 레벨 시작 경험치와 다음 레벨 경험치를 함께 준다.
 *
 * @param nextLevelExp 다음 레벨에 필요한 경험치. 최고 레벨이면 null
 * @param averageRating 평균 별점(소수 첫째 자리). 받은 별점이 없으면 null
 */
public record SellerLevelResponse(
        int level,
        int exp,
        int currentLevelExp,
        Integer nextLevelExp,
        int reviewCount,
        Double averageRating
) {

    public static SellerLevelResponse from(Member member) {
        SellerLevel level = SellerLevel.of(member.getExp());
        SellerLevel next = level.next();
        Double average = member.getReviewCount() == 0
                ? null
                : Math.round(member.getRatingSum() * 10.0 / member.getReviewCount()) / 10.0;

        return new SellerLevelResponse(
                level.getLevel(),
                member.getExp(),
                level.getMinExp(),
                next == null ? null : next.getMinExp(),
                member.getReviewCount(),
                average
        );
    }
}
