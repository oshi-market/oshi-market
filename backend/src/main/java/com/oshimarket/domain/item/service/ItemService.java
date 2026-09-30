package com.oshimarket.domain.item.service;

import com.oshimarket.domain.item.dto.ItemCreateRequest;
import com.oshimarket.domain.item.dto.ItemResponse;
import com.oshimarket.domain.item.dto.ItemSearchCondition;
import com.oshimarket.domain.item.dto.ItemUpdateRequest;
import com.oshimarket.domain.item.entity.Item;
import com.oshimarket.domain.item.repository.ItemRepository;
import com.oshimarket.domain.item.repository.ItemSpecifications;
import com.oshimarket.domain.work.repository.WorkRepository;
import com.oshimarket.global.exception.BusinessException;
import com.oshimarket.global.exception.ErrorCode;
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

    public ItemService(ItemRepository itemRepository, WorkRepository workRepository) {
        this.itemRepository = itemRepository;
        this.workRepository = workRepository;
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

        return ItemResponse.from(itemRepository.save(item));
    }

    public Page<ItemResponse> search(ItemSearchCondition condition, Pageable pageable) {
        Specification<Item> spec = ItemSpecifications.from(condition);
        return itemRepository.findAll(spec, pageable).map(ItemResponse::from);
    }

    public ItemResponse getItem(Long itemId) {
        return ItemResponse.from(findItemOrThrow(itemId));
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

        return ItemResponse.from(item);
    }

    @Transactional
    public void delete(Long itemId, Long memberId) {
        Item item = findItemOrThrow(itemId);
        validateOwner(item, memberId);
        itemRepository.delete(item);
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
