-- 구매처 큐레이션 초기 데이터 (MVP 7번).
-- 기준: 권리자/공식 운영사가 직접 운영하는 스토어만, 스토어 메인 주소로 링크한다 (2026-10-12 접속 확인).
-- 저작권/상표 문제를 피하려고 텍스트(작품명·스토어명·URL)만 저장하고 로고·상품 이미지·가격은 넣지 않는다.
-- work_tag는 work.name과 정확히 같아야 필터가 걸린다 (V2 초기 작품 데이터 참고).
-- period_start/period_end가 NULL이면 상시 판매.

INSERT INTO curation (work_tag, character_tag, store_name, url) VALUES
    -- 점프 캐릭터즈 스토어 (슈에이샤 공식)
    ('귀멸의 칼날', NULL, '점프 캐릭터즈 스토어', 'https://jumpcs.shueisha.co.jp/'),
    ('주술회전', NULL, '점프 캐릭터즈 스토어', 'https://jumpcs.shueisha.co.jp/'),
    ('하이큐!!', NULL, '점프 캐릭터즈 스토어', 'https://jumpcs.shueisha.co.jp/'),
    ('원피스', NULL, '점프 캐릭터즈 스토어', 'https://jumpcs.shueisha.co.jp/'),
    ('나의 히어로 아카데미아', NULL, '점프 캐릭터즈 스토어', 'https://jumpcs.shueisha.co.jp/'),
    ('체인소맨', NULL, '점프 캐릭터즈 스토어', 'https://jumpcs.shueisha.co.jp/'),
    ('스파이 패밀리', NULL, '점프 캐릭터즈 스토어', 'https://jumpcs.shueisha.co.jp/'),
    -- 프리미엄 반다이 (반다이 공식)
    ('드래곤볼', NULL, '프리미엄 반다이', 'https://p-bandai.com/'),
    ('원피스', NULL, '프리미엄 반다이', 'https://p-bandai.com/'),
    -- 굿스마일 컴퍼니 공식 숍
    ('봇치 더 록!', NULL, '굿스마일 컴퍼니 공식 숍', 'https://www.goodsmile.com/'),
    ('장송의 프리렌', NULL, '굿스마일 컴퍼니 공식 숍', 'https://www.goodsmile.com/'),
    -- 작품 전용 공식 스토어
    ('신세기 에반게리온', NULL, '에반게리온 스토어', 'https://www.evastore.jp/'),
    ('포켓몬스터', NULL, '포켓몬센터 온라인', 'https://www.pokemoncenter-online.com/'),
    ('쿠키런: 킹덤', NULL, '쿠키런 스토어', 'https://www.cookierunstore.com/'),
    ('홀로라이브', NULL, 'hololive production 공식 숍', 'https://shop.hololivepro.com/'),
    ('니지산지', NULL, '니지산지 오피셜 스토어', 'https://shop.nijisanji.jp/');
