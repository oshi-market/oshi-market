package com.oshimarket.domain.item.repository;

import com.oshimarket.domain.item.dto.ItemSearchCondition;
import com.oshimarket.domain.item.entity.Item;
import com.oshimarket.domain.item.entity.ItemStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * GET /api/items 필터 조건을 동적 쿼리로 조립.
 * 검색(키워드)은 현재 title 부분일치(ILIKE)로 처리 — V1 마이그레이션에 tsvector 컬럼이 없어
 * 우선 이 방식으로 구현하고, 검색 트래픽이 늘면 새 마이그레이션으로 tsvector 전문검색 전환
 * (4_DB분석서.md "Elasticsearch 도입 전까지" 참고).
 *
 * Spring Data JPA 4.x부터 Specification#and(null)이 예외를 던지도록 바뀌어서(이전엔 no-op),
 * 필터가 없을 때는 null 대신 Specification.unrestricted()(항상 참)를 반환해 Specification.allOf로
 * 안전하게 조립한다.
 */
public final class ItemSpecifications {

    private ItemSpecifications() {
    }

    public static Specification<Item> from(ItemSearchCondition condition) {
        return Specification.allOf(
                hasCategory(condition.category()),
                hasWorkTag(condition.work()),
                hasCharacterTag(condition.character()),
                hasStatus(condition.status()),
                hasSellerId(condition.sellerId()),
                titleContains(condition.keyword())
        );
    }

    private static Specification<Item> hasCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    private static Specification<Item> hasWorkTag(String workTag) {
        if (!StringUtils.hasText(workTag)) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("workTag"), workTag);
    }

    private static Specification<Item> hasCharacterTag(String characterTag) {
        if (!StringUtils.hasText(characterTag)) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("characterTag"), characterTag);
    }

    private static Specification<Item> hasStatus(ItemStatus status) {
        if (status == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private static Specification<Item> hasSellerId(Long sellerId) {
        if (sellerId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("sellerId"), sellerId);
    }

    private static Specification<Item> titleContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Specification.unrestricted();
        }
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern);
    }
}
