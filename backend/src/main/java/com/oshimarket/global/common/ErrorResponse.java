package com.oshimarket.global.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.oshimarket.global.exception.ErrorCode;
import java.time.LocalDateTime;

/** 공통 에러 응답 포맷: { code, message, timestamp } */
public record ErrorResponse(
        String code,
        String message,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), LocalDateTime.now());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.name(), message, LocalDateTime.now());
    }
}
