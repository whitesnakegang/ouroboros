# 기여 가이드

먼저 Ouroboros 프로젝트에 관심을 가져주셔서 감사합니다! 이 문서는 프로젝트에 기여하는 방법을 안내합니다.

## 📋 목차

- [행동 강령](#행동-강령)
- [시작하기](#시작하기)
- [기여 방법](#기여-방법)
- [개발 환경 설정](#개발-환경-설정)
- [브랜치 전략](#브랜치-전략)
- [커밋 메시지 규칙](#커밋-메시지-규칙)
- [코드 스타일 가이드](#코드-스타일-가이드)
- [테스트](#테스트)
- [Pull Request 프로세스](#pull-request-프로세스)
- [이슈 리포팅](#이슈-리포팅)
- [커뮤니티](#커뮤니티)

---

## 행동 강령

이 프로젝트와 프로젝트에 참여하는 모든 사람은 [행동 강령](CODE_OF_CONDUCT.md)을 준수해야 합니다. 참여함으로써 귀하는 이 강령을 지키는 데 동의합니다. 용납할 수 없는 행동은 프로젝트 팀에 보고해 주시기 바랍니다.

---

## 시작하기

### 어떻게 도울 수 있나요?

Ouroboros는 다양한 방식으로 기여할 수 있습니다:

- 🐛 **버그 리포트**: 발견한 버그를 이슈로 등록
- 💡 **기능 제안**: 새로운 기능 아이디어 제안
- 📝 **문서 개선**: 문서 오타 수정 및 내용 개선
- 💻 **코드 기여**: 버그 수정 및 새 기능 구현
- 🌐 **번역**: 다른 언어로 문서 번역
- 🧪 **테스트**: 테스트 케이스 작성 및 개선
- 🎨 **UI/UX**: 프론트엔드 디자인 및 사용성 개선

### 기여하기 좋은 이슈 찾기

- **Good First Issue**: 처음 기여하시는 분들에게 적합한 이슈
- **Help Wanted**: 도움이 필요한 이슈
- **Bug**: 버그 수정이 필요한 이슈
- **Enhancement**: 새로운 기능이나 개선사항

[이슈 목록 보기](https://github.com/whitesnakegang/ouroboros/issues)

---

## 기여 방법

### 1. Fork & Clone

```bash
# 1. GitHub에서 저장소를 Fork합니다
# 2. Fork한 저장소를 로컬에 Clone합니다
git clone https://github.com/YOUR_USERNAME/ouroboros.git
cd ouroboros

# 3. 원본 저장소를 upstream으로 추가합니다
git remote add upstream https://github.com/whitesnakegang/ouroboros.git

# 4. 최신 변경사항을 받아옵니다
git fetch upstream
```

### 2. 브랜치 생성

```bash
# develop 브랜치에서 새 브랜치 생성
git checkout develop
git pull upstream develop
git checkout -b feature/your-feature-name
```

### 3. 개발 및 커밋

```bash
# 변경사항 추가
git add .

# 커밋 메시지 규칙에 따라 커밋
git commit -m "feat: add new feature"
```

### 4. Push 및 Pull Request

```bash
# Fork한 저장소에 Push
git push origin feature/your-feature-name

# GitHub에서 Pull Request 생성
```

---

## 개발 환경 설정

### Backend (Spring Boot)

#### 요구 사항
- ☕ Java 17 이상
- 📦 Gradle 8.14.3+ (Wrapper 포함)
- 🍃 Spring Boot 3.5.7

#### 설정 및 실행

```bash
cd backend

# 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 애플리케이션 실행
./gradlew bootRun

# 로컬 Maven 저장소에 배포 (테스트용)
./gradlew publishToMavenLocal
```

### Frontend (React + TypeScript)

#### 요구 사항
- 📦 Node.js 18 이상
- 📦 npm 또는 yarn

#### 설정 및 실행

```bash
cd front

# 의존성 설치
npm install

# 개발 서버 실행
npm run dev

# 빌드
npm run build

# Lint 검사
npm run lint
```

---

## 브랜치 전략

우리는 **Git Flow** 전략을 사용합니다.

### 브랜치 종류

| 브랜치 | 용도 | Base Branch |
|--------|------|-------------|
| `main` | 프로덕션 배포 브랜치 | - |
| `develop` | 개발 통합 브랜치 | `main` |
| `feature/*` | 새로운 기능 개발 | `develop` |
| `fix/*` | 버그 수정 | `develop` |
| `hotfix/*` | 긴급 수정 | `main` |
| `release/*` | 배포 준비 | `develop` |

### 브랜치 네이밍 규칙

```
feature/rest-api-spec-crud
feature/mock-server-validation
fix/yaml-parser-null-pointer
fix/duplicate-schema-detection
hotfix/security-vulnerability
release/v0.1.0
```

### 워크플로우

#### 기능 개발
```bash
git checkout develop
git pull upstream develop
git checkout -b feature/your-feature
# ... 개발 ...
git push origin feature/your-feature
# PR 생성: feature/your-feature → develop
```

#### 버그 수정
```bash
git checkout develop
git pull upstream develop
git checkout -b fix/bug-name
# ... 수정 ...
git push origin fix/bug-name
# PR 생성: fix/bug-name → develop
```

#### 긴급 수정 (Hotfix)
```bash
git checkout main
git pull upstream main
git checkout -b hotfix/critical-bug
# ... 수정 ...
git push origin hotfix/critical-bug
# PR 생성: hotfix/critical-bug → main
# 이후 main → develop으로 merge
```

---

## 커밋 메시지 규칙

우리는 **Conventional Commits** 스타일을 따릅니다.

### 구조

```
<타입>: <간단한 설명>

[선택사항] 본문

[선택사항] Footer
```

### 타입

| 타입 | 설명 | 예시 |
|------|------|------|
| `feat` | 새로운 기능 추가 | `feat: add YAML import feature` |
| `fix` | 버그 수정 | `fix: resolve null pointer in parser` |
| `docs` | 문서 수정 | `docs: update contributing guide` |
| `style` | 코드 포맷팅 (기능 변화 없음) | `style: format code with prettier` |
| `refactor` | 코드 리팩토링 | `refactor: simplify validation logic` |
| `test` | 테스트 코드 추가/수정 | `test: add unit tests for service` |
| `chore` | 빌드, 패키지 매니저 설정 등 | `chore: update dependencies` |
| `perf` | 성능 개선 | `perf: optimize YAML parsing` |
| `ci` | CI 설정 수정 | `ci: add github actions workflow` |
| `build` | 빌드 관련 파일 수정 | `build: update gradle version` |

### 규칙

1. **제목**: 50자 이내, 첫 글자 소문자, 마침표 없음
2. **본문**: 72자마다 줄바꿈, **무엇을** 그리고 **왜** 변경했는지 설명
3. **Footer**: Breaking changes, 이슈 참조 등
4. **언어**: 한글 또는 영어 (일관성 유지)

### 예시

#### 간단한 커밋
```
feat: add schema validation
```

#### 상세한 커밋
```
feat: add automatic schema enrichment

- x-ouroboros-mock 필드 자동 추가
- x-ouroboros-orders 필드 자동 생성
- 기존 값이 있는 경우 보존

Closes #123
```

#### Breaking Change
```
feat!: change API response format

BREAKING CHANGE: GlobalApiResponse 구조 변경
- error 필드가 이제 객체가 아닌 배열로 반환됩니다
- 마이그레이션 가이드: docs/migration/v0.2.0.md

Closes #234
```

---

## 코드 스타일 가이드

### Java (Backend)

#### 기본 원칙
- **Google Java Style Guide** 준수
- **Javadoc 필수**: 모든 public 클래스, 메서드, 필드
- **주석 언어**: 영어 사용

#### Javadoc 예시
```java
/**
 * Service interface for REST API specification management.
 * <p>
 * Manages REST API endpoint specifications in the OpenAPI paths section.
 * Supports full CRUD operations with automatic YAML file synchronization.
 *
 * @since 0.0.1
 */
public interface RestApiSpecService {
    
    /**
     * Creates a new REST API specification.
     * <p>
     * Validates uniqueness of path+method combination and generates a UUID if not provided.
     * Writes the specification to the OpenAPI YAML file.
     *
     * @param request REST API specification details
     * @return created specification with generated ID
     * @throws Exception if specification creation fails or duplicate path+method exists
     */
    RestApiSpecResponse createRestApiSpec(CreateRestApiRequest request) throws Exception;
}
```

#### 네이밍 규칙
- **클래스**: `PascalCase` (예: `RestApiSpecService`)
- **메서드/변수**: `camelCase` (예: `createRestApiSpec`)
- **상수**: `UPPER_SNAKE_CASE` (예: `MAX_RETRY_COUNT`)
- **패키지**: `lowercase` (예: `kr.co.ouroboros.core.rest`)

#### Package-info 작성
각 패키지에 `package-info.java` 작성 필수:

```java
/**
 * REST API specification management services.
 * <p>
 * This package provides CRUD operations for managing REST API specifications
 * in OpenAPI 3.1.0 format.
 *
 * @since 0.0.1
 */
package kr.co.ouroboros.core.rest.spec.service;
```

### TypeScript/React (Frontend)

#### 기본 원칙
- **ESLint** 규칙 준수
- **함수형 컴포넌트** 사용
- **TypeScript Strict Mode** 활성화

#### 컴포넌트 예시
```typescript
interface Props {
  title: string;
  onSave: (data: SpecData) => void;
}

/**
 * API 명세 편집 폼 컴포넌트
 */
export const SpecForm: React.FC<Props> = ({ title, onSave }) => {
  // ...
};
```

#### 네이밍 규칙
- **컴포넌트**: `PascalCase` (예: `SpecForm`)
- **함수/변수**: `camelCase` (예: `handleSubmit`)
- **상수**: `UPPER_SNAKE_CASE` (예: `API_BASE_URL`)
- **타입/인터페이스**: `PascalCase` (예: `SpecFormProps`)

---

## 테스트

### Backend 테스트

#### 단위 테스트
```java
@SpringBootTest
class RestApiSpecServiceTest {
    
    @Test
    void createRestApiSpec_ShouldGenerateId_WhenIdNotProvided() {
        // given
        CreateRestApiRequest request = CreateRestApiRequest.builder()
            .path("/api/test")
            .method("GET")
            .build();
        
        // when
        RestApiSpecResponse response = service.createRestApiSpec(request);
        
        // then
        assertNotNull(response.getId());
    }
}
```

#### 실행
```bash
# 전체 테스트
./gradlew test

# 특정 테스트
./gradlew test --tests RestApiSpecServiceTest

# 테스트 커버리지
./gradlew test jacocoTestReport
```

### Frontend 테스트

#### 컴포넌트 테스트
```typescript
describe('SpecForm', () => {
  it('should render correctly', () => {
    const { getByText } = render(<SpecForm title="Test" />);
    expect(getByText('Test')).toBeInTheDocument();
  });
});
```

---

## Pull Request 프로세스

### PR 체크리스트

PR을 생성하기 전에 다음을 확인하세요:

- [ ] `develop` 브랜치에서 최신 코드를 받았습니다
- [ ] 브랜치 네이밍 규칙을 따릅니다
- [ ] 커밋 메시지 규칙을 준수합니다
- [ ] 코드 스타일 가이드를 따릅니다
- [ ] 새로운 코드에 대한 테스트를 작성했습니다
- [ ] 모든 테스트가 통과합니다
- [ ] Javadoc/JSDoc을 작성했습니다
- [ ] 문서를 업데이트했습니다 (필요한 경우)
- [ ] Linter 오류가 없습니다

### PR 템플릿

```markdown
## 📝 변경사항

이 PR이 무엇을 변경하는지 간단히 설명해주세요.

## 🎯 관련 이슈

Closes #이슈번호

## 🧪 테스트 방법

변경사항을 테스트하는 방법을 설명해주세요.

1. ...
2. ...

## 📸 스크린샷 (UI 변경 시)

UI 변경사항이 있다면 스크린샷을 첨부해주세요.

## 📋 체크리스트

- [ ] 코드 스타일 가이드를 따릅니다
- [ ] 테스트를 작성했습니다
- [ ] 문서를 업데이트했습니다
- [ ] Breaking changes가 있다면 마이그레이션 가이드를 작성했습니다
```

### 리뷰 프로세스

1. **PR 생성**: `develop` 브랜치로 PR을 생성합니다
2. **자동 검사**: CI/CD가 자동으로 빌드 및 테스트를 실행합니다
3. **코드 리뷰**: 최소 1명의 메인테이너 승인이 필요합니다
4. **수정**: 리뷰 의견에 따라 수정합니다
5. **Merge**: 승인 후 메인테이너가 Merge합니다

### Merge 전략

- **Squash and Merge**: 기능 브랜치는 squash merge
- **Merge Commit**: Release 브랜치는 merge commit
- **Rebase and Merge**: Hotfix는 rebase merge

---

## 이슈 리포팅

### 버그 리포트

버그를 발견하셨나요? 다음 정보를 포함하여 이슈를 생성해주세요:

#### 템플릿

```markdown
## 🐛 버그 설명

버그가 무엇인지 명확하고 간결하게 설명해주세요.

## 🔄 재현 방법

버그를 재현하는 단계:
1. '...'로 이동
2. '...'를 클릭
3. '...'로 스크롤
4. 에러 발생

## ✅ 예상 동작

어떤 동작을 기대했는지 설명해주세요.

## 📸 스크린샷

가능하다면 스크린샷을 첨부해주세요.

## 🖥️ 환경

- OS: [예: Windows 10]
- Java 버전: [예: 17]
- Spring Boot 버전: [예: 3.5.7]
- Ouroboros 버전: [예: 0.1.0]

## 📋 추가 정보

다른 관련 정보가 있다면 추가해주세요.
```

### 기능 제안

새로운 기능을 제안하고 싶으신가요?

#### 템플릿

```markdown
## 💡 기능 설명

제안하고 싶은 기능을 설명해주세요.

## 🎯 문제점

이 기능이 어떤 문제를 해결하나요?

## 💻 제안하는 해결책

어떻게 구현하면 좋을지 설명해주세요.

## 🔄 대안

고려한 다른 대안이 있나요?

## 📋 추가 정보

다른 관련 정보나 스크린샷이 있다면 추가해주세요.
```

---

## 커뮤니티

### 소통 채널

- **GitHub Issues**: 버그 리포트 및 기능 제안
- **GitHub Discussions**: 일반적인 질문 및 토론
- **Pull Requests**: 코드 기여 및 리뷰

### 질문하기

질문이 있으신가요?

1. **FAQ 확인**: [문서](./backend/docs/)를 먼저 확인해주세요
2. **이슈 검색**: 이미 같은 질문이 있는지 확인해주세요
3. **Discussion 생성**: 새로운 질문은 Discussion에 올려주세요

### 도움 받기

- 📚 [프로젝트 문서](./backend/PROJECT_DOCUMENTATION.md)
- 🔧 [트러블슈팅 가이드](./backend/docs/troubleshooting/README.md)
- 📖 [API 문서](./backend/docs/endpoints/README.md)

---

## 라이선스

이 프로젝트에 기여함으로써, 귀하의 기여가 [Apache License 2.0](./LICENSE)에 따라 라이선스됨에 동의합니다.

---

## 감사합니다!

Ouroboros 프로젝트에 시간을 내어 기여해주셔서 감사합니다! 여러분의 기여가 이 프로젝트를 더욱 발전시킵니다. 🎉

질문이 있으시면 언제든지 문의해주세요.

**Happy Coding!** 🚀

