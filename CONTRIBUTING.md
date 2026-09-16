# CONTRIBUTING — 오시마켓 개발 가이드

Windows/Mac 혼용 2인 팀 기준. 기획 문서는 [`README.md`](./README.md)의 핵심 문서 표 참고.

## 로컬 개발 환경 준비

- JDK 17 (Windows: Adoptium/Corretto 설치, Mac: `brew install openjdk@17` 또는 sdkman)
- Node.js 20+ (프론트엔드)
- Docker Desktop (로컬 DB 실행용)

## 처음 세팅

1. `git clone https://github.com/oshi-market/oshi-market.git`
2. `docker compose up -d` — 로컬 MySQL 컨테이너 기동 (최초 1회 후, PC 켤 때마다 필요하면 재실행)
3. 백엔드 실행: `cd backend && ./gradlew bootRun` (Windows는 `gradlew.bat bootRun`)
4. 프론트엔드 실행: `cd frontend && npm install && cp .env.example .env && npm run dev`

## 프로젝트 구조

```
oshi-market/
 ├── backend/    Spring Boot (도메인별 패키지, 3_아키텍처및서비스흐름.md 참고)
 ├── frontend/   React + Vite (src/features/{member,item,chat,transaction,curation})
 └── *.md        기획 문서
```

## Git 브랜치 전략

- `main`은 항상 정상 동작하는 상태로 유지
- 기능/도메인 단위로 브랜치 생성 (예: `feature/chat`, `feature/member-auth`)
- 작업 완료 후 PR로 `main`에 병합 (2인 팀이라 정식 승인 없이 서로 diff 확인 후 머지)
- 로컬 동기화는 재-clone이 아니라 `git checkout main && git pull`

## 크로스플랫폼 주의사항 (Windows/Mac 혼용)

- **줄바꿈 문자**: 저장소 루트 `.gitattributes`가 LF로 강제 설정되어 있어 신경 쓸 필요 없음. 그냥 평소대로 커밋하면 됨
- **절대경로 금지**: 파일 업로드 등 구현 시 `C:\Users\...` 같은 Windows 전용 경로를 쓰지 말 것 (상대경로 또는 추후 S3 등 클라우드 스토리지 사용)
- **OS 잡파일**: `.DS_Store`(Mac), `Thumbs.db`(Windows)는 `.gitignore`에 이미 등록되어 있어 커밋될 걱정 없음
- **로컬 DB**: 각자 MySQL을 따로 설치하지 말고 반드시 `docker compose up -d`로 통일해서 실행 (버전 차이로 인한 "내 컴퓨터에선 되는데" 문제 방지)

## 공통 파일 수정 시 주의

아래 파일은 두 사람 작업이 동시에 겹치기 쉬운 지점이라, 수정 전 서로 얘기하고 진행할 것:

- `backend/build.gradle` (의존성 추가)
- `backend/src/main/resources/application.yml` (설정)
- `backend/src/main/resources/db/migration/*.sql` (Flyway — 새 마이그레이션 파일을 추가하는 형태로만, 기존 파일 수정 금지)
- `frontend/package.json` (패키지 추가)
- `frontend/src/App.jsx` (라우트 등록)
