# 🖥️ Ouroboros Frontend

**Ouroboros** 프로젝트의 웹 기반 사용자 인터페이스입니다. React, TypeScript, Vite를 사용하여 구축된 모던한 SPA(Single Page Application)입니다.

---

## 📋 목차

- [소개](#-소개)
- [주요 기능](#-주요-기능)
- [기술 스택](#-기술-스택)
- [시작하기](#-시작하기)
- [프로젝트 구조](#-프로젝트-구조)
- [개발 가이드](#-개발-가이드)
- [빌드 및 배포](#-빌드-및-배포)

---

## 🎯 소개

Ouroboros Frontend는 REST API 명세서를 시각적으로 관리하고 편집할 수 있는 웹 인터페이스를 제공합니다. OpenAPI 3.1.0 명세를 직관적으로 작성, 수정, 관리할 수 있으며, 실시간 미리보기와 코드 스니펫 생성 기능을 제공합니다.

### 주요 특징

- 🎨 **모던한 UI/UX**: TailwindCSS를 활용한 깔끔하고 직관적인 디자인
- ⚡ **빠른 개발 경험**: Vite를 통한 초고속 HMR(Hot Module Replacement)
- 🔄 **실시간 반응형**: API 변경사항을 즉시 반영
- 🌐 **상태 관리**: Zustand를 활용한 경량 상태 관리
- 📱 **반응형 디자인**: 데스크톱, 태블릿, 모바일 모두 지원

---

## ✨ 주요 기능

### 📝 API 명세 편집기

- **직관적인 폼 기반 편집**: 복잡한 YAML을 작성할 필요 없이 폼으로 간편하게 작성
- **실시간 미리보기**: 작성 중인 명세를 즉시 확인
- **스키마 관리**: 재사용 가능한 스키마를 생성하고 참조
- **자동 완성**: Mock 데이터 표현식 자동 완성
- **Validation**: 입력값 실시간 검증

### 🗂️ 사이드바 네비게이션

- **엔드포인트 목록**: 모든 API 엔드포인트를 한눈에 확인
- **상태 필터링**: Mock, Implementing, Completed 상태별 필터링
- **HTTP 메서드별 그룹화**: GET, POST, PUT, DELETE 등으로 그룹화
- **검색 기능**: 경로, 메서드, 설명으로 빠른 검색

### 📊 API 프리뷰

- **요청/응답 카드**: 요청과 응답을 시각적으로 표시
- **코드 하이라이팅**: Syntax Highlighter를 통한 JSON/YAML 하이라이팅
- **코드 스니펫**: cURL, JavaScript, Python 등 다양한 언어의 예제 코드 생성
- **Markdown 내보내기**: API 문서를 Markdown 형식으로 내보내기

### 📥 Import/Export

- **YAML Import**: 외부 OpenAPI 파일 가져오기
- **검증 결과 표시**: Import 시 검증 에러를 시각적으로 표시
- **YAML Export**: 작성한 명세를 YAML 파일로 내보내기
- **Markdown Export**: API 문서를 Markdown으로 내보내기

---

## 🛠️ 기술 스택

### Core
- ⚛️ **React 19.1**: 최신 React 기능 활용
- 🔷 **TypeScript 5.9**: 타입 안정성 보장
- ⚡ **Vite 7.1**: 빠른 빌드 및 개발 서버

### UI/Styling
- 🎨 **TailwindCSS 3.4**: 유틸리티 우선 CSS 프레임워크
- 🎭 **PostCSS 8.4**: CSS 후처리

### State Management & Routing
- 🐻 **Zustand 5.0**: 경량 상태 관리
- 🚦 **React Router DOM 7.1**: 클라이언트 사이드 라우팅

### HTTP & Data
- 📡 **Axios 1.7**: HTTP 클라이언트
- 🎨 **React Syntax Highlighter 16.0**: 코드 하이라이팅

### Development Tools
- 🔍 **ESLint 9.x**: 코드 품질 검사
- 📝 **TypeScript ESLint 8.x**: TypeScript 전용 Lint 규칙

---

## 🚀 시작하기

### 전제 조건

- 📦 **Node.js**: 18.x 이상
- 📦 **npm**: 9.x 이상 또는 **yarn**: 1.22.x 이상

### 설치 및 실행

```bash
# 저장소 클론
git clone https://github.com/whitesnakegang/ouroboros.git
cd ouroboros/front

# 의존성 설치
npm install

# 개발 서버 실행 (http://localhost:5173)
npm run dev

# 프로덕션 빌드
npm run build

# 빌드 결과 미리보기
npm run preview

# Lint 검사
npm run lint
```

### 환경 변수 (선택 사항)

`.env` 파일 생성:

```bash
# Backend API Base URL
VITE_API_BASE_URL=http://localhost:8080

# Application Port
VITE_PORT=5173
```

---

## 📁 프로젝트 구조

```
front/
├── public/                    # 정적 파일
│   └── vite.svg
├── src/
│   ├── app/                   # 앱 설정
│   │   ├── layouts/           # 레이아웃 컴포넌트
│   │   │   └── MainLayout.tsx
│   │   └── providers/         # Context Providers
│   │       └── AppProvider.tsx
│   ├── assets/                # 정적 리소스
│   │   └── data/
│   │       └── fakerProviders.json
│   ├── features/              # 기능별 모듈
│   │   ├── sidebar/           # 사이드바 기능
│   │   │   ├── components/
│   │   │   │   ├── EndpointCard.tsx      # 엔드포인트 카드
│   │   │   │   ├── EndpointGroup.tsx     # 엔드포인트 그룹
│   │   │   │   ├── Sidebar.tsx           # 사이드바 메인
│   │   │   │   └── StatusFilter.tsx      # 상태 필터
│   │   │   └── store/
│   │   │       └── sidebar.store.ts      # 사이드바 상태 관리
│   │   └── spec/              # API 명세 기능
│   │       ├── components/
│   │       │   ├── ApiEditorLayout.tsx   # 편집기 레이아웃
│   │       │   ├── ApiPreviewCard.tsx    # 미리보기 카드
│   │       │   ├── ApiRequestCard.tsx    # 요청 카드
│   │       │   ├── ApiResponseCard.tsx   # 응답 카드
│   │       │   ├── CodeSnippetPanel.tsx  # 코드 스니펫
│   │       │   ├── FakerProviderSelect.tsx # Faker 선택기
│   │       │   ├── ImportResultModal.tsx # Import 결과 모달
│   │       │   ├── ProgressBar.tsx       # 진행 상태 바
│   │       │   ├── ProtocolTabs.tsx      # 프로토콜 탭
│   │       │   ├── SchemaModal.tsx       # 스키마 모달
│   │       │   ├── SpecForm.tsx          # 명세 폼
│   │       │   └── SpecToolbar.tsx       # 툴바
│   │       ├── services/
│   │       │   └── api.ts                # API 서비스
│   │       ├── store/
│   │       │   └── spec.store.ts         # 명세 상태 관리
│   │       └── utils/
│   │           ├── markdownExporter.ts   # Markdown 내보내기
│   │           └── yamlExporter.ts       # YAML 내보내기
│   ├── pages/                 # 페이지 컴포넌트
│   │   └── ExplorerPage.tsx
│   ├── App.tsx                # 루트 컴포넌트
│   ├── App.css                # 앱 스타일
│   ├── main.tsx               # 엔트리 포인트
│   └── index.css              # 글로벌 스타일
├── .eslintrc.config.js        # ESLint 설정
├── index.html                 # HTML 템플릿
├── package.json               # 프로젝트 메타데이터
├── postcss.config.js          # PostCSS 설정
├── tailwind.config.js         # TailwindCSS 설정
├── tsconfig.json              # TypeScript 설정 (기본)
├── tsconfig.app.json          # TypeScript 설정 (앱)
├── tsconfig.node.json         # TypeScript 설정 (Node)
└── vite.config.ts             # Vite 설정
```

---

## 💻 개발 가이드

### 컴포넌트 작성 규칙

#### 1. 함수형 컴포넌트 사용

```typescript
import React from 'react';

interface Props {
  title: string;
  onSubmit: (data: FormData) => void;
}

export const MyComponent: React.FC<Props> = ({ title, onSubmit }) => {
  // 컴포넌트 로직
  return (
    <div>
      {/* JSX */}
    </div>
  );
};
```

#### 2. TypeScript 활용

```typescript
// 타입 정의
interface ApiSpec {
  id: string;
  path: string;
  method: 'GET' | 'POST' | 'PUT' | 'DELETE';
  summary: string;
}

// Props 타입
interface ComponentProps {
  spec: ApiSpec;
  onUpdate: (spec: ApiSpec) => void;
}
```

#### 3. Zustand 상태 관리

```typescript
import { create } from 'zustand';

interface SpecStore {
  specs: ApiSpec[];
  selectedSpec: ApiSpec | null;
  setSelectedSpec: (spec: ApiSpec | null) => void;
  addSpec: (spec: ApiSpec) => void;
}

export const useSpecStore = create<SpecStore>((set) => ({
  specs: [],
  selectedSpec: null,
  setSelectedSpec: (spec) => set({ selectedSpec: spec }),
  addSpec: (spec) => set((state) => ({ specs: [...state.specs, spec] })),
}));
```

### API 통신

```typescript
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// API 호출 예시
export const createSpec = async (data: CreateSpecRequest) => {
  const response = await api.post('/ouro/rest-specs', data);
  return response.data;
};
```

### 스타일링

TailwindCSS 유틸리티 클래스 사용:

```tsx
<div className="flex items-center justify-between p-4 bg-white rounded-lg shadow-md">
  <h2 className="text-lg font-semibold text-gray-800">Title</h2>
  <button className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 transition">
    Action
  </button>
</div>
```

---

## 🏗️ 빌드 및 배포

### 프로덕션 빌드

```bash
# 빌드 실행
npm run build

# 빌드 결과는 dist/ 폴더에 생성됨
```

### 빌드 결과 구조

```
dist/
├── assets/
│   ├── index-[hash].js       # 번들된 JavaScript
│   └── index-[hash].css      # 번들된 CSS
├── index.html                 # 메인 HTML
└── vite.svg                   # 정적 파일
```

### Backend 통합 배포

프로덕션 환경에서는 빌드된 파일을 백엔드의 `static` 폴더에 복사:

```bash
# 빌드 후
npm run build

# 백엔드 리소스 폴더로 복사
cp -r dist/* ../backend/src/main/resources/static/

# 백엔드에서 접근: http://localhost:8080/
```

---

## 🧪 테스트

### Lint 검사

```bash
# ESLint 실행
npm run lint

# 자동 수정
npm run lint -- --fix
```

---

## 📚 추가 리소스

### React + Vite

이 템플릿은 Vite에서 React를 사용하기 위한 최소 설정을 제공합니다:

- **[@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react)**: Babel을 사용한 Fast Refresh
- **[@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react-swc)**: SWC를 사용한 Fast Refresh

### ESLint 설정 확장

프로덕션 애플리케이션을 개발 중이라면 타입 인식 lint 규칙을 활성화하는 것을 권장합니다:

```js
// eslint.config.js
export default defineConfig([
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      tseslint.configs.recommendedTypeChecked,
      tseslint.configs.stylisticTypeChecked,
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
    },
  },
])
```

---

## 🤝 기여하기

프론트엔드 개선에 기여하고 싶으시다면:

1. 저장소를 Fork합니다
2. Feature 브랜치를 생성합니다 (`feature/awesome-feature`)
3. 변경사항을 커밋합니다
4. 브랜치에 Push합니다
5. Pull Request를 생성합니다

자세한 내용은 [기여 가이드](../CONTRIBUTING.md)를 참조하세요.

---

## 📄 라이선스

이 프로젝트는 [Apache License 2.0](../LICENSE) 라이선스를 따릅니다.

---

<div align="center">

**Made with ❤️ by [Whitesnakegang](https://github.com/whitesnakegang)**

[🏠 메인 프로젝트로 돌아가기](../README.md)

</div>
