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
 *   (Principal 이름 = userId. convertAndSendToUser(userId, "/queue/errors")가 Principal 이름으로 배달되기 때문)
 * - SUBSCRIBE: 허용 목록 방식. /sub/chat/{chatRoomId}(그 방 구매자·판매자만)와 /user/queue/errors만 허용.
 *   SimpleBroker는 구독 주소의 * / ** 패턴을 받아 주므로, 모양이 다른 주소를 통과시키면 /sub/chat/* 로 모든 방을 엿볼 수 있다.
 * - SEND: /pub/** 만 허용. /sub/** 로 직접 보내면 컨트롤러 검사 없이 브로커가 바로 뿌려 남의 방에 가짜 메시지를 넣을 수 있다.
 * WebSocketConfig 안에 있던 인터셉터를 테스트할 수 있게 분리했다.
 */
public class StompAuthInterceptor implements ChannelInterceptor {

    private static final Pattern CHAT_TOPIC = Pattern.compile("^/sub/chat/(\\d+)$");
    private static final String ERROR_QUEUE = "/user/queue/errors";
    private static final String APP_PREFIX = "/pub/";

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
        } else if (StompCommand.SEND.equals(accessor.getCommand())) {
            authorizeSend(accessor);
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
        if (ERROR_QUEUE.equals(destination)) {
            return; // 개인 큐는 Principal 기준으로 배달되어 남의 것을 받을 수 없음
        }
        Matcher matcher = destination == null ? null : CHAT_TOPIC.matcher(destination);
        if (matcher == null || !matcher.matches()) {
            throw new MessageDeliveryException("구독할 수 없는 주소입니다: " + destination);
        }
        Object userId = accessor.getSessionAttributes() == null ? null : accessor.getSessionAttributes().get("userId");
        Long chatRoomId = Long.valueOf(matcher.group(1));
        if (!(userId instanceof Long memberId) || !isParticipant.test(chatRoomId, memberId)) {
            throw new MessageDeliveryException("본인이 참여한 채팅방만 구독할 수 있습니다.");
        }
    }

    private void authorizeSend(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(APP_PREFIX)) {
            throw new MessageDeliveryException("메시지는 /pub/ 주소로만 보낼 수 있습니다: " + destination);
        }
    }
}
