package com.oshimarket.domain.transaction.dto;

import com.oshimarket.domain.item.entity.Item;
import com.oshimarket.domain.transaction.entity.Transaction;
import com.oshimarket.domain.transaction.entity.TransactionStatus;
import java.time.LocalDateTime;

/**
 * 내 거래 내역 목록 한 줄. 목록에서 상품명/가격을 바로 보여줄 수 있게 상품 정보를 함께 담는다.
 * role은 조회한 회원 기준(구매한 거래면 BUY, 판매한 거래면 SELL).
 */
public record TransactionHistoryResponse(
        Long id,
        Long itemId,
        String itemTitle,
        Integer itemPrice,
        Long chatRoomId,
        Long buyerId,
        Long sellerId,
        TransactionRole role,
        TransactionStatus status,
        LocalDateTime transactedAt,
        LocalDateTime createdAt
) {

    /** item은 조회 시점에 없을 수 있어서(null) 상품 정보는 null로 둔다. */
    public static TransactionHistoryResponse of(Transaction transaction, Item item, Long memberId) {
        TransactionRole role = transaction.getBuyerId().equals(memberId) ? TransactionRole.BUY : TransactionRole.SELL;

        return new TransactionHistoryResponse(
                transaction.getId(),
                transaction.getItemId(),
                item != null ? item.getTitle() : null,
                item != null ? item.getPrice() : null,
                transaction.getChatRoomId(),
                transaction.getBuyerId(),
                transaction.getSellerId(),
                role,
                transaction.getStatus(),
                transaction.getTransactedAt(),
                transaction.getCreatedAt()
        );
    }
}
