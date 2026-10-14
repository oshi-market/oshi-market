package com.oshimarket.domain.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReviewCreateRequest(
        @NotNull(message = "별점을 선택해주세요.")
        @Min(value = 1, message = "별점은 1~5점만 줄 수 있습니다.")
        @Max(value = 5, message = "별점은 1~5점만 줄 수 있습니다.")
        Integer rating
) {
}
