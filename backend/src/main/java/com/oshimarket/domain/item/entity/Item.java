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

    @Column(nullable = false, length = 50)
    private String category;

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
            String category,
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
            String category,
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
            String category,
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

    public boolean isOwnedBy(Long memberId) {
        return this.sellerId.equals(memberId);
    }
}
