package com.oshimarket.domain.review.dto;

import com.oshimarket.domain.review.entity.Review;
import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long transactionId,
        int rating,
        LocalDateTime createdAt
) {

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(review.getId(), review.getTransactionId(), review.getRating(), review.getCreatedAt());
    }
}
