package com.oshimarket.domain.item.dto;

import com.oshimarket.domain.item.entity.ItemCategory;
import com.oshimarket.domain.item.entity.ItemStatus;

/**
 * GET /api/items 쿼리 파라미터 바인딩용.
 * 파라미터명은 3_아키텍처및서비스흐름.md API 명세("필터: category, work, character, status")를
 * 그대로 따른다 (내부 엔티티 필드명 workTag/characterTag와는 의도적으로 다름 — API 계약은 문서 기준).
 */
public record ItemSearchCondition(
        ItemCategory category,
        String work,
        String character,
        ItemStatus status,
        Long sellerId,
        String keyword
) {
}
