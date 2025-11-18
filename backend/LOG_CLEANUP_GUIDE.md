# 백엔드 로그 정리 가이드

이 문서는 개발 과정에서 검증을 위해 추가한 로그 중 삭제해야 할 항목과 유지해야 할 항목을 정리합니다.

## 🗑️ 삭제 대상 로그

### 1. 모든 `log.debug()` 로그
**이유**: 디버깅 전용이므로 프로덕션에서는 불필요합니다.

**예시**:
- `log.debug("Span does not have tryId attribute, skipping")`
- `log.debug("Added span to in-memory storage: tryId={}, traceId={}")`
- `log.debug("Created send-only operation: {} (reply: {})")`
- `log.debug("Synced channel '{}' from cache to file")`
- `log.debug("Map is null for key '{}'")`
- `log.debug("Channel {} already has prefix, skipping normalization")`
- `log.debug("Created new channel: {} with address: {}")`
- `log.debug("Reply channel not found: {}")`
- `log.debug("Added reply channel: {} with address: {}")`
- `log.debug("Updated message reference: {} -> {}")`
- `log.debug("Reusing existing server: {}")`
- `log.debug("Auto-created server: {}")`
- `log.debug("Updated $ref: {} -> {}")`
- `log.debug("Synced schema '{}' (as '{}') from cache to file")`
- `log.debug("Synced message '{}' (as '{}') from cache to file")`
- `log.debug("Retrieving trace data for tryId: {}")`
- `log.debug("Added tryId attribute to span: {}")`
- `log.debug("Shutting down InMemoryTrySpanProcessor")`
- `log.debug("Force flushing InMemoryTrySpanProcessor")`

### 2. Bean 생성 및 설정 로그
**이유**: Spring Bean 생성 과정은 프레임워크가 자동으로 처리하므로 사용자에게 불필요합니다.

**예시**:
- `log.info("Creating TempoTrySpanProcessor bean (Tempo enabled)")`
- `log.info("TempoTrySpanProcessor bean created successfully")`
- `log.info("Creating InMemoryTrySpanProcessor bean (Tempo disabled)")`
- `log.info("InMemoryTrySpanProcessor bean created successfully")`
- `log.info("Registering TryStompChannelInterceptor for inbound channel")`
- `log.info("Registering TryStompOutboundChannelInterceptor for outbound channel")`
- `log.info("TryOnlySampler bean created successfully")`

### 3. 내부 처리 과정 로그
**이유**: 내부 동기화, 정규화, 참조 업데이트 등은 사용자에게 보일 필요가 없습니다.

**예시**:
- `log.info("Detected application destination prefix from WebSocket config: {}")`
- `log.info("Using broker prefixes: {}")`
- `log.info("Synced cache-only operation '{}' to file (ID: {})")` - 동기화는 자동이므로 불필요
- `log.info("Created server ({}://{}) in file")` - 자동 생성이므로 불필요
- `log.info("Synced {} missing schema(s) from cache to file")` - 자동 동기화이므로 불필요
- `log.info("Synced {} missing message(s) from cache to file")` - 자동 동기화이므로 불필요
- `log.info("Deleted trace from in-memory storage: tryId={}, traceId={}")` - 내부 스토리지 관리
- `log.debug("Springwolf-based WebSocket handler is active")` - 이미 debug이므로 삭제
- `log.debug("Basic WebSocket handler is active (Springwolf disabled)")` - 이미 debug이므로 삭제

### 4. 개발 검증용 로그
**이유**: 개발 중 검증을 위해 추가한 로그입니다.

**예시**:
- `log.info("✓ SecuritySchemes in openApiDoc before save: {}")` - 개발 중 검증용
- `log.info("Auto-created {} missing schema(s)")` - 자동 생성은 사용자에게 알릴 필요 없음 (중요한 경우만 유지)
- `log.info("Auto-created security scheme: {}")` - 자동 생성은 사용자에게 알릴 필요 없음

### 5. 내부 참조 업데이트 로그
**이유**: 내부적으로 처리되는 참조 업데이트는 사용자에게 불필요합니다.

