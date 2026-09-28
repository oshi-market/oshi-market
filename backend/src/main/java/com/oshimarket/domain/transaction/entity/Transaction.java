package com.oshimarket.domain.transaction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 거래. item/chat_room/member는 다른 도메인이라 ChatRoom과 같이 ID만 들고 있음.
 * item_id/chat_room_id는 의도적으로 UNIQUE 아님 — 취소 후 재요청 허용 (4_DB분석서.md 참고).
 */
@Entity
@Table(name = "transaction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;

    /** 거래 완료 시점. 완료 전에는 null. */
    @Column(name = "transacted_at")
    private LocalDateTime transactedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Transaction(Long itemId, Long chatRoomId, Long buyerId, Long sellerId) {
        this.itemId = itemId;
        this.chatRoomId = chatRoomId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.status = TransactionStatus.REQUESTED;
    }

    public static Transaction request(Long itemId, Long chatRoomId, Long buyerId, Long sellerId) {
        return new Transaction(itemId, chatRoomId, buyerId, sellerId);
    }
}
