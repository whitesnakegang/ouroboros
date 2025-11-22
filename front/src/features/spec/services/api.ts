// 백엔드 API 서비스

// 환경변수에서 백엔드 URL 가져오기 (Vite는 import.meta.env 사용)
const API_BASE_URL = "/ouro/rest-specs";

/**
 * GlobalApiResponse 에러 응답 파싱 헬퍼
 */
async function parseErrorResponse(
  response: Response,
  defaultMessage: string
): Promise<string> {
  try {
    const errorResponse = await response.json();
    // GlobalApiResponse 구조: { status, data, message, error }
    let errorMessage = errorResponse.message || defaultMessage;
    if (errorResponse.error?.details) {
      errorMessage += `\n상세: ${errorResponse.error.details}`;
    }
    return errorMessage;
  } catch (e) {
    console.error("에러 응답 파싱 실패:", e);
    return defaultMessage;
  }
}

export interface RestApiSpecResponse {
  id: string;
  path: string;
  method: string;
  summary?: string;
  description?: string;
  tags?: string[];
  parameters?: unknown[];
  requestBody?: unknown;
  responses?: Record<string, unknown>;
  security?: unknown[];
  progress: string; // Response에만 유지 (읽기 전용)
  tag: string; // Response에만 유지 (읽기 전용)
  diff?: string; // Response에만 유지
}

export interface GetAllSpecsResponse {
  status: number;
  data: RestApiSpecResponse[];
  message: string;
}

export interface CreateApiRequest {
  path: string;
  method: string;
  summary?: string;
  description?: string;
  tags?: string[];
  parameters?: unknown[];
  requestBody?: unknown;
  responses?: Record<string, unknown>;
  security?: unknown[];
}

export interface CreateApiResponse {
  status: number;
  data: RestApiSpecResponse;
  message: string;
}

export interface UpdateApiRequest {
  path?: string;
  method?: string;
  summary?: string;
  description?: string;
  tags?: string[];
  parameters?: unknown[];
  requestBody?: unknown;
  responses?: Record<string, unknown>;
  security?: unknown[];
}

export interface UpdateApiResponse {
  status: number;
  data: RestApiSpecResponse;
  message: string;
}

export interface GetSpecResponse {
  status: number;
  data: RestApiSpecResponse;
  message: string;
}

export interface DeleteApiResponse {
  status: number;
  message: string;
}

// Schema 관련 인터페이스
export interface SchemaField {
  name: string;
  type: string;
  description?: string;
  mockExpression?: string;
  ref?: string;
}

export interface SchemaResponse {
  schemaName: string;
  type: string;
  title?: string;
  description?: string;
  properties?: Record<string, SchemaField>;
  required?: string[];
  orders?: string[];
  items?: any; // array 타입일 경우
  xmlName?: string;
}

export interface CreateSchemaRequest {
  schemaName: string;
  type?: string;
  title?: string;
  description?: string;
  properties?: Record<string, SchemaField>;
  required?: string[];
  orders?: string[];
  xmlName?: string;
}

export interface UpdateSchemaRequest {
  type?: string;
  title?: string;
  description?: string;
  properties?: Record<string, SchemaField>;
  required?: string[];
  orders?: string[];
  xmlName?: string;
}

export interface GetAllSchemasResponse {
  status: number;
  data: SchemaResponse[];
  message: string;
}

export interface GetSchemaResponse {
  status: number;
  data: SchemaResponse;
  message: string;
}

export interface CreateSchemaResponse {
  status: number;
  data: SchemaResponse;
  message: string;
}

export interface UpdateSchemaResponse {
  status: number;
  data: SchemaResponse;
  message: string;
}

export interface DeleteSchemaResponse {
  status: number;
  message: string;
}

// YAML Import 관련 인터페이스
export interface RenamedItem {
  type: "api" | "schema";
  original: string;
  renamed: string;
  method?: string; // API 항목에만 존재
}

export interface ImportYamlData {
  imported: number;
  renamed: number;
  summary: string;
  renamedList: RenamedItem[];
}

