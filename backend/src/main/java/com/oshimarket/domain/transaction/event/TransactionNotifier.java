package com.oshimarket.domain.transaction.event;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 거래 상태 변경을 채팅방 구독자(구매자·판매자)에게 실시간으로 알린다.
 * AFTER_COMMIT: 커밋 전에 보내면 롤백된 상태가 화면에 뜨거나, 알림을 받고 바로 조회했을 때 아직 바뀌기 전 값이 보일 수 있다.
 * 알림이 실패해도 거래는 이미 커밋됐으므로 되돌리지 않는다(화면의 ↻로 다시 확인 가능).
 */
@Component
public class TransactionNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    public TransactionNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransactionChanged(TransactionChangedEvent event) {
        messagingTemplate.convertAndSend("/sub/chat/" + event.transaction().chatRoomId(),
                TransactionNotification.of(event.transaction()));
    }
}
