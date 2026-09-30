package com.oshimarket.domain.curation.dto;

/**
 * GET /api/curations 쿼리 파라미터 바인딩용.
 * 파라미터명은 3_아키텍처및서비스흐름.md API 명세("필터: work, character")를 그대로 따른다
 * (내부 엔티티 필드명 workTag/characterTag와는 의도적으로 다름 — API 계약은 문서 기준).
 */
public record CurationSearchCondition(
        String work,
        String character
) {
}
