package com.oshimarket.global.exception;

import com.oshimarket.global.common.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                // 타입 변환 실패(예: GET /api/items?category=FOO)는 기본 메시지에 내부 클래스명이 노출돼서 대체
                .map(error -> error.getField() + ": "
                        + (error.isBindingFailure() ? "올바르지 않은 값입니다." : error.getDefaultMessage()))
                .orElse(ErrorCode.INVALID_INPUT.getMessage());

        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, message));
    }

    /** JSON 파싱 실패 (예: condition에 NEW/USED가 아닌 값) 시에도 공통 에러 포맷으로 응답. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT));
    }

    /** 쿼리 파라미터 타입 변환 실패 (예: role에 BUY/SELL이 아닌 값) 시에도 공통 에러 포맷으로 응답. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT));
    }

    /** 사진 용량 초과 (spring.servlet.multipart.max-file-size / max-request-size). */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(ErrorCode.IMAGE_TOO_LARGE.getStatus())
                .body(ErrorResponse.of(ErrorCode.IMAGE_TOO_LARGE));
    }

    /** multipart 요청에 files 파트가 없을 때. */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException e) {
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, "업로드할 사진을 선택해주세요."));
    }

    /**
     * FK/UNIQUE 등 DB 제약 위반 안전망. 서비스에서 미리 막지 못한 경우에도 500 대신 409로 응답하고,
     * 원인 파악을 위해 로그는 남긴다 (예: 찜 기능 구현 후 wishlist가 참조 중인 상품 삭제).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("DB 제약 위반: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(ErrorCode.DATA_CONFLICT.getStatus())
                .body(ErrorResponse.of(ErrorCode.DATA_CONFLICT));
    }

    /** 페이지 조회의 sort에 없는 필드를 넣은 경우 (예: GET /api/items?sort=foo). */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handlePropertyReference(PropertyReferenceException e) {
        return ResponseEntity.status(ErrorCode.INVALID_SORT.getStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_SORT));
    }

    /**
     * 위에서 처리하지 못한 모든 예외의 최후 안전망. 이게 없으면 예외가 /error로 넘어가 Spring 기본 포맷으로
     * 응답돼서 프론트의 공통 에러 처리({code, message})가 깨진다.
     * Spring MVC가 던지는 요청 오류(없는 주소 404, 지원 안 하는 메서드 405 등)는 원래 상태코드를 유지하고,
     * 그 외에는 원인 파악용 로그를 남긴 뒤 500으로 응답한다 (내부 메시지는 응답에 노출하지 않음).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        if (e instanceof org.springframework.web.ErrorResponse springError
                && springError.getStatusCode().is4xxClientError()) {
            HttpStatusCode status = springError.getStatusCode();
            ErrorCode errorCode = switch (status.value()) {
                case 404 -> ErrorCode.NOT_FOUND;
                case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
                default -> ErrorCode.INVALID_INPUT;
            };
            return ResponseEntity.status(status).body(ErrorResponse.of(errorCode));
        }

        log.error("처리되지 않은 예외", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
