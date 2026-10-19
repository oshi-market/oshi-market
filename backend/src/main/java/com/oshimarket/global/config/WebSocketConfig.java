package com.oshimarket.global.config;

import com.oshimarket.domain.chat.service.ChatService;
import com.oshimarket.global.security.JwtTokenProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;
    // 메시징 설정이 ChatService 빈보다 먼저 만들어질 수 있어 ObjectProvider로 늦게 조회
    private final ObjectProvider<ChatService> chatService;

    public WebSocketConfig(JwtTokenProvider jwtTokenProvider, ObjectProvider<ChatService> chatService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.chatService = chatService;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat").setAllowedOriginPatterns("*");
        registry.setErrorHandler(new StompErrorHandler());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub", "/queue");
        registry.setApplicationDestinationPrefixes("/pub");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new StompAuthInterceptor(jwtTokenProvider,
                (chatRoomId, memberId) -> chatService.getObject().isParticipant(chatRoomId, memberId)));
    }
}
