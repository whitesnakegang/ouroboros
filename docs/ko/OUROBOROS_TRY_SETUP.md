# Ouroboros SDK Try 기능 설정 가이드

이 문서는 Ouroboros SDK의 Try 기능을 사용하기 위한 설정 방법을 안내합니다.

## 목차

1. [의존성 설정](#1-의존성-설정)
2. [애플리케이션 설정](#2-애플리케이션-설정)
3. [Tempo 연동 설정](#3-tempo-연동-설정)
4. [Try 기능 사용 방법](#4-try-기능-사용-방법)
5. [Gitignore 설정](#5-gitignore-설정)
6. [자주 묻는 질문 (QnA)](#6-자주-묻는-질문-qna)

---

## 1. 의존성 설정

### build.gradle

프로젝트의 `build.gradle` 파일에 Ouroboros SDK 의존성을 추가합니다.

```gradle
dependencies {
    implementation 'io.github.whitesnakegang:ouroboros:1.0.0'
}
```

> **참고**: Ouroboros SDK 버전은 최신 버전으로 확인하여 업데이트해야 합니다.

---

## 2. 애플리케이션 설정

### application.properties

`src/main/resources/application.properties` 파일에 다음 설정을 추가합니다.

#### 기본 Ouroboros 설정

```properties
# Ouroboros SDK 활성화
ouroboros.enabled=true
ouroboros.server.url=https://your-api-server.com
ouroboros.server.description=Your API Server Description
```

> **참고**: `ouroboros.server.url`과 `ouroboros.server.description`은 실제 프로젝트의 API 서버 정보로 변경해야 합니다.

#### Tempo 연동 설정

```properties
# Tempo 연동 활성화 (Ouroboros SDK에서 QueryQL 사용)
ouroboros.tempo.enabled=true
ouroboros.tempo.base-url=http://${TEMPO_HOST:localhost}:${TEMPO_UI_PORT:3200}
```

#### Method Tracing 설정

```properties
# Method Tracing 활성화
ouroboros.method-tracing.enabled=true
ouroboros.method-tracing.allowed-packages=your.package.name
```

> **참고**: `allowed-packages`에는 트레이싱을 적용할 패키지 경로를 지정합니다. 예: `com.example.yourproject`, `your.package.name` 등

#### OpenTelemetry Exporter 설정

```properties
# OpenTelemetry Exporter (App -> Tempo)
# HTTP 방식 사용 (포트 4318)
management.tracing.enabled=true
management.otlp.tracing.endpoint=http://${TEMPO_HOST:localhost}:${TEMPO_HTTP_PORT:4318}/v1/traces

# Micrometer Tracing
management.tracing.sampling.probability=1.0
```

> **참고**: `${TEMPO_HOST:localhost}`와 `${TEMPO_HTTP_PORT:4318}`은 `.env` 파일에서 설정한 환경 변수를 참조합니다.  
> Try 기능은 HTTP 프로토콜을 통해 트레이스 데이터를 Tempo에 전송합니다.

#### 로깅 설정

```properties
# Ouroboros SDK 디버그 로그를 콘솔에 출력
logging.level.kr.co.ouroboros=DEBUG
```

---

## 3. Tempo 연동 설정

Try 기능은 **Grafana Tempo**와 연동되어 작동합니다. Tempo는 트레이스 데이터를 수집하고 저장하는 백엔드 서버입니다.

### 구성 파일 구조

```
.
├─ docker-compose.yml      # Tempo 컨테이너 설정
├─ tempo.yaml              # Tempo 설정 파일
├─ .env                    # Docker ↔ App 동기화 변수 (필수)
├─ tempo-data/             # 로컬 트레이스 데이터 저장 폴더 (자동 생성)
└─ src/main/resources/application.properties  # Spring Boot 앱 설정
```

### .env 파일 생성

프로젝트 루트(즉, `docker-compose.yml`이 있는 위치)에 `.env` 파일을 생성합니다.

```bash
# ====== Docker ↔ App 동기화 변수 ======
# Tempo 서버 주소 (Docker 컨테이너명 또는 localhost)
TEMPO_HOST=localhost

# Tempo 포트 (Docker Compose ↔ Spring Boot 동기화)
TEMPO_HTTP_PORT=4318  # OTLP HTTP (트레이스 데이터 전송)
TEMPO_UI_PORT=3200    # Tempo UI / Query API
```

> **참고**: 이 변수들은 Docker Compose와 Spring Boot 간의 포트/호스트 동기화를 위해 필수입니다.  
> 로컬 실행 시 → `TEMPO_HOST=localhost`  
> Docker 네트워크 내부 실행 시 → `TEMPO_HOST=tempo`

### Docker Compose 설정

프로젝트 루트에 `docker-compose.yml` 파일을 생성합니다.

```yaml
services:
  tempo:
    image: grafana/tempo:2.6.1
    container_name: tempo
    ports:
      - "${TEMPO_HTTP_PORT:-4318}:4318"  # OTLP HTTP (트레이스 데이터 수신)
      - "${TEMPO_UI_PORT:-3200}:3200"    # Tempo UI / Query API
    command: ["-config.file=/etc/tempo/tempo.yaml"]
    volumes:
      - ./tempo.yaml:/etc/tempo/tempo.yaml:ro
      - ./tempo-data:/var/tempo
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:3200/ready"]
      interval: 10s
      timeout: 3s
      retries: 10
    networks:
      - tempo-net

networks:
  tempo-net:
    driver: bridge
```

> **참고**: HTTP 프로토콜만 사용하므로 gRPC 포트(4317)는 필요하지 않습니다.

### Tempo 설정 파일

프로젝트 루트에 `tempo.yaml` 파일을 생성합니다:

```yaml
server:
  http_listen_port: 3200

storage:
  trace:
    backend: local
    local:
      path: /var/tempo/traces

distributor:
  receivers:
    otlp:
      protocols:
        http:
          endpoint: 0.0.0.0:4318
```

> **참고**: HTTP 프로토콜만 사용하므로 gRPC 프로토콜 설정은 제거했습니다.  
> 이 파일은 Ouroboros SDK에 의해 자동으로 관리될 수 있습니다. 프로젝트 루트에 `tempo.yaml` 파일을 생성하거나, 필요에 따라 수정할 수 있습니다.

### Tempo 실행

```bash
# Tempo 컨테이너 실행
docker compose up -d

# 실행 확인
docker ps
docker logs tempo -f
```

---

## 4. Try 기능 사용 방법

### Try 요청 방법

Try 기능을 사용하려면 API 요청 시 `X-Ouroboros-Try: on` 헤더를 추가합니다.

#### 예시: cURL

```bash
curl -X GET "http://localhost:8080/api/your-endpoint" \
  -H "X-Ouroboros-Try: on"
```

> **참고**: `localhost:8080`은 실제 애플리케이션 서버 주소와 포트로 변경해야 합니다.

#### 예시: JavaScript (Fetch API)

```javascript
fetch('http://localhost:8080/api/your-endpoint', {
  headers: {
    'X-Ouroboros-Try': 'on'
  }
})
.then(response => response.json())
.then(data => {
  console.log('Try ID:', data._ouroborosTryId);
});
```

> **참고**: 실제 API 엔드포인트 URL로 변경해야 합니다.

### Try 결과 조회

Try 요청 후 생성된 `tryId`를 사용하여 결과를 조회할 수 있습니다.

```bash
GET /ouro/tries/{tryId}
```

#### 응답 예시

```json
{
  "tryId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "completed",
  "createdAt": "2024-01-01T00:00:00Z",
  "analyzedAt": "2024-01-01T00:00:01Z",
  "spans": [...],
  "issues": [...],
  "spanCount": 10
}
```

### 일반 요청 (Try 없이)

Try 헤더 없이 일반 요청을 보내면 정상적으로 동작하며, Try 관련 정보는 포함되지 않습니다.

```bash
curl -X GET "http://localhost:8080/api/your-endpoint"
```

### Try 기능 동작 원리

1. **요청 감지**: `X-Ouroboros-Try: on` 헤더가 포함된 요청을 감지
2. **Try ID 생성**: 고유한 Try ID (UUID) 생성
3. **트레이스 수집**: OpenTelemetry를 통해 요청 실행 과정의 모든 트레이스 수집
4. **Tempo 저장**: 수집된 트레이스 데이터를 Tempo에 전송 및 저장
5. **결과 조회**: `/ouro/tries/{tryId}` 엔드포인트를 통해 분석 결과 조회

---

## 5. Gitignore 설정

### .gitignore 파일 설정

프로젝트의 `.gitignore` 파일에 다음 항목들을 추가하여 불필요한 파일이 Git에 커밋되지 않도록 합니다.

#### Tempo 관련 파일 및 디렉토리

```gitignore
# Tempo 데이터 디렉토리
tempo-data/
```

> **참고**: `tempo-data/` 디렉토리는 Tempo가 생성하는 트레이스 데이터를 저장하는 디렉토리입니다. 용량이 크고 로컬 환경에 따라 다르므로 Git에 커밋하지 않아야 합니다.

#### 환경 변수 파일

```gitignore
# 환경 변수 파일
.env
.env.local
.env.*.local
```

> **참고**: `.env` 파일에는 민감한 정보(API 키, 비밀번호 등)가 포함될 수 있으므로 Git에 커밋하지 않아야 합니다.  
> 대신 `.env.example` 파일을 만들어 필요한 환경 변수 목록만 공유하세요.

#### 전체 예시

프로젝트 루트의 `.gitignore` 파일에 다음 내용을 추가합니다:

```gitignore
# 기존 Gitignore 설정...

# ===== Ouroboros SDK Try 기능 관련 =====

# Tempo 데이터 디렉토리
tempo-data/

# 환경 변수 파일
.env
.env.local
.env.*.local

# Docker Compose 오버라이드
docker-compose.override.yml

# 로그 파일
*.log
logs/
```

### 주의사항

- `tempo.yaml`: 이 파일은 프로젝트 설정 파일이므로 **Git에 포함**해야 합니다. (팀원들과 공유해야 하는 설정)
- `docker-compose.yml`: Docker Compose 설정 파일이므로 **Git에 포함**해야 합니다.
- `tempo-data/`: Tempo 데이터 디렉토리는 **Git에 포함하지 않아야** 합니다. (로컬 데이터)
- `.env`: 환경 변수 파일은 **Git에 포함하지 않아야** 합니다. (민감한 정보)

> 💡 **이미 커밋된 경우**:
> ```bash
> git rm -r --cached tempo-data
> echo "tempo-data/" >> .gitignore
> git add .gitignore
> git commit -m "ignore tempo trace data directory"
> ```

---

## 체크리스트

설정이 완료되었는지 확인하기 위한 체크리스트입니다:

- [ ] `build.gradle`에 Ouroboros SDK 의존성 추가
- [ ] `.env` 파일 생성 및 Tempo 관련 환경 변수 설정
- [ ] `docker-compose.yml` 파일 생성 및 Tempo 서비스 설정
- [ ] `tempo.yaml` 파일 생성 및 Tempo 설정
- [ ] `application.properties`에 Ouroboros 기본 설정 추가
- [ ] `application.properties`에 Tempo 연동 설정 추가
- [ ] `application.properties`에 Method Tracing 설정 추가
- [ ] `application.properties`에 OpenTelemetry Exporter 설정 추가
- [ ] `.gitignore`에 `tempo-data/`, `.env` 추가
- [ ] Tempo 서버 실행 (`docker compose up -d`)
- [ ] 애플리케이션 실행 및 Try 요청 테스트

---

## 문제 해결

### Try 기능이 동작하지 않는 경우

1. **의존성 확인**: `build.gradle`에 Ouroboros SDK가 정상적으로 추가되었는지 확인
2. **설정 확인**: `application.properties`의 `ouroboros.enabled=true` 설정 확인
3. **헤더 확인**: 요청에 `X-Ouroboros-Try: on` 헤더가 정확히 포함되었는지 확인
4. **로깅 확인**: `logging.level.kr.co.ouroboros=DEBUG` 설정으로 상세 로그 확인

### Tempo 연동이 안 되는 경우

1. **Tempo 실행 확인**: Docker Compose로 Tempo가 정상 실행 중인지 확인
   ```bash
   docker ps
   docker logs tempo -f
   ```
2. **포트 확인**: `.env` 파일의 `TEMPO_HTTP_PORT`와 `application.properties`의 `management.otlp.tracing.endpoint` 설정 일치 확인
   - HTTP 포트는 4318이어야 합니다
   - `application.properties`에서 `http://` 프로토콜을 사용하는지 확인
3. **환경 변수 확인**: `.env` 파일이 프로젝트 루트에 있고 올바른 형식인지 확인
   - `TEMPO_HTTP_PORT=4318`이 설정되어 있는지 확인
   - `TEMPO_UI_PORT=3200`이 설정되어 있는지 확인
4. **네트워크 확인**: 애플리케이션에서 Tempo 서버로의 네트워크 연결 확인
   - 로컬 실행 시: `TEMPO_HOST=localhost`
   - Docker 네트워크 실행 시: `TEMPO_HOST=tempo`
5. **Tempo UI 접속**: [http://localhost:3200](http://localhost:3200)에서 Tempo UI가 정상적으로 표시되는지 확인
6. **프로토콜 확인**: HTTP 프로토콜만 사용하므로 gRPC 관련 설정이 없는지 확인

---

## 6. 자주 묻는 질문 (QnA)

### OTLP 포트 변경

**Q. OTLP 포트를 바꿀 수 있나요?**  
A. 가능은 하지만 권장하지 않습니다. 4317(gRPC), 4318(HTTP)은 OpenTelemetry 표준 포트입니다.

### Tempo UI 포트 변경

**Q. Tempo UI 포트를 바꾸고 싶어요.**  
A. `.env` 파일에서 `TEMPO_UI_PORT=3300`으로 변경 후 `docker compose down && docker compose up -d`로 재시작하세요.

### 트레이스 데이터 저장 위치

**Q. 트레이스 데이터는 어디에 저장되나요?**  
A. `./tempo-data/` 폴더에 로컬 저장됩니다.  
   운영 환경에서는 `tempo.yaml`을 수정해 S3 또는 MinIO 같은 객체 스토리지로 전환하세요.

---

## 참고 자료

- [Ouroboros SDK 공식 문서](https://github.com/whitesnakegang/ouroboros)
- [OpenTelemetry 문서](https://opentelemetry.io/docs/)
- [Grafana Tempo 문서](https://grafana.com/docs/tempo/latest/)

---

## 🚀 빠른 시작 요약

1. **의존성 추가**: `build.gradle`에 Ouroboros SDK 추가
2. **환경 변수 설정**: `.env` 파일 생성
3. **Docker 설정**: `docker-compose.yml`, `tempo.yaml` 생성
4. **애플리케이션 설정**: `application.properties`에 Tempo 연동 설정 추가
5. **Gitignore 설정**: `.gitignore`에 `tempo-data/`, `.env` 추가
6. **실행**: `docker compose up -d`로 Tempo 실행 후 애플리케이션 실행
7. **Try 요청**: `X-Ouroboros-Try: on` 헤더로 API 요청
8. **결과 조회**: `/ouro/tries/{tryId}`로 분석 결과 확인