export interface ImportYamlResponse {
  status: number;
  data: ImportYamlData;
  message: string;
  error?: {
    code: string;
    details: string;
  };
}

export interface ValidationError {
  location: string;
  errorCode: string;
  message: string;
}

export interface ImportYamlErrorData {
  validationErrors?: ValidationError[];
}

export interface ImportYamlErrorResponse {
  status: number;
  data: ImportYamlErrorData | null;
  message: string;
  error: {
    code: string;
    details: string;
  };
}

/**
 * 전체 REST API 스펙 조회
 */
export async function getAllRestApiSpecs(): Promise<GetAllSpecsResponse> {
  const response = await fetch(API_BASE_URL, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      "Cache-Control": "no-cache",
    },
    cache: "no-store", // 브라우저 HTTP 캐시 방지
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `API 스펙 조회 실패: ${response.status} ${response.statusText}`
    );
    console.error("API 스펙 호출 실패:", {
      url: API_BASE_URL,
      status: response.status,
    });
    throw new Error(errorMessage);
  }

  return response.json();
}

/**
 * 새로운 REST API 스펙 생성
 */
export async function createRestApiSpec(
  request: CreateApiRequest
): Promise<CreateApiResponse> {
  const response = await fetch(API_BASE_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `API 스펙 생성 실패: ${response.status} ${response.statusText}`
    );
    console.error("API 스펙 생성 실패:", { request, status: response.status });
    throw new Error(errorMessage);
  }

  return response.json();
}

/**
 * 특정 REST API 스펙 조회
 */
export async function getRestApiSpec(id: string): Promise<GetSpecResponse & { reqLog?: string; resLog?: string }> {
  const response = await fetch(`${API_BASE_URL}/${id}`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      "Cache-Control": "no-cache",
    },
    cache: "no-store", // 브라우저 HTTP 캐시 방지
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `API 스펙 조회 실패: ${response.status} ${response.statusText}`
    );
    console.error("API 스펙 조회 실패:", { id, status: response.status });
    throw new Error(errorMessage);
  }

  const data = await response.json();
  
  // 응답 헤더에서 diff 로그 정보 추출
  const headerReqLog = response.headers.get("x-ouroboros-req-log") || response.headers.get("X-Ouroboros-Req-Log");
  const headerResLog = response.headers.get("x-ouroboros-res-log") || response.headers.get("X-Ouroboros-Res-Log");
  
  // 응답 body에서도 diff 로그 정보 추출
  // data.reqLog/resLog (최상위 레벨) 또는 data.data.reqLog/resLog (data 객체 내부) 확인
  const bodyReqLog = data.reqLog || (data.data && data.data.reqLog);
  const bodyResLog = data.resLog || (data.data && data.data.resLog);
  
  // 헤더 우선, 없으면 body에서 가져옴
  const reqLog = headerReqLog || bodyReqLog;
  const resLog = headerResLog || bodyResLog;
  
  return {
    ...data,
    reqLog: reqLog || undefined,
    resLog: resLog || undefined,
  };
}

/**
 * REST API 스펙 수정
 */
