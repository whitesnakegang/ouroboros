# 🐍 Ouroboros

<div align="center">

![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)
![Java](https://img.shields.io/badge/Java-17-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen.svg)
![React](https://img.shields.io/badge/React-19.1-61DAFB.svg)

**OpenAPI 3.1.0 기반 REST API 명세 관리 및 Mock 서버 라이브러리**

[English](../../README.md) | **한국어**

[시작하기](#-빠른-시작) • [문서](#-문서) • [기여하기](./CONTRIBUTING.md) • [라이선스](#-라이선스)

</div>

---

## 📖 목차

- [소개](#-소개)
- [주요 기능](#-주요-기능)
- [아키텍처](#-아키텍처)
- [빠른 시작](#-빠른-시작)
- [사용법](#-사용법)
- [문서](#-문서)
- [기여하기](#-기여하기)
- [라이선스](#-라이선스)
- [팀](#-팀)

---

## 🎯 소개

**Ouroboros**는 REST API 개발 라이프사이클을 혁신하는 Spring Boot 라이브러리입니다. OpenAPI 3.1.0 표준을 기반으로 API 명세를 관리하고, 자동으로 Mock 서버를 생성하며, API 검증 및 테스트 기능을 제공합니다.

### 왜 Ouroboros인가?

- **명세 우선 개발**: OpenAPI 명세를 먼저 작성하고, 실제 구현은 나중에
- **즉시 사용 가능한 Mock 서버**: 프론트엔드 개발이 백엔드를 기다릴 필요 없음
- **자동 검증**: 실제 구현과 명세의 일치성을 자동으로 검증
- **개발자 친화적**: 직관적인 웹 UI와 RESTful API 제공
- **경량 라이브러리**: 기존 Spring Boot 애플리케이션에 간단히 추가

---

## ✨ 주요 기능

### 🔧 API 명세 관리
- ✅ **OpenAPI 3.1.0 완벽 지원**: 최신 OpenAPI 표준 준수
- ✅ **CRUD 작업**: REST API 명세 생성, 조회, 수정, 삭제
- ✅ **스키마 재사용**: `$ref`를 통한 스키마 참조 및 중복 제거
- ✅ **YAML Import/Export**: 외부 OpenAPI 파일 가져오기 및 내보내기
- ✅ **중복 감지**: path + method 조합 중복 자동 검증
- ✅ **버전 관리**: API 진행 상태 추적 (mock, implementing, completed)

### 🎭 자동 Mock 서버
- ✅ **즉시 사용 가능**: 명세 작성 즉시 Mock API 생성
- ✅ **실전 같은 데이터**: DataFaker 통합으로 실제적인 Mock 데이터 생성
- ✅ **요청 검증**: 파라미터, 헤더, 본문 자동 검증
- ✅ **다양한 형식 지원**: JSON, XML, Form Data 등
- ✅ **커스텀 Mock 표현식**: `x-ouroboros-mock` 필드로 세밀한 제어

### 🖥️ 웹 인터페이스
- ✅ **React 기반 모던 UI**: 직관적이고 반응형 웹 인터페이스
- ✅ **실시간 미리보기**: API 명세 변경사항 즉시 확인
- ✅ **코드 스니펫 생성**: cURL, JavaScript, Python 등 다양한 언어
- ✅ **Markdown 내보내기**: API 문서 자동 생성

### 🔍 검증 및 QA
- ✅ **명세 검증**: OpenAPI 표준 준수 여부 검증
- ✅ **실제 구현 비교**: `@ApiState` 어노테이션으로 코드와 명세 동기화
- ✅ **자동 Enrichment**: 누락된 Ouroboros 확장 필드 자동 추가
- ✅ **에러 리포팅**: 상세한 검증 에러 메시지
- ✅ **Try 기능**: API 실행 추적 및 분석 (📖 [설정 가이드](./OUROBOROS_TRY_SETUP.md))
  - **기본값**: In-memory trace 저장소 (설정 불필요)

---

## 🏗️ 아키텍처

### 전체 구조

```
┌──────────────────────────────────────────────────────────────┐
│                        사용자 애플리케이션                        │
│  ┌────────────────────────────────────────────────────────┐  │
│  │                    Spring Boot App                      │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐  │  │
│  │  │ Controllers  │  │   Services   │  │   Models    │  │  │
│  │  │  @ApiState   │  │              │  │             │  │  │
│  │  └──────────────┘  └──────────────┘  └─────────────┘  │  │
│  └────────────────────────────────────────────────────────┘  │
│                              │                                │
│                 ┌────────────▼────────────┐                  │
│                 │  Ouroboros Library      │                  │
│                 │  ┌──────────────────┐   │                  │
│                 │  │  Auto Config     │   │                  │
│                 │  ├──────────────────┤   │                  │
│                 │  │  Mock Filter     │◄──┼── Mock Requests │
│                 │  ├──────────────────┤   │                  │
│                 │  │  Spec Manager    │   │                  │
│                 │  ├──────────────────┤   │                  │
│                 │  │  YAML Parser     │   │                  │
│                 │  ├──────────────────┤   │                  │
│                 │  │  Validator       │   │                  │
│                 │  └──────────────────┘   │                  │
│                 └──────────┬──────────────┘                  │
│                            │                                  │
│                 ┌──────────▼──────────┐                      │
│                 │   ourorest.yml      │                      │
│                 │  (OpenAPI 3.1.0)    │                      │
│                 └─────────────────────┘                      │
└──────────────────────────────────────────────────────────────┘
```

### 핵심 컴포넌트

#### Backend (Spring Boot Library)
- **`core/global`**: 자동 설정, 응답 포맷, 예외 처리
- **`core/rest/spec`**: API 명세 CRUD 서비스
- **`core/rest/mock`**: Mock 서버 필터 및 레지스트리
- **`core/rest/validation`**: OpenAPI 검증 및 Enrichment
- **`ui/controller`**: REST API 엔드포인트

#### Frontend (React + TypeScript)
- **`features/spec`**: API 명세 편집기 및 뷰어
- **`features/sidebar`**: 엔드포인트 네비게이션
- **`services`**: 백엔드 API 통신
- **`store`**: Zustand 상태 관리

#### 데이터 저장
- **`ourorest.yml`**: 모든 API 명세를 담은 단일 OpenAPI 파일
- **위치**: `{프로젝트}/src/main/resources/ouroboros/rest/ourorest.yml`

---

## 🚀 빠른 시작

### 전제 조건
- ☕ Java 17 이상
- 🍃 Spring Boot 3.x
- 📦 Gradle 또는 Maven

### 설치

#### Gradle
```gradle
dependencies {
    implementation 'io.github.whitesnakegang:ouroboros:1.0.1'
    implementation 'org.springframework.boot:spring-boot-starter-web'
}
```

#### Maven
```xml
<dependency>
    <groupId>io.github.whitesnakegang</groupId>
    <artifactId>ouroboros</artifactId>
    <version>1.0.1</version>
</dependency>
```

> **참고**: Lombok을 사용하는 경우 반드시 <code>annotationProcessor 'org.projectlombok:lombok'</code>를 추가해야 <code>lombok</code> 기반 자동 스캔이 정상 동작합니다.

### 설정 (선택 사항)

> **Method Tracing**: 내부 메서드 추적은 기본적으로 비활성화되어 있습니다. Try 기능에서 내부 메서드를 추적하려면 `method-tracing` 설정을 추가하고 `management.tracing.sampling.probability=1.0`을 함께 설정해야 합니다.

> **⚠️ Method Tracing 필수 설정**: Method Tracing 사용 시 `management.tracing.sampling.probability=1.0`을 설정하여 모든 트레이스를 수집해야 합니다.

`application.yml`:
```yaml
ouroboros:
  enabled: true  # 기본값: true
  server:
    url: http://localhost:8080
    description: Local Development Server
  # Method Tracing 설정 (Try 기능에서 내부 메서드 추적 시 필요)
  # 기본적으로 내부 메서드 추적은 비활성화되어 있습니다
  method-tracing:
    enabled: true
    allowed-packages: your.package.name  # 추적할 패키지 경로 지정

# Micrometer Tracing (Method Tracing 필수 설정)
# 모든 트레이스를 수집하기 위해 sampling probability를 1.0으로 설정
management:
  tracing:
    sampling:
      probability: 1.0
```

### 사용 시작

1. **Spring Boot 애플리케이션 실행**
   ```bash
   ./gradlew bootRun
   ```

2. **웹 UI 접속** 🖥️
   
   브라우저에서 다음 주소로 접속하세요:
   ```
   http://localhost:8080/ouroboros
   ```
   
   직관적인 웹 인터페이스에서 다음을 할 수 있습니다:
   - ✅ API 명세를 시각적으로 생성하고 편집
   - ✅ 드래그 앤 드롭으로 스키마 관리
   - ✅ 실시간으로 API 문서 미리보기
   - ✅ OpenAPI YAML 파일 가져오기/내보내기
   - ✅ 코드 스니펫 생성 (cURL, JavaScript, Python 등)

3. **첫 번째 API 명세 생성**
   
   웹 UI에서:
   1. "New API" 버튼 클릭
   2. 폼 작성 (경로, 메서드, 요약 등)
   3. 요청/응답 스키마 정의
   4. "저장" 클릭 - Mock API가 바로 준비됩니다!

4. **Mock API 즉시 테스트**
   
   생성한 API는 지정한 경로에서 바로 사용 가능:
   ```bash
   curl http://localhost:8080/api/users
   # 자동으로 Mock 데이터 반환!
   ```

> 💡 **Pro Tip**: 프로그래밍 방식의 접근을 선호한다면 REST API 엔드포인트를 직접 사용할 수도 있습니다. 자세한 내용은 [API 문서](../../backend/docs/endpoints/README.md)를 참고하세요.

---

## 📚 사용법

### 기본 워크플로우 (웹 UI 사용)

#### 1단계: 재사용 가능한 스키마 정의
1. 웹 UI에서 **"스키마"** 탭으로 이동
2. **"새 스키마"** 버튼 클릭
3. 스키마 폼 작성:
   - **이름**: `User`
   - **타입**: `object`
   - 속성 추가:
     - `id` (string) - Mock: `{{random.uuid}}`
     - `name` (string) - Mock: `{{name.fullName}}`
     - `email` (string) - Mock: `{{internet.emailAddress}}`
   - `id`와 `name`을 필수 필드로 지정
4. **"저장"** 클릭

#### 2단계: API 명세 생성
1. **"API"** 탭으로 이동
2. **"새 API"** 버튼 클릭
3. API 폼 작성:
   - **경로**: `/api/users`
   - **메서드**: `POST`
   - **요약**: `사용자 생성`
   - **요청 본문**: `User` 스키마 참조
   - **응답 (201)**: `User` 스키마 참조
   - **진행 상태**: `mock`
4. **"저장"** 클릭 - Mock API가 바로 동작합니다!

#### 3단계: Mock API 테스트
Mock API가 즉시 사용 가능합니다:

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "홍길동", "email": "hong@example.com"}'

# 응답 (자동 생성):
{
  "id": "a3b5c7d9-1234-5678-90ab-cdef12345678",
  "name": "홍길동",
  "email": "hong@example.com"
}
```

#### 4단계: 실제 구현 및 검증 (백엔드 개발자)
Controller에 `@ApiState` 어노테이션 추가:

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @PostMapping
    @ApiState(
        state = ApiState.State.IMPLEMENTING,
        owner = "backend-team",
        description = "사용자 생성 API 구현 중"
    )
    public ResponseEntity<User> createUser(@RequestBody User user) {
        // 실제 구현...
        return ResponseEntity.status(201).body(savedUser);
    }
}
```

애플리케이션 시작 시 Ouroboros가 자동으로 구현과 명세를 비교하여 검증합니다.

#### 5단계: 상태 업데이트
구현 완료 후 웹 UI에서 상태 업데이트:
1. 목록에서 해당 API 선택
2. **진행 상태**를 `mock`에서 `completed`로 변경
3. **"저장"** 클릭

### 외부 OpenAPI 파일 가져오기

1. 웹 UI에서 **"Import"** 버튼 클릭
2. OpenAPI YAML 파일 선택 (`.yml` 또는 `.yaml`)
3. **"업로드"** 클릭

Ouroboros가 자동으로:
- ✅ OpenAPI 3.1.0 표준 준수 여부 검증
- ✅ 중복 API/스키마 처리 (`-import` 접미사로 자동 이름 변경)
- ✅ Ouroboros 확장 필드 추가
- ✅ 모든 `$ref` 참조 자동 업데이트

> 📖 **프로그래밍 방식으로 사용**하려면 [REST API 문서](../../backend/docs/endpoints/README.md)를 참고하세요

---

## 📖 문서

### 공식 사이트
- [https://ouroboros.co.kr](https://ouroboros.co.kr) — 최신 가이드와 배포 문서를 확인할 수 있습니다.

### API 문서
- [API 엔드포인트 전체 문서](../../backend/docs/endpoints/README.md)
- [REST API 명세 관리](../../backend/docs/endpoints/01-create-rest-api-spec.md)
- [스키마 관리](../../backend/docs/endpoints/06-create-schema.md)
- [YAML Import](../../backend/docs/endpoints/11-import-yaml.md)

### 개발자 가이드
- [프로젝트 문서](../../backend/PROJECT_DOCUMENTATION.md)
- [GraphQL 설계](../../backend/docs/graphql/DESIGN.md)
- [트러블슈팅](../../backend/docs/troubleshooting/README.md)
- [Try 기능 설정 가이드](./OUROBOROS_TRY_SETUP.md)

### OpenAPI 확장 필드

Ouroboros는 OpenAPI 3.1.0에 다음 커스텀 필드를 추가합니다:

**Operation 레벨:**
- `x-ouroboros-id`: API 명세 고유 식별자 (UUID)
- `x-ouroboros-progress`: 개발 진행 상태 (`mock` | `completed`)
- `x-ouroboros-tag`: 개발 태그 (`none` | `implementing` | `bugfix`)
- `x-ouroboros-isvalid`: 검증 상태 (boolean)

**Schema 레벨:**
- `x-ouroboros-mock`: DataFaker 표현식 (예: `{{name.fullName}}`)
- `x-ouroboros-orders`: 필드 순서 배열

---

## 🤝 기여하기

Ouroboros는 오픈소스 프로젝트이며 여러분의 기여를 환영합니다!

### 기여 방법

1. **이슈 확인**: [GitHub Issues](https://github.com/whitesnakegang/ouroboros/issues)에서 작업할 이슈 찾기
2. **Fork & Clone**: 저장소를 Fork하고 로컬에 Clone
3. **브랜치 생성**: `feature/기능명` 또는 `fix/버그명` 브랜치 생성
4. **개발**: 코드 작성 및 테스트
5. **커밋**: [커밋 컨벤션](./CONTRIBUTING.md#커밋-메시지-규칙) 준수
6. **Pull Request**: `develop` 브랜치로 PR 생성

자세한 내용은 [기여 가이드](./CONTRIBUTING.md)를 참고하세요.

### 행동 강령

이 프로젝트는 [행동 강령](./CODE_OF_CONDUCT.md)을 준수합니다. 참여함으로써 귀하는 이를 지키는 데 동의합니다.

---

## 📄 라이선스

이 프로젝트는 [Apache License 2.0](../../LICENSE) 라이선스를 따릅니다.

```
Copyright 2025 Whitesnakegang

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## 👥 팀

### 메인테이너
- **Whitesnakegang** - *프로젝트 창시자 및 메인테이너* - [@whitesnakegang](https://github.com/whitesnakegang)

### 기여자
이 프로젝트에 기여해주신 모든 분들께 감사드립니다!

[전체 기여자 목록](https://github.com/whitesnakegang/ouroboros/graphs/contributors)

---

## 🔗 링크

- **GitHub**: https://github.com/whitesnakegang/ouroboros
- **Issues**: https://github.com/whitesnakegang/ouroboros/issues
- **Maven Central**: https://search.maven.org/artifact/io.github.whitesnakegang/ouroboros

---

## 📞 지원

문제가 있거나 질문이 있으신가요?

- 📝 [Issue 생성](https://github.com/whitesnakegang/ouroboros/issues/new)
- 💬 [Discussion 참여](https://github.com/whitesnakegang/ouroboros/discussions)

---

<div align="center">

**Ouroboros로 더 나은 API 개발을 경험하세요!**

⭐ 이 프로젝트가 도움이 되었다면 Star를 눌러주세요!

Made with ❤️ by [Whitesnakegang](https://github.com/whitesnakegang)

</div>

