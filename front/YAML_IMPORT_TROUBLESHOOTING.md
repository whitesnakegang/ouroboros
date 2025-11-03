# YAML Import 트러블슈팅 가이드

## 🔍 에러 확인 방법

### 1. 브라우저 개발자 도구 열기
- **Chrome/Edge**: `F12` 또는 `Ctrl + Shift + I`
- **Firefox**: `F12`

### 2. Console 탭 확인
Import 시도 시 다음과 같은 로그가 표시됩니다:

```javascript
YAML Import 시작: {
  fileName: "api-spec.yml",
  fileSize: 1234,
  fileType: "application/x-yaml",
  endpoint: "/ouro/rest-specs/import"
}

YAML Import 응답: {
  status: 400,  // 또는 500
  statusText: "Bad Request",
  ok: false
}
```

### 3. Network 탭 확인
1. Network 탭 클릭
2. Import 버튼 클릭 후 파일 선택
3. `import` 요청 찾기
4. 클릭하여 상세 정보 확인
   - **Headers**: 요청 헤더 확인
   - **Payload**: 업로드된 파일 확인
   - **Response**: 서버 응답 확인

---

## ❌ 주요 에러 유형

### 1️⃣ 백엔드 서버 미실행 (500 Internal Server Error)
**증상:**
```
GET http://localhost:5173/ouro/rest-specs/import 500 (Internal Server Error)
```

**원인:**
- 백엔드 서버가 실행되지 않음
- 백엔드가 8080 포트가 아닌 다른 포트에서 실행 중

**해결:**
1. 백엔드 서버 실행 확인:
   ```bash
   # backend 디렉토리에서
   ./gradlew bootRun
   ```

2. 포트 확인:
   ```bash
   # Windows
   netstat -ano | findstr :8080
   
   # Linux/Mac
   lsof -i :8080
   ```

---

### 2️⃣ 파일 확장자 오류
**증상:**
```
YAML 파일(.yml 또는 .yaml)만 업로드 가능합니다.
```

**원인:**
- `.txt`, `.json` 등 잘못된 확장자

**해결:**
- `.yml` 또는 `.yaml` 확장자로 파일명 변경

---

### 3️⃣ YAML 검증 실패 (400 Bad Request)
**증상:**
```
YAML 검증 실패:
- openapi: OpenAPI version must be 3.x.x (found: 2.0.0)
- info.title: Missing required field 'info.title'
- paths./api/users.posts: Invalid HTTP method: 'posts'
```

**원인:**
- OpenAPI 버전이 3.x가 아님
- 필수 필드 누락
- 잘못된 HTTP 메소드 사용

**해결:**
테스트용 최소 YAML 파일 사용 (`front/test-import.yml`):
```yaml
openapi: 3.1.0
info:
  title: Test API
  version: 1.0.0
paths:
  /api/test:
    get:
      summary: Test endpoint
      description: This is a test endpoint
      tags:
        - Test
      responses:
        '200':
          description: Success
```

---

### 4️⃣ CORS 에러
**증상:**
```
Access to fetch at 'http://localhost:8080/ouro/rest-specs/import' 
from origin 'http://localhost:5173' has been blocked by CORS policy
```

**원인:**
- 백엔드에서 CORS 설정이 없음

**해결:**
백엔드 `application.properties`에 추가:
```properties
# CORS 설정
spring.web.cors.allowed-origins=http://localhost:5173
spring.web.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
spring.web.cors.allowed-headers=*
```

---

### 5️⃣ 파일 크기 제한 초과
**증상:**
```
Maximum upload size exceeded
```

**원인:**
- 업로드 파일이 너무 큼 (기본 1MB 제한)

**해결:**
백엔드 `application.properties`에 추가:
```properties
# 파일 업로드 크기 제한 (10MB)
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

---

## ✅ 정상 동작 확인

### 성공 시 콘솔 로그:
```javascript
YAML Import 시작: {
  fileName: "api-spec.yml",
  fileSize: 1234,
  fileType: "application/x-yaml",
  endpoint: "/ouro/rest-specs/import"
}

YAML Import 응답: {
  status: 200,
  statusText: "OK",
  ok: true
}
```

### 성공 시 모달 표시:
- ✅ YAML Import 성공
- 📊 통계 표시 (imported, renamed)
- ⚠️ 중복 항목 리스트 (있는 경우)

---

## 🔧 디버깅 체크리스트

- [ ] 백엔드 서버 실행 중? (`./gradlew bootRun`)
- [ ] 백엔드가 8080 포트에서 실행 중?
- [ ] 파일 확장자가 `.yml` 또는 `.yaml`?
- [ ] OpenAPI 버전이 3.x.x?
- [ ] `info.title`, `info.version` 필드 존재?
- [ ] `paths` 섹션에 최소 1개 경로 정의?
- [ ] HTTP 메소드가 유효한가? (get, post, put, delete, patch 등)
- [ ] CORS 설정 완료?

---

## 📞 추가 도움

브라우저 콘솔의 **전체 에러 메시지**와 **Network 탭의 Response**를 확인하여 정확한 원인을 파악하세요.

