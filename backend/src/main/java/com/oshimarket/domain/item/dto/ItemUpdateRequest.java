package com.oshimarket.domain.item.dto;

import com.oshimarket.domain.item.entity.ItemCondition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * MVP 범위에서는 부분 수정(PATCH의 일부 필드만 변경) 대신 필드 전체를 다시 받는 방식으로 단순화.
 * 실제 부분 수정이 필요해지면 각 필드를 Optional/wrapper 타입으로 바꿔 null 여부로 변경 대상만
 * 골라 적용하도록 확장.
 */
public record ItemUpdateRequest(

        @NotBlank(message = "상품명을 입력해주세요.")
        @Size(max = 100)
        String title,

        @Size(max = 2000)
        String description,

        @NotBlank(message = "카테고리를 입력해주세요.")
        @Size(max = 50)
        String category,

        @Size(max = 50)
        String workTag,

        @Size(max = 50)
        String characterTag,

        @NotNull(message = "가격을 입력해주세요.")
        @Positive(message = "가격은 0보다 커야 합니다.")
        Integer price,

        @NotNull(message = "상품 상태(새 상품/중고)를 선택해주세요.")
        ItemCondition condition
) {
}
