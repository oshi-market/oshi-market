package com.oshimarket.domain.review.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ReviewExpTest {

    @ParameterizedTest
    @CsvSource({"1, -5", "2, -2", "3, 2", "4, 6", "5, 10"})
    void 별점별_경험치(int rating, int expected) {
        assertThat(ReviewExp.of(rating)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6})
    void 범위_밖_별점은_예외(int rating) {
        assertThatThrownBy(() -> ReviewExp.of(rating)).isInstanceOf(IllegalArgumentException.class);
    }
}
