# DemoApiGen Editor - React Frontend

React 기반 웹 UI로 API 엔드포인트를 시각적으로 편집할 수 있습니다.

## 개발 환경 설정

### 1. 의존성 설치

```bash
cd frontend
npm install
```

### 2. 개발 서버 실행

```bash
npm run dev
```

개발 서버가 `http://localhost:5173`에서 실행됩니다. API 요청은 자동으로 `http://localhost:8080`으로 프록시됩니다.

### 3. 프로덕션 빌드

```bash
npm run build
```

빌드된 파일은 `../src/main/resources/static/demoapigen` 디렉토리에 생성됩니다.

## 기술 스택

- **React 18**: UI 프레임워크
- **Vite**: 빌드 도구
- **Axios**: HTTP 클라이언트

## 프로젝트 구조

```
frontend/
├── src/
│   ├── components/
│   │   ├── EndpointList.jsx      # 엔드포인트 목록
│   │   ├── EndpointEditor.jsx    # 엔드포인트 편집기
│   │   ├── FieldEditor.jsx       # 필드 편집기
│   │   └── PreviewPanel.jsx      # 더미 데이터 미리보기
│   ├── App.jsx                   # 메인 앱 컴포넌트
│   ├── main.jsx                  # 엔트리 포인트
│   └── index.css                 # 글로벌 스타일
├── index.html                    # HTML 템플릿
├── vite.config.js                # Vite 설정
└── package.json                  # 의존성 및 스크립트
```

## 주요 기능

### 1. 엔드포인트 관리
- 엔드포인트 추가/수정/삭제
- HTTP 메소드 선택 (GET, POST, PUT, DELETE, PATCH)
- 경로 및 설명 편집

### 2. 응답 구조 정의
- 응답 타입 선택 (object, array, string, number, boolean)
- 필드 추가/삭제
- 필드 타입 선택 (string, number, boolean, object, array, faker)
- Faker 타입 지정 (예: name.fullName, internet.email)

### 3. 실시간 미리보기
- Preview 버튼으로 더미 데이터 즉시 생성
- JSON 형식으로 표시

### 4. 저장 및 리로드
- Save 버튼으로 yml 파일에 저장
- Reload 버튼으로 현재 yml 파일 다시 로드

## API 엔드포인트

### GET /demoapigen/api/definition
현재 API 정의를 가져옵니다.

### POST /demoapigen/api/definition
API 정의를 저장하고 엔드포인트를 다시 로드합니다.

### POST /demoapigen/api/preview
엔드포인트의 더미 데이터 미리보기를 생성합니다.
