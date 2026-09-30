package com.oshimarket.domain.item.repository;

import com.oshimarket.domain.item.entity.ItemImage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemImageRepository extends JpaRepository<ItemImage, Long> {

    List<ItemImage> findByItemIdOrderBySortOrderAsc(Long itemId);

    /** 목록 페이지의 상품 사진을 한 번에 조회 (N+1 방지). */
    List<ItemImage> findByItemIdInOrderBySortOrderAsc(Collection<Long> itemIds);

    Optional<ItemImage> findByIdAndItemId(Long id, Long itemId);
}
