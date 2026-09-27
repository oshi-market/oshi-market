package com.oshimarket.domain.item.controller;

import com.oshimarket.domain.item.dto.ItemCreateRequest;
import com.oshimarket.domain.item.dto.ItemResponse;
import com.oshimarket.domain.item.dto.ItemSearchCondition;
import com.oshimarket.domain.item.dto.ItemUpdateRequest;
import com.oshimarket.domain.item.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 목록/상세 조회(GET)는 비로그인 사용자도 볼 수 있게 SecurityConfig에서 permitAll 처리했고,
 * 등록/수정/삭제는 인증 필요. 상세 근거는 1_MVP기획서.md 서비스 흐름(둘러보기는 누구나,
 * 실제 거래 액션부터 로그인 필요) 참고.
 */
@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    public ResponseEntity<ItemResponse> register(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody ItemCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.register(memberId, request));
    }

    /** 필터: category, workTag, characterTag, status / 내 상품 목록 조회는 sellerId=내 memberId로 전달. */
    @GetMapping
    public Page<ItemResponse> search(
            @ModelAttribute ItemSearchCondition condition,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return itemService.search(condition, pageable);
    }

    @GetMapping("/{itemId}")
    public ItemResponse getItem(@PathVariable Long itemId) {
        return itemService.getItem(itemId);
    }

    @PatchMapping("/{itemId}")
    public ItemResponse update(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long itemId,
            @Valid @RequestBody ItemUpdateRequest request
    ) {
        return itemService.update(itemId, memberId, request);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long itemId
    ) {
        itemService.delete(itemId, memberId);
        return ResponseEntity.noContent().build();
    }
}
