package com.oshimarket.domain.wishlist.controller;

import com.oshimarket.domain.item.dto.ItemResponse;
import com.oshimarket.domain.wishlist.dto.WishlistStatusResponse;
import com.oshimarket.domain.wishlist.service.WishlistService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 찜하기·해제는 인증 필요. 상품별 찜 상태 조회(GET /api/items/**)는 상품 조회처럼 비로그인도 볼 수 있고,
 * 그때 memberId는 null이다(SecurityConfig).
 */
@RestController
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @PostMapping("/api/items/{itemId}/wishlist")
    public WishlistStatusResponse add(@AuthenticationPrincipal Long memberId, @PathVariable Long itemId) {
        return wishlistService.add(memberId, itemId);
    }

    @DeleteMapping("/api/items/{itemId}/wishlist")
    public WishlistStatusResponse remove(@AuthenticationPrincipal Long memberId, @PathVariable Long itemId) {
        return wishlistService.remove(memberId, itemId);
    }

    @GetMapping("/api/items/{itemId}/wishlist")
    public WishlistStatusResponse getStatus(@AuthenticationPrincipal Long memberId, @PathVariable Long itemId) {
        return wishlistService.getStatus(memberId, itemId);
    }

    @GetMapping("/api/members/me/wishlist")
    public Page<ItemResponse> getMyWishlist(
            @AuthenticationPrincipal Long memberId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return wishlistService.getMyWishlist(memberId, pageable);
    }
}
