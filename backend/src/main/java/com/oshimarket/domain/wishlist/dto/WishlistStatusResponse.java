package com.oshimarket.domain.wishlist.dto;

/** 상품 상세의 ♡ 버튼용. 로그인하지 않았으면 wishlisted는 항상 false. */
public record WishlistStatusResponse(boolean wishlisted, long count) {
}
