package com.oshimarket.domain.item.dto;

import com.oshimarket.domain.item.entity.Item;
import com.oshimarket.domain.item.entity.ItemCategory;
import com.oshimarket.domain.item.entity.ItemCondition;
import com.oshimarket.domain.item.entity.ItemStatus;
import java.time.LocalDateTime;

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
        LocalDateTime createdAt
) {

    public static ItemResponse from(Item item) {
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
                item.getCreatedAt()
        );
    }
}
