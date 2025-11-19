import { useState, useEffect } from "react";
import { SchemaModal } from "./SchemaModal";
import { SchemaFieldEditor } from "./SchemaFieldEditor";
import { SchemaViewer } from "./SchemaViewer";
import { getAllSchemas, getSchema, type SchemaResponse } from "../services/api";
import type {
  RequestBody,
  SchemaField,
  SchemaType,
  PrimitiveTypeName,
} from "../types/schema.types";
import {
  createDefaultField,
  createPrimitiveField,
  createObjectField,
  createArrayField,
  createRefField,
  isPrimitiveSchema,
  isObjectSchema,
  isArraySchema,
  isRefSchema,
} from "../types/schema.types";

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
  pathParams?: KeyValuePair[];
  requestHeaders: KeyValuePair[];
  setRequestHeaders: (headers: KeyValuePair[]) => void;
  requestBody: RequestBody;
  setRequestBody: (body: RequestBody) => void;
  auth: AuthConfig;
  setAuth: (auth: AuthConfig) => void;
  isReadOnly?: boolean;
  isDocumentView?: boolean;
}

export function ApiRequestCard({
  queryParams,
  setQueryParams,
  pathParams = [],
  requestHeaders,
  setRequestHeaders,
  requestBody,
  setRequestBody,
  auth,
  setAuth,
  isReadOnly = false,
  isDocumentView = false,
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
  const [currentSchemaName, setCurrentSchemaName] = useState<string | null>(null);
  const [refSchemaFields, setRefSchemaFields] = useState<SchemaField[]>([]);
  const [isExpandedRefSchema, setIsExpandedRefSchema] = useState(false);

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

  // 기존에 선택된 스키마의 이름 및 필드 로드
  useEffect(() => {
    const loadSchemaData = async () => {
      // rootSchemaType이 ref 타입이고 schemaName이 없으면 schemaRef에서 로드
      if (
        requestBody.rootSchemaType &&
        isRefSchema(requestBody.rootSchemaType) &&
        !requestBody.rootSchemaType.schemaName &&
        requestBody.schemaRef
      ) {
        try {
          const schemaResponse = await getSchema(requestBody.schemaRef);
          const schemaData = schemaResponse.data;
          setCurrentSchemaName(schemaData.schemaName);
          
          // 스키마 필드 로드
          if (schemaData.properties) {
            const { parseOpenAPISchemaToSchemaField } = await import("../utils/schemaConverter");
            const requiredFields = schemaData.required || [];
            const fields: SchemaField[] = Object.entries(schemaData.properties).map(
              ([key, propSchema]: [string, any]) => {
                const field = parseOpenAPISchemaToSchemaField(key, propSchema);
                field.required = requiredFields.includes(key);
                return field;
              }
            );
            setRefSchemaFields(fields);
            setIsExpandedRefSchema(true);
          }
          
          // rootSchemaType의 schemaName 업데이트
          setRequestBody({
            ...requestBody,
            rootSchemaType: {
              ...requestBody.rootSchemaType,
              schemaName: schemaData.schemaName,
            },
          });
        } catch (error) {
          console.error("스키마 데이터 로드 실패:", error);
        }
      } else if (
        requestBody.rootSchemaType &&
        isRefSchema(requestBody.rootSchemaType) &&
        requestBody.rootSchemaType.schemaName
      ) {
        // 이미 schemaName이 있으면 스키마 필드 로드
        setCurrentSchemaName(requestBody.rootSchemaType.schemaName);
        try {
          const schemaResponse = await getSchema(requestBody.rootSchemaType.schemaName);
          const schemaData = schemaResponse.data;
          if (schemaData.properties) {
            const { parseOpenAPISchemaToSchemaField } = await import("../utils/schemaConverter");
            const requiredFields = schemaData.required || [];
            const fields: SchemaField[] = Object.entries(schemaData.properties).map(
              ([key, propSchema]: [string, any]) => {
                const field = parseOpenAPISchemaToSchemaField(key, propSchema);
                field.required = requiredFields.includes(key);
                return field;
              }
            );
            setRefSchemaFields(fields);
            setIsExpandedRefSchema(true);
          }
        } catch (error) {
          console.error("스키마 필드 로드 실패:", error);
        }
      } else if (requestBody.schemaRef && !requestBody.rootSchemaType) {
        // 기존 방식: schemaRef만 있고 rootSchemaType이 없는 경우
        try {
          const schemaResponse = await getSchema(requestBody.schemaRef);
          setCurrentSchemaName(schemaResponse.data.schemaName);
        } catch (error) {
          console.error("스키마 이름 로드 실패:", error);
        }
      } else {
        setCurrentSchemaName(null);
        setRefSchemaFields([]);
        setIsExpandedRefSchema(false);
      }
    };

    loadSchemaData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [requestBody.rootSchemaType, requestBody.schemaRef]);

  // Schema 선택 핸들러
  const handleSchemaSelect = (schema: {
    name: string;
    fields: SchemaField[];
  }) => {
    setCurrentSchemaName(schema.name);
    
    // rootSchemaType이 ref 타입이면 rootSchemaType 업데이트
    if (requestBody.rootSchemaType && isRefSchema(requestBody.rootSchemaType)) {
      setRefSchemaFields(schema.fields);
      setIsExpandedRefSchema(true);
      setRequestBody({
        ...requestBody,
        rootSchemaType: {
          kind: "ref",
          schemaName: schema.name,
        },
        schemaRef: schema.name,
      });
    } else if (
      requestBody.rootSchemaType &&
      isObjectSchema(requestBody.rootSchemaType)
    ) {
      // object 타입일 때는 rootSchemaType의 properties를 스키마의 fields로 채움
      setRequestBody({
        ...requestBody,
        rootSchemaType: {
          ...requestBody.rootSchemaType,
          properties: schema.fields,
        },
        schemaRef: schema.name,
      });
    } else {
      // 기존 방식: SchemaModal에서 이미 재귀적으로 변환된 필드를 그대로 사용
      setRequestBody({
        ...requestBody,
        schemaRef: schema.name,
        fields: schema.fields,
      });
    }
  };

  // 문서 형식 뷰
  if (isDocumentView) {
    return (
      <div className="space-y-6">
        {/* Path Parameters */}
        {pathParams.length > 0 && (
          <div>
            <h3 className="text-sm font-semibold text-gray-700 dark:text-[#C9D1D9] mb-2">
              Path Parameters
            </h3>
            <div className="space-y-2">
              {pathParams.map((param, index) => (
                <div key={index} className="flex items-start gap-3 text-sm">
                  <span className="font-mono text-gray-900 dark:text-[#E6EDF3] min-w-[120px]">
                    {param.key}
                  </span>
                  <span className="text-gray-600 dark:text-[#8B949E]">:</span>
                  <span className="text-gray-500 dark:text-[#8B949E] text-xs">
                    ({param.type || "string"})
                  </span>
                  {param.value && (
                    <span className="text-gray-900 dark:text-[#E6EDF3] flex-1">
                      {param.value}
                    </span>
                  )}
                  {param.required && (
                    <span className="px-2 py-0.5 bg-red-100 dark:bg-red-900/20 text-red-700 dark:text-red-300 text-xs rounded">
                      Required
                    </span>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Query Parameters */}
        {queryParams.length > 0 && (
          <div>
            <h3 className="text-sm font-semibold text-gray-700 dark:text-[#C9D1D9] mb-2">
              Query Parameters
            </h3>
            <div className="space-y-2">
              {queryParams.map((param, index) => (
                <div key={index} className="flex items-start gap-3 text-sm">
                  <span className="font-mono text-gray-900 dark:text-[#E6EDF3] min-w-[120px]">
                    {param.key}
                  </span>
                  <span className="text-gray-600 dark:text-[#8B949E]">:</span>
                  <span className="text-gray-500 dark:text-[#8B949E] text-xs">
                    ({param.type || "string"})
                  </span>
                  {param.value && (
                    <span className="text-gray-900 dark:text-[#E6EDF3] flex-1">
                      {param.value}
                    </span>
                  )}
                  {param.required && (
                    <span className="px-2 py-0.5 bg-red-100 dark:bg-red-900/20 text-red-700 dark:text-red-300 text-xs rounded">
                      Required
                    </span>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Request Headers */}
        {requestHeaders.length > 0 && (
          <div>
            <h3 className="text-sm font-semibold text-gray-700 dark:text-[#C9D1D9] mb-2">
              Headers
            </h3>
            <div className="space-y-2">
              {requestHeaders.map((header, index) => (
                <div key={index} className="flex items-start gap-3 text-sm">
                  <span className="font-mono text-gray-900 dark:text-[#E6EDF3] min-w-[120px]">
                    {header.key}
                  </span>
                  <span className="text-gray-600 dark:text-[#8B949E]">:</span>
                  <span className="text-gray-900 dark:text-[#E6EDF3] flex-1">
                    {header.value || (
                      <span className="text-gray-400 italic">(empty)</span>
                    )}
                  </span>
                  {header.required && (
                    <span className="px-2 py-0.5 bg-red-100 dark:bg-red-900/20 text-red-700 dark:text-red-300 text-xs rounded">
                      Required
                    </span>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Request Body */}
        {requestBody.type !== "none" && (
          <div>
            <SchemaViewer
              schemaType={requestBody.rootSchemaType}
              fields={requestBody.fields}
              schemaRef={requestBody.schemaRef}
              schemaName={currentSchemaName}
              description={requestBody.description}
              contentType={
                requestBody.type === "json"
                  ? "application/json"
                  : requestBody.type === "xml"
                  ? "application/xml"
                  : requestBody.type === "form-data"
                  ? "multipart/form-data"
                  : "application/x-www-form-urlencoded"
              }
            />
          </div>
        )}

        {/* Auth */}
        {auth.type !== "none" && (
          <div>
            <h3 className="text-sm font-semibold text-gray-700 dark:text-[#C9D1D9] mb-2">
              Authentication
            </h3>
            <div className="text-sm text-gray-900 dark:text-[#E6EDF3]">
              Type: <span className="font-mono">{auth.type}</span>
            </div>
          </div>
        )}

        {pathParams.length === 0 &&
          queryParams.length === 0 &&
          requestHeaders.length === 0 &&
          requestBody.type === "none" &&
          auth.type === "none" && (
            <div className="text-sm text-gray-500 dark:text-[#8B949E] italic">
              No request parameters configured.
            </div>
          )}
      </div>
    );
  }

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

      {/* Tabs */}
      <div className="flex gap-1 border-b border-gray-200 dark:border-[#2D333B] mb-4">
        {["params", "headers", "body", "auth"].map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`px-4 py-2 text-sm font-medium transition-colors capitalize border-b-2 focus:outline-none focus:ring-0 ${
              activeTab === tab
                ? "text-gray-900 dark:text-[#E6EDF3] border-gray-900 dark:border-[#E6EDF3]"
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
              <button
                onClick={addHeader}
                disabled={isReadOnly}
                className={`px-3 py-1 text-xs font-medium rounded-md transition-colors ${
                  isReadOnly
                    ? "text-gray-400 dark:text-[#8B949E] cursor-not-allowed"
                    : "text-[#2563EB] hover:text-[#1E40AF]"
                }`}
              >
                + Add Header
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
                    className="flex-1 px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-gray-400 dark:focus:ring-gray-500 focus:border-gray-400 dark:focus:border-gray-500 text-sm"
                  />
                  <input
                    type="text"
                    value={header.value}
                    onChange={(e) =>
                      updateHeader(index, "value", e.target.value)
                    }
                    placeholder="Header Value (e.g., abc123)"
                    disabled={isReadOnly}
                    className="flex-1 px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-gray-400 dark:focus:ring-gray-500 focus:border-gray-400 dark:focus:border-gray-500 text-sm"
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
            <div className="flex gap-1 mb-3 mt-4 border-b border-gray-200 dark:border-[#2D333B]">
              {bodyTypes.map((type) => (
                <button
                  key={type}
                  onClick={() => {
                    const newBody: RequestBody = {
                      type: type,
                      fields:
                        type === "none"
                          ? []
                          : type === "json" || type === "xml"
                          ? []
                          : [createDefaultField()],
                      // json/xml일 때는 rootSchemaType을 object로 기본 설정
                      rootSchemaType:
                        type === "json" || type === "xml"
                          ? createObjectField("").schemaType
                          : undefined,
                    };
                    setRequestBody(newBody);
                  }}
                  disabled={isReadOnly}
                  className={`px-4 py-2 text-sm font-medium transition-colors border-b-2 focus:outline-none focus:ring-0 ${
                    requestBody.type === type
                      ? "text-gray-900 dark:text-[#E6EDF3] border-gray-900 dark:border-[#E6EDF3]"
                      : "text-gray-500 dark:text-[#8B949E] border-transparent hover:text-gray-900 dark:hover:text-[#E6EDF3]"
                  } disabled:opacity-50`}
                >
                  {type}
                </button>
              ))}
            </div>

            {/* Table Format for all types except none */}
            {requestBody.type !== "none" && (
              <div>
                {/* JSON/XML 타입일 때 Root Type 선택 */}
                {(requestBody.type === "json" ||
                  requestBody.type === "xml") && (
                  <div className="mb-4">
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      Root Type
                    </label>
                    <select
                      value={requestBody.rootSchemaType?.kind || "object"}
                      onChange={(e) => {
                        const kind = e.target.value as SchemaType["kind"];
                        let newRootSchemaType: SchemaType;

                        switch (kind) {
                          case "object":
                            newRootSchemaType =
                              createObjectField("").schemaType;
                            break;
                          case "array":
                            newRootSchemaType = createArrayField("").schemaType;
                            break;
                          case "primitive":
                            newRootSchemaType = createPrimitiveField(
                              "",
                              "string"
                            ).schemaType;
                            break;
                          case "ref":
                            newRootSchemaType = createRefField(
                              "",
                              ""
                            ).schemaType;
                            break;
                          default:
                            newRootSchemaType =
                              createObjectField("").schemaType;
                        }

                        setCurrentSchemaName(null);
                        setRequestBody({
                          ...requestBody,
                          rootSchemaType: newRootSchemaType,
                          fields: kind === "object" ? [] : undefined,
                          schemaRef: undefined,
                        });
                        // ref 타입이 아닐 때 refSchemaFields 초기화
                        if (kind !== "ref") {
                          setRefSchemaFields([]);
                          setIsExpandedRefSchema(false);
                          setCurrentSchemaName(null);
                        }
                      }}
                      disabled={isReadOnly}
                      className="w-full px-3 py-2 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] focus:outline-none focus:ring-1 focus:ring-gray-400 dark:focus:ring-gray-500 focus:border-gray-400 dark:focus:border-gray-500 text-sm"
                    >
                      <option value="object">Object</option>
                      <option value="array">Array</option>
                      <option value="primitive">
                        Primitive (string, number, etc.)
                      </option>
                      <option value="ref">Schema Reference</option>
                    </select>
                  </div>
                )}

                <div className="mb-3 flex gap-2 items-center">
                  {/* Schema 참조 표시 (JSON/XML이 아닐 때만) */}
                  {requestBody.schemaRef &&
                    requestBody.type !== "json" &&
                    requestBody.type !== "xml" && (
                      <div className="flex items-center gap-2 px-3 py-1.5 bg-emerald-50 dark:bg-emerald-900/20 border border-emerald-200 dark:border-emerald-800 rounded-md">
                        <span className="text-sm font-medium text-emerald-700 dark:text-emerald-400">
                          Schema: {currentSchemaName || requestBody.schemaRef}
                        </span>
                        <button
                          onClick={() => {
                            setCurrentSchemaName(null);
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
                    )}

                  {/* JSON/XML이 아닐 때만 Add Field 버튼 표시 */}
                  {requestBody.type !== "json" &&
                    requestBody.type !== "xml" && (
                      <>
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
                            isReadOnly || requestBody.schemaRef
                              ? "opacity-50 cursor-not-allowed"
                              : ""
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
                          {requestBody.schemaRef
                            ? "Change Schema"
                            : "+ Add Schema"}
                        </button>
                      </>
                    )}
                </div>

                {/* JSON/XML 타입일 때 Root Type에 따른 편집 UI */}
                {(requestBody.type === "json" || requestBody.type === "xml") &&
                  requestBody.rootSchemaType && (
                    <div>
                      {/* Object 타입 */}
                      {isObjectSchema(requestBody.rootSchemaType) &&
                        (() => {
                          const objectSchema = requestBody.rootSchemaType;
                          // properties가 undefined일 수 있으므로 빈 배열로 초기화
                          const properties = objectSchema.properties || [];
                          // schemaRef가 있으면 필드 편집 불가 (form-data와 동일한 동작)
                          const hasSchemaRef = !!requestBody.schemaRef;
                          return (
                            <div>
                              {/* Schema 참조 표시 */}
                              {hasSchemaRef && (
                                <div className="mb-3 flex items-center gap-2 px-3 py-1.5 bg-emerald-50 dark:bg-emerald-900/20 border border-emerald-200 dark:border-emerald-800 rounded-md">
                                  <span className="text-sm font-medium text-emerald-700 dark:text-emerald-400">
                                    Schema: {currentSchemaName || requestBody.schemaRef}
                                  </span>
                                  <button
                                    onClick={() => {
                                      setCurrentSchemaName(null);
                                      if (
                                        isObjectSchema(
                                          requestBody.rootSchemaType!
                                        )
                                      ) {
                                        setRequestBody({
                                          ...requestBody,
                                          schemaRef: undefined,
                                          fields: [],
                                          rootSchemaType: {
                                            ...requestBody.rootSchemaType!,
                                            properties: [],
                                          },
                                        });
                                      } else {
                                        setRequestBody({
                                          ...requestBody,
                                          schemaRef: undefined,
                                          fields: [],
                                        });
                                      }
                                    }}
                                    disabled={isReadOnly}
                                    className="text-emerald-600 hover:text-emerald-700 dark:text-emerald-400 dark:hover:text-emerald-300"
                                    title="Remove Schema"
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
                              )}
                              <div className="mb-3 flex gap-2 items-center">
                                <button
                                  onClick={() => {
                                    setRequestBody({
                                      ...requestBody,
                                      rootSchemaType: {
                                        ...objectSchema,
                                        properties: [
                                          ...properties,
                                          createDefaultField(),
                                        ],
                                      },
                                    });
                                  }}
                                  disabled={isReadOnly || hasSchemaRef}
                                  className={`px-3 py-1 text-sm text-[#2563EB] hover:text-[#1E40AF] font-medium ${
                                    isReadOnly || hasSchemaRef
                                      ? "opacity-50 cursor-not-allowed"
                                      : ""
                                  }`}
                                >
                                  + Add Field
                                </button>
                                <button
                                  onClick={() => setIsSchemaModalOpen(true)}
                                  disabled={isReadOnly}
                                  className={`px-3 py-1 text-sm text-emerald-600 hover:text-emerald-700 font-medium ${
                                    isReadOnly
                                      ? "opacity-50 cursor-not-allowed"
                                      : ""
                                  }`}
                                >
                                  {hasSchemaRef
                                    ? "Change Schema"
                                    : "+ Add Schema"}
                                </button>
                              </div>
                              <div className="space-y-2">
                                {properties.map((field, index) => (
                                  <SchemaFieldEditor
                                    key={index}
                                    field={field}
                                    onChange={(newField) => {
                                      const updated = [...properties];
                                      updated[index] = newField;
                                      setRequestBody({
                                        ...requestBody,
                                        rootSchemaType: {
                                          ...objectSchema,
                                          properties: updated,
                                        },
                                      });
                                    }}
                                    onRemove={() => {
                                      const updated = properties.filter(
                                        (_, i) => i !== index
                                      );
                                      setRequestBody({
                                        ...requestBody,
                                        rootSchemaType: {
                                          ...objectSchema,
                                          properties: updated,
                                        },
                                      });
                                    }}
                                    isReadOnly={isReadOnly || hasSchemaRef}
                                    allowFileType={false}
                                  />
                                ))}
                              </div>
                              {(properties.length === 0) &&
                                !hasSchemaRef && (
                                  <div className="text-center py-8 text-gray-500 dark:text-gray-400 text-sm">
                                    <p>
                                      No fields yet. Click "+ Add Field" to add
                                      one.
                                    </p>
                                  </div>
                                )}
                            </div>
                          );
                        })()}

                      {/* Array 타입 */}
                      {isArraySchema(requestBody.rootSchemaType) && (
                        <div>
                          <p className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                            Array Items:
                          </p>
                          <div className="ml-4">
                            <SchemaFieldEditor
                              field={{
                                key: "items",
                                schemaType: requestBody.rootSchemaType.items,
                                description:
                                  requestBody.rootSchemaType.itemsDescription,
                                required:
                                  requestBody.rootSchemaType.itemsRequired,
                              }}
                              onChange={(newField) => {
                                setRequestBody({
                                  ...requestBody,
                                  rootSchemaType: {
                                    ...requestBody.rootSchemaType!,
                                    items: newField.schemaType,
                                    itemsDescription: newField.description,
                                    itemsRequired: newField.required,
                                  } as typeof requestBody.rootSchemaType,
                                });
                              }}
                              depth={0}
                              isReadOnly={isReadOnly}
                              allowFileType={false}
                            />
                            <div className="flex gap-2 mt-2">
                              <input
                                type="number"
                                value={
                                  requestBody.rootSchemaType.minItems || ""
                                }
                                onChange={(e) => {
                                  setRequestBody({
                                    ...requestBody,
                                    rootSchemaType: {
                                      ...requestBody.rootSchemaType!,
                                      minItems: e.target.value
                                        ? parseInt(e.target.value)
                                        : undefined,
                                    } as typeof requestBody.rootSchemaType,
                                  });
                                }}
                                placeholder="Min items"
                                disabled={isReadOnly}
                                className="px-2 py-1 text-xs rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900"
                              />
                              <input
                                type="number"
                                value={
                                  requestBody.rootSchemaType.maxItems || ""
                                }
                                onChange={(e) => {
                                  setRequestBody({
                                    ...requestBody,
                                    rootSchemaType: {
                                      ...requestBody.rootSchemaType!,
                                      maxItems: e.target.value
                                        ? parseInt(e.target.value)
                                        : undefined,
                                    } as typeof requestBody.rootSchemaType,
                                  });
                                }}
                                placeholder="Max items"
                                disabled={isReadOnly}
                                className="px-2 py-1 text-xs rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900"
                              />
                            </div>
                          </div>
                        </div>
                      )}

                      {/* Primitive 타입 */}
                      {isPrimitiveSchema(requestBody.rootSchemaType) && (
                        <div>
                          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                            Primitive Type
                          </label>
                          <select
                            value={requestBody.rootSchemaType.type}
                            onChange={(e) => {
                              setRequestBody({
                                ...requestBody,
                                rootSchemaType: {
                                  ...requestBody.rootSchemaType!,
                                  type: e.target.value as PrimitiveTypeName,
                                  format:
                                    e.target.value === "file"
                                      ? "binary"
                                      : undefined,
                                } as typeof requestBody.rootSchemaType,
                              });
                            }}
                            disabled={isReadOnly}
                            className="w-full px-3 py-2 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] focus:outline-none focus:ring-1 focus:ring-gray-400 dark:focus:ring-gray-500 focus:border-gray-400 dark:focus:border-gray-500 text-sm"
                          >
                            <option value="string">string</option>
                            <option value="integer">integer</option>
                            <option value="number">number</option>
                            <option value="boolean">boolean</option>
                          </select>
                        </div>
                      )}

                      {/* Ref 타입 */}
                      {isRefSchema(requestBody.rootSchemaType) && (
                        <div className="space-y-3">
                          <div>
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                              Schema Reference
                            </label>
                            <button
                              onClick={() => setIsSchemaModalOpen(true)}
                              disabled={isReadOnly}
                              className={`w-full px-3 py-2 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] text-left hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors disabled:opacity-50 text-sm ${
                                isReadOnly ? "opacity-50 cursor-not-allowed" : ""
                              }`}
                            >
                              {requestBody.rootSchemaType.schemaName ||
                                currentSchemaName ||
                                requestBody.schemaRef ||
                                "Select Schema..."}
                            </button>
                          </div>
                          
                          {/* Schema 참조 표시 및 필드 목록 */}
                          {(requestBody.rootSchemaType.schemaName || currentSchemaName || requestBody.schemaRef) && refSchemaFields.length > 0 && (
                            <div>
                              {/* Schema 참조 표시 */}
                              <div className="mb-3 flex items-center justify-between px-3 py-1.5 bg-emerald-50 dark:bg-emerald-900/20 border border-emerald-200 dark:border-emerald-800 rounded-md">
                                <div className="flex items-center gap-2">
                                  <span className="text-sm font-medium text-emerald-700 dark:text-emerald-400">
                                    Schema: {currentSchemaName || requestBody.rootSchemaType.schemaName || requestBody.schemaRef}
                                  </span>
                                  <span className="text-xs text-gray-500 dark:text-gray-400">
                                    ({refSchemaFields.length} fields)
                                  </span>
                                </div>
                                <div className="flex items-center gap-2">
                                  <button
                                    onClick={() => {
                                      if (isReadOnly) return;
                                      setIsExpandedRefSchema(!isExpandedRefSchema);
                                    }}
                                    disabled={isReadOnly}
                                    className={`flex items-center gap-1.5 px-2 py-1 text-xs border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-700 dark:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#161B22] transition-colors ${
                                      isReadOnly
                                        ? "opacity-60 cursor-not-allowed"
                                        : ""
                                    }`}
                                    title={
                                      isExpandedRefSchema
                                        ? "Hide Schema Fields"
                                        : "Show Schema Fields"
                                    }
                                  >
                                    <span>
                                      {isExpandedRefSchema
                                        ? "Hide Fields"
                                        : "Show Fields"}
                                    </span>
                                    <svg
                                      className={`w-4 h-4 transition-transform ${
                                        isExpandedRefSchema
                                          ? "rotate-180"
                                          : ""
                                      }`}
                                      fill="none"
                                      stroke="currentColor"
                                      viewBox="0 0 24 24"
                                    >
                                      <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth={2}
                                        d="M19 9l-7 7-7-7"
                                      />
                                    </svg>
                                  </button>
                                  <button
                                    onClick={() => {
                                      if (isReadOnly) return;
                                      setCurrentSchemaName(null);
                                      setRefSchemaFields([]);
                                      setIsExpandedRefSchema(false);
                                      setRequestBody({
                                        ...requestBody,
                                        rootSchemaType: {
                                          kind: "ref",
                                        },
                                        schemaRef: undefined,
                                      });
                                    }}
                                    disabled={isReadOnly}
                                    className="text-emerald-600 hover:text-emerald-700 dark:text-emerald-400 dark:hover:text-emerald-300"
                                    title="Remove Schema"
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
                              </div>
                              
                              {/* 필드 목록 */}
                              {isExpandedRefSchema && (
                                <div className="space-y-2 border border-gray-200 dark:border-[#2D333B] rounded-md p-3 bg-gray-50 dark:bg-[#0D1117]">
                                  {refSchemaFields.map((field, index) => (
                                    <div
                                      key={index}
                                      className="flex items-start gap-3 text-sm py-2 border-b border-gray-200 dark:border-[#2D333B] last:border-b-0"
                                    >
                                      <span className="font-mono text-gray-900 dark:text-[#E6EDF3] min-w-[120px]">
                                        {field.key}
                                      </span>
                                      <span className="text-gray-600 dark:text-[#8B949E]">:</span>
                                      <span className="text-gray-500 dark:text-[#8B949E] text-xs">
                                        ({(() => {
                                          if (field.schemaType.kind === "primitive") {
                                            return field.schemaType.type;
                                          } else if (field.schemaType.kind === "object") {
                                            return "object";
                                          } else if (field.schemaType.kind === "array") {
                                            const itemType = field.schemaType.items.kind === "primitive" 
                                              ? field.schemaType.items.type 
                                              : field.schemaType.items.kind === "object" 
                                              ? "object" 
                                              : field.schemaType.items.kind === "ref"
                                              ? field.schemaType.items.schemaName
                                              : "unknown";
                                            return `${itemType}[]`;
                                          } else if (field.schemaType.kind === "ref") {
                                            return field.schemaType.schemaName;
                                          }
                                          return "string";
                                        })()})
                                      </span>
                                      {field.required && (
                                        <span className="px-2 py-0.5 bg-red-100 dark:bg-red-900/20 text-red-700 dark:text-red-300 text-xs rounded">
                                          Required
                                        </span>
                                      )}
                                    </div>
                                  ))}
                                </div>
                              )}
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  )}

                {/* JSON/XML이 아닐 때 기존 필드 편집 UI */}
                {requestBody.type !== "json" &&
                  requestBody.type !== "xml" &&
                  requestBody.fields && (
                    <>
                      <div className="space-y-2">
                        {requestBody.fields.map((field, index) => (
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
                              const updated = requestBody.fields!.filter(
                                (_, i) => i !== index
                              );
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

                      {(!requestBody.fields ||
                        requestBody.fields.length === 0) &&
                        !requestBody.schemaRef && (
                          <div className="text-center py-8 text-gray-500 dark:text-gray-400 text-sm">
                            <p>
                              No fields yet. Click "+ Add Field" to add one.
                            </p>
                          </div>
                        )}
                    </>
                  )}
              </div>
            )}
          </div>
        )}

        {activeTab === "params" && (
          <div className="space-y-6">
            {/* Query Parameters */}
            <div>
              <div className="flex items-center justify-between mb-3">
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
                {queryParams.length > 0 && (
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
                        placeholder="Key"
                        disabled={isReadOnly}
                        className="flex-1 px-2 py-1.5 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-gray-400 dark:focus:ring-gray-500 focus:border-gray-400 dark:focus:border-gray-500"
                      />
                      <select
                        value={param.type || "string"}
                        onChange={(e) => {
                          const updated = [...queryParams];
                          updated[index].type = e.target.value;
                          setQueryParams(updated);
                        }}
                        disabled={isReadOnly}
                        className="px-2 py-1.5 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] focus:outline-none focus:ring-1 focus:ring-gray-400 dark:focus:ring-gray-500 focus:border-gray-400 dark:focus:border-gray-500"
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
                        placeholder="Value"
                        disabled={isReadOnly}
                        className="flex-1 px-2 py-1.5 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-gray-400 dark:focus:ring-gray-500 focus:border-gray-400 dark:focus:border-gray-500"
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
                    setAuth({
                      type: "apiKey",
                      apiKey: { key: "X-API-Key", value: "", addTo: "header" },
                    });
                  } else if (newType === "basicAuth") {
                    setAuth({
                      type: "basicAuth",
                      basicAuth: { username: "", password: "" },
                    });
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
                    placeholder="Bearer token"
                    disabled={isReadOnly}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-[#30363D] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div className="p-3 bg-gray-50 dark:bg-[#0D1117] border border-gray-200 dark:border-[#2D333B] rounded-md">
                  <p className="text-xs text-gray-600 dark:text-[#8B949E] mb-1">
                    <strong>Header format:</strong>
                  </p>
                  <code className="text-xs text-gray-900 dark:text-[#E6EDF3] font-mono">
                    Authorization: Bearer{" "}
                    {auth.bearer?.token || "&lt;token&gt;"}
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
                    placeholder="API Key value"
                    disabled={isReadOnly}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-[#30363D] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div className="p-3 bg-gray-50 dark:bg-[#0D1117] border border-gray-200 dark:border-[#2D333B] rounded-md">
                  <p className="text-xs text-gray-600 dark:text-[#8B949E] mb-1">
                    <strong>Header format:</strong>
                  </p>
                  <code className="text-xs text-gray-900 dark:text-[#E6EDF3] font-mono">
                    {auth.apiKey?.key || "X-API-Key"}:{" "}
                    {auth.apiKey?.value || "&lt;value&gt;"}
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
                    <strong>Header format:</strong>
                  </p>
                  <code className="text-xs text-gray-900 dark:text-[#E6EDF3] font-mono break-all">
                    Authorization: Basic{" "}
                    {auth.basicAuth?.username && auth.basicAuth?.password
                      ? safeBase64(
                          `${auth.basicAuth.username}:${auth.basicAuth.password}`
                        ) ?? "&lt;base64(username:password)&gt;"
                      : "&lt;base64(username:password)&gt;"}
                  </code>
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Schema Modal */}
      <SchemaModal
        isOpen={isSchemaModalOpen}
        onClose={() => setIsSchemaModalOpen(false)}
        onSelect={handleSchemaSelect}
        schemas={schemas}
        setSchemas={setSchemas}
        protocol="REST"
      />
    </div>
  );
}
