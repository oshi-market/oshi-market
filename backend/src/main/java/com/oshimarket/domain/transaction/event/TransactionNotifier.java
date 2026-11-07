package com.oshimarket.domain.transaction.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 거래 상태 변경을 채팅방 구독자(구매자·판매자)에게 실시간으로 알린다.
 * AFTER_COMMIT: 커밋 전에 보내면 롤백된 상태가 화면에 뜨거나, 알림을 받고 바로 조회했을 때 아직 바뀌기 전 값이 보일 수 있다.
 * 알림이 실패해도 거래는 이미 커밋됐으므로 되돌리지 않는다(화면의 ↻로 다시 확인 가능).
 * AFTER_COMMIT 리스너의 예외는 Spring이 잡아 로그만 남기므로 호출자에게 전달되지 않는다.
 * 어떤 거래의 알림이 실패했는지 알 수 있도록 여기서 잡아 거래 id와 함께 경고 로그를 남긴다.
 */
@Component
public class TransactionNotifier {

    private static final Logger log = LoggerFactory.getLogger(TransactionNotifier.class);

    private final SimpMessagingTemplate messagingTemplate;

    public TransactionNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransactionChanged(TransactionChangedEvent event) {
        try {
            messagingTemplate.convertAndSend("/sub/chat/" + event.transaction().chatRoomId(),
                    TransactionNotification.of(event.transaction()));
        } catch (RuntimeException e) { // 전송 실패(MessagingException)뿐 아니라 변환 중 예외도 같은 형식으로 기록
            log.warn("거래 알림 전송 실패 (transactionId={}): {}", event.transaction().id(), e.getMessage());
        }
    }
}
