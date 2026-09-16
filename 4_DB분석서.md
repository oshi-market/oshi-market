# 4. DB 분석서 — 오시마켓 (Oshi Market)

ERD([`3_아키텍처및서비스흐름.md`](./3_아키텍처및서비스흐름.md))의 구조를 실제 스키마 수준(타입·제약조건·인덱스)으로 구체화하고, DB 운영 전략을 정리.

DB는 원래 MySQL로 확정했었으나, 팀이 MySQL은 충분히 다뤄봤으니 학습 목적으로 **PostgreSQL 16**으로 전환(2026-09-16, 외부 피드백). 스키마 설계 자체는 RDB 공통이라 큰 차이는 없고, 타입 표기(`BIGSERIAL`, `TIMESTAMP` 등)만 PostgreSQL 문법 기준.

## 1. 설계 원칙

- 3NF(제3정규형) 기준으로 설계, 중복 데이터를 최소화
- Item–Curation은 FK 대신 `work_tag`/`character_tag` 매칭 (설계 이유는 3번 문서 참고)
- 모든 PK는 `BIGSERIAL`(자동 증가), 모든 테이블에 `created_at` 포함 (감사 추적용)
- `USER`는 PostgreSQL 예약어라 테이블명은 `USER_ACCOUNT`로 사용

## 2. 테이블 상세 스키마

**USER_ACCOUNT**

| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| email | VARCHAR(255) | NOT NULL, UNIQUE |
| password | VARCHAR(255) | NOT NULL (BCrypt 해시 저장, 평문 저장 금지) |
| nickname | VARCHAR(50) | NOT NULL, UNIQUE |
| trust_score | INT | NOT NULL, DEFAULT 0 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

인덱스: `email`(UNIQUE, 로그인 조회), `nickname`(UNIQUE, 중복 가입 방지)

**ITEM**

| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| seller_id | BIGINT | NOT NULL, FK → USER_ACCOUNT(id) |
| title | VARCHAR(100) | NOT NULL |
| description | TEXT | |
| category | VARCHAR(50) | NOT NULL |
| work_tag | VARCHAR(50) | |
| character_tag | VARCHAR(50) | |
| price | INT | NOT NULL |
| condition | VARCHAR(20) | NOT NULL (ENUM: NEW, USED) |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'SELLING' (ENUM: SELLING, IN_TRANSACTION, SOLD_OUT) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

인덱스: `(category, work_tag, character_tag)` 복합 인덱스(검색/필터가 MVP 핵심 기능이라 우선순위 높음), `seller_id`(내 상품 조회), `status`

Elasticsearch 도입 전까지는 이 인덱스 + PostgreSQL 내장 풀텍스트 검색(`tsvector`)으로 검색을 처리 ([`2_기술스택분석서.md`](./2_기술스택분석서.md) 향후 확장 계획 참고).

**CHATROOM** (MVP 핵심, 담당: yjdev101)

| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| item_id | BIGINT | NOT NULL, FK → ITEM(id) |
| buyer_id | BIGINT | NOT NULL, FK → USER_ACCOUNT(id) |
| seller_id | BIGINT | NOT NULL, FK → USER_ACCOUNT(id) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

제약: `(item_id, buyer_id)` UNIQUE (같은 상품에 같은 구매자가 채팅방을 중복 생성하지 못하도록)
인덱스: `buyer_id`, `seller_id` (내 채팅방 목록 조회)

**MESSAGE** (MVP 핵심, 담당: yjdev101)

| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| chat_room_id | BIGINT | NOT NULL, FK → CHATROOM(id) |
| sender_id | BIGINT | NOT NULL, FK → USER_ACCOUNT(id) |
| content | VARCHAR(1000) | NOT NULL |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

인덱스: `(chat_room_id, created_at)` 복합 인덱스 (채팅방 대화 내역을 시간순 페이지네이션 조회)

이 테이블이 채팅 트래픽이 늘었을 때 가장 먼저 병목이 될 가능성이 높은 지점 — MongoDB/Redis 전환 검토 대상 ([`2_기술스택분석서.md`](./2_기술스택분석서.md) 향후 확장 계획 참고).

**TRANSACTION**

| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| item_id | BIGINT | NOT NULL, FK → ITEM(id) |
| chat_room_id | BIGINT | NOT NULL, FK → CHATROOM(id) (거래는 반드시 채팅방에서 생성) |
| buyer_id | BIGINT | NOT NULL, FK → USER_ACCOUNT(id) |
| seller_id | BIGINT | NOT NULL, FK → USER_ACCOUNT(id) |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'REQUESTED' (REQUESTED, IN_PROGRESS, COMPLETED, CANCELLED) |
| transacted_at | TIMESTAMP | NULL 허용 (완료 시점에 채움) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