**예시**:
- `log.debug("🔗 Updated channel message reference: {} -> {}")`
- `log.debug("🔗 Updated channel message $ref: {} -> {}")`
- `log.debug("🔗 Updated operation message $ref: {} -> {}")`
- `log.debug("🔗 Updated reply message $ref: {} -> {}")`

### 6. 내부 채널 관리 로그
**예시**:
- `log.debug("Auto-created channel: {} (address: {})")` - 자동 생성이므로 불필요

## ✅ 유지 대상 로그

### 1. 모든 에러 로그 (`log.error()`)
**이유**: 에러는 항상 사용자에게 중요합니다.

**예시**:
- 모든 `log.error()` 호출

### 2. 모든 경고 로그 (`log.warn()`)
**이유**: 경고는 사용자에게 알려야 할 중요한 정보입니다.

**예시**:
- `log.warn("⚠️ SecuritySchemes is null after autoCreate!")`
- `log.warn("No application destination prefix detected, using default: {}")`
- `log.warn("SimpAnnotationMethodMessageHandler not found, using default application prefix: {}")`
- 모든 타입 검증 경고 (`Expected String for key '{}' but got {}`)

### 3. 사용자 작업 결과 로그
**이유**: 사용자가 수행한 작업의 결과를 확인할 수 있어야 합니다.

**예시**:
- `log.info("Created REST API spec: {} {} (ID: {})")`
- `log.info("Updated REST API spec: {} {} (ID: {})")`
- `log.info("Deleted REST API spec: {} {} (ID: {})")`
- `log.info("Created schema: {}")`
- `log.info("Updated schema: {}")`
- `log.info("Deleted schema: {}")`
- `log.info("Created WebSocket schema: {}")`
- `log.info("Updated WebSocket schema: {}")`
- `log.info("Deleted WebSocket schema: {}")`
- `log.info("Created WebSocket message: {}")`
- `log.info("Updated WebSocket message: {}")`
- `log.info("Deleted WebSocket message: {}")`
- `log.info("Created {} WebSocket operations")`
- `log.info("Updated WebSocket operation: {}")`
- `log.info("Deleted WebSocket operation: {}")`

### 4. 사용자 요청 처리 로그
**이유**: 사용자가 요청한 작업의 진행 상황을 확인할 수 있어야 합니다.

**예시**:
- `log.info("Retrieving trace for tryId: {}")` - 사용자 요청
- `log.info("Deleting trace for tryId: {}")` - 사용자 요청
- `log.info("Retrieving summary for tryId: {}")` - 사용자 요청
- `log.info("Retrieving method list for tryId: {}, page: {}, size: {}")` - 사용자 요청
- `log.info("Retrieving issues for tryId: {}")` - 사용자 요청
- `log.info("Received AsyncAPI import request: filename={}, size={}")` - 사용자 요청

### 5. 파일 Import/Export 결과 로그
**이유**: 사용자가 수행한 Import/Export 작업의 결과를 확인할 수 있어야 합니다.

**예시**:
- `log.info("========================================")`
- `log.info("📥 Starting AsyncAPI YAML import...")`
- `log.info("✅ AsyncAPI YAML Import Completed")`
- `log.info("   📊 Servers imported: {}")`
- `log.info("   📊 Channels imported: {}")`
- `log.info("   📊 Operations imported: {}")`
- `log.info("   📊 Schemas imported: {}")`
- `log.info("   📊 Messages imported: {}")`
- `log.info("   📊 Items renamed: {}")`
- `log.info("🔄 Schema '{}' renamed to '{}' due to duplicate")`
- `log.info("🔄 Message '{}' renamed to '{}' due to duplicate")`
- `log.info("🔄 Channel '{}' renamed to '{}' due to duplicate")`
- `log.info("🔄 Server '{}' renamed to '{}' due to duplicate")`
- `log.info("🔄 Operation '{}' ({}) renamed to '{}' due to duplicate")`

