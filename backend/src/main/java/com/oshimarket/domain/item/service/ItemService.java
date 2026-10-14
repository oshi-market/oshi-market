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
import com.oshimarket.domain.member.entity.Member;
import com.oshimarket.domain.member.repository.MemberRepository;
import com.oshimarket.domain.work.repository.WorkRepository;
import com.oshimarket.global.exception.BusinessException;
import com.oshimarket.global.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class ItemService {

    /** 상품 목록에서 정렬 가능한 필드. 그 외 필드(sellerId, title 등)로 정렬하면 INVALID_SORT(400). */
    private static final Set<String> SORTABLE_PROPERTIES = Set.of("createdAt", "price");

    private final ItemRepository itemRepository;
    private final WorkRepository workRepository;
    private final ItemImageRepository itemImageRepository;
    private final ItemImageService itemImageService;
    private final MemberRepository memberRepository;

    public ItemService(
            ItemRepository itemRepository,
            WorkRepository workRepository,
            ItemImageRepository itemImageRepository,
            ItemImageService itemImageService,
            MemberRepository memberRepository
    ) {
        this.itemRepository = itemRepository;
        this.workRepository = workRepository;
        this.itemImageRepository = itemImageRepository;
        this.itemImageService = itemImageService;
        this.memberRepository = memberRepository;
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
        return ItemResponse.from(itemRepository.save(item), List.of(), findNickname(sellerId));
    }

    public Page<ItemResponse> search(ItemSearchCondition condition, Pageable pageable) {
        Specification<Item> spec = ItemSpecifications.from(condition);
        Page<Item> items = itemRepository.findAll(spec, withStableSort(pageable));

        List<Long> itemIds = items.stream().map(Item::getId).toList();
        Map<Long, List<ItemImage>> imagesByItemId = itemImageRepository.findByItemIdInOrderBySortOrderAsc(itemIds)
                .stream()
                .collect(Collectors.groupingBy(ItemImage::getItemId));

        // 판매자 닉네임도 사진처럼 한 번에 모아서 조회 (상품마다 따로 조회하면 쿼리가 1+N번 나감)
        List<Long> sellerIds = items.stream().map(Item::getSellerId).distinct().toList();
        Map<Long, String> nicknameBySellerId = memberRepository.findAllById(sellerIds)
                .stream()
                .collect(Collectors.toMap(Member::getId, Member::getNickname));

        return items.map(item -> ItemResponse.from(
                item,
                imagesByItemId.getOrDefault(item.getId(), List.of()),
                nicknameBySellerId.get(item.getSellerId())
        ));
    }

    /**
     * 허용된 필드만 정렬하게 막고, 마지막에 id 내림차순을 붙인다. 가격이 같은 상품이 여러 개면 DB가 순서를
     * 보장하지 않아 페이지를 넘길 때 같은 상품이 두 번 나오거나 빠질 수 있어서, 항상 순서가 하나로 정해지게 한다.
     */
    private static Pageable withStableSort(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE_PROPERTIES.contains(order.getProperty())) {
                throw new BusinessException(ErrorCode.INVALID_SORT);
            }
        }
        Sort sort = pageable.getSort().and(Sort.by(Sort.Direction.DESC, "id"));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    public ItemResponse getItem(Long itemId) {
        return toResponse(findItemOrThrow(itemId));
    }

    @Transactional
    public ItemResponse update(Long itemId, Long memberId, ItemUpdateRequest request) {
        Item item = findItemOrThrow(itemId);
        validateOwner(item, memberId);
        validateEditable(item);
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
        if (itemRepository.hasChatOrTransaction(itemId)) {
            throw new BusinessException(ErrorCode.ITEM_HAS_CHAT_OR_TRANSACTION);
        }

        List<ItemImage> images = itemImageRepository.findByItemIdOrderBySortOrderAsc(itemId);
        itemRepository.delete(item);
        itemRepository.flush();
        // DB 삭제가 성공한 뒤에 저장소 파일 정리 (먼저 지웠다가 DB 삭제가 실패하면 사진만 사라짐)
        itemImageService.deleteStoredFiles(images);
    }

    private ItemResponse toResponse(Item item) {
        return ItemResponse.from(
                item,
                itemImageRepository.findByItemIdOrderBySortOrderAsc(item.getId()),
                findNickname(item.getSellerId())
        );
    }

    private String findNickname(Long memberId) {
        return memberRepository.findById(memberId).map(Member::getNickname).orElse(null);
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

    private void validateEditable(Item item) {
        if (!item.isEditable()) {
            throw new BusinessException(ErrorCode.ITEM_NOT_EDITABLE);
        }
    }
}