export async function updateRestApiSpec(
  id: string,
  request: UpdateApiRequest
): Promise<UpdateApiResponse> {
  const response = await fetch(`${API_BASE_URL}/${id}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `API 스펙 수정 실패: ${response.status} ${response.statusText}`
    );
    console.error("API 스펙 수정 실패:", {
      id,
      request,
      status: response.status,
    });
    throw new Error(errorMessage);
  }

  return response.json();
}

/**
 * REST API 스펙 삭제
 */
export async function deleteRestApiSpec(
  id: string
): Promise<DeleteApiResponse> {
  const response = await fetch(`${API_BASE_URL}/${id}`, {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `API 스펙 삭제 실패: ${response.status} ${response.statusText}`
    );
    console.error("API 스펙 삭제 실패:", { id, status: response.status });
    throw new Error(errorMessage);
  }

  return response.json();
}

/**
 * REST API 스펙 동기화 (캐시 -> 파일)
 */
export async function syncRestApiSpec(
  id: string
): Promise<GetSpecResponse> {
  const response = await fetch(`${API_BASE_URL}/${id}/sync`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `API 스펙 동기화 실패: ${response.status} ${response.statusText}`
    );
    throw new Error(errorMessage);
  }

  return response.json();
}

// REST Schema 관련 API 함수들
const SCHEMA_API_BASE_URL = "/ouro/rest-specs/schemas";

// WebSocket Schema 관련 API 함수들
const WS_SCHEMA_API_BASE_URL = "/ouro/websocket-specs/schemas";

/**
 * 전체 Schema 조회
 */
export async function getAllSchemas(): Promise<GetAllSchemasResponse> {
  const response = await fetch(SCHEMA_API_BASE_URL, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `Schema 조회 실패: ${response.status} ${response.statusText}`
    );
    console.error("Schema API 호출 실패:", {
      url: SCHEMA_API_BASE_URL,
      status: response.status,
    });
    throw new Error(errorMessage);
  }

  return response.json();
}

/**
 * 특정 Schema 조회
 */
export async function getSchema(
  schemaName: string
): Promise<GetSchemaResponse> {
  const response = await fetch(`${SCHEMA_API_BASE_URL}/${schemaName}`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `Schema 조회 실패: ${response.status} ${response.statusText}`
    );
    console.error("Schema 조회 실패:", { schemaName, status: response.status });
    throw new Error(errorMessage);
  }

  return response.json();
}

/**
 * 새로운 Schema 생성
 */
export async function createSchema(
  request: CreateSchemaRequest
): Promise<CreateSchemaResponse> {
  const response = await fetch(SCHEMA_API_BASE_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `Schema 생성 실패: ${response.status} ${response.statusText}`
    );
    console.error("Schema 생성 실패:", { request, status: response.status });
    throw new Error(errorMessage);
  }

  return response.json();
}

/**
 * Schema 수정
 */
export async function updateSchema(
  schemaName: string,
  request: UpdateSchemaRequest
): Promise<UpdateSchemaResponse> {
  const response = await fetch(`${SCHEMA_API_BASE_URL}/${schemaName}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `Schema 수정 실패: ${response.status} ${response.statusText}`
    );
    console.error("Schema 수정 실패:", {
      schemaName,
      request,
      status: response.status,
    });
    throw new Error(errorMessage);
  }

  return response.json();
}

/**
 * Schema 삭제
 */
export async function deleteSchema(
  schemaName: string
): Promise<DeleteSchemaResponse> {
  const response = await fetch(`${SCHEMA_API_BASE_URL}/${schemaName}`, {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `Schema 삭제 실패: ${response.status} ${response.statusText}`
    );
    console.error("Schema 삭제 실패:", { schemaName, status: response.status });
    throw new Error(errorMessage);
  }

  return response.json();
}

/**
 * YAML 파일 Import
 * OpenAPI 3.1.0 YAML 파일을 업로드하여 ourorest.yml에 병합
 */
export async function importYaml(file: File): Promise<ImportYamlResponse> {
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(`${API_BASE_URL}/import`, {
    method: "POST",
    body: formData,
    // multipart/form-data는 브라우저가 자동으로 설정
  });

  if (!response.ok) {
    let errorMessage = `YAML Import 실패: ${response.status} ${response.statusText}`;

    // 검증 에러 상세 정보 추출
    try {
      const errorData: ImportYamlErrorResponse = await response.json();

      // GlobalApiResponse 구조에서 메시지 추출
      if (errorData.message) {
        errorMessage = errorData.message;
      }

      if (errorData.error?.details) {
        errorMessage += `\n상세: ${errorData.error.details}`;
      }

      // 검증 에러가 있으면 상세 표시
      if (errorData.data?.validationErrors) {
        const validationMessages = errorData.data.validationErrors
          .map((err) => `- ${err.location}: ${err.message}`)
          .join("\n");
        errorMessage = `YAML 검증 실패:\n${validationMessages}\n\n${errorMessage}`;
      }
    } catch (e) {
      console.error("YAML Import 에러 응답 파싱 실패:", e);
    }

    console.error("YAML Import 실패:", { status: response.status });
    throw new Error(errorMessage);
  }

  return response.json();
}

