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
import com.oshimarket.domain.item.entity.ItemCategory;
import com.oshimarket.domain.item.entity.ItemCondition;
import com.oshimarket.domain.item.entity.ItemImage;
import com.oshimarket.domain.item.repository.ItemImageRepository;
import com.oshimarket.domain.item.repository.ItemRepository;
import com.oshimarket.domain.work.repository.WorkRepository;
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
    private WorkRepository workRepository;
    private ItemImageRepository itemImageRepository;
    private ItemImageService itemImageService;
    private ItemService itemService;

    @BeforeEach
    void setUp() {
        itemRepository = mock(ItemRepository.class);
        workRepository = mock(WorkRepository.class);
        itemImageRepository = mock(ItemImageRepository.class);
        itemImageService = mock(ItemImageService.class);
        itemService = new ItemService(itemRepository, workRepository, itemImageRepository, itemImageService);
        when(workRepository.existsByName("나루토")).thenReturn(true);
    }

    @Test
    void register_성공하면_판매중_상태로_저장한다() {
        ItemCreateRequest request = new ItemCreateRequest(
                "나루토 피규어", "미개봉", ItemCategory.FIGURE, "나루토", "우즈마키 나루토", 50_000, ItemCondition.NEW
        );
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 1L));

        ItemResponse response = itemService.register(SELLER_ID, request);

        assertThat(response.sellerId()).isEqualTo(SELLER_ID);
        assertThat(response.title()).isEqualTo(request.title());
        assertThat(response.status().name()).isEqualTo("SELLING");
    }

    @Test
    void register_등록되지_않은_작품이면_예외가_발생한다() {
        ItemCreateRequest request = new ItemCreateRequest(
                "피규어", "미개봉", ItemCategory.FIGURE, "없는 작품", null, 50_000, ItemCondition.NEW
        );

        assertThatThrownBy(() -> itemService.register(SELLER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.WORK_NOT_FOUND);

        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void register_작품명을_비워두면_작품_검증없이_저장한다() {
        ItemCreateRequest request = new ItemCreateRequest(
                "캔뱃지", null, ItemCategory.CAN_BADGE, null, null, 5_000, ItemCondition.USED
        );
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 1L));

        ItemResponse response = itemService.register(SELLER_ID, request);

        assertThat(response.category()).isEqualTo(ItemCategory.CAN_BADGE);
        assertThat(response.workTag()).isNull();
        verify(workRepository, never()).existsByName(any());
    }

    @Test
    void update_등록되지_않은_작품으로_바꾸면_예외가_발생한다() {
        Item item = withId(sampleItem(), 1L);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        ItemUpdateRequest request = new ItemUpdateRequest(
                "수정된 제목", "설명", ItemCategory.FIGURE, "없는 작품", null, 60_000, ItemCondition.USED
        );

        assertThatThrownBy(() -> itemService.update(1L, SELLER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.WORK_NOT_FOUND);
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
                "수정된 제목", "설명", ItemCategory.FIGURE, "나루토", "우즈마키 나루토", 60_000, ItemCondition.USED
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
                "수정된 제목", "수정된 설명", ItemCategory.FIGURE, "나루토", "우즈마키 나루토", 60_000, ItemCondition.USED
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
    void delete_채팅이나_거래가_있으면_삭제하지_않고_예외가_발생한다() {
        Item item = withId(sampleItem(), 1L);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemRepository.hasChatOrTransaction(1L)).thenReturn(true);

        assertThatThrownBy(() -> itemService.delete(1L, SELLER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ITEM_HAS_CHAT_OR_TRANSACTION);

        verify(itemRepository, never()).delete(any(Item.class));
        verify(itemImageService, never()).deleteStoredFiles(any());
    }

    @Test
    void delete_상품을_지운_뒤_저장소의_사진_파일도_정리한다() {
        Item item = withId(sampleItem(), 1L);
        List<ItemImage> images = List.of(ItemImage.of(1L, "https://img/1.jpg", "items/1", 0));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemImageRepository.findByItemIdOrderBySortOrderAsc(1L)).thenReturn(images);

        itemService.delete(1L, SELLER_ID);

        var inOrder = org.mockito.Mockito.inOrder(itemRepository, itemImageService);
        inOrder.verify(itemRepository).delete(item);
        inOrder.verify(itemImageService).deleteStoredFiles(images);
    }

    @Test
    void getItem_사진이_있으면_첫_사진을_썸네일로_반환한다() {
        Item item = withId(sampleItem(), 1L);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemImageRepository.findByItemIdOrderBySortOrderAsc(1L)).thenReturn(List.of(
                ItemImage.of(1L, "https://img/first.jpg", "items/first", 0),
                ItemImage.of(1L, "https://img/second.jpg", "items/second", 1)
        ));

        ItemResponse response = itemService.getItem(1L);

        assertThat(response.thumbnailUrl()).isEqualTo("https://img/first.jpg");
        assertThat(response.images()).hasSize(2);
    }

    @Test
    void search_조건에_맞는_페이지를_반환한다() {
        Item item = withId(sampleItem(), 1L);
        Pageable pageable = PageRequest.of(0, 20);
        when(itemRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item), pageable, 1));

        ItemSearchCondition condition = new ItemSearchCondition(ItemCategory.FIGURE, "나루토", null, null, null, null);
        var page = itemService.search(condition, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).title()).isEqualTo(item.getTitle());
    }

    private static Item sampleItem() {
        return Item.register(
                SELLER_ID, "나루토 피규어", "미개봉", ItemCategory.FIGURE, "나루토", "우즈마키 나루토", 50_000, ItemCondition.NEW
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
