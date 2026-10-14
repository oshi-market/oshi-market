package com.oshimarket.domain.review.entity;

/**
 * 별점 → 경험치 환산표. 낮은 별점은 경험치를 깎아서 거래만 많은 나쁜 판매자가 레벨로 구별되게 한다.
 * 숫자를 조정할 때는 이 표만 고치면 된다 (이미 반영된 경험치는 바뀌지 않음).
 */
public final class ReviewExp {

    private ReviewExp() {
    }

    public static int of(int rating) {
        return switch (rating) {
            case 1 -> -5;
            case 2 -> -2;
            case 3 -> 2;
            case 4 -> 6;
            case 5 -> 10;
            default -> throw new IllegalArgumentException("별점은 1~5만 가능합니다: " + rating);
        };
    }
}
