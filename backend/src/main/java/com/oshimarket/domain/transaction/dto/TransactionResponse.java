package com.oshimarket.domain.transaction.dto;

import com.oshimarket.domain.transaction.entity.Transaction;
import com.oshimarket.domain.transaction.entity.TransactionStatus;
import java.time.LocalDateTime;

// 거래 요청·조회·상태 변경 응답
public record TransactionResponse(
        Long id,
        Long itemId,
        Long chatRoomId,
        Long buyerId,
        Long sellerId,
        TransactionStatus status,
        LocalDateTime transactedAt,
        LocalDateTime createdAt
) {

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getItemId(),
                transaction.getChatRoomId(),
                transaction.getBuyerId(),
                transaction.getSellerId(),
                transaction.getStatus(),
                transaction.getTransactedAt(),
                transaction.getCreatedAt()
        );
    }
}
