# DemoApiGen - Dynamic API Mock Generator

Spring Boot 라이브러리로, `api.yml` 파일을 읽고 동적으로 더미 API 엔드포인트를 생성하는 라이브러리입니다. Faker 라이브러리를 사용하여 의미있는 더미 데이터를 자동으로 생성하며, 프론트엔드 개발 시 백엔드 API 완성을 기다리지 않고 개발할 수 있도록 도와줍니다.

## 주요 기능

- **YAML 파일 기반 API 엔드포인트 정의**
- **동적 엔드포인트 등록** (애플리케이션 시작 시 자동)
- **Faker 라이브러리 통합** - 실제와 유사한 더미 데이터 생성
- **스마트 타입 추론** - 필드명만으로 자동으로 적절한 더미 데이터 생성
- **웹 기반 에디터** - React로 만든 GUI에서 API 정의 편집 (`/demoapigen/editor`)
- **다양한 인증 방식 지원** (Bearer Token, Basic Auth, API Key, Custom Header)
- **Status Code별 응답 설정** - 200, 201, 400, 401, 403 등 각각 다른 응답 정의
- **X-Mock-Status 헤더** - 개발 중 특정 에러 상황 강제 테스트 가능
- **Request Body / Query Parameters 지원**
- **Path Variable 지원** (`/api/users/{user_id}`)
- **고정값 지원** - 에러 메시지 등 고정된 응답값 설정 가능
- **중첩된 객체, 배열** 등 복잡한 응답 구조 지원
- **Spring Boot Auto-configuration** 지원

## 요구사항

- Java 17 이상
- Spring Boot 3.2.0 이상

## 설치 방법

### 1. 로컬 Maven 저장소에 설치

```bash
./gradlew publishToMavenLocal
```

### 2. 의존성 추가

당신의 Spring Boot 프로젝트의 `build.gradle`에 다음을 추가하세요:

```gradle
dependencies {
    implementation 'io.c102:demoapigen:1.0.0'
}
```

또는 Maven의 경우 `pom.xml`에:

```xml
<dependency>
    <groupId>io.c102</groupId>
    <artifactId>demoapigen</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 사용 방법

### 1. api.yml 파일 생성

프로젝트의 `src/main/resources` 디렉토리에 `api.yml` 파일을 생성합니다:

```yaml
endpoints:
  - path: /api/users
    method: GET
    description: Get list of users
    requiresAuth: true
    authType: bearer
    responses:
      - statusCode: 200
        response:
          type: array
          arrayItemType:
            type: object
            fields:
              - name: id
                type: number
              - name: username
                type: faker
                fakerType: internet.username
              - name: email
                type: faker
                fakerType: internet.emailAddress
              - name: fullName
                type: faker
                fakerType: name.fullName
      - statusCode: 401
        response:
          type: object
          fields:
            - name: error
              type: string
              defaultValue: "Unauthorized"
            - name: message
              type: string
              defaultValue: "Authentication required"

  - path: /api/users/{user_id}
    method: GET
    description: Get user by ID
    requiresAuth: true
    authType: bearer
    responses:
      - statusCode: 200
        response:
          type: object
          fields:
            - name: id
              type: number
            - name: username
              type: faker
              fakerType: internet.username
            - name: email
              type: faker
              fakerType: internet.emailAddress
      - statusCode: 401
        response:
          type: object
          fields:
            - name: error
              type: string
              defaultValue: "Unauthorized"
            - name: message
              type: string
              defaultValue: "Authentication required"

  - path: /api/users
    method: POST
    description: Create new user
    requiresAuth: true
    authType: bearer
    request:
      type: body
      contentType: json
      fields:
        - name: username
          type: string
          required: true
        - name: email
          type: string
          required: true
        - name: fullName
          type: string
          required: false
    responses:
      - statusCode: 201
        response:
          type: object
          fields:
            - name: id
              type: number
            - name: createdAt
              type: string
      - statusCode: 400
        response:
          type: object
          fields:
            - name: error
              type: string
              defaultValue: "Bad Request"
            - name: message
              type: string
              defaultValue: "Invalid request parameters"
