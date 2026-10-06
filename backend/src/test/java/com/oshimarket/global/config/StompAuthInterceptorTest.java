package com.oshimarket.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.oshimarket.global.security.JwtTokenProvider;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

class StompAuthInterceptorTest {

    private static final Long ME = 2L;
    private static final Long MY_ROOM = 100L;
    private static final Long OTHER_ROOM = 200L;

    private StompAuthInterceptor interceptor;
    private final MessageChannel channel = mock(MessageChannel.class);

    @BeforeEach
    void setUp() {
        JwtTokenProvider jwt = mock(JwtTokenProvider.class);
        when(jwt.validateToken("good")).thenReturn(true);
        when(jwt.getMemberId("good")).thenReturn(ME);
        interceptor = new StompAuthInterceptor(jwt, (roomId, memberId) -> roomId.equals(MY_ROOM) && memberId.equals(ME));
    }

    @Test
    void CONNECT_토큰이_맞으면_userId와_Principal을_설정한다() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer good");
        Map<String, Object> session = new HashMap<>();
        accessor.setSessionAttributes(session);

        interceptor.preSend(message(accessor), channel);

        assertThat(session.get("userId")).isEqualTo(ME);
        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo("2");
    }

    @Test
    void SUBSCRIBE_내_채팅방은_허용한다() {
        Message<byte[]> msg = subscribe("/sub/chat/" + MY_ROOM);
        assertThat(interceptor.preSend(msg, channel)).isSameAs(msg);
    }

    @Test
    void SUBSCRIBE_남의_채팅방은_거절한다() {
        assertThatThrownBy(() -> interceptor.preSend(subscribe("/sub/chat/" + OTHER_ROOM), channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void SUBSCRIBE_로그인_안한_세션은_거절한다() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/sub/chat/" + MY_ROOM);
        accessor.setSessionAttributes(new HashMap<>());

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    private Message<byte[]> subscribe(String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        Map<String, Object> session = new HashMap<>();
        session.put("userId", ME);
        accessor.setSessionAttributes(session);
        return message(accessor);
    }

    private static Message<byte[]> message(StompHeaderAccessor accessor) {
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
