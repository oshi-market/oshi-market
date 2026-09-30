package com.oshimarket.domain.item.dto;

import com.oshimarket.domain.item.entity.ItemImage;

public record ItemImageResponse(
        Long id,
        String url
) {

    public static ItemImageResponse from(ItemImage image) {
        return new ItemImageResponse(image.getId(), image.getUrl());
    }
}
