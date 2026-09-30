package com.oshimarket.domain.transaction.service;

import com.oshimarket.domain.item.entity.Item;
import com.oshimarket.domain.item.repository.ItemRepository;
import com.oshimarket.domain.transaction.dto.TransactionHistoryResponse;
import com.oshimarket.domain.transaction.dto.TransactionRole;
import com.oshimarket.domain.transaction.entity.Transaction;
import com.oshimarket.domain.transaction.repository.TransactionRepository;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 내 거래 내역 조회 전용. 거래 요청/상태 변경 로직(TransactionService)과 담당이 달라서 분리함.
 * Transaction은 itemId만 들고 있으므로 페이지에 나온 상품을 한 번에 조회해서 붙인다 (N+1 방지).
 */
@Service
@Transactional(readOnly = true)
public class TransactionHistoryService {

    private final TransactionRepository transactionRepository;
    private final ItemRepository itemRepository;

    public TransactionHistoryService(TransactionRepository transactionRepository, ItemRepository itemRepository) {
        this.transactionRepository = transactionRepository;
        this.itemRepository = itemRepository;
    }

    /** role이 null이면 구매/판매 전체. */
    public Page<TransactionHistoryResponse> getMyTransactions(Long memberId, TransactionRole role, Pageable pageable) {
        Page<Transaction> transactions = findTransactions(memberId, role, pageable);

        Set<Long> itemIds = transactions.stream()
                .map(Transaction::getItemId)
                .collect(Collectors.toSet());
        Map<Long, Item> itemsById = itemRepository.findAllById(itemIds).stream()
                .collect(Collectors.toMap(Item::getId, Function.identity()));

        return transactions.map(transaction ->
                TransactionHistoryResponse.of(transaction, itemsById.get(transaction.getItemId()), memberId));
    }

    private Page<Transaction> findTransactions(Long memberId, TransactionRole role, Pageable pageable) {
        if (role == null) {
            return transactionRepository.findByBuyerIdOrSellerId(memberId, memberId, pageable);
        }
        return switch (role) {
            case BUY -> transactionRepository.findByBuyerId(memberId, pageable);
            case SELL -> transactionRepository.findBySellerId(memberId, pageable);
        };
    }
}
