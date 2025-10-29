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
