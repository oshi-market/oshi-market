# 배포 (시연 환경: 홈 서버 + Cloudflare 터널)

목적: 포트폴리오·지원서에 넣을 **시연 주소**를 만드는 것. 운영 수준의 백업·모니터링은 하지 않는다(데이터가 사라져도 괜찮은 데모).

- 시연 주소: **https://oshi.jj-labs.dev**
- 서버: 집에 있는 우분투 서버 1대 (Ubuntu Server 26.04, RAM 8GB)
- EC2로 옮길 때 달라지는 점은 [맨 아래](#ec2로-옮길-때)에 정리했다.

## 구조

```
브라우저 ══ HTTPS ══▶ Cloudflare (oshi.jj-labs.dev)
                          ║ 터널 (서버가 먼저 바깥으로 건 연결)
                          ▼
                     cloudflared ──▶ Nginx(frontend) ──┬─ /          → React 빌드 파일
                                                       ├─ /api/**    → backend:8080
                                                       └─ /ws-chat   → backend:8080 (WebSocket)
                                                                          │
                                                                          ▼
                                                                    postgres:5432
```

- 외부에서 들어오는 길은 **Cloudflare 터널 하나뿐**이다. 공유기 포트를 열지 않고, 집 IP도 드러나지 않는다.
- HTTPS 인증서는 Cloudflare가 붙인다. 터널 → Nginx → Spring 구간은 서버 안이라 `http`다.
- Nginx(80)는 서버 자신(`127.0.0.1`)에서만 열린다. 백엔드(8080)·DB(5432)는 컨테이너끼리만 통신한다.
- actuator는 Nginx가 넘기지 않아 외부에서 닿지 않고, `health`만 열려 있다.

## 관련 파일

| 파일 | 하는 일 |
|---|---|
| `docker-compose.prod.yml` | postgres · backend · frontend(Nginx) · cloudflared 네 컨테이너, 자동 재시작, 로그 크기 제한 |
| `backend/Dockerfile` | 다단계 빌드: JDK로 jar 빌드 → JRE 이미지에서 실행 |
| `frontend/Dockerfile` | 다단계 빌드: Node로 `npm run build` → Nginx 이미지에 결과물만 복사 |
| `frontend/nginx.conf` | 정적 파일, `/api`·`/ws-chat` 프록시, 새로고침 시 `index.html` |
| `backend/src/main/resources/application-prod.yml` | DB 비밀번호 기본값 금지, CORS 허용 출처(배포 도메인) |
| `.env.prod.example` | 서버에서 채울 비밀값 목록 (`.env.prod`는 git에 올리지 않음) |

## 1. 서버 준비 (처음 한 번)

노트북에서 SSH로 접속해서 작업한다. 서버 화면(콘솔)에서 직접 치면 한글이 깨진다(콘솔에 한글 글꼴이 없음).

```bash
ssh -i ~/.ssh/id_ed25519_lenovo yjdev101@192.168.1.3
```

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y docker.io docker-compose-v2 git
sudo usermod -aG docker $USER      # 다시 로그인해야 sudo 없이 docker 사용 가능
systemctl is-enabled docker        # enabled → 부팅할 때 Docker가 자동으로 켜짐
```

- `docker.io`만 설치하면 `docker compose`가 `unknown command`로 나온다. `docker-compose-v2`를 같이 설치한다.

## 2. Cloudflare 준비 (처음 한 번)

1. 도메인: Cloudflare Registrar에서 구매하면 네임서버가 처음부터 Cloudflare라 기다릴 필요가 없다.
2. 터널 만들기: **Zero Trust → 네트워크 → 터널 → 터널 만들기**, 종류 `Cloudflared`, 이름 `home-server`, 환경 `Docker`
3. 화면에 나온 `docker run ... --token <토큰>`에서 **토큰만** 복사해 둔다. (채팅·문서에 붙여 넣지 않는다)
4. 경로(공개 호스트 이름) 추가는 **터널 컨테이너를 띄운 뒤**(3단계) 한다. 연결이 없으면 "연결이 감지되지 않는다"고 나온다.
   - 하위 도메인 `oshi`, 도메인 `jj-labs.dev`, 경로 칸은 비움
   - 서비스 URL: **`http://frontend:80`** (`localhost`가 아니라 compose 서비스 이름. `https`가 아니라 `http`)

## 3. 처음 배포

```bash
git clone https://github.com/oshi-market/oshi-market.git
cd oshi-market
cp .env.prod.example .env.prod
```

비밀값은 화면에 보이지 않게 넣는다.

```bash
# DB 비밀번호·JWT 키를 무작위로 만들어 바로 파일에 넣기
sed -i "s|^DB_PASSWORD=.*|DB_PASSWORD=$(openssl rand -hex 32)|" .env.prod
sed -i "s|^JWT_SECRET=.*|JWT_SECRET=$(openssl rand -base64 48)|" .env.prod

# 터널 토큰: 입력이 화면에 안 보이는 게 정상
read -rsp "토큰: " T && sed -i "s|^TUNNEL_TOKEN=.*|TUNNEL_TOKEN=$T|" .env.prod && unset T && echo " 저장 완료"

# 값은 숨기고, 채워졌는지 이름만 확인
grep -E "^(DB_PASSWORD|JWT_SECRET|TUNNEL_TOKEN)=.+" .env.prod | cut -d= -f1
```

- `CLOUDINARY_URL`은 사진 업로드용이다. 비워 두면 서버는 뜨고, 사진 업로드만 안내 메시지(503)가 나온다. 예시 값(`cloudinary://API_KEY...`)은 지우거나 실제 값으로 바꾼다.

띄우기:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

- 첫 빌드는 약 3분(167초) 걸렸다. 가장 오래 걸리는 단계는 Gradle 라이브러리 다운로드다.

확인:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod ps      # 4개 모두 Up, postgres는 (healthy)
docker compose -f docker-compose.prod.yml --env-file .env.prod logs backend | grep "Started"
docker compose -f docker-compose.prod.yml --env-file .env.prod logs cloudflared | grep -i "registered"   # 연결 4개
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1/                                              # 200
```

마지막으로 **휴대폰 데이터(LTE·5G)**로 `https://oshi.jj-labs.dev`에 접속해 회원가입 → 로그인 → 상품 등록 → 다른 계정으로 채팅까지 확인한다. 자동 점검 스크립트만으로는 부족하다(아래 CORS 사례).

## 4. 다시 배포 (코드가 바뀌었을 때)

```
노트북: 수정 → commit → push
서버:   git pull → docker compose ... up -d --build
```

```bash
git pull
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

- Docker는 바뀐 단계부터만 다시 빌드한다. `index.html`만 고쳤을 때는 **4초** 만에 끝났다(백엔드 전체·`npm ci`는 캐시 사용).
- `build.gradle`·`package.json`을 고치면 라이브러리부터 다시 받아서 오래 걸린다.
- 코드는 그대로이고 compose 설정만 바꿨으면 `--build` 없이 `up -d`만 해도 된다. 설정이 바뀐 컨테이너만 새로 만든다.
- DB 데이터는 `postgres_data` 볼륨에 남는다. 처음부터 비우려면 `down -v` (되돌릴 수 없음).

## 5. 자동 재시작·로그

- 모든 서비스가 `restart: unless-stopped`다. 직접 멈추지 않는 한, 꺼지면 다시 켠다.
  - 확인한 것: 백엔드를 강제로 종료(`sudo pkill -f app.jar`) → 몇 초 뒤 자동 재시작(`RestartCount` 0 → 1)
  - 확인한 것: 서버 재부팅(`sudo reboot`) → 아무 명령 없이 사이트가 다시 열림
- 로그 보기:
  ```bash
  docker compose -f docker-compose.prod.yml --env-file .env.prod logs -f --tail 20 backend   # Ctrl + C로 나오기
  ```
- 로그는 컨테이너마다 **10MB 파일 3개**까지만 남긴다(`x-logging`). Docker 기본값은 무제한이라 디스크가 가득 찰 수 있다.
  ```bash
  docker inspect -f '{{.HostConfig.LogConfig}}' oshi-market-backend-1   # {json-file map[max-file:3 max-size:10m]}
  ```

## 6. 문제가 생기면

| 증상 | 원인·확인 |
|---|---|
| 도메인에서 로그인 403, 채팅 "연결 중..."(웹소켓 502) | **CORS.** 터널 뒤에서 백엔드는 `http`로 받기 때문에, 브라우저 출처 `https://...`를 다른 출처로 판단한다. `application-prod.yml`의 `cors.allowed-origins`(또는 환경변수 `CORS_ALLOWED_ORIGINS`)에 실제 도메인이 있는지 확인. Origin 헤더를 보내지 않는 curl·스크립트로는 재현되지 않는다 |
| backend가 계속 재시작 | `logs backend`에서 `jwtTokenProvider` 오류면 `.env.prod`의 `JWT_SECRET`이 비었거나 32바이트보다 짧음 (**의도된 안전장치**) |
| Cloudflare에서 "연결이 감지되지 않음" | cloudflared 컨테이너가 안 떴거나 토큰이 틀림. `logs cloudflared`에서 `Registered tunnel connection` 확인 |
| 서비스 URL 형식 오류 | `frontend:80`이 아니라 `http://frontend:80`처럼 프로토콜까지 쓴다 |
| 집 안에서 `http://192.168.1.3`이 안 열림 | 정상. 80은 `127.0.0.1`에만 열어 두었다 |
| 사진 업로드 503 `IMAGE_STORAGE_NOT_CONFIGURED` | `.env.prod`의 `CLOUDINARY_URL` |
| 사진 업로드 413 | nginx `client_max_body_size`(50m) |
| Git Bash에 붙여 넣은 명령 앞에 `^[[200~`가 붙음 | 오른쪽 클릭 → Paste나 `Shift + Insert`로 붙여 넣거나 직접 친다 |

## 배운 점

- **Docker는 방화벽(ufw)을 건너뛴다.** ufw는 SSH만 열어 두었는데 `80:80`으로 띄우자 집 안 다른 기기에서 접속됐다. → `127.0.0.1:80:80`으로 묶고 외부는 터널로만.
- **같은 주소여도 HTTPS 프록시 뒤에서는 CORS가 문제 된다.** 집 안 `http` 주소에서는 같은 출처라 드러나지 않았다.
- **자동 점검이 다 통과해도 실제 브라우저로 확인해야 한다.** 점검 스크립트 33개가 통과했지만, 스크립트는 Origin을 보내지 않아 CORS를 잡지 못했다.

## EC2로 옮길 때

- 인스턴스: **t3.small(메모리 2GB)** 이상. t3.micro(1GB)는 Gradle 빌드 중 메모리가 모자랄 수 있다. 2GB면 스왑 2GB 추가:
  ```bash
  sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
  sudo mkswap /swapfile && sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
  ```
- 터널을 그대로 쓰면 보안 그룹 인바운드는 **22(SSH, 내 IP만)**만 열면 된다. 80·443·5432·8080은 열지 않는다.
- 도메인이 바뀌면 `CORS_ALLOWED_ORIGINS` 환경변수로 덮어쓴다.

## 배포 전 체크리스트

- [ ] `.env.prod`에 `DB_PASSWORD`·`JWT_SECRET`을 **새로 만든 긴 값**으로 넣었다 (로컬 기본값 금지)
- [ ] `TUNNEL_TOKEN`을 넣었고, 토큰을 채팅·문서에 붙여 넣지 않았다
- [ ] 80·5432·8080이 외부에서 직접 열리지 않는다
- [ ] 휴대폰 데이터로 회원가입·로그인·상품 등록·채팅·거래 요청까지 한 바퀴 확인했다
- [ ] 시연 전에 테스트 데이터·계정을 정리했다
- [ ] (나중에) WebSocket `setAllowedOriginPatterns("*")`를 실제 도메인으로 좁히기
- [ ] (나중에) `cloudflare/cloudflared:latest`를 버전 고정
