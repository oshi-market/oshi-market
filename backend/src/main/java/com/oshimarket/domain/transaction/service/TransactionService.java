package com.oshimarket.domain.transaction.service;

import com.oshimarket.domain.chat.entity.ChatRoom;
import com.oshimarket.domain.chat.repository.ChatRoomRepository;
import com.oshimarket.domain.item.entity.Item;
import com.oshimarket.domain.item.repository.ItemRepository;
import com.oshimarket.domain.transaction.dto.TransactionResponse;
import com.oshimarket.domain.transaction.entity.Transaction;
import com.oshimarket.domain.transaction.entity.TransactionStatus;
import com.oshimarket.domain.transaction.event.TransactionChangedEvent;
import com.oshimarket.domain.transaction.repository.TransactionRepository;
import com.oshimarket.global.exception.BusinessException;
import com.oshimarket.global.exception.ErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 거래 요청·조회·상태 변경. 거래는 채팅방에서만 만들 수 있고(3_아키텍처 5. 서비스 흐름),
 * 같은 상품에 진행 중인 거래가 2건 이상 생기지 않도록 Item row를 비관적 락으로 잠근 뒤 상태를 바꾼다(4_DB분석서 3. 동시성 제어).
 */
// 거래 요청·조회·상태 변경. 거래 상태가 바뀌면 상품 상태도 함께 맞춘다
//  (요청 → IN_TRANSACTION, 취소 → SELLING, 완료 → SOLD_OUT)
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ItemRepository itemRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TransactionService(TransactionRepository transactionRepository, ChatRoomRepository chatRoomRepository,
                              ItemRepository itemRepository, ApplicationEventPublisher eventPublisher) {
        this.transactionRepository = transactionRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.itemRepository = itemRepository;
        this.eventPublisher = eventPublisher;
    }

    /** POST /api/chatrooms/{chatRoomId}/transactions — 채팅방 구매자만 요청할 수 있다. */
    @Transactional
    public TransactionResponse request(Long chatRoomId, Long memberId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        if (!memberId.equals(chatRoom.getBuyerId())) {
            throw new BusinessException(memberId.equals(chatRoom.getSellerId())
                    ? ErrorCode.TRANSACTION_REQUEST_FORBIDDEN : ErrorCode.CHAT_ROOM_FORBIDDEN);
        }
        // 잠근 뒤에 SELLING인지 확인해야, 동시에 요청한 다른 구매자는 락이 풀린 뒤 IN_TRANSACTION을 보고 거절된다
        Item item = itemRepository.findByIdForUpdate(chatRoom.getItemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
        if (!item.isSelling()) {
            throw new BusinessException(ErrorCode.ITEM_NOT_SELLING);
        }
        item.startTransaction();
        Transaction saved = transactionRepository.save(
                Transaction.request(item.getId(), chatRoom.getId(), chatRoom.getBuyerId(), chatRoom.getSellerId()));
        return publish(TransactionResponse.from(saved));
    }

    /** GET /api/transactions/{transactionId} — 거래 당사자만 조회 */
    @Transactional(readOnly = true)
    public TransactionResponse get(Long transactionId, Long memberId) {
        return TransactionResponse.from(checkParticipant(transactionRepository.findById(transactionId), memberId));
    }

    /**
     * PATCH /api/transactions/{transactionId}/status — 수락(IN_PROGRESS)은 판매자만, 완료·취소는 당사자 둘 다.
     * 상품 상태도 함께 맞춘다: 취소 → SELLING, 완료 → SOLD_OUT.
     * 락 순서는 거래 → 상품. 거래를 잠근 뒤에 상태를 확인해야 동시 변경에서 두 번째 요청이 바뀐 상태를 본다.
     */
    @Transactional
    public TransactionResponse changeStatus(Long transactionId, Long memberId, TransactionStatus next) {
        Transaction transaction = checkParticipant(transactionRepository.findByIdForUpdate(transactionId), memberId);
        if (!transaction.canChangeTo(next)) {
            throw new BusinessException(ErrorCode.INVALID_TRANSACTION_STATUS);
        }
        if (next == TransactionStatus.IN_PROGRESS && !memberId.equals(transaction.getSellerId())) {
            throw new BusinessException(ErrorCode.TRANSACTION_ACCEPT_FORBIDDEN);
        }
        Item item = itemRepository.findByIdForUpdate(transaction.getItemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
        transaction.changeStatus(next);
        if (next == TransactionStatus.CANCELLED) {
            item.reopen();
        } else if (next == TransactionStatus.COMPLETED) {
            item.markSoldOut();
        }
        return publish(TransactionResponse.from(transaction));
    }

    // 채팅방 알림은 커밋 뒤에 나간다(TransactionNotifier). 여기서는 이벤트만 남긴다
    private TransactionResponse publish(TransactionResponse response) {
        eventPublisher.publishEvent(new TransactionChangedEvent(response));
        return response;
    }

    private Transaction checkParticipant(Optional<Transaction> found, Long memberId) {
        Transaction transaction = found
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND));
        if (!transaction.isParticipant(memberId)) {
            throw new BusinessException(ErrorCode.TRANSACTION_FORBIDDEN);
        }
        return transaction;
    }
}
