package com.oshimarket.global.config;

import io.jsonwebtoken.JwtException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

/**
 * STOMP ERROR 프레임의 message 헤더를 정리한다.
 * - CONNECT에서 JWT가 없거나 만료/무효(JwtException)면 "UNAUTHORIZED" → 프론트가 재연결을 멈추고 로그아웃 처리
 *   (그대로 두면 @stomp/stompjs가 reconnectDelay마다 같은 만료 토큰으로 다시 붙어 "연결 중"이 끝없이 반복된다)
 * - 그 밖의 오류는 기본 메시지에 내부 채널 이름 등이 섞여 있어서 공통 문구로 바꾼다
 */
public class StompErrorHandler extends StompSubProtocolErrorHandler {

    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    static final String GENERIC = "채팅 서버 오류가 발생했습니다.";

    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
        String message = hasJwtCause(ex) ? UNAUTHORIZED : GENERIC;
        return super.handleClientMessageProcessingError(clientMessage, new MessageDeliveryException(message));
    }

    private static boolean hasJwtCause(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof JwtException) {
                return true;
            }
        }
        return false;
    }
}
