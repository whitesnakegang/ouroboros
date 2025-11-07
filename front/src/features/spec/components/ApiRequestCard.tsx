import { useState, useEffect } from "react";
import { SchemaModal } from "./SchemaModal";
import { SchemaFieldEditor } from "./SchemaFieldEditor";
import { getAllSchemas, type SchemaResponse } from "../services/api";
import type { RequestBody, SchemaField } from "../types/schema.types";
import { createDefaultField } from "../types/schema.types";

// UTF-8 문자열을 Base64로 안전하게 인코딩하는 함수
const safeBase64 = (value: string): string | null => {
  try {
    return window.btoa(unescape(encodeURIComponent(value)));
  } catch {
    return null;
  }
};

interface KeyValuePair {
  key: string;
  value: string;
  required?: boolean;
  description?: string;
  type?: string;
}

interface AuthConfig {
  type:
    | "none"
    | "apiKey"
    | "bearer"
    | "jwtBearer"
    | "basicAuth"
    | "digestAuth"
    | "oauth2"
    | "oauth1";
  apiKey?: { key: string; value: string; addTo: "header" | "query" };
  bearer?: { token: string };
  basicAuth?: { username: string; password: string };
  oauth2?: { accessToken: string; tokenType?: string };
}

interface ApiRequestCardProps {
  queryParams: KeyValuePair[];
  setQueryParams: (params: KeyValuePair[]) => void;
  requestHeaders: KeyValuePair[];
  setRequestHeaders: (headers: KeyValuePair[]) => void;
  requestBody: RequestBody;
  setRequestBody: (body: RequestBody) => void;
  auth: AuthConfig;
  setAuth: (auth: AuthConfig) => void;
  isReadOnly?: boolean;
}

