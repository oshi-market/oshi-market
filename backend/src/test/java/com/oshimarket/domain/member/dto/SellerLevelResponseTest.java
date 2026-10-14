package com.oshimarket.domain.member.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.oshimarket.domain.member.entity.Member;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class SellerLevelResponseTest {

    @Test
    void 경험치_바에_필요한_값과_평균_별점을_계산한다() {
        // 별점 3개(5, 5, 4) → 합계 14, 경험치 26
        Member member = member(26, 3, 14);

        SellerLevelResponse response = SellerLevelResponse.from(member);

        assertThat(response.level()).isEqualTo(2);
        assertThat(response.currentLevelExp()).isEqualTo(20);
        assertThat(response.nextLevelExp()).isEqualTo(50);
        assertThat(response.reviewCount()).isEqualTo(3);
        assertThat(response.averageRating()).isEqualTo(4.7);
    }

    @Test
    void 받은_별점이_없으면_평균은_null() {
        SellerLevelResponse response = SellerLevelResponse.from(member(0, 0, 0));

        assertThat(response.level()).isEqualTo(1);
        assertThat(response.averageRating()).isNull();
    }

    @Test
    void 최고_레벨이면_다음_레벨_경험치는_null() {
        assertThat(SellerLevelResponse.from(member(1500, 150, 750)).nextLevelExp()).isNull();
    }

    private static Member member(int exp, int reviewCount, int ratingSum) {
        Member member = Member.of("seller@example.com", "encoded-password", "판매자");
        set(member, "exp", exp);
        set(member, "reviewCount", reviewCount);
        set(member, "ratingSum", ratingSum);
        return member;
    }

    private static void set(Member member, String fieldName, int value) {
        try {
            Field field = Member.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(member, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
