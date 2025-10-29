
# 🧭 PROJECT_OVERVIEW.md

## 1️⃣ 프로젝트명  
**Ouroboros QA SDK**  
(내부 명칭: *Newtopia QA Layer* / Cursor 협업용 QA Integration 모듈)

---

## 2️⃣ 프로젝트 배경

소규모 개발팀은 문서-코드 간 불일치, 테스트 환경 차이, 프로토콜 간 불균형으로 인해 QA 과정이 불안정하다.  
이를 해결하기 위해 Ouroboros SDK는 “**설계-구현-테스트 간 피드백 루프 자동화**”를 목표로 한다.

---

## 3️⃣ 한줄 요약
> **“실시간으로 구현 사항을 반영하여 설계 불일치 검증부터 간단한 동작 확인까지 도와주는 QA SDK”**

---

## 4️⃣ 주요 타겟

- **초기 스타트업 / 소규모 개발팀**
- **설계부터 병행 개발하는 프로젝트 팀**
- **OpenAPI, AsyncAPI, GraphQL 기반 협업 환경**

---

## 5️⃣ 주요 기능 개요

| 기능 | 설명 |
|------|------|
| 문서-구현 일치율 검증 | OpenAPI 명세와 실제 서버 응답을 자동 비교 |
| Mock API 자동 생성 | 미구현 엔드포인트 테스트용 가짜 API 생성 |
| 프로토콜별 테스트 통합 시각화 | REST, WebSocket, GraphQL 결과를 통합 확인 |
| API 명세서 자동 관리 | 코드 변경 시 문서 자동 업데이트 |
| QA Try 시스템 | 실시간 성능/병목 분석 실행 기능 |

---

## 6️⃣ 현재 단계

**1단계 (REST 기반 Try 계측)** 진행 중  
→ 향후 WebSocket → GraphQL → SSE 순 확장

**핵심 목표:**  
Try 요청만 계측 → Tempo에서 Trace 조회 → 병목 분석 → 결과 반환

---

## 7️⃣ 구조 개요

```
kr.co.ouroboros.core.qa/
├── controller/      # try-sessions 발급 및 결과 조회 API
├── filter/          # Try 요청 식별 필터 (REST)
├── tracing/         # Conditional Sampler (OpenTelemetry)
├── tempo/           # Tempo REST Client
├── analysis/        # Span 트리 및 병목 디텍터
├── result/          # 결과 저장/조회
├── session/         # tryId 발급 및 Registry 관리
└── config/          # 설정 프로퍼티 및 Sampler 등록
```

---

## 8️⃣ 동작 흐름 요약

```
UI → POST /ouroboros/tries (tryId 발급)
     ↓
실제 API 호출 (/api/orders/123, X-Ouroboros-Try 헤더 포함)
     ↓
OpenTelemetry Sampler (Try 요청만 record)
     ↓
Grafana Tempo 저장
     ↓
GET /ouroboros/tries/{tryId} → 분석 및 결과 JSON 응답
```

---

## 9️⃣ 핵심 설계 개념

| 구분 | 설명 |
|------|------|
| Stateless API | JWS 없이 TTL/IP 바인딩 기반 세션 관리 |
| 조건부 트레이싱 | Try 요청만 계측 (Baggage 기반) |
| 세션 식별 | X-Ouroboros-Try 헤더로 요청 구분 |
| Tempo Polling | 인덱싱 대기 후 Trace 조회 |
| 결과 저장 | 단일 테이블 `ouroboros_try_log` |
| 확장성 | REST → WebSocket → GraphQL → SSE |

---

## 🔄 단계별 로드맵

| 단계 | 목표 | 주요 기술 |
|------|------|-----------|
| 1단계 (현재) | REST 기반 Try 계측 | OpenTelemetry + Tempo |
| 2단계 | WebSocket 확장 | STOMP, Interceptor |
| 3단계 | GraphQL 계측 | GraphQL Instrumentation |
| 4단계 | SSE 자동화 | AsyncAPI + SSE |

---

## 📋 Cursor 협업 포인트

- SDK는 **서버 내에 주입되는 경량 계측 모듈**
- **Try 요청만** 계측 → 일반 요청은 trace 생성 금지
- **서명(JWS)** 없음, 세션 TTL/IP로 제한
- **단일 테이블(`ouroboros_try_log`)** 구조 유지
- Cursor는 각 기능을 **클래스 단위로 생성 후 테스트 주도 개발** 방식으로 진행

---

## 🎯 구현 목표 (Cursor용)

- 세션 발급 API + REST 필터 + Conditional Sampler 완성
- TempoClient + TraceAnalysisService + ResultController 연결
- 병목 탐지(Slow DB, N+1) 동작 확인
- Try 없는 요청은 trace 미생성

---

## ✅ 주요 수용 기준 (AC)

- AC-01: Try 없는 요청은 trace 생성 안 됨  
- AC-02: Try 요청은 항상 trace 기록  
- AC-03: TraceQL로 tryId 기반 검색 가능  
- AC-04: 병목 탐지 결과 반환  
- AC-05: 저장 실패 시 경고만 남김  
- AC-06: Tempo 폴링 평균 8초 이내 응답  

---

## 🧾 참고 문서

- [OpenTelemetry Docs](https://opentelemetry.io/)
- [Grafana Tempo](https://grafana.com/oss/tempo/)
- [AsyncAPI Spec](https://www.asyncapi.com/)
- 내부 Notion 문서: *“사용자 DB 및 QA 스키마 자동 생성 전략”*