```

### 2. 애플리케이션 실행

Spring Boot 애플리케이션을 실행하면 자동으로 `api.yml`에 정의된 엔드포인트들이 등록됩니다.

```bash
./gradlew bootRun
```

### 3. API 호출

#### 정상 요청 (200 OK)
```bash
curl -H "Authorization: Bearer test-token" http://localhost:8080/api/users
```

응답:
```json
[
  {
    "id": 123,
    "username": "john_doe",
    "email": "john.doe@example.com",
    "fullName": "John Doe"
  }
]
```

#### 인증 없이 요청 (401 Unauthorized)
```bash
curl http://localhost:8080/api/users
```

응답:
```json
{
  "error": "Unauthorized",
  "message": "Authentication required"
}
```

#### 에러 상황 강제 테스트 (X-Mock-Status 헤더)
```bash
curl -H "Authorization: Bearer test-token" \
     -H "X-Mock-Status: 400" \
     http://localhost:8080/api/users
```

응답:
```json
{
  "error": "Bad Request",
  "message": "Invalid request parameters"
}
```

## 웹 에디터 사용하기

### 접속

Spring Boot 애플리케이션을 실행한 후:

```
http://localhost:8080/demoapigen/editor
```

### 기능

#### 1. 엔드포인트 관리
- **Add 버튼**으로 새 엔드포인트 추가
- 엔드포인트 클릭해서 편집
- **× 버튼**으로 삭제
- **Save 버튼**으로 api.yml에 저장 및 자동 재등록

#### 2. 엔드포인트 설정
- HTTP 메소드 선택 (GET, POST, PUT, DELETE, PATCH)
- 경로 입력 (Path Variable 지원: `/api/users/{user_id}`)
- 설명 입력

#### 3. 인증 설정
- **Requires Authentication** 체크박스
- 인증 타입 선택:
  - **Bearer Token**: `Authorization: Bearer <token>`
  - **Basic Auth**: `Authorization: Basic <credentials>`
  - **API Key**: Query parameter `?api_key=xxx` 또는 `X-API-Key` 헤더
  - **Custom Header**: 사용자 정의 헤더명 (예: `X-Auth-Token`)

#### 4. Request 설정
- **Request Type** 선택:
  - **None**: Request body/query parameters 없음
  - **Query Parameters**: URL query string으로 전달
  - **Request Body**: JSON 또는 multipart/form-data
- **Required** 체크박스로 필수/선택 필드 설정
- **Default Value** 입력으로 기본값 설정

#### 5. Response 설정
- **Add Status Response** 버튼으로 여러 상태 코드 응답 추가
- 각 Status Code별로 다른 Response 구조 정의:
  - **200 OK**: 정상 응답
  - **201 Created**: 생성 성공
  - **400 Bad Request**: 잘못된 요청
  - **401 Unauthorized**: 인증 필요
  - **403 Forbidden**: 권한 없음
- **Response Type** 선택:
  - `object`: 객체
  - `array`: 배열
  - `string`, `number`, `boolean`: 기본 타입

#### 6. 필드 타입 및 더미 데이터
- **string**: 문자열 (필드명에서 자동 추론 또는 고정값 설정)
- **number**: 숫자
- **boolean**: true/false
- **object**: 중첩 객체
- **array**: 배열
- **faker**: 명시적 Faker 타입 지정
- **file**: 파일 업로드 (multipart/form-data)

#### 7. 스마트 타입 추론
필드명만으로 자동으로 적절한 더미 데이터 생성:
- `email` → 이메일 주소
- `username` → 사용자명
- `phone`, `phoneNumber` → 전화번호
- `address` → 주소
- `city` → 도시명
- `company` → 회사명
- `productName` → 상품명
- `price` → 가격
- 등등...

#### 8. 고정값 설정
에러 응답 등에서 고정된 메시지를 출력하려면:
- Response fields에서 string 타입 필드 추가
- **"Fixed value"** 입력란에 고정 문자열 입력
- 예: `error` 필드에 "Unauthorized", `message` 필드에 "Authentication required"

빈 값으로 두면 Faker나 스마트 타입 추론으로 랜덤 데이터 생성됩니다.

#### 9. 미리보기
- **Preview 버튼**으로 실시간 더미 데이터 확인
- 성공 응답(2xx) 중 가장 낮은 status code의 응답 미리보기

## 고급 기능

### X-Mock-Status 헤더로 에러 테스트

프론트엔드 개발 중 특정 에러 상황을 테스트하고 싶을 때:

```bash
# 400 에러 강제 발생
curl -H "Authorization: Bearer token" \
     -H "X-Mock-Status: 400" \
     http://localhost:8080/api/users

# 500 에러 강제 발생 (yml에 정의되지 않아도 가능)
curl -H "Authorization: Bearer token" \
     -H "X-Mock-Status: 500" \
     http://localhost:8080/api/users
