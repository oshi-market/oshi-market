package com.oshimarket.domain.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 거래 후 구매자가 판매자에게 주는 별점. 거래당 1개(uk_review_transaction), 별점 1~5(ck_review_rating, V4).
 * 한 번 매긴 별점은 수정·삭제할 수 없어서 변경 메서드를 두지 않는다 (경험치가 이미 반영됐기 때문).
 * 텍스트 후기(comment 컬럼)는 추후 도입 예정이라 지금은 저장하지 않는다.
 */
@Entity
@Table(name = "review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, updatable = false)
    private Long transactionId;

    @Column(name = "reviewer_id", nullable = false, updatable = false)
    private Long reviewerId;

    /** DB 컬럼이 SMALLINT라 ddl-auto: validate를 통과하도록 타입을 맞춘다. */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "rating", nullable = false, updatable = false)
    private int rating;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Review(Long transactionId, Long reviewerId, int rating) {
        this.transactionId = transactionId;
        this.reviewerId = reviewerId;
        this.rating = rating;
    }

    public static Review of(Long transactionId, Long reviewerId, int rating) {
        return new Review(transactionId, reviewerId, rating);
    }

    /** 이 별점으로 판매자가 얻는(잃는) 경험치. */
    public int expDelta() {
        return ReviewExp.of(rating);
    }
}
