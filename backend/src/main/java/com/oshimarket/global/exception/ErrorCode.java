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
    DATA_CONFLICT(HttpStatus.CONFLICT, "다른 데이터와 연결되어 있어 처리할 수 없습니다."),
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    ITEM_FORBIDDEN(HttpStatus.FORBIDDEN, "본인이 등록한 상품만 수정/삭제할 수 있습니다."),
    ITEM_HAS_CHAT_OR_TRANSACTION(HttpStatus.CONFLICT, "채팅이나 거래가 진행된 상품은 삭제할 수 없습니다."),
    WORK_NOT_FOUND(HttpStatus.BAD_REQUEST, "등록되지 않은 작품입니다. 목록에서 선택해주세요."),
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "사진을 찾을 수 없습니다."),
    IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "사진은 상품당 최대 5장까지 등록할 수 있습니다."),
    IMAGE_INVALID_TYPE(HttpStatus.BAD_REQUEST, "사진은 JPG, PNG, WEBP 형식만 올릴 수 있습니다."),
    IMAGE_TOO_LARGE(HttpStatus.BAD_REQUEST, "사진은 한 장당 10MB 이하만 올릴 수 있습니다."),
    IMAGE_UPLOAD_FAILED(HttpStatus.BAD_GATEWAY, "사진 업로드에 실패했습니다. 잠시 후 다시 시도해주세요."),
    IMAGE_DELETE_FAILED(HttpStatus.BAD_GATEWAY, "사진 삭제에 실패했습니다."),
    IMAGE_STORAGE_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "사진 저장소가 설정되지 않았습니다. (CLOUDINARY_URL)"),
    CURATION_NOT_FOUND(HttpStatus.NOT_FOUND, "큐레이션 정보를 찾을 수 없습니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    CHAT_ROOM_FORBIDDEN(HttpStatus.FORBIDDEN, "본인이 참여한 채팅방만 이용할 수 있습니다.");

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
