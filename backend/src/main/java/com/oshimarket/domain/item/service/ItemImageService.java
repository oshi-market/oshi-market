package com.oshimarket.domain.item.service;

import com.oshimarket.domain.item.dto.ItemImageResponse;
import com.oshimarket.domain.item.entity.Item;
import com.oshimarket.domain.item.entity.ItemImage;
import com.oshimarket.domain.item.repository.ItemImageRepository;
import com.oshimarket.domain.item.repository.ItemRepository;
import com.oshimarket.global.exception.BusinessException;
import com.oshimarket.global.exception.ErrorCode;
import com.oshimarket.global.storage.ImageStorage;
import com.oshimarket.global.storage.StoredImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 상품 사진 업로드/삭제. 상품 등록(JSON)과 사진 업로드(multipart)는 API를 분리해서
 * 프론트는 상품 저장 → 사진 업로드 순서로 호출한다 (3_아키텍처및서비스흐름.md 상품 API 참고).
 */
@Service
@Transactional(readOnly = true)
public class ItemImageService {

    private static final Logger log = LoggerFactory.getLogger(ItemImageService.class);
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final ItemRepository itemRepository;
    private final ItemImageRepository itemImageRepository;
    private final ImageStorage imageStorage;

    public ItemImageService(
            ItemRepository itemRepository,
            ItemImageRepository itemImageRepository,
            ImageStorage imageStorage
    ) {
        this.itemRepository = itemRepository;
        this.itemImageRepository = itemImageRepository;
        this.imageStorage = imageStorage;
    }

    /** 기존 사진 뒤에 이어 붙이고, 해당 상품의 전체 사진 목록을 반환. */
    @Transactional
    public List<ItemImageResponse> upload(Long itemId, Long memberId, List<MultipartFile> files) {
        validateOwner(itemId, memberId);
        List<ItemImage> existing = itemImageRepository.findByItemIdOrderBySortOrderAsc(itemId);
        validateFiles(files, existing.size());

        int nextOrder = existing.isEmpty() ? 0 : existing.get(existing.size() - 1).getSortOrder() + 1;
        List<StoredImage> stored = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                stored.add(imageStorage.upload(file));
            }
            for (StoredImage image : stored) {
                itemImageRepository.save(ItemImage.of(itemId, image.url(), image.publicId(), nextOrder++));
            }
        } catch (RuntimeException e) {
            // 중간에 실패하면 이미 저장소에 올라간 파일은 고아가 되므로 정리 (DB는 트랜잭션 롤백)
            stored.forEach(image -> deleteQuietly(image.publicId()));
            throw e;
        }

        return itemImageRepository.findByItemIdOrderBySortOrderAsc(itemId).stream()
                .map(ItemImageResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long itemId, Long imageId, Long memberId) {
        validateOwner(itemId, memberId);
        ItemImage image = itemImageRepository.findByIdAndItemId(imageId, itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));

        itemImageRepository.delete(image);
        deleteQuietly(image.getPublicId());
    }

    /** 상품 삭제 후 호출. DB 행은 FK ON DELETE CASCADE로 지워지고, 저장소 파일만 정리. */
    public void deleteStoredFiles(List<ItemImage> images) {
        images.forEach(image -> deleteQuietly(image.getPublicId()));
    }

    private void validateOwner(Long itemId, Long memberId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
        if (!item.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.ITEM_FORBIDDEN);
        }
    }

    private void validateFiles(List<MultipartFile> files, int existingCount) {
        if (files == null || files.isEmpty() || files.stream().allMatch(MultipartFile::isEmpty)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "업로드할 사진을 선택해주세요.");
        }
        if (existingCount + files.size() > ItemImage.MAX_PER_ITEM) {
            throw new BusinessException(ErrorCode.IMAGE_LIMIT_EXCEEDED);
        }
        // Content-Type은 클라이언트가 보내는 값이라 1차 필터. 실제 이미지 여부는 Cloudinary(resource_type=image)가 한 번 더 검증
        boolean hasInvalidType = files.stream()
                .anyMatch(file -> !ALLOWED_CONTENT_TYPES.contains(file.getContentType()));
        if (hasInvalidType) {
            throw new BusinessException(ErrorCode.IMAGE_INVALID_TYPE);
        }
    }

    /** 저장소 파일 삭제 실패는 사용자 요청을 실패시키지 않고 로그만 남긴다 (고아 파일은 무료 용량만 차지). */
    private void deleteQuietly(String publicId) {
        try {
            imageStorage.delete(publicId);
        } catch (RuntimeException e) {
            log.warn("이미지 저장소 파일 삭제 실패 (publicId={})", publicId, e);
        }
    }
}
