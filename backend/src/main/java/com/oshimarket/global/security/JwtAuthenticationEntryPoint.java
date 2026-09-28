package com.oshimarket.global.security;

import com.oshimarket.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 인증 실패(토큰 없음/무효) 시 Security 기본 응답 대신 공통 에러 포맷({code, message, timestamp})으로
 * 401을 내려준다. 이 필터 단계는 MVC의 @RestControllerAdvice(GlobalExceptionHandler)보다 앞서 동작해서
 * 그쪽을 타지 않으므로 여기서 직접 JSON을 만든다. Jackson ObjectMapper 빈 주입은 Boot 4의 Jackson
 * 2/3 공존 구조상 타입이 갈릴 수 있어(둘 다 클래스패스에 존재) 일부러 의존하지 않고 문자열로 조립한다.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        String body = """
                {"code":"%s","message":"%s","timestamp":"%s"}""".formatted(
                errorCode.name(), errorCode.getMessage(), LocalDateTime.now()
        );
        response.getWriter().write(body);
    }
}