```

### 자연스러운 에러 응답

`X-Mock-Status` 헤더가 없으면 실제 API처럼 동작:
- 인증 필요한데 헤더 없음 → 401 응답
- 정상 요청 → 2xx 응답 (yml에 정의된 가장 낮은 성공 코드)

### Path Variable 지원

```yaml
- path: /api/users/{user_id}
  method: GET
```

```bash
curl http://localhost:8080/api/users/123
curl http://localhost:8080/api/users/abc
# 모두 동일한 더미 응답 반환
```

## api.yml 스키마

### Endpoint 구조

```yaml
endpoints:
  - path: string                  # 엔드포인트 경로 (path variable 지원)
    method: string                # HTTP 메소드 (GET, POST, PUT, DELETE, PATCH)
    description: string           # 설명 (선택)
    requiresAuth: boolean         # 인증 필요 여부
    authType: string              # 인증 타입 (bearer, basic, apikey, custom)
    authHeader: string            # custom 타입일 때 헤더명
    request:                      # Request 정의
      type: string                # none, body, query
      contentType: string         # json, formData (body일 때)
      fields: Field[]             # 필드 목록
    responses:                    # 여러 status code별 응답
      - statusCode: number        # HTTP 상태 코드
        response: Response        # 응답 구조
```

### Field 구조

```yaml
fields:
  - name: string              # 필드명
    type: string              # string, number, boolean, object, array, faker, file
    fakerType: string         # faker 타입일 때 (예: internet.emailAddress)
    required: boolean         # 필수 여부 (request fields만)
    defaultValue: string      # 고정값 또는 기본값
    fields: Field[]           # object 타입일 때 중첩 필드
    arrayItemType: Field      # array 타입일 때 아이템 타입
```

### Response 구조

```yaml
response:
  type: string                # object, array, string, number, boolean
  fields: Field[]             # object 타입일 때
  arrayItemType: Field        # array 타입일 때
```

### 기본 타입

- `string`: 문자열 (스마트 타입 추론 또는 고정값)
- `number`: 숫자
- `boolean`: true/false
- `object`: 객체 (fields 속성 필요)
- `array`: 배열 (arrayItemType 속성 필요)
- `faker`: Faker 라이브러리 사용 (fakerType 속성 필요)
- `file`: 파일 (multipart/form-data)

### Faker 타입 예시

```yaml
fields:
  - name: fullName
    type: faker
    fakerType: name.fullName

  - name: email
    type: faker
    fakerType: internet.emailAddress

  - name: phoneNumber
    type: faker
    fakerType: phoneNumber.cellPhone

  - name: address
    type: faker
    fakerType: address.fullAddress

  - name: company
    type: faker
    fakerType: company.name

  - name: productName
    type: faker
    fakerType: commerce.productName

  - name: price
    type: faker
    fakerType: commerce.price
```

### 고정값 예시 (에러 메시지)

```yaml
responses:
  - statusCode: 401
    response:
      type: object
      fields:
        - name: error
          type: string
          defaultValue: "Unauthorized"      # 고정 문자열
        - name: message
          type: string
          defaultValue: "Authentication required"
```

### 중첩 객체 예시

```yaml
fields:
  - name: user
    type: object
    fields:
      - name: name
        type: faker
        fakerType: name.fullName
      - name: address
        type: object
        fields:
          - name: street
            type: faker
            fakerType: address.streetAddress
          - name: city
            type: faker
            fakerType: address.city
```

### 배열 예시

```yaml
response:
  type: array
  arrayItemType:
    type: object
    fields:
      - name: id
        type: number
      - name: name
        type: string
