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
export async function getRestApiSpec(id: string): Promise<GetSpecResponse> {
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

  return response.json();
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

// Schema 관련 API 함수들
const SCHEMA_API_BASE_URL = "/ouro/rest-specs/schemas";

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
  console.log("YAML Import 시작:", {
    fileName: file.name,
    fileSize: file.size,
    fileType: file.type,
    endpoint: `${API_BASE_URL}/import`,
  });

  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(`${API_BASE_URL}/import`, {
    method: "POST",
    body: formData,
    // multipart/form-data는 브라우저가 자동으로 설정
  });

  console.log("YAML Import 응답:", {
    status: response.status,
    statusText: response.statusText,
    ok: response.ok,
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
