package com.oshimarket.domain.item.entity;

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

/** 상품 사진. 파일 자체는 이미지 저장소(Cloudinary)에 있고 여기엔 URL/삭제용 publicId만 보관. */
@Entity
@Table(name = "item_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemImage {

    /** 상품당 최대 사진 수. */
    public static final int MAX_PER_ITEM = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "public_id", nullable = false)
    private String publicId;

    /** 작을수록 앞. 가장 앞 사진이 목록 썸네일. */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private ItemImage(Long itemId, String url, String publicId, int sortOrder) {
        this.itemId = itemId;
        this.url = url;
        this.publicId = publicId;
        this.sortOrder = sortOrder;
    }

    public static ItemImage of(Long itemId, String url, String publicId, int sortOrder) {
        return new ItemImage(itemId, url, publicId, sortOrder);
    }
}