/**
 * YAML 파일 Export
 * 백엔드에서 ourorest.yml 파일 전체 내용을 가져옵니다.
 */
export async function exportYaml(): Promise<string> {
  const response = await fetch(`${API_BASE_URL}/export/yaml`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `YAML Export 실패: ${response.status} ${response.statusText}`
    );
    console.error("YAML Export 실패:", { status: response.status });
    throw new Error(errorMessage);
  }

  return response.text();
}

// ========== WebSocket Spec Import/Export ==========
const WS_SPEC_BASE_URL = "/ouro/websocket-specs";

export interface WsImportYamlErrorData {
  validationErrors?: ValidationError[];
}

export interface WsImportYamlErrorResponse {
  status: number;
  data: WsImportYamlErrorData | null;
  message: string;
  error: {
    code: string;
    details: string;
  };
}

/**
 * WebSocket YAML 파일 Import
 * AsyncAPI 3.0.0 YAML 파일을 업로드하여 ourowebsocket.yml에 병합
 */
export async function importWebSocketYaml(file: File): Promise<any> {
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(`${WS_SPEC_BASE_URL}/import`, {
    method: "POST",
    body: formData,
  });

  if (!response.ok) {
    let errorMessage = `WS YAML Import 실패: ${response.status} ${response.statusText}`;
    try {
      const errorData: WsImportYamlErrorResponse = await response.json();
      if (errorData.message) {
        errorMessage = errorData.message;
      }
      if (errorData.error?.details) {
        errorMessage += `\n상세: ${errorData.error.details}`;
      }
      if (errorData.data?.validationErrors) {
        const validationMessages = errorData.data.validationErrors
          .map((err) => `- ${err.location}: ${err.message}`)
          .join("\n");
        errorMessage = `YAML 검증 실패:\n${validationMessages}\n\n${errorMessage}`;
      }
    } catch (e) {
      console.error("WS YAML Import 에러 응답 파싱 실패:", e);
    }
    console.error("WS YAML Import 실패:", { status: response.status });
    throw new Error(errorMessage);
  }

  return response.json();
}

/**
 * WebSocket YAML 파일 Export
 * 백엔드에서 ourowebsocket.yml 파일 전체 내용을 가져옵니다.
 */
export async function exportWebSocketYaml(): Promise<string> {
  const response = await fetch(`${WS_SPEC_BASE_URL}/export/yaml`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `WS YAML Export 실패: ${response.status} ${response.statusText}`
    );
    console.error("WS YAML Export 실패:", { status: response.status });
    throw new Error(errorMessage);
  }

  return response.text();
}

// Try 관련 API 인터페이스
export interface TryMethodParameter {
  type: string;
  name: string;
}

export interface TryMethod {
  spanId: string;
  name: string;
  methodName: string;
  className: string;
  parameters?: TryMethodParameter[] | null;
  selfDurationMs: number;
  selfPercentage: number;
}

export interface TryMethodListData {
  tryId: string;
  traceId: string | null;
  totalDurationMs: number;
  totalCount: number;
  page: number;
  size: number;
  hasMore: boolean;
  methods: TryMethod[];
}

export interface TryMethodListResponse {
  status: number;
  data: TryMethodListData;
  message: string;
  error?: {
    code: string;
    details: string;
  } | null;
}

const TRY_API_BASE_URL = "/ouro/tries";

/**
 * Try 메소드 리스트 조회 (페이지네이션)
 */
export async function getTryMethodList(
  tryId: string,
  page: number = 0,
  size: number = 100
): Promise<TryMethodListResponse> {
  const response = await fetch(
    `${TRY_API_BASE_URL}/${tryId}/methods?page=${page}&size=${size}`,
    {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    }
  );

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `Try 메소드 리스트 조회 실패: ${response.status} ${response.statusText}`
    );
    console.error("Try 메소드 리스트 조회 실패:", {
      tryId,
      status: response.status,
    });
    throw new Error(errorMessage);
  }

  return response.json();
}

// Try Trace 관련 인터페이스
export interface TryTraceParameter {
  type: string;
  name: string;
}