export function ApiRequestCard({
  queryParams,
  setQueryParams,
  requestHeaders,
  setRequestHeaders,
  requestBody,
  setRequestBody,
  auth,
  setAuth,
  isReadOnly = false,
}: ApiRequestCardProps) {
  const bodyTypes: RequestBody["type"][] = [
    "none",
    "form-data",
    "x-www-form-urlencoded",
    "json",
    "xml",
  ];

  const addHeader = () => {
    if (isReadOnly) return;
    setRequestHeaders([
      ...requestHeaders,
      { key: "", value: "", required: false },
    ]);
  };

  const removeHeader = (index: number) => {
    if (isReadOnly) return;
    setRequestHeaders(requestHeaders.filter((_, i) => i !== index));
  };

  const updateHeader = (
    index: number,
    field: "key" | "value",
    value: string
  ) => {
    if (isReadOnly) return;
    const updated = [...requestHeaders];
    updated[index] = { ...updated[index], [field]: value };
    setRequestHeaders(updated);
  };

  const [activeTab, setActiveTab] = useState("body");
  const [schemas, setSchemas] = useState<SchemaResponse[]>([]);
  const [isSchemaModalOpen, setIsSchemaModalOpen] = useState(false);

  // 스키마 목록 로드
  const loadSchemas = async () => {
    try {
      const response = await getAllSchemas();
      setSchemas(response.data);
    } catch (err) {
      console.error("스키마 로드 실패:", err);
    }
  };

  // 컴포넌트 마운트 시 스키마 목록 로드
  useEffect(() => {
    loadSchemas();
  }, []);

  // 모달이 열릴 때마다 스키마 목록 다시 로드
  useEffect(() => {
    if (isSchemaModalOpen) {
      loadSchemas();
    }
  }, [isSchemaModalOpen]);

  // Schema 선택 핸들러
  const handleSchemaSelect = (schema: {
    name: string;
    fields: SchemaField[];
  }) => {
    // SchemaModal에서 이미 재귀적으로 변환된 필드를 그대로 사용
    setRequestBody({
      ...requestBody,
      schemaRef: schema.name,
      fields: schema.fields,
    });
  };

  return (
    <div className="rounded-md border border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#161B22] p-4 shadow-sm mb-6">
      {/* Header */}
      <div className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3] mb-2 flex items-center gap-2">
        <svg
          className="h-4 w-4 text-gray-500 dark:text-[#8B949E]"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M12 4v16m8-8H4"
          />
        </svg>
        <span>Request</span>
      </div>
      <p className="text-xs text-gray-600 dark:text-[#8B949E] mb-4">
        요청 파라미터 및 헤더 설정
      </p>

      {/* Tabs */}
      <div className="flex gap-1 border-b border-gray-200 dark:border-[#2D333B] mb-4">
        {["params", "headers", "body", "auth"].map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`px-4 py-2 text-sm font-medium transition-colors capitalize border-b-2 ${
              activeTab === tab
                ? "text-gray-900 dark:text-[#E6EDF3] border-[#2563EB]"
                : "text-gray-500 dark:text-[#8B949E] border-transparent hover:text-gray-900 dark:hover:text-[#E6EDF3]"
            }`}
          >
            {tab === "params" && "Params"}
            {tab === "headers" && "Headers"}
            {tab === "body" && "Body"}
            {tab === "auth" && "Auth"}
          </button>
        ))}
      </div>

      {/* Tab Content */}
      <div className="space-y-4">
        {activeTab === "headers" && (
          <div>
            <div className="flex items-center justify-between mb-3">
              <p className="text-xs text-gray-600 dark:text-[#8B949E]">
                요청 헤더 설정
              </p>
              <button
                onClick={addHeader}
                disabled={isReadOnly}
                className={`px-3 py-1 text-xs font-medium rounded-md transition-colors ${
                  isReadOnly
                    ? "text-gray-400 dark:text-[#8B949E] cursor-not-allowed"
                    : "text-[#2563EB] hover:text-[#1E40AF]"
                }`}
              >
                + Add
              </button>
            </div>
            <div className="space-y-2">
              {requestHeaders.map((header, index) => (
                <div key={index} className="flex gap-2 items-center">
                  <input
                    type="checkbox"
                    checked={header.required || false}
                    onChange={(e) => {
                      const updated = [...requestHeaders];
                      updated[index].required = e.target.checked;
                      setRequestHeaders(updated);
                    }}
                    disabled={isReadOnly}
                    className="w-4 h-4"
                    title="Required"
                  />
                  <input
                    type="text"
                    value={header.key}
                    onChange={(e) => updateHeader(index, "key", e.target.value)}
                    placeholder="Header Name (e.g., X-API-Key)"
                    disabled={isReadOnly}
                    className="flex-1 px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm"
                  />
                  <input
                    type="text"
                    value={header.value}
                    onChange={(e) =>
                      updateHeader(index, "value", e.target.value)
                    }
                    placeholder="Header Value (e.g., abc123)"
                    disabled={isReadOnly}
                    className="flex-1 px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm"
                  />
                  <button
                    onClick={() => removeHeader(index)}
                    disabled={isReadOnly}
                    className="p-2 text-gray-500 dark:text-[#8B949E] hover:text-red-500 transition-colors disabled:opacity-50"
                  >
                    <svg
                      className="w-5 h-5"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                      />
                    </svg>
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        {activeTab === "body" && (
          <div>
            <div className="flex gap-1 mb-3 border-b border-gray-200 dark:border-[#2D333B]">
              {bodyTypes.map((type) => (
                <button
                  key={type}
                  onClick={() => {
                    const newBody: RequestBody = {
                      type: type,
                      fields: type === "none" ? [] : [createDefaultField()],
                    };
                    setRequestBody(newBody);
                  }}
                  disabled={isReadOnly}
                  className={`px-4 py-2 text-sm font-medium transition-colors border-b-2 ${
                    requestBody.type === type
                      ? "text-gray-900 dark:text-[#E6EDF3] border-[#2563EB]"
                      : "text-gray-500 dark:text-[#8B949E] border-transparent hover:text-gray-900 dark:hover:text-[#E6EDF3]"
                  } disabled:opacity-50`}
                >
                  {type}
                </button>
              ))}
            </div>

            {/* Table Format for all types except none */}
            {requestBody.type !== "none" && requestBody.fields && (
              <div>
                <div className="mb-3 flex gap-2 items-center">
                  {/* Schema 참조 표시 */}
                  {requestBody.schemaRef && (
                    <div className="flex items-center gap-2 px-3 py-1.5 bg-emerald-50 dark:bg-emerald-900/20 border border-emerald-200 dark:border-emerald-800 rounded-md">
                      <span className="text-sm font-medium text-emerald-700 dark:text-emerald-400">
                        Schema: {requestBody.schemaRef}
                      </span>
                      <button
                        onClick={() => {
                          setRequestBody({
                            ...requestBody,
                            schemaRef: undefined,
                            fields: [],
                          });
                        }}
                        disabled={isReadOnly}
                        className="text-emerald-600 hover:text-emerald-700 dark:text-emerald-400 dark:hover:text-emerald-300"
                        title="Remove Schema"
                      >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                      </button>
                    </div>
                  )}
                  
                  <button
                    onClick={() => {
                      setRequestBody({
                        ...requestBody,
                        fields: [
                          ...(requestBody.fields || []),
                          createDefaultField(),
                        ],
                      });
                    }}
                    disabled={isReadOnly || !!requestBody.schemaRef}
                    className={`px-3 py-1 text-sm text-[#2563EB] hover:text-[#1E40AF] font-medium ${
                      isReadOnly || requestBody.schemaRef ? "opacity-50 cursor-not-allowed" : ""
                    }`}
                  >
                    + Add Field
                  </button>
                  <button
                    onClick={() => setIsSchemaModalOpen(true)}
                    disabled={isReadOnly}
                    className={`px-3 py-1 text-sm text-emerald-600 hover:text-emerald-700 font-medium ${
                      isReadOnly ? "opacity-50 cursor-not-allowed" : ""
                    }`}
                  >
                    {requestBody.schemaRef ? "Change Schema" : "+ Add Schema"}
                  </button>
                </div>
                
                {/* 새로운 재귀적 SchemaField Editor */}
                <div className="space-y-2">
                  {requestBody.fields && requestBody.fields.map((field, index) => (
                    <SchemaFieldEditor
                      key={index}
                      field={field}
                      onChange={(newField) => {
                        const updated = [...(requestBody.fields || [])];
                        updated[index] = newField;
                        setRequestBody({
                          ...requestBody,
                          fields: updated,
                        });
                      }}
                      onRemove={() => {
                        const updated = requestBody.fields!.filter((_, i) => i !== index);
                        setRequestBody({
                          ...requestBody,
                          fields: updated,
                        });
                      }}
                      isReadOnly={isReadOnly || !!requestBody.schemaRef}
                      allowFileType={requestBody.type === "form-data"}
                    />
                  ))}
                </div>
                
                {(!requestBody.fields || requestBody.fields.length === 0) && !requestBody.schemaRef && (
                  <div className="text-center py-8 text-gray-500 dark:text-gray-400 text-sm">
                    <p>No fields yet. Click "+ Add Field" to add one.</p>
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {activeTab === "params" && (
          <div>
            <div className="flex items-center justify-between mb-3">
              <p className="text-xs text-gray-600 dark:text-[#8B949E]">
                쿼리 파라미터 설정 (URL 뒤에 ?key=value 형태)
              </p>
              <button
                onClick={() =>
                  setQueryParams([
                    ...queryParams,
                    { key: "", value: "", type: "string", required: false },
                  ])
                }
                disabled={isReadOnly}
                className={`px-3 py-1 text-xs font-medium rounded-md transition-colors ${
                  isReadOnly
                    ? "text-gray-400 dark:text-[#8B949E] cursor-not-allowed"
                    : "text-[#2563EB] hover:text-[#1E40AF]"
                }`}
              >
                + Add Param
              </button>
            </div>
            <div className="space-y-2">
              {queryParams.length === 0 ? (
                <div className="text-center py-8 text-gray-500 dark:text-gray-400">
                  <p className="text-sm">쿼리 파라미터가 없습니다.</p>
                </div>
              ) : (
                queryParams.map((param, index) => (
                  <div key={index} className="flex gap-2 items-center">
                    <input
                      type="checkbox"
                      checked={param.required || false}
                      onChange={(e) => {
                        const updated = [...queryParams];
                        updated[index].required = e.target.checked;
                        setQueryParams(updated);
                      }}
                      disabled={isReadOnly}
                      className="w-4 h-4"
                      title="Required"
                    />
                    <input
                      type="text"
                      value={param.key}
                      onChange={(e) => {
                        const updated = [...queryParams];
                        updated[index].key = e.target.value;
                        setQueryParams(updated);
                      }}
                      placeholder="Key (예: page, limit)"
                      disabled={isReadOnly}
                      className="flex-1 px-2 py-1.5 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB]"
                    />
                    <select
                      value={param.type || "string"}
                      onChange={(e) => {
                        const updated = [...queryParams];
                        updated[index].type = e.target.value;
                        setQueryParams(updated);
                      }}
                      disabled={isReadOnly}
                      className="px-2 py-1.5 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB]"
                    >
                      <option value="string">string</option>
                      <option value="number">number</option>
                      <option value="integer">integer</option>
                      <option value="boolean">boolean</option>
                    </select>
                    <input
                      type="text"
                      value={param.value}
                      onChange={(e) => {
                        const updated = [...queryParams];
                        updated[index].value = e.target.value;
                        setQueryParams(updated);
                      }}
                      placeholder="Value (예: 1, true)"
                      disabled={isReadOnly}
                      className="flex-1 px-2 py-1.5 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB]"
                    />
                    <button
                      onClick={() =>
                        setQueryParams(
                          queryParams.filter((_, i) => i !== index)
                        )
                      }
                      disabled={isReadOnly}
                      className="p-1.5 text-red-500 hover:text-red-600 disabled:opacity-50"
                    >
                      <svg
                        className="w-4 h-4"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M6 18L18 6M6 6l12 12"
                        />
                      </svg>
                    </button>
                  </div>
                ))
              )}
            </div>
          </div>
        )}

        {activeTab === "auth" && (
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Authentication Type
              </label>
              <select
                value={auth.type}
                onChange={(e) => {
                  const newType = e.target.value as AuthConfig["type"];
                  // 타입 변경 시 기본값 설정
                  if (newType === "bearer") {
                    setAuth({ type: "bearer", bearer: { token: "" } });
                  } else if (newType === "apiKey") {
                    setAuth({ type: "apiKey", apiKey: { key: "X-API-Key", value: "", addTo: "header" } });
                  } else if (newType === "basicAuth") {
                    setAuth({ type: "basicAuth", basicAuth: { username: "", password: "" } });
                  } else {
                    setAuth({ type: "none" });
                  }
                }}
                disabled={isReadOnly}
                className="w-full px-3 py-2 border border-gray-300 dark:border-[#30363D] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="none">No Auth</option>
                <option value="bearer">Bearer Token</option>
                <option value="apiKey">API Key</option>
                <option value="basicAuth">Basic Auth</option>
              </select>
            </div>

            {auth.type === "bearer" && (
              <div className="space-y-3">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Bearer Token
                  </label>
                  <input
                    type="text"
                    value={auth.bearer?.token || ""}
                    onChange={(e) =>
                      setAuth({
                        ...auth,
                        bearer: { token: e.target.value },
                      })
                    }
                    placeholder="Bearer token (예: 123)"
                    disabled={isReadOnly}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-[#30363D] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div className="p-3 bg-gray-50 dark:bg-[#0D1117] border border-gray-200 dark:border-[#2D333B] rounded-md">
                  <p className="text-xs text-gray-600 dark:text-[#8B949E] mb-1">
                    <strong>헤더 형식:</strong>
                  </p>
                  <code className="text-xs text-gray-900 dark:text-[#E6EDF3] font-mono">
                    Authorization: Bearer {auth.bearer?.token || "&lt;token&gt;"}
                  </code>
                </div>
              </div>
            )}

            {auth.type === "apiKey" && (
              <div className="space-y-3">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    API Key Name
                  </label>
                  <input
                    type="text"
                    value={auth.apiKey?.key || "X-API-Key"}
                    onChange={(e) =>
                      setAuth({
                        ...auth,
                        apiKey: {
                          ...auth.apiKey,
                          key: e.target.value || "X-API-Key",
                          value: auth.apiKey?.value || "",
                          addTo: auth.apiKey?.addTo || "header",
                        },
                      })
                    }
                    placeholder="X-API-Key"
                    disabled={isReadOnly}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-[#30363D] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    API Key Value
                  </label>
                  <input
                    type="text"
                    value={auth.apiKey?.value || ""}
                    onChange={(e) =>
                      setAuth({
                        ...auth,
                        apiKey: {
                          ...auth.apiKey,
                          key: auth.apiKey?.key || "X-API-Key",
                          value: e.target.value,
                          addTo: auth.apiKey?.addTo || "header",
                        },
                      })
                    }
                    placeholder="API Key 값 (예: abc123xyz456)"
                    disabled={isReadOnly}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-[#30363D] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div className="p-3 bg-gray-50 dark:bg-[#0D1117] border border-gray-200 dark:border-[#2D333B] rounded-md">
                  <p className="text-xs text-gray-600 dark:text-[#8B949E] mb-1">
                    <strong>헤더 형식:</strong>
                  </p>
                  <code className="text-xs text-gray-900 dark:text-[#E6EDF3] font-mono">
                    {auth.apiKey?.key || "X-API-Key"}: {auth.apiKey?.value || "&lt;value&gt;"}
                  </code>
                </div>
              </div>
            )}

            {auth.type === "basicAuth" && (
              <div className="space-y-3">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Username
                  </label>
                  <input
                    type="text"
                    value={auth.basicAuth?.username || ""}
                    onChange={(e) =>
                      setAuth({
                        ...auth,
                        basicAuth: {
                          username: e.target.value,
                          password: auth.basicAuth?.password || "",
                        },
                      })
                    }
                    placeholder="Username"
                    disabled={isReadOnly}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-[#30363D] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Password
                  </label>
                  <input
                    type="password"
                    value={auth.basicAuth?.password || ""}
                    onChange={(e) =>
                      setAuth({
                        ...auth,
                        basicAuth: {
                          username: auth.basicAuth?.username || "",
                          password: e.target.value,
                        },
                      })
                    }
                    placeholder="Password"
                    disabled={isReadOnly}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-[#30363D] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div className="p-3 bg-gray-50 dark:bg-[#0D1117] border border-gray-200 dark:border-[#2D333B] rounded-md">
                  <p className="text-xs text-gray-600 dark:text-[#8B949E] mb-1">
                    <strong>헤더 형식:</strong>
                  </p>
                  <code className="text-xs text-gray-900 dark:text-[#E6EDF3] font-mono break-all">
                    Authorization: Basic{" "}
                    {auth.basicAuth?.username && auth.basicAuth?.password
                      ? safeBase64(`${auth.basicAuth.username}:${auth.basicAuth.password}`) ??
                        "&lt;base64(username:password)&gt;"
                      : "&lt;base64(username:password)&gt;"}
                  </code>
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Send Button removed: Spec 작성 화면에서는 실제 전송 기능을 제공하지 않음 */}

      {/* Schema Modal */}
      <SchemaModal
        isOpen={isSchemaModalOpen}
        onClose={() => setIsSchemaModalOpen(false)}
        onSelect={handleSchemaSelect}
        schemas={schemas}
        setSchemas={setSchemas}
      />
    </div>
  );
}
