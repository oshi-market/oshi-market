package com.oshimarket.domain.item.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ItemSpecificationsTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "나루토 피규어|나루토 피규어",
            "100%|100\\%",
            "a_b|a\\_b",
            "c:\\temp|c:\\\\temp"
    })
    void 검색어의_와일드카드_문자는_글자_그대로_찾도록_이스케이프한다(String keyword, String expected) {
        assertThat(ItemSpecifications.escapeLikePattern(keyword)).isEqualTo(expected);
    }
}
