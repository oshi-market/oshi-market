package com.oshimarket.domain.item.entity;

/**
 * 상품 카테고리. 종류가 적고 거의 바뀌지 않아서 마스터 테이블 대신 enum으로 관리
 * (작품은 계속 늘어나므로 work 테이블로 관리). 화면 표시명은 프론트 constants.js에서 매핑.
 */
public enum ItemCategory {
    FIGURE,
    ACRYLIC,
    KEYRING,
    CAN_BADGE,
    PLUSH,
    PHOTO_CARD,
    TRADING_CARD,
    POSTER,
    FABRIC,
    MEDIA,
    ETC
}
