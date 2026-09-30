-- DB 보강 (DB 점검 항목 2, 6, 7, 8, 10 + 한글 정렬). 4_DB분석서.md 참고.

-- ---------------------------------------------------------------------------
-- [7] 시간대: DB 세션 기본 시간대를 한국 시간으로 고정.
-- 컨테이너 기본값(UTC)과 앱(KST)이 달라 DB DEFAULT CURRENT_TIMESTAMP와 앱이 만든 시각이 9시간 어긋나던 문제.
-- 앱 JVM 시간대도 OshiMarketApplication에서 Asia/Seoul로 고정. (TIMESTAMPTZ 전환은 엔티티 수정이 필요해 후속 작업)
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    EXECUTE format('ALTER DATABASE %I SET timezone TO %L', current_database(), 'Asia/Seoul');
END
$$;
SET timezone TO 'Asia/Seoul';

-- ---------------------------------------------------------------------------
-- [6] CHECK 제약: 애플리케이션 검증을 우회한 잘못된 값(음수 가격, 별점 999 등)을 DB에서 차단
-- ---------------------------------------------------------------------------
ALTER TABLE item
    ADD CONSTRAINT ck_item_price CHECK (price > 0),
    ADD CONSTRAINT ck_item_condition CHECK (condition IN ('NEW', 'USED')),
    ADD CONSTRAINT ck_item_status CHECK (status IN ('SELLING', 'IN_TRANSACTION', 'SOLD_OUT'));

ALTER TABLE review
    ADD CONSTRAINT ck_review_rating CHECK (rating BETWEEN 1 AND 5);

ALTER TABLE item_image
    ADD CONSTRAINT ck_item_image_sort_order CHECK (sort_order >= 0);

-- ---------------------------------------------------------------------------
-- 한글 정렬: 기본 collation(en_US.utf8)은 한글이 가나다순으로 정렬되지 않아
-- 이름성 컬럼에 한국어 ICU collation 지정 (ORDER BY name이 가나다순이 됨)
-- ---------------------------------------------------------------------------
ALTER TABLE user_account ALTER COLUMN nickname TYPE VARCHAR(50) COLLATE "ko-KR-x-icu";
ALTER TABLE item ALTER COLUMN title TYPE VARCHAR(100) COLLATE "ko-KR-x-icu";
ALTER TABLE item ALTER COLUMN work_tag TYPE VARCHAR(50) COLLATE "ko-KR-x-icu";
ALTER TABLE item ALTER COLUMN character_tag TYPE VARCHAR(50) COLLATE "ko-KR-x-icu";
ALTER TABLE work ALTER COLUMN name TYPE VARCHAR(50) COLLATE "ko-KR-x-icu";
ALTER TABLE curation ALTER COLUMN work_tag TYPE VARCHAR(50) COLLATE "ko-KR-x-icu";
ALTER TABLE curation ALTER COLUMN character_tag TYPE VARCHAR(50) COLLATE "ko-KR-x-icu";
ALTER TABLE curation ALTER COLUMN store_name TYPE VARCHAR(100) COLLATE "ko-KR-x-icu";

-- ---------------------------------------------------------------------------
-- [2] 한국어 검색: tsvector는 한국어 형태소 분석기가 없어 제대로 동작하지 않으므로,
-- 현재 검색 방식(title ILIKE '%키워드%')을 그대로 가속하는 trigram GIN 인덱스 사용
-- ---------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_item_title_trgm ON item USING GIN (title gin_trgm_ops);

-- ---------------------------------------------------------------------------
-- [8] 인덱스: 실제 쿼리(최신순 정렬 + 필터)에 맞춤
-- ---------------------------------------------------------------------------
-- 상품 목록/홈 "방금 올라온 굿즈" (created_at DESC 정렬)
CREATE INDEX idx_item_created ON item (created_at DESC);
-- 내 상품 (seller_id 필터 + 최신순) — 기존 seller_id 단일 인덱스 대체
CREATE INDEX idx_item_seller_created ON item (seller_id, created_at DESC);
DROP INDEX idx_item_seller;
-- status 단일 인덱스는 값 종류가 3개뿐이라 선택도가 낮아 거의 쓰이지 않음
DROP INDEX idx_item_status;
-- 내 거래 내역 (구매자/판매자 + 최신순) — 기존 단일 인덱스 대체
CREATE INDEX idx_transaction_buyer_created ON transaction (buyer_id, created_at DESC);
CREATE INDEX idx_transaction_seller_created ON transaction (seller_id, created_at DESC);
DROP INDEX idx_transaction_buyer;
DROP INDEX idx_transaction_seller;

-- ---------------------------------------------------------------------------
-- [10] updated_at: 수정 가능한 테이블에 추가. 트리거로 UPDATE 시 자동 갱신하므로
-- 엔티티/서비스 코드를 고치지 않아도 항상 채워진다 (기존 행은 created_at으로 초기화)
-- ---------------------------------------------------------------------------
CREATE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

ALTER TABLE user_account ADD COLUMN updated_at TIMESTAMP;
UPDATE user_account SET updated_at = created_at;
ALTER TABLE user_account ALTER COLUMN updated_at SET NOT NULL, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
CREATE TRIGGER trg_user_account_updated_at BEFORE UPDATE ON user_account
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

ALTER TABLE item ADD COLUMN updated_at TIMESTAMP;
UPDATE item SET updated_at = created_at;
ALTER TABLE item ALTER COLUMN updated_at SET NOT NULL, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
CREATE TRIGGER trg_item_updated_at BEFORE UPDATE ON item
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

ALTER TABLE transaction ADD COLUMN updated_at TIMESTAMP;
UPDATE transaction SET updated_at = created_at;
ALTER TABLE transaction ALTER COLUMN updated_at SET NOT NULL, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
CREATE TRIGGER trg_transaction_updated_at BEFORE UPDATE ON transaction
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

ALTER TABLE review ADD COLUMN updated_at TIMESTAMP;
UPDATE review SET updated_at = created_at;
ALTER TABLE review ALTER COLUMN updated_at SET NOT NULL, ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
CREATE TRIGGER trg_review_updated_at BEFORE UPDATE ON review
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- curation은 created_at이 없어 현재 시각으로 초기화
ALTER TABLE curation ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
CREATE TRIGGER trg_curation_updated_at BEFORE UPDATE ON curation
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
