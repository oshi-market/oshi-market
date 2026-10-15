package com.oshimarket.domain.wishlist.service;

import com.oshimarket.domain.item.dto.ItemResponse;
import com.oshimarket.domain.item.entity.Item;
import com.oshimarket.domain.item.repository.ItemRepository;
import com.oshimarket.domain.item.service.ItemService;
import com.oshimarket.domain.wishlist.dto.WishlistStatusResponse;
import com.oshimarket.domain.wishlist.entity.Wishlist;
import com.oshimarket.domain.wishlist.repository.WishlistRepository;
import com.oshimarket.global.exception.BusinessException;
import com.oshimarket.global.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ItemRepository itemRepository;
    private final ItemService itemService;

    public WishlistService(WishlistRepository wishlistRepository, ItemRepository itemRepository, ItemService itemService) {
        this.wishlistRepository = wishlistRepository;
        this.itemRepository = itemRepository;
        this.itemService = itemService;
    }

    /** 이미 찜한 상품이면 아무것도 바꾸지 않고 현재 상태를 돌려준다. */
    @Transactional
    public WishlistStatusResponse add(Long memberId, Long itemId) {
        Item item = findItemOrThrow(itemId);
        if (item.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.WISHLIST_SELF_ITEM);
        }
        wishlistRepository.insertIfAbsent(memberId, itemId);
        return new WishlistStatusResponse(true, wishlistRepository.countByItemId(itemId));
    }

    /** 찜하지 않은 상품이어도 에러 없이 현재 상태를 돌려준다. */
    @Transactional
    public WishlistStatusResponse remove(Long memberId, Long itemId) {
        findItemOrThrow(itemId);
        wishlistRepository.deleteByUserIdAndItemId(memberId, itemId);
        return new WishlistStatusResponse(false, wishlistRepository.countByItemId(itemId));
    }

    /** memberId가 null이면(비로그인) 찜 수만 의미가 있다. */
    public WishlistStatusResponse getStatus(Long memberId, Long itemId) {
        findItemOrThrow(itemId);
        boolean wishlisted = memberId != null && wishlistRepository.existsByUserIdAndItemId(memberId, itemId);
        return new WishlistStatusResponse(wishlisted, wishlistRepository.countByItemId(itemId));
    }

    /** 최근에 찜한 순. 상품 정보(사진·닉네임)는 상품 목록과 같은 방식으로 한 번에 모아서 만든다. */
    public Page<ItemResponse> getMyWishlist(Long memberId, Pageable pageable) {
        Page<Wishlist> wishlists = wishlistRepository.findByUserIdOrderByCreatedAtDescIdDesc(memberId, pageable);

        List<Long> itemIds = wishlists.stream().map(Wishlist::getItemId).toList();
        Map<Long, Item> itemById = itemRepository.findAllById(itemIds).stream()
                .collect(Collectors.toMap(Item::getId, Function.identity()));
        // findAllById는 순서를 보장하지 않아서 찜한 순서대로 다시 맞춘다.
        // 조회 사이에 상품이 지워졌으면(찜도 CASCADE로 지워짐) 그 칸은 건너뛴다.
        List<Item> items = itemIds.stream().map(itemById::get).filter(Objects::nonNull).toList();

        return new PageImpl<>(itemService.toResponses(items), pageable, wishlists.getTotalElements());
    }

    private Item findItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
    }
}
