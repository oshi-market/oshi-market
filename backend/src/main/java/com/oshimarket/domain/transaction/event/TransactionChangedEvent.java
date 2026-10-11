package com.oshimarket.domain.transaction.event;

import com.oshimarket.domain.transaction.dto.TransactionResponse;

// 거래가 만들어지거나 상태가 바뀌었을 때 TransactionService가 발행한다.
// 커밋이 끝난 뒤 TransactionNotifier가 받아 채팅방에 알린다
public record TransactionChangedEvent(TransactionResponse transaction) {
}
