package com.oshimarket.domain.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oshimarket.domain.item.dto.ItemCreateRequest;
import com.oshimarket.domain.item.dto.ItemResponse;
import com.oshimarket.domain.item.dto.ItemSearchCondition;
import com.oshimarket.domain.item.dto.ItemUpdateRequest;
import com.oshimarket.domain.item.entity.Item;
import com.oshimarket.domain.item.entity.ItemCondition;
import com.oshimarket.domain.item.repository.ItemRepository;
import com.oshimarket.global.exception.BusinessException;
import com.oshimarket.global.exception.ErrorCode;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class ItemServiceTest {

    private static final Long SELLER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;

    private ItemRepository itemRepository;
    private ItemService itemService;

    @BeforeEach
    void setUp() {
        itemRepository = mock(ItemRepository.class);
        itemService = new ItemService(itemRepository);
    }

    @Test
    void register_성공하면_판매중_상태로_저장한다() {
        ItemCreateRequest request = new ItemCreateRequest(
                "나루토 피규어", "미개봉", "피규어", "나루토", "우즈마키 나루토", 50_000, ItemCondition.NEW
        );
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 1L));

        ItemResponse response = itemService.register(SELLER_ID, request);

        assertThat(response.sellerId()).isEqualTo(SELLER_ID);
        assertThat(response.title()).isEqualTo(request.title());
        assertThat(response.status().name()).isEqualTo("SELLING");
    }

    @Test
    void getItem_존재하지_않으면_예외가_발생한다() {
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.getItem(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ITEM_NOT_FOUND);
    }

    @Test
    void getItem_존재하면_상세정보를_반환한다() {
        Item item = withId(sampleItem(), 1L);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        ItemResponse response = itemService.getItem(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo(item.getTitle());
    }

    @Test
    void update_소유자가_아니면_예외가_발생한다() {
        Item item = withId(sampleItem(), 1L);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        ItemUpdateRequest request = new ItemUpdateRequest(
                "수정된 제목", "설명", "피규어", "나루토", "우즈마키 나루토", 60_000, ItemCondition.USED
        );

        assertThatThrownBy(() -> itemService.update(1L, OTHER_MEMBER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ITEM_FORBIDDEN);
    }

    @Test
    void update_소유자이면_필드가_변경된다() {
        Item item = withId(sampleItem(), 1L);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        ItemUpdateRequest request = new ItemUpdateRequest(
                "수정된 제목", "수정된 설명", "피규어", "나루토", "우즈마키 나루토", 60_000, ItemCondition.USED
        );

        ItemResponse response = itemService.update(1L, SELLER_ID, request);

        assertThat(response.title()).isEqualTo("수정된 제목");
        assertThat(response.price()).isEqualTo(60_000);
        assertThat(response.condition()).isEqualTo(ItemCondition.USED);
    }

    @Test
    void delete_소유자가_아니면_예외가_발생한다() {
        Item item = withId(sampleItem(), 1L);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> itemService.delete(1L, OTHER_MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ITEM_FORBIDDEN);

        verify(itemRepository, never()).delete(any(Item.class));
    }

    @Test
    void delete_소유자이면_삭제된다() {
        Item item = withId(sampleItem(), 1L);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        itemService.delete(1L, SELLER_ID);

        verify(itemRepository).delete(item);
    }

    @Test
    void search_조건에_맞는_페이지를_반환한다() {
        Item item = withId(sampleItem(), 1L);
        Pageable pageable = PageRequest.of(0, 20);
        when(itemRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item), pageable, 1));

        ItemSearchCondition condition = new ItemSearchCondition("피규어", "나루토", null, null, null, null);
        var page = itemService.search(condition, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).title()).isEqualTo(item.getTitle());
    }

    private static Item sampleItem() {
        return Item.register(
                SELLER_ID, "나루토 피규어", "미개봉", "피규어", "나루토", "우즈마키 나루토", 50_000, ItemCondition.NEW
        );
    }

    /** BIGSERIAL로 생성되는 id는 리플렉션으로 채워서 저장 이후 상태를 흉내낸다 (DB 없이 단위 테스트). */
    private static Item withId(Item item, Long id) {
        try {
            Field idField = Item.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(item, id);
            return item;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
