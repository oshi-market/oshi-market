package com.oshimarket.domain.item.dto;

import com.oshimarket.domain.item.entity.Item;
import com.oshimarket.domain.item.entity.ItemCategory;
import com.oshimarket.domain.item.entity.ItemCondition;
import com.oshimarket.domain.item.entity.ItemImage;
import com.oshimarket.domain.item.entity.ItemStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ItemResponse(
        Long id,
        Long sellerId,
        String title,
        String description,
        ItemCategory category,
        String workTag,
        String characterTag,
        int price,
        ItemCondition condition,
        ItemStatus status,
        LocalDateTime createdAt,
        /** 목록 카드용 대표 사진(첫 번째). 사진이 없으면 null. */
        String thumbnailUrl,
        List<ItemImageResponse> images
) {

    /** images는 sortOrder 순으로 정렬된 이 상품의 사진. */
    public static ItemResponse from(Item item, List<ItemImage> images) {
        List<ItemImageResponse> imageResponses = images.stream().map(ItemImageResponse::from).toList();
        return new ItemResponse(
                item.getId(),
                item.getSellerId(),
                item.getTitle(),
                item.getDescription(),
                item.getCategory(),
                item.getWorkTag(),
                item.getCharacterTag(),
                item.getPrice(),
                item.getCondition(),
                item.getStatus(),
                item.getCreatedAt(),
                imageResponses.isEmpty() ? null : imageResponses.get(0).url(),
                imageResponses
        );
    }
}
