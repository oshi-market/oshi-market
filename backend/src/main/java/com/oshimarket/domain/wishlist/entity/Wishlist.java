package com.oshimarket.domain.wishlist.entity;

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

/**
 * 찜(관심 상품). 회원·상품당 1개(uk_wishlist_user_item). 상품이 지워지면 같이 지워진다(V8 ON DELETE CASCADE).
 * Item처럼 다른 도메인과의 결합을 피하려고 연관관계 대신 id만 들고 있는다.
 * 저장은 WishlistRepository.insertIfAbsent(ON CONFLICT)로만 하므로 생성 메서드를 두지 않는다.
 */
@Entity
@Table(name = "wishlist")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "item_id", nullable = false, updatable = false)
    private Long itemId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
