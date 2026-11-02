# 배포 (보여 주기용, EC2 한 대)

목적: 포트폴리오·지원서에 넣을 **접속 주소**를 만드는 것. 운영 수준 백업·모니터링은 하지 않는다(데이터가 사라져도 괜찮은 데모).

## 구조

```
브라우저 ──80──▶ Nginx(frontend 컨테이너)
                 ├─ /          → React 빌드 파일
                 ├─ /api/**    → backend:8080
                 └─ /ws-chat   → backend:8080 (WebSocket Upgrade)
                                    │
                                    ▼
                              postgres:5432
```

- 외부에 여는 포트는 **80 하나**. DB와 백엔드는 컨테이너끼리만 통신한다.
- 화면과 API가 같은 주소라서 브라우저 입장에서 같은 출처 → **CORS 설정을 따로 하지 않아도 된다.**
- actuator는 Nginx가 넘기지 않아 외부에서 닿지 않고, `prod` 프로필에서도 `health`만 연다.

## 관련 파일

| 파일 | 하는 일 |
|---|---|
| `docker-compose.prod.yml` | postgres + backend + frontend(Nginx) 세 컨테이너 |
| `backend/Dockerfile` | Gradle로 jar 빌드 → JRE 이미지에서 실행 |
| `frontend/Dockerfile` | `npm run build` → Nginx 이미지에 결과물 복사 |
| `frontend/nginx.conf` | 정적 파일, `/api`·`/ws-chat` 프록시, 새로고침 시 index.html |
| `backend/src/main/resources/application-prod.yml` | 비밀값 기본값 제거, actuator health만 |
| `.env.prod.example` | 서버에서 채울 환경변수 목록 (`.env.prod`는 git 제외) |

## 1. EC2 준비 (처음 한 번)

1. 인스턴스: Ubuntu 24.04, **t3.small(메모리 2GB)** 이상 권장. t3.micro(1GB)는 Gradle 빌드 중 메모리가 모자랄 수 있다.
2. 보안 그룹 인바운드: **22(SSH, 내 IP만)**, **80(HTTP, 전체)**. 5432·8080은 열지 않는다.
3. 접속 후 Docker 설치:
   ```bash
   sudo apt-get update && sudo apt-get install -y docker.io docker-compose-v2 git
   sudo usermod -aG docker $USER   # 다시 로그인해야 적용
   ```
4. 메모리가 2GB면 스왑 2GB 추가(빌드 중 멈춤 방지):
   ```bash
   sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
   sudo mkswap /swapfile && sudo swapon /swapfile
   echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
   ```

## 2. 배포

```bash
git clone https://github.com/oshi-market/oshi-market.git && cd oshi-market
cp .env.prod.example .env.prod
# .env.prod 편집: DB_PASSWORD, JWT_SECRET(openssl rand -base64 48), CLOUDINARY_URL
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

확인:
```bash
docker compose -f docker-compose.prod.yml ps          # 세 개 모두 running
curl http://localhost/api/items                         # 상품 목록 JSON
```
브라우저에서 `http://<EC2 퍼블릭 IP>/` → 회원가입 → 상품 등록 → 다른 계정으로 채팅까지 확인.

## 3. 다시 배포 (main이 바뀌었을 때)

```bash
git pull
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```
DB 데이터는 `postgres_data` 볼륨에 남는다. 처음부터 비우려면 `down -v`.

## 4. 문제가 생기면

| 증상 | 확인 |
|---|---|
| backend가 계속 재시작 | `docker compose -f docker-compose.prod.yml logs backend` → `Error creating bean with name 'jwtTokenProvider'`이면 `.env.prod`의 JWT_SECRET이 비었거나 32바이트보다 짧음 (**의도된 안전장치**) |
| 채팅이 "연결 중..."에서 안 바뀜 | nginx.conf의 `/ws-chat`에 Upgrade 헤더가 있는지, 브라우저 개발자 도구 Network → WS |
| 사진 업로드 413 | nginx `client_max_body_size`(50m) |
| 사진 업로드 500 `IMAGE_STORAGE_NOT_CONFIGURED` | `.env.prod`의 CLOUDINARY_URL |

## 확인한 것 (11/2, 로컬 Docker)

- 세 이미지 빌드 성공, `up` 후 약 20초 만에 `/api/items` 200
- Nginx(80) 경유로 가입 → 로그인 → 상품 등록 → 채팅방 → STOMP 메시지 → 거래 요청 알림까지 정상
- 참여하지 않은 사람의 구독, 잘못된 토큰 CONNECT는 거절
- 백엔드 내부에서 `/actuator/health`는 UP, `/actuator/prometheus`는 404
- `JWT_SECRET`을 비우면 서버가 뜨지 않음
- 사진 업로드(Cloudinary)는 키가 없어 확인하지 않음

## 배포 전 체크리스트

- [ ] `.env.prod`에 DB_PASSWORD·JWT_SECRET을 **새로 만든 긴 값**으로 넣었다 (로컬 기본값 금지)
- [ ] 보안 그룹에 5432·8080이 열려 있지 않다
- [ ] 회원가입·로그인·상품 등록·사진 업로드·채팅·거래 요청까지 한 바퀴 확인했다
- [ ] (선택) 도메인 연결 + HTTPS(Let's Encrypt). 지금 구성은 HTTP만 — 데모용으로는 충분하지만 로그인 정보가 암호화되지 않는다
- [ ] (나중에) WebSocket `setAllowedOriginPatterns("*")`를 실제 도메인으로 좁히기 — PR #31이 같은 파일을 고치므로 머지 뒤에
