package com.oshimarket.domain.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oshimarket.domain.item.dto.ItemImageResponse;
import com.oshimarket.domain.item.entity.Item;
import com.oshimarket.domain.item.entity.ItemCategory;
import com.oshimarket.domain.item.entity.ItemCondition;
import com.oshimarket.domain.item.entity.ItemImage;
import com.oshimarket.domain.item.repository.ItemImageRepository;
import com.oshimarket.domain.item.repository.ItemRepository;
import com.oshimarket.global.exception.BusinessException;
import com.oshimarket.global.exception.ErrorCode;
import com.oshimarket.global.storage.ImageStorage;
import com.oshimarket.global.storage.StoredImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class ItemImageServiceTest {

    private static final Long SELLER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;
    private static final Long ITEM_ID = 10L;

    private ItemRepository itemRepository;
    private ItemImageRepository itemImageRepository;
    private ImageStorage imageStorage;
    private ItemImageService itemImageService;

    /** save된 사진을 흉내내는 가짜 DB. upload 후 전체 목록 조회에 쓰인다. */
    private final List<ItemImage> savedImages = new ArrayList<>();

    @BeforeEach
    void setUp() {
        itemRepository = mock(ItemRepository.class);
        itemImageRepository = mock(ItemImageRepository.class);
        imageStorage = mock(ImageStorage.class);
        itemImageService = new ItemImageService(itemRepository, itemImageRepository, imageStorage);

        Item item = Item.register(SELLER_ID, "피규어", null, ItemCategory.FIGURE, null, null, 10_000, ItemCondition.NEW);
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
        when(itemImageRepository.findByItemIdOrderBySortOrderAsc(ITEM_ID)).thenAnswer(invocation -> List.copyOf(savedImages));
        when(itemImageRepository.save(any(ItemImage.class))).thenAnswer(invocation -> {
            savedImages.add(invocation.getArgument(0));
            return invocation.getArgument(0);
        });
    }

    @Test
    void upload_저장소에_올리고_순서대로_저장한다() {
        when(imageStorage.upload(any())).thenReturn(
                new StoredImage("https://img/a.jpg", "items/a"),
                new StoredImage("https://img/b.jpg", "items/b")
        );

        List<ItemImageResponse> result = itemImageService.upload(ITEM_ID, SELLER_ID, List.of(jpg("a"), jpg("b")));

        assertThat(result).extracting(ItemImageResponse::url).containsExactly("https://img/a.jpg", "https://img/b.jpg");
        assertThat(savedImages).extracting(ItemImage::getSortOrder).containsExactly(0, 1);
    }

    @Test
    void upload_기존_사진_뒤에_이어서_순서를_매긴다() {
        savedImages.add(ItemImage.of(ITEM_ID, "https://img/old.jpg", "items/old", 3));
        when(imageStorage.upload(any())).thenReturn(new StoredImage("https://img/new.jpg", "items/new"));

        itemImageService.upload(ITEM_ID, SELLER_ID, List.of(jpg("new")));

        assertThat(savedImages.get(1).getSortOrder()).isEqualTo(4);
    }

    @Test
    void upload_판매자가_아니면_예외가_발생한다() {
        assertThatThrownBy(() -> itemImageService.upload(ITEM_ID, OTHER_MEMBER_ID, List.of(jpg("a"))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ITEM_FORBIDDEN);
        verify(imageStorage, never()).upload(any());
    }

    @Test
    void upload_합쳐서_5장을_넘으면_예외가_발생한다() {
        for (int i = 0; i < 4; i++) {
            savedImages.add(ItemImage.of(ITEM_ID, "https://img/" + i, "items/" + i, i));
        }

        assertThatThrownBy(() -> itemImageService.upload(ITEM_ID, SELLER_ID, List.of(jpg("a"), jpg("b"))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.IMAGE_LIMIT_EXCEEDED);
        verify(imageStorage, never()).upload(any());
    }

    @Test
    void upload_이미지가_아닌_파일이면_예외가_발생한다() {
        MultipartFile pdf = new MockMultipartFile("files", "doc.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> itemImageService.upload(ITEM_ID, SELLER_ID, List.of(pdf)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.IMAGE_INVALID_TYPE);
    }

    @Test
    void upload_중간에_실패하면_이미_올라간_파일을_지운다() {
        when(imageStorage.upload(any()))
                .thenReturn(new StoredImage("https://img/a.jpg", "items/a"))
                .thenThrow(new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED));

        assertThatThrownBy(() -> itemImageService.upload(ITEM_ID, SELLER_ID, List.of(jpg("a"), jpg("b"))))
                .isInstanceOf(BusinessException.class);

        verify(imageStorage).delete("items/a");
        verify(itemImageRepository, never()).save(any());
    }

    @Test
    void delete_DB에서_지우고_저장소_파일도_삭제한다() {
        ItemImage image = ItemImage.of(ITEM_ID, "https://img/a.jpg", "items/a", 0);
        when(itemImageRepository.findByIdAndItemId(5L, ITEM_ID)).thenReturn(Optional.of(image));

        itemImageService.delete(ITEM_ID, 5L, SELLER_ID);

        verify(itemImageRepository).delete(image);
        verify(imageStorage).delete("items/a");
    }

    @Test
    void delete_저장소_삭제가_실패해도_요청은_성공한다() {
        ItemImage image = ItemImage.of(ITEM_ID, "https://img/a.jpg", "items/a", 0);
        when(itemImageRepository.findByIdAndItemId(5L, ITEM_ID)).thenReturn(Optional.of(image));
        doThrow(new BusinessException(ErrorCode.IMAGE_DELETE_FAILED)).when(imageStorage).delete("items/a");

        itemImageService.delete(ITEM_ID, 5L, SELLER_ID);

        verify(itemImageRepository).delete(image);
    }

    @Test
    void delete_다른_상품의_사진이면_예외가_발생한다() {
        when(itemImageRepository.findByIdAndItemId(5L, ITEM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemImageService.delete(ITEM_ID, 5L, SELLER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.IMAGE_NOT_FOUND);
        verify(imageStorage, never()).delete(any());
    }

    private static MultipartFile jpg(String name) {
        return new MockMultipartFile("files", name + ".jpg", "image/jpeg", new byte[]{1, 2, 3});
    }
}