export interface TryTraceSpan {
  spanId: string;
  name: string;
  className: string;
  methodName: string;
  parameters?: TryTraceParameter[] | null;
  durationMs: number;
  selfDurationMs: number;
  percentage: number;
  selfPercentage: number;
  kind: string;
  children: TryTraceSpan[];
}

export interface TryTraceData {
  tryId: string;
  traceId: string | null;
  totalDurationMs: number;
  spans: TryTraceSpan[];
}

export interface TryTraceResponse {
  status: number;
  data: TryTraceData;
  message: string;
  error?: {
    code: string;
    details: string;
  } | null;
}

/**
 * Try Trace 조회
 */
export async function getTryTrace(tryId: string): Promise<TryTraceResponse> {
  const response = await fetch(`${TRY_API_BASE_URL}/${tryId}/trace`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `Try Trace 조회 실패: ${response.status} ${response.statusText}`
    );
    console.error("Try Trace 조회 실패:", {
      tryId,
      status: response.status,
    });
    throw new Error(errorMessage);
  }

  return response.json();
}

// ========== WebSocket Types ==========

// WebSocket Operation 관련 인터페이스
export interface ChannelReference {
  ref: string;
}

export interface MessageReference {
  ref: string;
}

export interface ReplyInfo {
  channel: ChannelReference;
  messages: MessageReference[];
}

export interface Operation {
  action: "receive" | "send";
  channel: ChannelReference;
  messages: MessageReference[];
  reply?: ReplyInfo | null;
  bindings?: { stomp?: {} };
  id?: string;
  entrypoint?: string;
  diff?: string;
  progress?: string;
}

export interface OperationResponse {
  operationName: string;
  tag?: string;
  operation: Operation;
  protocol?: "ws" | "wss" | null; // WebSocket protocol (ws/wss/null)
}

export interface ChannelMessageInfo {
  address?: string | null;
  channelRef?: string | null;
  messages?: string[];
}

export interface CreateOperationRequest {
  protocol: string;
  pathname: string;
  receives?: ChannelMessageInfo[] | null;
  replies?: ChannelMessageInfo[] | null;
  tags?: string[]; // 백엔드에서 관리할 tags 필드 추가
}

export interface UpdateOperationRequest {
  protocol?: string;
  pathname?: string;
  receive?: ChannelMessageInfo;
  reply?: ChannelMessageInfo;
  tags?: string[]; // 백엔드에서 관리할 tags 필드 추가
  progress?: string; // progress 수동 관리용 (none/completed)
}

export interface GetAllOperationsResponse {
  status: number;
  data: OperationResponse[];
  message: string;
}

export interface GetOperationResponse {
  status: number;
  data: OperationResponse;
  message: string;
}

export interface CreateOperationResponse {
  status: number;
  data: OperationResponse[];
  message: string;
}

export interface UpdateOperationResponse {
  status: number;
  data: OperationResponse;
  message: string;
}

export interface DeleteOperationResponse {
  status: number;
  message: string;
}

// WebSocket Message 관련 인터페이스
export interface MessageResponse {
  messageName: string;
  name?: string;
  contentType?: string;
  description?: string;
  headers?: any;
  payload?: any;
}

export interface CreateMessageRequest {
  messageName: string;
  name?: string;
  contentType?: string;
  description?: string;
  headers?: any;
  payload?: any;
}

export interface UpdateMessageRequest {
  name?: string;
  contentType?: string;
  description?: string;
  headers?: any;
  payload?: any;
}

export interface GetAllMessagesResponse {
  status: number;
  data: MessageResponse[];
  message: string;
}

export interface GetMessageResponse {
  status: number;
  data: MessageResponse;
  message: string;
}

export interface CreateMessageResponse {
  status: number;
  data: MessageResponse;
  message: string;
}

export interface UpdateMessageResponse {
  status: number;
  data: MessageResponse;
  message: string;
}

export interface DeleteMessageResponse {
  status: number;
  message: string;
}

// WebSocket Channel 관련 인터페이스
export interface ChannelResponse {
  channelName: string;
  channel: {
    address: string;
    messages: Record<string, MessageReference>;
    bindings?: { stomp?: {} };
  };
}

