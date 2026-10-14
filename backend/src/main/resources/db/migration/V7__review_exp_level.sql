-- 리뷰(별점) → 판매자 경험치·레벨 (2차 개발 ⭐1).
-- 구매자가 거래 완료 후 판매자에게 별점(1~5)을 주면 별점에 따라 경험치가 오르거나 깎이고(ReviewExp),
-- 레벨은 경험치로 그때그때 계산한다(SellerLevel). 레벨 기준을 바꿔도 DB는 고칠 필요가 없게 경험치만 저장.

-- 1) 지금까지 쓰지 않던 trust_score(항상 0)를 경험치 칸으로 사용. 낮은 별점이 깎아도 0 아래로는 내려가지 않음
ALTER TABLE user_account RENAME COLUMN trust_score TO exp;
ALTER TABLE user_account
    ADD CONSTRAINT ck_user_account_exp_non_negative CHECK (exp >= 0);

-- 2) 평균 별점 표시용. 리뷰를 매번 다시 세지 않도록 받은 별점 개수·합계를 판매자 행에 함께 저장
--    (평균 = rating_sum / review_count). 경험치와 같은 UPDATE 한 번으로 함께 올린다.
ALTER TABLE user_account
    ADD COLUMN review_count INT NOT NULL DEFAULT 0,
    ADD COLUMN rating_sum INT NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_user_account_review_count CHECK (review_count >= 0),
    ADD CONSTRAINT ck_user_account_rating_sum CHECK (rating_sum >= 0);
