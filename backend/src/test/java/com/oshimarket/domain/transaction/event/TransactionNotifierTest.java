package com.oshimarket.domain.transaction.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.oshimarket.domain.transaction.dto.TransactionResponse;
import com.oshimarket.domain.transaction.entity.TransactionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageDeliveryException;
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

    @Test
    void 알림_전송이_실패해도_예외를_밖으로_던지지_않는다() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        doThrow(new MessageDeliveryException("broker down"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));
        TransactionNotifier notifier = new TransactionNotifier(messagingTemplate);
        TransactionResponse transaction = new TransactionResponse(
                30L, 10L, 20L, 2L, 1L, TransactionStatus.IN_PROGRESS, null, null);

        assertThatCode(() -> notifier.onTransactionChanged(new TransactionChangedEvent(transaction)))
                .doesNotThrowAnyException();
    }
}