export interface GetAllChannelsResponse {
  status: number;
  data: ChannelResponse[];
  message: string;
}

export interface GetChannelResponse {
  status: number;
  data: ChannelResponse;
  message: string;
}

// ========== WebSocket Schema API 함수들 ==========

/**
 * 전체 WebSocket Schema 조회
 */
export async function getAllWebSocketSchemas(): Promise<GetAllSchemasResponse> {
  const response = await fetch(WS_SCHEMA_API_BASE_URL, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `WebSocket Schema 조회 실패: ${response.status} ${response.statusText}`
    );
    console.error("WebSocket Schema API 호출 실패:", {
      url: WS_SCHEMA_API_BASE_URL,
      status: response.status,
    });
    throw new Error(errorMessage);
  }

  return response.json();
}

/**
 * 특정 WebSocket Schema 조회
 */
export async function getWebSocketSchema(
  schemaName: string
): Promise<GetSchemaResponse> {
  const response = await fetch(`${WS_SCHEMA_API_BASE_URL}/${schemaName}`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `WebSocket Schema 조회 실패: ${response.status} ${response.statusText}`
    );
    console.error("WebSocket Schema 조회 실패:", {
      schemaName,
      status: response.status,
    });
    throw new Error(errorMessage);
  }

  return response.json();
}

/**
 * 새로운 WebSocket Schema 생성
 */
export async function createWebSocketSchema(
  request: CreateSchemaRequest
): Promise<CreateSchemaResponse> {
  const response = await fetch(WS_SCHEMA_API_BASE_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `WebSocket Schema 생성 실패: ${response.status} ${response.statusText}`
    );
    console.error("WebSocket Schema 생성 실패:", {
      request,
      status: response.status,
    });
    throw new Error(errorMessage);
  }

  return response.json();
}

/**
 * WebSocket Schema 수정
 */
