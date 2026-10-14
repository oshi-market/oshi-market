package com.oshimarket.domain.review.controller;

import com.oshimarket.domain.review.dto.ReviewCreateRequest;
import com.oshimarket.domain.review.dto.ReviewResponse;
import com.oshimarket.domain.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions/{transactionId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** 거래 완료 후 구매자가 판매자에게 별점. 한 번 등록하면 수정·삭제 불가. */
    @PostMapping
    public ResponseEntity<ReviewResponse> create(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long transactionId,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(transactionId, memberId, request));
    }
}
