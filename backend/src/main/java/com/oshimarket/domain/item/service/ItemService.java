package com.oshimarket.domain.item.service;

import com.oshimarket.domain.item.dto.ItemCreateRequest;
import com.oshimarket.domain.item.dto.ItemResponse;
import com.oshimarket.domain.item.dto.ItemSearchCondition;
import com.oshimarket.domain.item.dto.ItemUpdateRequest;
import com.oshimarket.domain.item.entity.Item;
import com.oshimarket.domain.item.entity.ItemImage;
import com.oshimarket.domain.item.repository.ItemImageRepository;
import com.oshimarket.domain.item.repository.ItemRepository;
import com.oshimarket.domain.item.repository.ItemSpecifications;
import com.oshimarket.domain.work.repository.WorkRepository;
import com.oshimarket.global.exception.BusinessException;
import com.oshimarket.global.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;
    private final WorkRepository workRepository;
    private final ItemImageRepository itemImageRepository;
    private final ItemImageService itemImageService;

    public ItemService(
            ItemRepository itemRepository,
            WorkRepository workRepository,
            ItemImageRepository itemImageRepository,
            ItemImageService itemImageService
    ) {
        this.itemRepository = itemRepository;
        this.workRepository = workRepository;
        this.itemImageRepository = itemImageRepository;
        this.itemImageService = itemImageService;
    }

    @Transactional
    public ItemResponse register(Long sellerId, ItemCreateRequest request) {
        validateWork(request.workTag());

        Item item = Item.register(
                sellerId,
                request.title(),
                request.description(),
                request.category(),
                request.workTag(),
                request.characterTag(),
                request.price(),
                request.condition()
        );

        // 사진은 등록 직후 별도 API(POST /api/items/{id}/images)로 올리므로 이 시점엔 항상 빈 목록
        return ItemResponse.from(itemRepository.save(item), List.of());
    }

    public Page<ItemResponse> search(ItemSearchCondition condition, Pageable pageable) {
        Specification<Item> spec = ItemSpecifications.from(condition);
        Page<Item> items = itemRepository.findAll(spec, pageable);

        List<Long> itemIds = items.stream().map(Item::getId).toList();
        Map<Long, List<ItemImage>> imagesByItemId = itemImageRepository.findByItemIdInOrderBySortOrderAsc(itemIds)
                .stream()
                .collect(Collectors.groupingBy(ItemImage::getItemId));

        return items.map(item -> ItemResponse.from(item, imagesByItemId.getOrDefault(item.getId(), List.of())));
    }

    public ItemResponse getItem(Long itemId) {
        return toResponse(findItemOrThrow(itemId));
    }

    @Transactional
    public ItemResponse update(Long itemId, Long memberId, ItemUpdateRequest request) {
        Item item = findItemOrThrow(itemId);
        validateOwner(item, memberId);
        validateWork(request.workTag());

        item.update(
                request.title(),
                request.description(),
                request.category(),
                request.workTag(),
                request.characterTag(),
                request.price(),
                request.condition()
        );

        return toResponse(item);
    }

    @Transactional
    public void delete(Long itemId, Long memberId) {
        Item item = findItemOrThrow(itemId);
        validateOwner(item, memberId);

        List<ItemImage> images = itemImageRepository.findByItemIdOrderBySortOrderAsc(itemId);
        itemRepository.delete(item);
        itemRepository.flush();
        // DB 삭제가 성공한 뒤에 저장소 파일 정리 (먼저 지웠다가 DB 삭제가 실패하면 사진만 사라짐)
        itemImageService.deleteStoredFiles(images);
    }

    private ItemResponse toResponse(Item item) {
        return ItemResponse.from(item, itemImageRepository.findByItemIdOrderBySortOrderAsc(item.getId()));
    }

    private Item findItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
    }

    /** 작품명은 선택 입력이지만, 입력했다면 work 테이블에 있는 이름이어야 검색/큐레이션 필터가 맞게 걸린다. */
    private void validateWork(String workTag) {
        if (StringUtils.hasText(workTag) && !workRepository.existsByName(workTag)) {
            throw new BusinessException(ErrorCode.WORK_NOT_FOUND);
        }
    }

    private void validateOwner(Item item, Long memberId) {
        if (!item.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.ITEM_FORBIDDEN);
        }
    }
}
