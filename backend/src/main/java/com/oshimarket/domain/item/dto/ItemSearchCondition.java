package com.oshimarket.domain.item.dto;

import com.oshimarket.domain.item.entity.ItemStatus;

/** GET /api/items 쿼리 파라미터 바인딩용 (3_아키텍처및서비스흐름.md 필터: category, work, character, status). */
public record ItemSearchCondition(
        String category,
        String workTag,
        String characterTag,
        ItemStatus status,
        Long sellerId,
        String keyword
) {
}
