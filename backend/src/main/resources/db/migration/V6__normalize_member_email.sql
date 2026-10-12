-- 이메일 대소문자 중복 가입 방지. 앱에서 가입·로그인 시 이메일을 LOWER(TRIM())로 맞추고(Member#normalizeEmail),
-- DB에도 같은 규칙을 둬서 다른 경로로 대소문자가 섞인 값이 들어오지 못하게 한다.

-- 1) 기존 데이터 정규화. 이미 같은 이메일(대소문자만 다른 것)이 있는 행은 UNIQUE 위반이 나므로 건너뛴다.
--    (개발 DB에만 있을 수 있는 경우라 자동으로 합치지 않음. 남은 행은 로그인이 안 되니 직접 정리 필요)
UPDATE user_account u
SET email = LOWER(TRIM(u.email))
WHERE u.email <> LOWER(TRIM(u.email))
  AND NOT EXISTS (
      SELECT 1 FROM user_account o
      WHERE o.id <> u.id AND LOWER(TRIM(o.email)) = LOWER(TRIM(u.email))
  );

-- 2) 앞으로 저장되는 이메일은 정규화된 값만 허용. NOT VALID라 1)에서 건너뛴 기존 행이 있어도 마이그레이션은
--    실패하지 않고, 새로 넣거나 바꾸는 행부터 검사한다. 정규화된 값끼리는 기존 uk_user_email(UNIQUE)이
--    동시 가입까지 막아준다.
ALTER TABLE user_account
    ADD CONSTRAINT ck_user_account_email_normalized CHECK (email = LOWER(TRIM(email))) NOT VALID;
