package com.oshimarket.domain.member.entity;

/**
 * 판매자 레벨. DB에는 경험치만 저장하고 레벨은 여기서 계산해서, 기준을 바꿀 때 이 표만 고치면 된다.
 * 레벨 이름(새싹 덕후 등)은 화면 표시용이라 프론트 constants.js에서 매핑 (카테고리와 같은 방식).
 * ★5 한 번 = +10 EXP 기준으로, 처음엔 금방 오르고 높은 레벨일수록 천천히 오르게 잡았다.
 */
public enum SellerLevel {
    LV1(1, 0),
    LV2(2, 20),
    LV3(3, 50),
    LV4(4, 100),
    LV5(5, 170),
    LV6(6, 260),
    LV7(7, 380),
    LV8(8, 530),
    LV9(9, 720),
    LV10(10, 1000);

    private final int level;
    private final int minExp;

    SellerLevel(int level, int minExp) {
        this.level = level;
        this.minExp = minExp;
    }

    public int getLevel() {
        return level;
    }

    public int getMinExp() {
        return minExp;
    }

    /** 경험치가 도달한 가장 높은 레벨. */
    public static SellerLevel of(int exp) {
        SellerLevel result = LV1;
        for (SellerLevel candidate : values()) {
            if (exp >= candidate.minExp) {
                result = candidate;
            }
        }
        return result;
    }

    /** 다음 레벨. 최고 레벨이면 null. */
    public SellerLevel next() {
        int nextIndex = ordinal() + 1;
        return nextIndex < values().length ? values()[nextIndex] : null;
    }
}
