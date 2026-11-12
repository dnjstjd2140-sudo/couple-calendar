# CC · 우리 CC할까?

둘이 같이 보는 공유 달력. 들어가면 바로 이번 달 달력이 나오고, 날짜를 누르면 그날 일정이 자세히 열린다.

- 달력 칸: 일정 제목만 짧게 (색 = 누구 일정인지: 파랑 첫 번째 사람 / 분홍 두 번째 사람 / 보라 같이)
- 날짜 누르기: 시간·장소·메모까지 자세히 + 추가/수정/삭제
- 공유: 상단 **링크 공유** → 복사된 `/c/<코드>` 링크를 상대에게 보내면 같은 달력을 같이 보고 수정
- 상대가 고친 내용은 30초마다, 그리고 앱으로 돌아올 때 자동으로 새로 불러온다
- ⚙ 설정: 두 사람 이름, "이 기기에서 나는 누구" (새 일정의 기본 주인)

> 링크 코드를 아는 사람은 누구나 보고 고칠 수 있다. 링크는 둘만 갖고 있을 것.
> 첫 화면(`/`)은 이 기기에서 마지막으로 연 달력으로 간다. 처음 여는 기기면 **새 달력을 만든다** —
> 그래서 상대는 `/` 가 아니라 공유받은 링크로 들어와야 한다.

## 구조

```
couple-calendar/
├─ client/   React + Vite + TypeScript (dev 5174)
├─ server/   Spring Boot 4 + JPA + PostgreSQL (8081)
├─ deploy/   nginx.conf, Dockerfile.web
└─ docker-compose.prod.yml   운영: db + app + web(8081 공개)
```

## API

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/calendars` | 새 달력 (링크 코드 발급) |
| GET | `/api/calendars/{code}` | 달력 정보 (이름) |
| PATCH | `/api/calendars/{code}` | 이름 변경 `{memberAName, memberBName}` |
| GET | `/api/calendars/{code}/events?from=YYYY-MM-DD&to=YYYY-MM-DD` | 기간과 겹치는 일정 |
| POST | `/api/calendars/{code}/events` | 일정 추가 |
| PUT | `/api/calendars/{code}/events/{id}` | 일정 수정 |
| DELETE | `/api/calendars/{code}/events/{id}` | 일정 삭제 |

일정 본문: `{title, startDate, endDate, startTime|null, endTime|null, location, memo, owner: "A"|"B"|"BOTH"}` (startTime 이 null 이면 하루 종일)

## 로컬 실행 (Windows)

Docker Desktop 을 켠 뒤:

```powershell
cd C:\workspace\couple-calendar\server
docker compose up -d          # Postgres (5433 포트, telpo 의 5432 와 안 겹침)
.\gradlew.bat bootRun         # API 서버 :8081
```

다른 터미널에서:

```powershell
cd C:\workspace\couple-calendar\client
npm install
npm run dev                   # http://localhost:5174
```

## 배포 (telpo 와 같은 Vultr 서버)

telpo 는 80 포트, 이 앱은 **8081 포트**로 같은 서버에 따로 올린다. 컨테이너·DB·볼륨이 모두 분리돼 있어서 서로 영향이 없다.

### 1) GitHub 에 올리기 (내 PC)

GitHub 에서 `couple-calendar` 레포를 **Private** 으로 만든 뒤:

```powershell
cd C:\workspace\couple-calendar
git init -b main
git add .
git commit -m "CC 공유 달력 첫 버전"
git remote add origin https://github.com/<내 계정>/couple-calendar.git
git push -u origin main
```

### 2) 서버에서 처음 세팅

```bash
ssh root@<서버 IP>
cd /opt && git clone https://github.com/<내 계정>/couple-calendar.git
cd /opt/couple-calendar
cp .env.example .env && nano .env      # DB_PASSWORD 를 긴 비밀번호로
ufw allow 8081/tcp                      # 8081 포트 열기
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml ps   # db / app / web 이 Up 인지 확인
```

접속: `http://<서버 IP>:8081` → 달력이 만들어지고 주소가 `/c/<코드>` 로 바뀐다 → **링크 공유** 로 상대에게 전달.

### 3) 코드 수정 후 다시 배포

```bash
cd /opt/couple-calendar && git pull
docker compose -f docker-compose.prod.yml up -d --build
```

### 메모리

서버 RAM 이 2GB 라 Spring Boot 힙을 256MB 로 제한해 뒀다 (`server/Dockerfile`).
`free -h`, `docker stats --no-stream` 으로 가끔 확인할 것.

## 나중에 시중에 낼 때 할 일

- 도메인 + HTTPS (Let's Encrypt). 지금은 `http://IP:8081` 이라 클립보드 API 등이 막혀서 복사는 예전 방식으로 대체돼 있다.
- 회원가입/로그인 (링크 유출 대비), 달력 링크 재발급
- DB 백업 (`pg_dump` 크론)
- 반복 일정, 알림, 공휴일 표시
