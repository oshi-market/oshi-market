# CLAUDE.md — 오시마켓 (Oshi Market)

서브컬쳐(애니메이션·게임) 굿즈 마니아층을 위한 중고거래 + 구매처 큐레이션 플랫폼. 팀 프로젝트.

자세한 기획 배경은 [`프로젝트_기획서.md`](./프로젝트_기획서.md) 참고.

## 핵심 문서

| 문서 | 내용 |
|---|---|
| [`1_MVP기획서.md`](./1_MVP기획서.md) | 서비스 흐름별 전체 기능 나열, 핵심/부가 기능 분류, MVP 스코프 |
| [`2_기술스택분석서.md`](./2_기술스택분석서.md) | 기술 스택 선정 및 선택 이유 |
| [`3_아키텍처및서비스흐름.md`](./3_아키텍처및서비스흐름.md) | 아키텍처 개요, 패키지 구조, ERD, API 명세, 서비스 흐름도 |
| [`4_DB분석서.md`](./4_DB분석서.md) | DB 스키마 상세, 동시성 제어, 마이그레이션/백업 전략 |

## 기술 스택

- 언어/프레임워크: **Java 17 + Spring Boot 4.0.8**
- 프론트엔드: **React**
- DB: **MySQL 8.0**
- 인증: **JWT (Spring Security)**
- 실시간 통신: **WebSocket + STOMP** (실시간 채팅)
- 빌드 툴: **Gradle**
- 배포: **Docker + AWS EC2**
- 모니터링: **Spring Boot Actuator + Prometheus + Grafana**
- 부하 테스트: **k6**

상세 선택 이유는 [`2_기술스택분석서.md`](./2_기술스택분석서.md) 참고.

## 아키텍처

도메인별 패키지 + 각 도메인 내부는 Controller-Service-Repository 레이어드 구조. 풀 DDD는 2인 팀 규모에 오버엔지니어링이라 채택하지 않음. 상세는 [`3_아키텍처및서비스흐름.md`](./3_아키텍처및서비스흐름.md) 참고.

## Git 브랜치 전략

- `main`은 항상 정상 동작하는 상태로 유지
- 기능/도메인 단위로 브랜치를 나눠 작업 (예: `feature/member-auth`, `feature/item-crud`)
- 작업 완료 후 PR로 `main`에 병합 (2인 팀이라 정식 승인 프로세스 대신 서로 diff 확인 후 머지)
- 로컬 동기화는 재-clone이 아니라 `git pull` 사용

## 현재 상태

기획 확정(MVP 스코프, 기술 스택, 아키텍처, DB 설계, 업무 분장) + 프로젝트 뼈대 세팅 완료. `backend/`(Spring Boot, 도메인별 패키지+Flyway 초기 마이그레이션)와 `frontend/`(React+Vite, feature별 폴더) 스켈레톤이 준비되어 있으므로 각자 담당 도메인 구현을 바로 시작하면 됨. 로컬 개발 환경 세팅은 [`CONTRIBUTING.md`](./CONTRIBUTING.md) 참고.
