package com.oshimarket.domain.wishlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oshimarket.domain.item.dto.ItemResponse;
import com.oshimarket.domain.item.entity.Item;
import com.oshimarket.domain.item.entity.ItemCategory;
import com.oshimarket.domain.item.entity.ItemCondition;
import com.oshimarket.domain.item.repository.ItemRepository;
import com.oshimarket.domain.item.service.ItemService;
import com.oshimarket.domain.wishlist.dto.WishlistStatusResponse;
import com.oshimarket.domain.wishlist.entity.Wishlist;
import com.oshimarket.domain.wishlist.repository.WishlistRepository;
import com.oshimarket.global.exception.BusinessException;
import com.oshimarket.global.exception.ErrorCode;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class WishlistServiceTest {

    private static final Long ITEM_ID = 1L;
    private static final Long SELLER_ID = 10L;
    private static final Long BUYER_ID = 20L;

    private WishlistRepository wishlistRepository;
    private ItemRepository itemRepository;
    private ItemService itemService;
    private WishlistService wishlistService;

    @BeforeEach
    void setUp() {
        wishlistRepository = mock(WishlistRepository.class);
        itemRepository = mock(ItemRepository.class);
        itemService = mock(ItemService.class);
        wishlistService = new WishlistService(wishlistRepository, itemRepository, itemService);
    }

    @Test
    void 찜하면_저장하고_찜_수를_돌려준다() {
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item(ITEM_ID)));
        when(wishlistRepository.countByItemId(ITEM_ID)).thenReturn(3L);

        WishlistStatusResponse response = wishlistService.add(BUYER_ID, ITEM_ID);

        verify(wishlistRepository).insertIfAbsent(BUYER_ID, ITEM_ID);
        assertThat(response.wishlisted()).isTrue();
        assertThat(response.count()).isEqualTo(3L);
    }

    @Test
    void 이미_찜한_상품을_다시_찜해도_에러가_아니다() {
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item(ITEM_ID)));
        // ON CONFLICT DO NOTHING이라 이미 있으면 0행 저장
        when(wishlistRepository.insertIfAbsent(BUYER_ID, ITEM_ID)).thenReturn(0);
        when(wishlistRepository.countByItemId(ITEM_ID)).thenReturn(1L);

        WishlistStatusResponse response = wishlistService.add(BUYER_ID, ITEM_ID);

        assertThat(response.wishlisted()).isTrue();
        assertThat(response.count()).isEqualTo(1L);
    }

    @Test
    void 본인_상품은_찜할_수_없다() {
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item(ITEM_ID)));

        assertErrorCode(() -> wishlistService.add(SELLER_ID, ITEM_ID), ErrorCode.WISHLIST_SELF_ITEM);
        verify(wishlistRepository, never()).insertIfAbsent(anyLong(), anyLong());
    }

    @Test
    void 없는_상품은_찜할_수_없다() {
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.empty());

        assertErrorCode(() -> wishlistService.add(BUYER_ID, ITEM_ID), ErrorCode.ITEM_NOT_FOUND);
    }

    @Test
    void 찜을_해제하면_삭제하고_찜_수를_돌려준다() {
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item(ITEM_ID)));
        when(wishlistRepository.countByItemId(ITEM_ID)).thenReturn(0L);

        WishlistStatusResponse response = wishlistService.remove(BUYER_ID, ITEM_ID);

        verify(wishlistRepository).deleteByUserIdAndItemId(BUYER_ID, ITEM_ID);
        assertThat(response.wishlisted()).isFalse();
        assertThat(response.count()).isZero();
    }

    @Test
    void 비로그인이면_찜_여부는_false이고_찜_수만_보인다() {
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item(ITEM_ID)));
        when(wishlistRepository.countByItemId(ITEM_ID)).thenReturn(5L);

        WishlistStatusResponse response = wishlistService.getStatus(null, ITEM_ID);

        assertThat(response.wishlisted()).isFalse();
        assertThat(response.count()).isEqualTo(5L);
        verify(wishlistRepository, never()).existsByUserIdAndItemId(any(), any());
    }

    @Test
    void 로그인했으면_내가_찜했는지_알려준다() {
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item(ITEM_ID)));
        when(wishlistRepository.existsByUserIdAndItemId(BUYER_ID, ITEM_ID)).thenReturn(true);
        when(wishlistRepository.countByItemId(ITEM_ID)).thenReturn(1L);

        assertThat(wishlistService.getStatus(BUYER_ID, ITEM_ID).wishlisted()).isTrue();
    }

    @Test
    void 내_찜_목록은_찜한_순서를_유지한다() {
        Pageable pageable = PageRequest.of(0, 20);
        // 최근에 찜한 순: 상품 2 → 상품 1
        Page<Wishlist> wishlists = new PageImpl<>(List.of(wishlist(2L), wishlist(1L)), pageable, 2);
        when(wishlistRepository.findByUserIdOrderByCreatedAtDescIdDesc(BUYER_ID, pageable)).thenReturn(wishlists);
        // DB는 id 순으로 돌려줄 수 있다
        Item item1 = item(1L);
        Item item2 = item(2L);
        when(itemRepository.findAllById(List.of(2L, 1L))).thenReturn(List.of(item1, item2));
        when(itemService.toResponses(List.of(item2, item1))).thenReturn(List.of(mock(ItemResponse.class), mock(ItemResponse.class)));

        Page<ItemResponse> result = wishlistService.getMyWishlist(BUYER_ID, pageable);

        verify(itemService).toResponses(List.of(item2, item1));
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    private static void assertErrorCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }

    private static Item item(Long id) {
        Item item = Item.register(SELLER_ID, "나루토 피규어", null, ItemCategory.FIGURE, null, null, 50_000, ItemCondition.NEW);
        setField(Item.class, item, "id", id);
        return item;
    }

    private static Wishlist wishlist(Long itemId) {
        Wishlist wishlist = newInstance();
        setField(Wishlist.class, wishlist, "userId", BUYER_ID);
        setField(Wishlist.class, wishlist, "itemId", itemId);
        return wishlist;
    }

    private static Wishlist newInstance() {
        try {
            var constructor = Wishlist.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static <T> void setField(Class<T> type, T target, String name, Object value) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
