package com.oshimarket.domain.item.entity;

/** 거래 진행 상태. 등록 시 기본값 SELLING (4_DB분석서.md). */
public enum ItemStatus {
    SELLING,
    IN_TRANSACTION,
    SOLD_OUT
}
