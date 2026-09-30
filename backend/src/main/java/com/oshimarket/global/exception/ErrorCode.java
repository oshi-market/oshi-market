package com.oshimarket.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 공통 에러 코드. 응답 포맷은 { code, message, timestamp }
 * (3_아키텍처및서비스흐름.md 공통 예외처리 참고).
 */
public enum ErrorCode {

    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    ITEM_FORBIDDEN(HttpStatus.FORBIDDEN, "본인이 등록한 상품만 수정/삭제할 수 있습니다."),
    WORK_NOT_FOUND(HttpStatus.BAD_REQUEST, "등록되지 않은 작품입니다. 목록에서 선택해주세요."),
    CURATION_NOT_FOUND(HttpStatus.NOT_FOUND, "큐레이션 정보를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
