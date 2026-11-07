package com.oshimarket.domain.item.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 상품 엔티티. seller_id는 member 도메인과의 결합을 피하기 위해 연관관계(@ManyToOne) 대신
 * ID 참조로만 보관한다 (3_아키텍처및서비스흐름.md: 풀 DDD 대신 도메인별 패키지 + 느슨한 결합 채택).
 */
@Entity
@Table(name = "item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ItemCategory category;

    @Column(name = "work_tag", length = 50)
    private String workTag;

    @Column(name = "character_tag", length = 50)
    private String characterTag;

    @Column(nullable = false)
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemCondition condition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Item(
            Long sellerId,
            String title,
            String description,
            ItemCategory category,
            String workTag,
            String characterTag,
            int price,
            ItemCondition condition
    ) {
        this.sellerId = sellerId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.workTag = workTag;
        this.characterTag = characterTag;
        this.price = price;
        this.condition = condition;
        this.status = ItemStatus.SELLING;
    }

    public static Item register(
            Long sellerId,
            String title,
            String description,
            ItemCategory category,
            String workTag,
            String characterTag,
            int price,
            ItemCondition condition
    ) {
        return new Item(sellerId, title, description, category, workTag, characterTag, price, condition);
    }

    public void update(
            String title,
            String description,
            ItemCategory category,
            String workTag,
            String characterTag,
            int price,
            ItemCondition condition
    ) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.workTag = workTag;
        this.characterTag = characterTag;
        this.price = price;
        this.condition = condition;
    }

    /**
     * 판매 중일 때만 상품 정보·사진을 바꿀 수 있다. 거래 중이거나 판매 완료된 뒤에 바뀌면 구매자가 보고 거래한 내용과
     * 달라지고, 내 거래 내역도 상품의 현재 정보를 보여주므로 같이 바뀐다.
     */
    public boolean isEditable() {
        return this.status == ItemStatus.SELLING;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.sellerId.equals(memberId);
    }

    // 거래 진행에 맞춰 상품 상태를 바꾸는 메서드 (TransactionService에서 사용)
    public boolean isSelling() {
        return this.status == ItemStatus.SELLING;
    }

    public boolean isSoldOut() {
        return this.status == ItemStatus.SOLD_OUT;
    }

    /** 거래 요청이 들어와 다른 사람이 거래를 걸 수 없게 잠근다. */
    public void startTransaction() {
        this.status = ItemStatus.IN_TRANSACTION;
    }

    /** 거래가 취소되면 다시 판매 중으로 되돌린다. */
    public void reopen() {
        this.status = ItemStatus.SELLING;
    }

    /** 거래가 완료되면 판매 완료. */
    public void markSoldOut() {
        this.status = ItemStatus.SOLD_OUT;
    }
}