export async function updateWebSocketSchema(
  schemaName: string,
  request: UpdateSchemaRequest
): Promise<UpdateSchemaResponse> {
  const response = await fetch(`${WS_SCHEMA_API_BASE_URL}/${schemaName}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `WebSocket Schema 수정 실패: ${response.status} ${response.statusText}`
    );
    console.error("WebSocket Schema 수정 실패:", {
      schemaName,
      request,
      status: response.status,
    });
    throw new Error(errorMessage);
  }

  return response.json();
}

/**
 * WebSocket Schema 삭제
 */
export async function deleteWebSocketSchema(
  schemaName: string
): Promise<DeleteSchemaResponse> {
  const response = await fetch(`${WS_SCHEMA_API_BASE_URL}/${schemaName}`, {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `WebSocket Schema 삭제 실패: ${response.status} ${response.statusText}`
    );
    console.error("WebSocket Schema 삭제 실패:", {
      schemaName,
      status: response.status,
    });
    throw new Error(errorMessage);
  }

  return response.json();
}

// ========== WebSocket Operation API 함수들 ==========

const WS_OPERATION_API_BASE_URL = "/ouro/websocket-specs/operations";

// $ref를 ref로 변환하는 헬퍼 함수
function transformOperationResponse(data: any): any {
  if (!data) return data;
  
  // operation 객체 변환
  if (data.operation) {
    const op = data.operation;
    
    // x-ouroboros-* 필드를 간단한 이름으로 매핑
    if (op['x-ouroboros-id']) {
      op.id = op['x-ouroboros-id'];
    }
    if (op['x-ouroboros-entrypoint']) {
      op.entrypoint = op['x-ouroboros-entrypoint'];
    }
    if (op['x-ouroboros-diff']) {
      op.diff = op['x-ouroboros-diff'];
    }
    if (op['x-ouroboros-progress']) {
      op.progress = op['x-ouroboros-progress'];
    }
    
    // channel.$ref → channel.ref 변환
    if (op.channel && op.channel['$ref']) {
      op.channel.ref = op.channel['$ref'];
    }
    
    // reply.channel.$ref → reply.channel.ref 변환
    if (op.reply && op.reply.channel && op.reply.channel['$ref']) {
      op.reply.channel.ref = op.reply.channel['$ref'];
    }
    
    // messages.$ref → messages.ref 변환
    if (Array.isArray(op.messages)) {
      op.messages = op.messages.map((msg: any) => {
        if (msg['$ref']) {
          return { ref: msg['$ref'] };
        }
        return msg;
      });
    }
    
    // reply.messages.$ref → reply.messages.ref 변환
    if (op.reply && Array.isArray(op.reply.messages)) {
      op.reply.messages = op.reply.messages.map((msg: any) => {
        if (msg['$ref']) {
          return { ref: msg['$ref'] };
        }
        return msg;
      });
    }
  }
  
  return data;
}

/**
 * 전체 WebSocket Operation 조회
 */
export async function getAllWebSocketOperations(): Promise<GetAllOperationsResponse> {
  const response = await fetch(WS_OPERATION_API_BASE_URL, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `WebSocket Operation 조회 실패: ${response.status} ${response.statusText}`
    );
    console.error("WebSocket Operation API 호출 실패:", {
      url: WS_OPERATION_API_BASE_URL,
      status: response.status,
    });
    throw new Error(errorMessage);
  }

  const result = await response.json();
  
  // 각 operation 데이터를 변환
  if (result.data && Array.isArray(result.data)) {
    result.data = result.data.map(transformOperationResponse);
  }
  
  return result;
}

/**
 * 특정 WebSocket Operation 조회
 */
export async function getWebSocketOperation(
  operationId: string
): Promise<GetOperationResponse> {
  const response = await fetch(`${WS_OPERATION_API_BASE_URL}/${operationId}`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `WebSocket Operation 조회 실패: ${response.status} ${response.statusText}`
    );
    console.error("WebSocket Operation 조회 실패:", {
      operationId,
      status: response.status,
    });
    throw new Error(errorMessage);
  }

  const result = await response.json();
  
  // operation 데이터 변환
  if (result.data) {
    result.data = transformOperationResponse(result.data);
  }
  
  return result;
}

/**
 * 새로운 WebSocket Operation 생성
 */
export async function createWebSocketOperation(
  request: CreateOperationRequest
): Promise<CreateOperationResponse> {
  const response = await fetch(WS_OPERATION_API_BASE_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `WebSocket Operation 생성 실패: ${response.status} ${response.statusText}`
    );
    console.error("WebSocket Operation 생성 실패:", {
      request,
      status: response.status,
    });
    throw new Error(errorMessage);
  }

  const result = await response.json();
  
  // 생성된 operation 데이터들 변환
  if (result.data && Array.isArray(result.data)) {
    result.data = result.data.map(transformOperationResponse);
  }
  
  return result;
}

/**
 * WebSocket Operation 수정
 */
export async function updateWebSocketOperation(
  operationId: string,
  request: UpdateOperationRequest
): Promise<UpdateOperationResponse> {
  const response = await fetch(`${WS_OPERATION_API_BASE_URL}/${operationId}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `WebSocket Operation 수정 실패: ${response.status} ${response.statusText}`
    );
    console.error("WebSocket Operation 수정 실패:", {
      operationId,
      request,
      status: response.status,
    });
    throw new Error(errorMessage);
  }

  const result = await response.json();
  
  // 수정된 operation 데이터 변환
  if (result.data) {
    result.data = transformOperationResponse(result.data);
  }
  
  return result;
}

/**
 * WebSocket Operation 삭제
 */
export async function deleteWebSocketOperation(
  operationId: string
): Promise<DeleteOperationResponse> {
  const response = await fetch(`${WS_OPERATION_API_BASE_URL}/${operationId}`, {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `WebSocket Operation 삭제 실패: ${response.status} ${response.statusText}`
    );
    console.error("WebSocket Operation 삭제 실패:", {
      operationId,
      status: response.status,
    });
    throw new Error(errorMessage);
  }

  return response.json();
}

/**
 * WebSocket Operation 동기화 (캐시 -> 파일)
 */
