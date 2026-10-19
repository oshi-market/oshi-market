package com.oshimarket.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

class StompErrorHandlerTest {

    private final StompErrorHandler handler = new StompErrorHandler();

    @Test
    void CONNECT에서_JWT_오류면_UNAUTHORIZED를_보낸다() {
        // 인터셉터의 JwtException은 채널을 거치며 MessageDeliveryException에 감싸져서 온다
        Throwable ex = new MessageDeliveryException(connectMessage(), "Failed to send message",
                new JwtException("유효하지 않은 토큰입니다."));

        assertThat(errorText(handler.handleClientMessageProcessingError(connectMessage(), ex)))
                .isEqualTo(StompErrorHandler.UNAUTHORIZED);
    }

    @Test
    void 그_밖의_오류는_내부_메시지_대신_공통_문구를_보낸다() {
        Throwable ex = new MessageDeliveryException("Failed to send message to ExecutorSubscribableChannel[clientInboundChannel]");

        assertThat(errorText(handler.handleClientMessageProcessingError(connectMessage(), ex)))
                .isEqualTo(StompErrorHandler.GENERIC);
    }

    private static Message<byte[]> connectMessage() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static String errorText(Message<byte[]> error) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(error);
        assertThat(accessor.getCommand()).isEqualTo(StompCommand.ERROR);
        return accessor.getMessage();
    }
}
