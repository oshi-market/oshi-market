package com.oshimarket.global.config;

import com.oshimarket.global.security.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * STOMP 인증·인가.
 * - CONNECT: Authorization: Bearer 헤더의 JWT 검증 → userId를 세션 속성과 Principal에 저장
 * - SUBSCRIBE /sub/chat/{chatRoomId}: 그 채팅방의 구매자·판매자만 구독 허용
 */
// STOMP 인바운드 인증/인가 (WebSocketConfig 안에 있던 인터셉터를 테스트할 수 있게 분리)
//  - CONNECT: JWT 검증 후 Principal(이름 = userId) 설정. convertAndSendToUser(userId, "/queue/errors")가 Principal 이름으로 배달되기 때문
//  - SUBSCRIBE: /sub/chat/{chatRoomId}는 그 채팅방 참여자만 구독 가능 (다른 사람 대화 구독 방지)
public class StompAuthInterceptor implements ChannelInterceptor {

    private static final Pattern CHAT_TOPIC = Pattern.compile("^/sub/chat/(\\d+)$");

    private final JwtTokenProvider jwtTokenProvider;
    /** (chatRoomId, memberId) → 참여자인지. ChatService#isParticipant를 넘긴다 (설정 클래스가 서비스에 직접 묶이지 않게). */
    private final BiPredicate<Long, Long> isParticipant;

    public StompAuthInterceptor(JwtTokenProvider jwtTokenProvider, BiPredicate<Long, Long> isParticipant) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.isParticipant = isParticipant;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscribe(accessor);
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new JwtException("Authorization 헤더가 없습니다.");
        }
        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            throw new JwtException("유효하지 않은 토큰입니다.");
        }
        Long userId = jwtTokenProvider.getMemberId(token);
        accessor.getSessionAttributes().put("userId", userId);
        accessor.setUser(new UsernamePasswordAuthenticationToken(String.valueOf(userId), null, List.of()));
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        Matcher matcher = destination == null ? null : CHAT_TOPIC.matcher(destination);
        if (matcher == null || !matcher.matches()) {
            return; // /user/queue/errors 같은 개인 큐는 Principal 기준이라 별도 검사 불필요
        }
        Object userId = accessor.getSessionAttributes() == null ? null : accessor.getSessionAttributes().get("userId");
        Long chatRoomId = Long.valueOf(matcher.group(1));
        if (!(userId instanceof Long memberId) || !isParticipant.test(chatRoomId, memberId)) {
            throw new MessageDeliveryException("본인이 참여한 채팅방만 구독할 수 있습니다.");
        }
    }
}