```

## 프론트엔드 개발

```bash
cd frontend
npm install
npm run dev    # 개발 서버 실행 (localhost:5173)
npm run build  # 프로덕션 빌드
```

빌드된 파일은 `src/main/resources/static/demoapigen/`에 자동으로 저장되며, 라이브러리 JAR에 포함됩니다.

자세한 내용은 [frontend/README.md](frontend/README.md)를 참고하세요.

## 프로젝트 구조

```
src/main/java/c102/com/demoapigen/
├── config/
│   ├── DemoApiAutoConfiguration.java       # Spring Boot Auto-configuration
│   ├── DemoApiGenProperties.java           # 설정 프로퍼티
│   └── WebConfig.java                      # 정적 리소스 설정
├── controller/
│   ├── EditorApiController.java            # 에디터 REST API
│   └── EditorViewController.java           # 에디터 뷰 컨트롤러
├── model/
│   ├── ApiDefinition.java                  # API 정의 모델
│   ├── Endpoint.java                       # 엔드포인트 모델
│   ├── StatusResponse.java                 # Status code별 응답 모델
│   ├── Request.java                        # Request 모델
│   ├── Response.java                       # Response 모델
│   └── Field.java                          # 필드 모델
└── service/
    ├── ApiYamlParser.java                  # YAML 파서
    ├── ApiDefinitionService.java           # API 정의 관리
    ├── DummyDataGenerator.java             # 더미 데이터 생성기 (스마트 타입 추론)
    ├── DynamicEndpointRegistrar.java       # 동적 엔드포인트 등록
    └── DynamicEndpointController.java      # 엔드포인트 컨트롤러 (인증, 응답 처리)

frontend/
├── src/
│   ├── components/                         # React 컴포넌트
│   │   ├── EndpointList.jsx               # 엔드포인트 목록
│   │   ├── EndpointEditor.jsx             # 엔드포인트 편집기
│   │   ├── FieldEditor.jsx                # 필드 편집기
│   │   └── PreviewPanel.jsx               # 미리보기 패널
│   ├── utils/
│   │   ├── dummyTypes.js                  # 더미 데이터 타입 정의
│   │   └── statusTemplates.js             # Status code 템플릿
│   ├── App.jsx                            # 메인 앱
│   └── main.jsx                           # 엔트리 포인트
└── vite.config.js                         # Vite 설정
```

## 빌드

```bash
# 프론트엔드 빌드
cd frontend && npm run build && cd ..

# 백엔드 빌드 (테스트 제외)
./gradlew clean build -x test

# JAR 생성
./gradlew jar

# 로컬 Maven 저장소에 게시
./gradlew publishToMavenLocal
```

## 프로젝트 이름 및 그룹 변경하기

### 그룹 ID 변경

`build.gradle` 파일에서:

```gradle
group = 'io.c102'  // 원하는 그룹 ID로 변경
```

### 아티팩트 ID (프로젝트 이름) 변경

`settings.gradle` 파일에서:

```gradle
rootProject.name = 'your-project-name'
```

변경 후 다시 빌드:

```bash
./gradlew clean build -x test
./gradlew publishToMavenLocal
```

## 사용 예시

### 시나리오 1: 인증이 필요한 사용자 목록 API

프론트엔드 개발자가 사용자 목록을 가져오는 API가 필요한 상황:

1. 웹 에디터에서 엔드포인트 생성
2. Path: `/api/users`, Method: `GET`
3. "Requires Authentication" 체크, Auth Type: Bearer Token
4. Responses 추가:
   - 200: 사용자 배열 (name, email 필드)
   - 401: 에러 메시지 (error, message 필드에 고정값)
5. Save

프론트엔드 코드:
```javascript
// 정상 호출
const response = await fetch('/api/users', {
  headers: { 'Authorization': 'Bearer token' }
})
const users = await response.json()
// [{"name": "John Doe", "email": "john@example.com"}, ...]

// 인증 에러 테스트
const response = await fetch('/api/users')
const error = await response.json()
// {"error": "Unauthorized", "message": "Authentication required"}

// 400 에러 테스트 (헤더 사용)
const response = await fetch('/api/users', {
  headers: {
    'Authorization': 'Bearer token',
    'X-Mock-Status': '400'
  }
})
const error = await response.json()
// {"error": "Bad Request", "message": "Invalid request parameters"}
```

### 시나리오 2: POST로 사용자 생성

1. Path: `/api/users`, Method: `POST`
2. Request Type: `body`, Content Type: `json`
3. Request Fields:
   - username (string, required)
   - email (string, required)
   - fullName (string, optional)
4. Responses:
   - 201: {id, createdAt}
   - 400: 에러 메시지

프론트엔드 코드:
```javascript
const response = await fetch('/api/users', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer token'
  },
  body: JSON.stringify({
    username: 'johndoe',
    email: 'john@example.com',
    fullName: 'John Doe'
  })
})
const result = await response.json()
// {"id": 123, "createdAt": "2025-10-13T16:00:00Z"}
```

## 라이센스

MIT License

## 기여

이슈와 풀 리퀘스트는 언제나 환영합니다!

## 문의

문제가 발생하거나 질문이 있으시면 Issue를 생성해주세요.
