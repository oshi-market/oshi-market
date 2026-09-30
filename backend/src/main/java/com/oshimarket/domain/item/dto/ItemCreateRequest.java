package com.oshimarket.domain.item.dto;

import com.oshimarket.domain.item.entity.ItemCategory;
import com.oshimarket.domain.item.entity.ItemCondition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ItemCreateRequest(

        @NotBlank(message = "상품명을 입력해주세요.")
        @Size(max = 100, message = "상품명은 100자 이하로 입력해주세요.")
        String title,

        @Size(max = 2000, message = "상품 설명은 2000자 이하로 입력해주세요.")
        String description,

        @NotNull(message = "카테고리를 선택해주세요.")
        ItemCategory category,

        /** 선택 입력. 값이 있으면 work 테이블에 등록된 작품명이어야 함 (ItemService에서 검증). */
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
