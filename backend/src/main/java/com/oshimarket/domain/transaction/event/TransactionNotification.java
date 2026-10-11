package com.oshimarket.domain.transaction.event;

import com.oshimarket.domain.transaction.dto.TransactionResponse;

/**
 * /sub/chat/{chatRoomId}로 보내는 거래 알림.
 * 채팅 메시지(ChatMessageResponse)와 같은 주소로 가므로, 프론트는 type == "TRANSACTION"으로 구분한다.
 */
public record TransactionNotification(String type, TransactionResponse transaction) {

    public static final String TYPE = "TRANSACTION";

    public static TransactionNotification of(TransactionResponse transaction) {
        return new TransactionNotification(TYPE, transaction);
    }
}