인덱스: `item_id`, `chat_room_id`, `buyer_id`, `seller_id` (내 거래 내역 조회)

**item_id/chat_room_id는 UNIQUE가 아님(의도적)**: 거래가 취소(CANCELLED)된 뒤에는 같은 상품·같은 채팅방에서 새 거래를 다시 생성할 수 있어야 함. DB가 막아야 할 건 "이 상품에 평생 거래 1건"이 아니라 "이 상품에 **동시에 진행 중인** 거래가 2건 이상 생기는 것"이며, 이건 아래 동시성 제어(비관적 락 + `item.status` 체크)로 이미 처리됨. UNIQUE로 걸면 정상적인 재요청 시나리오까지 막혀버림. (2026-09-16 외부 피드백으로 수정)

**CURATION**

| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| work_tag | VARCHAR(50) | NOT NULL |
| character_tag | VARCHAR(50) | |
| store_name | VARCHAR(100) | NOT NULL |
| url | VARCHAR(500) | NOT NULL |
| period_start | DATE | |
| period_end | DATE | |

인덱스: `(work_tag, character_tag)` 복합 인덱스

**REVIEW** (2차 개발, 담당: xx-jhh)

| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| transaction_id | BIGINT | NOT NULL, UNIQUE, FK → TRANSACTION(id) |
| reviewer_id | BIGINT | NOT NULL, FK → USER_ACCOUNT(id) |
| rating | SMALLINT | NOT NULL (1~5) |
| comment | VARCHAR(500) | |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

**WISHLIST** (2차 개발)

| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGSERIAL | PK |
| user_id | BIGINT | NOT NULL, FK → USER_ACCOUNT(id) |
| item_id | BIGINT | NOT NULL, FK → ITEM(id) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

제약: `(user_id, item_id)` UNIQUE (중복 찜 방지)

## 3. 동시성 제어 — 동시 거래 요청 처리

**문제**: 중고거래 특성상 인기 상품에 여러 구매자가 동시에 "거래 요청"을 누르면, 하나의 Item에 두 개의 Transaction이 생기는 경합(race condition)이 발생할 수 있음.

**전략: 비관적 락(Pessimistic Lock)**
- 거래 요청 시 `SELECT ... FOR UPDATE`로 Item row를 잠그고, `status`가 `SELLING`인지 확인한 뒤 `IN_TRANSACTION`으로 변경 + Transaction 생성을 하나의 트랜잭션(`@Transactional`) 안에서 처리
- JPA: `@Lock(LockModeType.PESSIMISTIC_WRITE)`를 Item 조회 쿼리에 적용

**왜 낙관적 락(Optimistic Lock, `@Version`)이 아닌가**
- 낙관적 락은 충돌 시 애플리케이션에서 재시도 로직을 직접 구현해야 해서 복잡도가 올라감
- 이 프로젝트 규모(동시 접속자 적음)에서는 비관적 락으로 인한 대기 시간이 사용자 경험에 영향을 줄 정도로 크지 않음
- "정확성 우선, 단순한 구현"이 이 단계에서는 "높은 동시성 처리"보다 우선순위가 높다고 판단

## 4. 마이그레이션 관리

**Flyway** 채택 — SQL 파일(`V1__init.sql`, `V2__add_review.sql` ...) 기반이라 팀이 이미 아는 SQL 문법 그대로 사용 가능하고, Spring Boot가 기본 지원. PostgreSQL은 Flyway 코어가 기본 지원해 별도 모듈이 필요 없음. (대안 Liquibase는 XML/YAML 기반이라 학습 비용이 더 듦)

## 5. 커넥션 풀

Spring Boot 기본 내장 **HikariCP** 사용, 초기값(최대 커넥션 10)으로 시작. k6 부하 테스트([`2_기술스택분석서.md`](./2_기술스택분석서.md)) 결과를 보고 병목이 확인되면 조정.

## 6. 백업 전략

RDS 대신 EC2 컨테이너에 PostgreSQL을 직접 올리는 구조([`2_기술스택분석서.md`](./2_기술스택분석서.md) 배포 환경 참고)라 관리형 자동 백업이 없음. `pg_dump`를 cron으로 주기 실행해 EC2 로컬 또는 S3에 백업 (발표/MVP 단계에서는 최소 구현으로 충분, 실서비스 전환 시 RDS 이전 고려 대상).
