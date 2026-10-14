-- 찜(2차 개발 4번). wishlist 테이블은 V1에 이미 있다.
-- 누가 찜한 상품을 판매자가 지우면 fk_wishlist_item 때문에 삭제가 실패(409)했다.
-- 찜은 상품이 없어지면 의미가 없는 데이터라 상품과 함께 지워지게 한다.
ALTER TABLE wishlist DROP CONSTRAINT fk_wishlist_item;
ALTER TABLE wishlist
    ADD CONSTRAINT fk_wishlist_item FOREIGN KEY (item_id) REFERENCES item (id) ON DELETE CASCADE;

-- 내 찜 목록(user_id로 조회)은 uk_wishlist_user_item(user_id, item_id) 인덱스를 쓰고,
-- 상품별 찜 수(item_id로 COUNT)용 인덱스만 추가한다.
CREATE INDEX idx_wishlist_item ON wishlist (item_id);
