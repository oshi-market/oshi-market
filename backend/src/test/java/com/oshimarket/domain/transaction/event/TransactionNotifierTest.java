package com.oshimarket.domain.transaction.event;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.oshimarket.domain.transaction.dto.TransactionResponse;
import com.oshimarket.domain.transaction.entity.TransactionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class TransactionNotifierTest {

    @Test
    void 거래가_바뀌면_그_채팅방_주소로_TRANSACTION_알림을_보낸다() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        TransactionNotifier notifier = new TransactionNotifier(messagingTemplate);
        TransactionResponse transaction = new TransactionResponse(
                30L, 10L, 20L, 2L, 1L, TransactionStatus.IN_PROGRESS, null, null);

        notifier.onTransactionChanged(new TransactionChangedEvent(transaction));

        verify(messagingTemplate).convertAndSend(eq("/sub/chat/20"),
                eq(new TransactionNotification("TRANSACTION", transaction)));
    }
}