### 6. 파일 검증 결과 로그
**이유**: 사용자가 확인해야 할 중요한 검증 결과입니다.

**예시**:
- `log.info("========================================")`
- `log.info("📝 Starting ourorest.yml validation...")`
- `log.info("✅ Successfully parsed ourorest.yml")`
- `log.info("💾 Saved enriched ourorest.yml")`
- `log.info("✅ ourorest.yml Validation Completed")`
- `log.info("   📊 Operations enriched: {}")`
- `log.info("   📊 Schemas enriched: {}")`
- `log.info("   📦 Missing schemas auto-created: {}")`
- `log.info("========================================")`
- `log.info("📝 Starting ourowebsocket.yml validation...")`
- `log.info("✅ ourowebsocket.yml Validation Completed")`
- `log.info("   📊 Channels enriched: {}")`
- `log.info("   📊 Operations enriched: {}")`
- `log.info("   📊 Messages enriched: {}")`
- `log.info("🔧 Fixed: Added missing components.schemas")`
- `log.info("🔧 Fixed: Added missing components.messages")`

### 7. 중요한 자동 생성 로그 (선택적)
**이유**: 사용자가 알아야 할 중요한 자동 생성 작업입니다.

**예시**:
- `log.info("Deleted unused channel: {} (no longer referenced by any operation)")` - 정리 작업이므로 유지

## 📋 정리 작업 체크리스트

### Phase 1: Debug 로그 삭제
- [ ] 모든 `log.debug()` 호출 삭제 또는 주석 처리

### Phase 2: Bean 생성 로그 삭제
- [ ] `TraceStorageConfig.java` - Bean 생성 로그 삭제
- [ ] `TryStompConfig.java` - Interceptor 등록 로그 삭제

### Phase 3: 내부 처리 로그 삭제
- [ ] `WebSocketPrefixProperties.java` - prefix 감지 로그 삭제
- [ ] `WebSocketOperationServiceImpl.java` - 동기화 관련 debug 로그 삭제
- [ ] `InMemoryTraceStorage.java` - 내부 스토리지 관리 로그 삭제
- [ ] `ChannelAddressNormalizer.java` - 정규화 과정 로그 삭제
- [ ] `WebSocketServerManager.java` - 서버 관리 로그 삭제
- [ ] `WebSocketReferenceUpdater.java` - 참조 업데이트 로그 삭제
- [ ] `WebSocketChannelManager.java` - 채널 자동 생성 로그 삭제
- [ ] `RestApiSpecServiceimpl.java` - 개발 검증용 로그 삭제
- [ ] `EndpointDiffHelper.java` - 내부 동기화 로그 삭제

### Phase 4: 검증 및 테스트
- [ ] 애플리케이션 실행 후 로그 확인
- [ ] 사용자 작업 로그가 정상적으로 출력되는지 확인
- [ ] 에러/경고 로그가 정상적으로 출력되는지 확인

## 💡 로그 레벨 권장사항

프로덕션 환경에서는 다음 로그 레벨을 권장합니다:

```properties
# application.properties
logging.level.kr.co.ouroboros=INFO
logging.level.root=WARN
```

이렇게 설정하면:
- `INFO` 레벨: 사용자 작업 결과, Import/Export 결과, 검증 결과만 출력
- `WARN` 레벨: 경고 메시지 출력
- `ERROR` 레벨: 에러 메시지 출력
- `DEBUG` 레벨: 출력되지 않음 (이미 삭제했으므로)

## 📝 참고사항

- **에러 로그는 절대 삭제하지 마세요**: 모든 `log.error()` 호출은 유지해야 합니다.
- **경고 로그도 유지**: `log.warn()` 호출도 사용자에게 중요한 정보이므로 유지합니다.
- **사용자 작업 로그는 필수**: 사용자가 수행한 CRUD 작업의 결과는 반드시 로그로 남겨야 합니다.
- **자동 생성 로그는 선택적**: 자동 생성은 일반적으로 조용히 처리하되, 중요한 경우(예: 파일 생성, 정리 작업)만 로그를 남깁니다.

