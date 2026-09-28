package com.oshimarket.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // TODO(chat): member-auth의 JWT 필터가 merge되면 /api/** 인가 규칙까지 여기서 함께 정리 필요.
    // 지금은 채팅 기능 동작 확인을 위해 /ws-chat만 우선 열어둠 (실제 인증은 STOMP CONNECT 단계의 JwtProvider가 담당).
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/ws-chat/**").permitAll()
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}
