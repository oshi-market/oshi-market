-- 작품 마스터 테이블 + 상품 카테고리 enum 전환
-- item.work_tag / curation.work_tag는 FK 없이 문자열 유지 (work.name과 값을 맞춤). 4_DB분석서.md 참고.

CREATE TABLE work (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_work_name UNIQUE (name)
);

-- 초기 작품 데이터 (한국 정식 발매명 기준). 이후 작품 추가는 코드 수정 없이 INSERT.
INSERT INTO work (name) VALUES
    -- 애니메이션
    ('귀멸의 칼날'),
    ('주술회전'),
    ('하이큐!!'),
    ('체인소맨'),
    ('봇치 더 록!'),
    ('나루토'),
    ('원피스'),
    ('스파이 패밀리'),
    ('최애의 아이'),
    ('명탐정 코난'),
    ('진격의 거인'),
    ('나의 히어로 아카데미아'),
    ('도쿄 리벤저스'),
    ('장송의 프리렌'),
    ('약사의 혼잣말'),
    ('블루 록'),
    ('던전밥'),
    ('신세기 에반게리온'),
    ('은혼'),
    ('헌터×헌터'),
    ('드래곤볼'),
    ('슬램덩크'),
    ('문호 스트레이독스'),
    ('짱구는 못말려'),
    ('러브라이브!'),
    ('아이돌마스터'),
    -- 게임
    ('원신'),
    ('블루 아카이브'),
    ('프로젝트 세카이'),
    ('포켓몬스터'),
    ('붕괴: 스타레일'),
    ('우마무스메 프리티 더비'),
    ('명일방주'),
    ('젠레스 존 제로'),
    ('명조: 워더링 웨이브'),
    ('승리의 여신: 니케'),
    ('앙상블 스타즈!!'),
    ('페이트/그랜드 오더'),
    ('쿠키런: 킹덤'),
    ('트릭컬 리바이브'),
    -- 버추얼 유튜버
    ('홀로라이브'),
    ('니지산지');

-- 기존 자유 입력 카테고리를 ItemCategory enum 코드로 변환. CASE는 위에서부터 매칭되므로
-- 더 구체적인 조건(아크릴 키링 → KEYRING, 포켓몬 카드 → TRADING_CARD)을 먼저 둔다. 매칭 안 되면 ETC.
UPDATE item SET category = CASE
    WHEN category ILIKE '%피규어%' OR category ILIKE '%넨도%' THEN 'FIGURE'
    WHEN category ILIKE '%키링%' OR category ILIKE '%스트랩%' THEN 'KEYRING'
    WHEN category ILIKE '%아크릴%' OR category ILIKE '%아크스%' THEN 'ACRYLIC'
    WHEN category ILIKE '%뱃지%' OR category ILIKE '%배지%' THEN 'CAN_BADGE'
    WHEN category ILIKE '%인형%' OR category ILIKE '%누이%' THEN 'PLUSH'
    WHEN category ILIKE '%포켓몬 카드%' OR category ILIKE '%유희왕%' OR category ILIKE '%tcg%'
        OR category ILIKE '%트레이딩%' THEN 'TRADING_CARD'
    WHEN category ILIKE '%카드%' OR category ILIKE '%코롯타%' THEN 'PHOTO_CARD'
    WHEN category ILIKE '%포스터%' OR category ILIKE '%태피%' OR category ILIKE '%브로마이드%' THEN 'POSTER'
    WHEN category ILIKE '%쿠션%' OR category ILIKE '%담요%' OR category ILIKE '%티셔츠%'
        OR category ILIKE '%패브릭%' THEN 'FABRIC'
    WHEN category ILIKE '%음반%' OR category ILIKE '%앨범%' OR category ILIKE '%블루레이%'
        OR category ILIKE '%도서%' OR category ILIKE '%화집%' THEN 'MEDIA'
    ELSE 'ETC'
END
WHERE category NOT IN ('FIGURE', 'ACRYLIC', 'KEYRING', 'CAN_BADGE', 'PLUSH', 'PHOTO_CARD',
                       'TRADING_CARD', 'POSTER', 'FABRIC', 'MEDIA', 'ETC');
