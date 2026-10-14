package com.oshimarket.domain.member.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SellerLevelTest {

    @ParameterizedTest
    @CsvSource({"0, 1", "19, 1", "20, 2", "49, 2", "50, 3", "999, 9", "1000, 10", "99999, 10"})
    void 경험치로_레벨을_계산한다(int exp, int expectedLevel) {
        assertThat(SellerLevel.of(exp).getLevel()).isEqualTo(expectedLevel);
    }

    @Test
    void 최고_레벨은_다음_레벨이_없다() {
        assertThat(SellerLevel.LV9.next()).isEqualTo(SellerLevel.LV10);
        assertThat(SellerLevel.LV10.next()).isNull();
    }

    @Test
    void 레벨_기준_경험치는_오름차순이다() {
        SellerLevel[] levels = SellerLevel.values();
        for (int i = 1; i < levels.length; i++) {
            assertThat(levels[i].getMinExp()).isGreaterThan(levels[i - 1].getMinExp());
            assertThat(levels[i].getLevel()).isEqualTo(i + 1);
        }
    }
}
