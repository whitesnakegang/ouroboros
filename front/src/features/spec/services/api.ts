// 백엔드 API 서비스

// 환경변수에서 백엔드 URL 가져오기 (Vite는 import.meta.env 사용)
const API_BASE_URL = "/ouro/rest-specs";

export interface RestApiSpecResponse {
  id: string;
  path: string;
  method: string;
  summary?: string;
  description?: string;
  deprecated?: boolean;
  tags?: string[];
  parameters?: unknown[];
  requestBody?: unknown;
  responses?: Record<string, unknown>;
  security?: unknown[];
  progress: string;
  tag: string;
  isValid?: boolean;
  diff?: string;
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
  deprecated?: boolean;
  tags?: string[];
  parameters?: unknown[];
  requestBody?: unknown;
  responses?: Record<string, unknown>;
  security?: unknown[];
  progress?: string;
  tag?: string;
  isValid?: boolean;
  diff?: string;
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
  deprecated?: boolean;
  tags?: string[];
  parameters?: unknown[];
  requestBody?: unknown;
  responses?: Record<string, unknown>;
  security?: unknown[];
  progress?: string;
  tag?: string;
  isValid?: boolean;
  diff?: string;
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

/**
 * 전체 REST API 스펙 조회
 */
export async function getAllRestApiSpecs(): Promise<GetAllSpecsResponse> {
  const response = await fetch(API_BASE_URL, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    throw new Error(`API 호출 실패: ${response.statusText}`);
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
    const error = await response.json();
    throw new Error(error.message || `API 호출 실패: ${response.statusText}`);
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
    },
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || `API 호출 실패: ${response.statusText}`);
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
    const error = await response.json();
    throw new Error(error.message || `API 호출 실패: ${response.statusText}`);
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
    const error = await response.json();
    throw new Error(error.message || `API 호출 실패: ${response.statusText}`);
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
    const error = await response.json();
    throw new Error(
      error.message || `Schema 조회 실패: ${response.statusText}`
    );
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
    const error = await response.json();
    throw new Error(
      error.message || `Schema 조회 실패: ${response.statusText}`
    );
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
    const error = await response.json();
    throw new Error(
      error.message || `Schema 생성 실패: ${response.statusText}`
    );
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
    const error = await response.json();
    throw new Error(
      error.message || `Schema 수정 실패: ${response.statusText}`
    );
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
    const error = await response.json();
    throw new Error(
      error.message || `Schema 삭제 실패: ${response.statusText}`
    );
  }

  return response.json();
}
