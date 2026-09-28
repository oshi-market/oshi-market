package com.oshimarket.global.config;

import com.oshimarket.global.security.JwtProvider;
import io.jsonwebtoken.JwtException;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtProvider jwtProvider;

    public WebSocketConfig(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub");
        registry.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new JwtHandshakeInterceptor(jwtProvider));
    }

    private record JwtHandshakeInterceptor(JwtProvider jwtProvider) implements ChannelInterceptor {

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor =
                    MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

            if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                String authHeader = accessor.getFirstNativeHeader("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    throw new JwtException("Authorization 헤더가 없습니다.");
                }
                Long userId = jwtProvider.extractUserId(authHeader.substring(7));
                accessor.getSessionAttributes().put("userId", userId);
            }
            return message;
        }
    }
}