export async function syncWebSocketOperation(
  operationId: string
): Promise<GetOperationResponse> {
  const response = await fetch(`${WS_OPERATION_API_BASE_URL}/${operationId}/sync`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `WebSocket Operation 동기화 실패: ${response.status} ${response.statusText}`
    );
    throw new Error(errorMessage);
  }

  return response.json();
}

// ========== WebSocket Message API 함수들 ==========

const WS_MESSAGE_API_BASE_URL = "/ouro/websocket-specs/messages";

/**
 * 전체 WebSocket Message 조회
 */
export async function getAllWebSocketMessages(): Promise<GetAllMessagesResponse> {
  const response = await fetch(WS_MESSAGE_API_BASE_URL, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `WebSocket Message 조회 실패: ${response.status} ${response.statusText}`
    );
    console.error("WebSocket Message API 호출 실패:", {
      url: WS_MESSAGE_API_BASE_URL,
      status: response.status,
    });
    throw new Error(errorMessage);
  }

  return response.json();
}

/**
 * 특정 WebSocket Message 조회
 */
export async function getWebSocketMessage(
  messageName: string
): Promise<GetMessageResponse> {
  const response = await fetch(`${WS_MESSAGE_API_BASE_URL}/${messageName}`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `WebSocket Message 조회 실패: ${response.status} ${response.statusText}`
    );
    console.error("WebSocket Message 조회 실패:", {
      messageName,
      status: response.status,
    });
    throw new Error(errorMessage);
  }

  return response.json();
}

/**
 * 새로운 WebSocket Message 생성
 */
export async function createWebSocketMessage(
  request: CreateMessageRequest
): Promise<CreateMessageResponse> {
  const response = await fetch(WS_MESSAGE_API_BASE_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `WebSocket Message 생성 실패: ${response.status} ${response.statusText}`
    );
    console.error("WebSocket Message 생성 실패:", {
      request,
      status: response.status,
    });
    throw new Error(errorMessage);
  }

  return response.json();
}

/**
 * WebSocket Message 수정
 */
export async function updateWebSocketMessage(
  messageName: string,
  request: UpdateMessageRequest
): Promise<UpdateMessageResponse> {
  const response = await fetch(`${WS_MESSAGE_API_BASE_URL}/${messageName}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `WebSocket Message 수정 실패: ${response.status} ${response.statusText}`
    );
    console.error("WebSocket Message 수정 실패:", {
      messageName,
      request,
      status: response.status,
    });
    throw new Error(errorMessage);
  }

  return response.json();
}

/**
 * WebSocket Message 삭제
 */
export async function deleteWebSocketMessage(
  messageName: string
): Promise<DeleteMessageResponse> {
  const response = await fetch(`${WS_MESSAGE_API_BASE_URL}/${messageName}`, {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `WebSocket Message 삭제 실패: ${response.status} ${response.statusText}`
    );
    console.error("WebSocket Message 삭제 실패:", {
      messageName,
      status: response.status,
    });
    throw new Error(errorMessage);
  }

  return response.json();
}

// ========== WebSocket Channel API 함수들 ==========

const WS_CHANNEL_API_BASE_URL = "/ouro/websocket-specs/channels";

/**
 * 전체 WebSocket Channel 조회
 */
export async function getAllWebSocketChannels(): Promise<GetAllChannelsResponse> {
  const response = await fetch(WS_CHANNEL_API_BASE_URL, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `WebSocket Channel 조회 실패: ${response.status} ${response.statusText}`
    );
    console.error("WebSocket Channel API 호출 실패:", {
      url: WS_CHANNEL_API_BASE_URL,
      status: response.status,
    });
    throw new Error(errorMessage);
  }

  return response.json();
}

/**
 * 특정 WebSocket Channel 조회
 */
export async function getWebSocketChannel(
  channelName: string
): Promise<GetChannelResponse> {
  const response = await fetch(`${WS_CHANNEL_API_BASE_URL}/${channelName}`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(
      response,
      `WebSocket Channel 조회 실패: ${response.status} ${response.statusText}`
    );
    console.error("WebSocket Channel 조회 실패:", {
      channelName,
      status: response.status,
    });
    throw new Error(errorMessage);
  }

  return response.json();
}
