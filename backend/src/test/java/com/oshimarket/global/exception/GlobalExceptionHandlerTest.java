package com.oshimarket.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.oshimarket.global.common.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleUnexpected_예상못한_예외는_500과_공통포맷으로_응답한다() {
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(new IllegalStateException("내부 메시지"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_SERVER_ERROR");
        // 내부 예외 메시지는 사용자에게 노출하지 않는다
        assertThat(response.getBody().message()).doesNotContain("내부 메시지");
    }

    @Test
    void handleUnexpected_Spring_요청오류는_원래_상태코드를_유지한다() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnexpected(new HttpRequestMethodNotSupportedException("PUT"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody().code()).isEqualTo("METHOD_NOT_ALLOWED");
    }
}
