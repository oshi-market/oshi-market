package com.oshimarket.global.storage;

/**
 * 저장소에 올라간 이미지. url은 화면 표시용, publicId는 삭제할 때 필요한 저장소 내부 식별자.
 */
public record StoredImage(
        String url,
        String publicId
) {
}
