package com.oshimarket.domain.item.repository;

import com.oshimarket.domain.item.dto.ItemSearchCondition;
import com.oshimarket.domain.item.entity.Item;
import com.oshimarket.domain.item.entity.ItemCategory;
import com.oshimarket.domain.item.entity.ItemStatus;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
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

    private static final char LIKE_ESCAPE = '\\';

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

    private static Specification<Item> hasCategory(ItemCategory category) {
        if (category == null) {
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

    /**
     * 제목 부분일치를 대소문자 무시(ILIKE)로 검색한다. V4의 trigram 인덱스(idx_item_title_trgm)는 title ILIKE 형태에만
     * 쓰이고, lower(title) LIKE로 쓰면 인덱스를 못 타고 전체를 훑는다(EXPLAIN으로 확인). 표준 JPA에는 ilike가 없어서
     * Hibernate 확장(HibernateCriteriaBuilder)을 쓴다.
     * 사용자가 입력한 %, _ 는 "아무 글자" 와일드카드가 아니라 글자 그대로 찾도록 이스케이프한다.
     */
    private static Specification<Item> titleContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Specification.unrestricted();
        }
        String pattern = "%" + escapeLikePattern(keyword) + "%";
        return (root, query, cb) -> ((HibernateCriteriaBuilder) cb).ilike(root.get("title"), pattern, LIKE_ESCAPE);
    }

    static String escapeLikePattern(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
