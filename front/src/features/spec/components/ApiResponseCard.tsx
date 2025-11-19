/* eslint-disable @typescript-eslint/no-explicit-any */
import { useState, useEffect } from "react";
import React from "react";
import { SchemaModal } from "./SchemaModal";
import { SchemaViewer } from "./SchemaViewer";
import { SchemaFieldEditor } from "./SchemaFieldEditor";
import { getAllSchemas, getSchema, type SchemaResponse } from "../services/api";
import { parseOpenAPISchemaToSchemaField } from "../utils/schemaConverter";
import type {
  SchemaField,
  SchemaType,
  PrimitiveTypeName,
} from "../types/schema.types";

interface StatusCode {
  code: string;
  type: "Success" | "Error";
  message: string;
  headers?: Array<{ key: string; value: string }>; // Response headers
  schema?: {
    ref?: string; // 스키마 참조 (예: "User")
    properties?: Record<string, any>; // 인라인 스키마
    type?: string; // Primitive 타입 (string, integer, number, boolean)
    isArray?: boolean; // Array of Schema 여부
    minItems?: number; // Array 최소 개수
    maxItems?: number; // Array 최대 개수
  };
}

interface ApiResponseCardProps {
  statusCodes: StatusCode[];
  setStatusCodes: (codes: StatusCode[]) => void;
  isReadOnly?: boolean;
  isDocumentView?: boolean;
}

export function ApiResponseCard({
  statusCodes,
  setStatusCodes,
  isReadOnly = false,
  isDocumentView = false,
}: ApiResponseCardProps) {
  const statusCodeTemplates: StatusCode[] = [
    { code: "200", type: "Success", message: "Successfully processed request" },
    { code: "201", type: "Success", message: "Successfully created resource" },
    { code: "204", type: "Success", message: "Success (no response body)" },
    { code: "400", type: "Error", message: "Bad request format" },
    {
      code: "401",
      type: "Error",
      message: "Authentication failed (invalid token)",
    },
    { code: "403", type: "Error", message: "Access denied" },
    { code: "404", type: "Error", message: "Resource not found" },
    { code: "409", type: "Error", message: "Resource conflict" },
    { code: "422", type: "Error", message: "Validation failed" },
    { code: "500", type: "Error", message: "Internal server error" },
    { code: "502", type: "Error", message: "Gateway error" },
    { code: "503", type: "Error", message: "Service unavailable" },
  ];

  const addStatusCode = (template?: StatusCode) => {
    if (isReadOnly) return;
    if (template) {
      setStatusCodes([...statusCodes, { ...template }]);
    } else {
      setStatusCodes([
        ...statusCodes,
        { code: "", type: "Success", message: "" },
      ]);
    }
  };

  const removeStatusCode = (index: number) => {
    if (isReadOnly) return;
    setStatusCodes(statusCodes.filter((_, i) => i !== index));
  };

  const updateStatusCode = (
    index: number,
    field: "code" | "type" | "message",
    value: string
  ) => {
    if (isReadOnly) return;
    const updated = [...statusCodes];
    updated[index] = { ...updated[index], [field]: value };
    setStatusCodes(updated);
  };

  const [schemas, setSchemas] = useState<SchemaResponse[]>([]);
  const [isResponseSchemaModalOpen, setIsResponseSchemaModalOpen] =
    useState(false);
  const [selectedStatusCodeIndex, setSelectedStatusCodeIndex] = useState<
    number | null
  >(null);

  // Response Headers 상태
  const [expandedStatusCode, setExpandedStatusCode] = useState<number | null>(
    null
  );

  // 스키마 필드 표시 상태
  const [expandedSchemaIndex, setExpandedSchemaIndex] = useState<number | null>(
    null
  );
  const [schemaFieldsMap, setSchemaFieldsMap] = useState<
    Record<number, SchemaField[]>
  >({});
  const [schemaNamesMap, setSchemaNamesMap] = useState<
    Record<number, string>
  >({});

  // 스키마 목록 로드
  const loadSchemas = async () => {
    try {
      const response = await getAllSchemas();
      setSchemas(response.data);
    } catch (err) {
      console.error("Failed to load schemas:", err);
    }
  };

  // 컴포넌트 마운트 시 스키마 목록 로드
  useEffect(() => {
    loadSchemas();
  }, []);

  // 모달이 열릴 때마다 스키마 목록 다시 로드
  useEffect(() => {
    if (isResponseSchemaModalOpen) {
      loadSchemas();
    }
  }, [isResponseSchemaModalOpen]);

  // 기존에 선택된 스키마의 필드 정보 로드
  useEffect(() => {
    const loadExistingSchemaFields = async () => {
      for (let i = 0; i < statusCodes.length; i++) {
        const statusCode = statusCodes[i];
        if (
          statusCode.schema?.ref &&
          !schemaFieldsMap[i] &&
          !schemaNamesMap[i]
        ) {
          try {
            const schemaResponse = await getSchema(statusCode.schema.ref);
            const schemaData = schemaResponse.data;

            if (schemaData.properties) {
              const fields = Object.entries(schemaData.properties).map(
                ([key, propSchema]: [string, any]) => {
                  const field = parseOpenAPISchemaToSchemaField(
                    key,
                    propSchema
                  );
                  if (
                    schemaData.required &&
                    schemaData.required.includes(key)
                  ) {
                    field.required = true;
                  }
                  return field;
                }
              );

              setSchemaFieldsMap((prev) => ({
                ...prev,
                [i]: fields,
              }));
              setSchemaNamesMap((prev) => ({
                ...prev,
                [i]: statusCode.schema!.ref!,
              }));

              // 기존 스키마가 있고 필드가 로드되면 자동 확장 (단, 이미 확장된 것이 없을 때만)
              if (expandedSchemaIndex === null) {
                setExpandedSchemaIndex(i);
              }
            }
          } catch (error) {
            console.error("스키마 필드 로드 실패:", error);
          }
        }
      }
    };

    loadExistingSchemaFields();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusCodes]);

  // 문서 형식 뷰
  if (isDocumentView) {
    return (
      <div className="space-y-4">
        {statusCodes.length > 0 ? (
          statusCodes.map((statusCode, index) => (
            <div
              key={index}
              className="border-b border-gray-200 dark:border-[#2D333B] pb-4 last:border-b-0 last:pb-0"
            >
              <div className="flex items-center gap-3 mb-2">
                <span
                  className={`px-2 py-1 rounded text-sm font-semibold ${
                    statusCode.type === "Success"
                      ? "bg-green-100 dark:bg-green-900/20 text-green-700 dark:text-green-300"
                      : "bg-red-100 dark:bg-red-900/20 text-red-700 dark:text-red-300"
                  }`}
                >
                  {statusCode.code}
                </span>
                <span className="text-sm font-medium text-gray-900 dark:text-[#E6EDF3]">
                  {statusCode.message}
                </span>
              </div>
              {statusCode.schema && (
                <div className="mt-4">
                  {(() => {
                    // StatusCode schema를 SchemaField[]로 변환
                    const convertSchemaToFields = ():
                      | SchemaField[]
                      | undefined => {
                      if (statusCode.schema?.ref) {
                        return undefined; // ref는 SchemaViewer에서 처리
                      }
                      if (statusCode.schema?.properties) {
                        return Object.entries(statusCode.schema.properties).map(
                          ([key, prop]: [string, any]) => {
                            const field: SchemaField = {
                              key,
                              description: prop.description,
                              required: prop.required || false,
                              schemaType:
                                prop.type === "array"
                                  ? {
                                      kind: "array" as const,
                                      items: prop.items?.$ref
                                        ? {
                                            kind: "ref" as const,
                                            schemaName: prop.items.$ref.replace(
                                              "#/components/schemas/",
                                              ""
                                            ),
                                          }
                                        : {
                                            kind: "primitive" as const,
                                            type: (prop.items?.type ||
                                              "string") as PrimitiveTypeName,
                                          },
                                    }
                                  : prop.$ref
                                  ? {
                                      kind: "ref" as const,
                                      schemaName: prop.$ref.replace(
                                        "#/components/schemas/",
                                        ""
                                      ),
                                    }
                                  : {
                                      kind: "primitive" as const,
                                      type: (prop.type ||
                                        "string") as PrimitiveTypeName,
                                    },
                            };
                            return field;
                          }
                        );
                      }
                      return undefined;
                    };

                    const fields = convertSchemaToFields();
                    const schemaType: SchemaType | undefined =
                      statusCode.schema?.type && !statusCode.schema?.ref
                        ? statusCode.schema.isArray
                          ? {
                              kind: "array" as const,
                              items: {
                                kind: "primitive" as const,
                                type: statusCode.schema
                                  .type as PrimitiveTypeName,
                              },
                            }
                          : {
                              kind: "primitive" as const,
                              type: statusCode.schema.type as PrimitiveTypeName,
                            }
                        : undefined;

                    return (
                      <SchemaViewer
                        schemaType={schemaType}
                        fields={fields}
                        schemaRef={statusCode.schema?.ref}
                        contentType="application/json"
                      />
                    );
                  })()}
                </div>
              )}
              {statusCode.headers && statusCode.headers.length > 0 && (
                <div className="mt-2 ml-5 space-y-1">
                  <div className="text-xs font-semibold text-gray-700 dark:text-[#C9D1D9]">
                    Headers:
                  </div>
                  {statusCode.headers.map((header, hIndex) => (
                    <div
                      key={hIndex}
                      className="text-xs text-gray-600 dark:text-[#8B949E] ml-2"
                    >
                      {header.key}: {header.value}
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))
        ) : (
          <div className="text-sm text-gray-500 dark:text-[#8B949E] italic">
            No response codes configured.
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="rounded-md border border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#161B22] p-4 shadow-sm">
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
            d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
          />
        </svg>
        <span>Response</span>
      </div>

      {/* Content */}
      <div className="space-y-4">
        <div>
          <div className="flex items-center justify-between mb-4">
            <p className="text-sm text-gray-600 dark:text-gray-400">
              HTTP Status Code Management
            </p>
            {!isReadOnly && (
              <div className="flex gap-2">
                <select
                  onChange={(e) => {
                    const selectedCode = e.target.value;
                    if (selectedCode) {
                      const template = statusCodeTemplates.find(
                        (t) => t.code === selectedCode
                      );
                      if (template) {
                        addStatusCode(template);
                        e.target.value = "";
                      }
                    }
                  }}
                  className="px-3 py-1 text-sm border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] focus:outline-none focus:ring-1 focus:ring-gray-400 dark:focus:ring-gray-500 focus:border-gray-400 dark:focus:border-gray-500"
                >
                  <option value="">Select Template</option>
                  <optgroup label="Success">
                    {statusCodeTemplates
                      .filter((t) => t.type === "Success")
                      .map((template) => (
                        <option key={template.code} value={template.code}>
                          {template.code} - {template.message}
                        </option>
                      ))}
                  </optgroup>
                  <optgroup label="Error">
                    {statusCodeTemplates
                      .filter((t) => t.type === "Error")
                      .map((template) => (
                        <option key={template.code} value={template.code}>
                          {template.code} - {template.message}
                        </option>
                      ))}
                  </optgroup>
                </select>
                <button
                  onClick={() => addStatusCode()}
                  className="px-3 py-1 text-sm text-[#2563EB] font-medium border border-[#2563EB] rounded-md hover:bg-[#2563EB] hover:text-white transition-all active:translate-y-[1px] focus:outline-none focus-visible:outline-none focus-visible:[box-shadow:inset_2px_0_0_#3B82F6] dark:focus-visible:[box-shadow:inset_2px_0_0_#60A5FA]"
                >
                  + Add Custom
                </button>
              </div>
            )}
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead>
                <tr className="border-b border-gray-200 dark:border-gray-700">
                  <th className="px-2 py-2 font-medium text-gray-700 dark:text-gray-300 w-24">
                    Status Code
                  </th>
                  <th className="px-2 py-2 font-medium text-gray-700 dark:text-gray-300 w-20">
                    Type
                  </th>
                  <th className="px-2 py-2 font-medium text-gray-700 dark:text-gray-300 min-w-[200px]">
                    Message / Description
                  </th>
                  <th className="px-2 py-2 font-medium text-gray-700 dark:text-gray-300 min-w-[280px]">
                    Schema
                  </th>
                  <th className="px-2 py-2 font-medium text-gray-700 dark:text-gray-300 w-24">
                    Headers
                  </th>
                  <th className="px-2 py-2 w-10"></th>
                </tr>
              </thead>
              <tbody>
                {statusCodes.map((statusCode, index) => (
                  <React.Fragment key={index}>
                    <tr className="border-b border-gray-100 dark:border-gray-800 hover:bg-gray-50 dark:hover:bg-[#161B22]">
                      <td className="px-2 py-2">
                        <input
                          type="text"
                          value={statusCode.code}
                          onChange={(e) =>
                            updateStatusCode(index, "code", e.target.value)
                          }
                          placeholder="200"
                          disabled={isReadOnly}
                          className={`w-full px-2 py-1 text-xs border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-gray-400 dark:focus:ring-gray-500 focus:border-gray-400 dark:focus:border-gray-500 ${
                            isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                          }`}
                        />
                      </td>
                      <td className="px-2 py-2">
                        <select
                          value={statusCode.type}
                          onChange={(e) =>
                            updateStatusCode(index, "type", e.target.value)
                          }
                          disabled={isReadOnly}
                          className={`w-full px-2 py-1 text-xs border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] focus:outline-none focus:ring-1 focus:ring-gray-400 dark:focus:ring-gray-500 focus:border-gray-400 dark:focus:border-gray-500 ${
                            isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                          }`}
                        >
                          <option value="Success">Success</option>
                          <option value="Error">Error</option>
                        </select>
                      </td>
                      <td className="px-2 py-2">
                        <input
                          type="text"
                          value={statusCode.message}
                          onChange={(e) =>
                            updateStatusCode(index, "message", e.target.value)
                          }
                          placeholder="e.g. Successfully processed request"
                          disabled={isReadOnly}
                          className={`w-full px-2 py-1 text-xs border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-gray-400 dark:focus:ring-gray-500 focus:border-gray-400 dark:focus:border-gray-500 ${
                            isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                          }`}
                        />
                      </td>
                      <td className="px-2 py-2">
                        <div className="flex flex-col gap-1.5">
                          {/* 첫 번째 줄: Kind 선택, Schema/Primitive 선택, 보조 버튼들 */}
                          <div className="flex items-center gap-1 flex-nowrap">
                            {/* Kind 선택 (Schema / Primitive / None) */}
                            <select
                              value={
                                statusCode.schema?.ref ||
                                statusCode.schema?.properties
                                  ? "schema"
                                  : statusCode.schema?.type
                                  ? "primitive"
                                  : "none"
                              }
                              onChange={async (e) => {
                                if (isReadOnly) return;
                                const updated = [...statusCodes];
                                const kind = e.target.value;
                                if (kind === "none") {
                                  updated[index] = {
                                    ...updated[index],
                                    schema: undefined,
                                  };
                                  // 필드 정보 제거
                                  setSchemaFieldsMap((prev) => {
                                    const newMap = { ...prev };
                                    delete newMap[index];
                                    return newMap;
                                  });
                                  setSchemaNamesMap((prev) => {
                                    const newMap = { ...prev };
                                    delete newMap[index];
                                    return newMap;
                                  });
                                  setExpandedSchemaIndex(null);
                                } else if (kind === "primitive") {
                                  updated[index] = {
                                    ...updated[index],
                                    schema: {
                                      type: "string",
                                      isArray: false,
                                    },
                                  };
                                  // 필드 정보 제거
                                  setSchemaFieldsMap((prev) => {
                                    const newMap = { ...prev };
                                    delete newMap[index];
                                    return newMap;
                                  });
                                  setSchemaNamesMap((prev) => {
                                    const newMap = { ...prev };
                                    delete newMap[index];
                                    return newMap;
                                  });
                                  setExpandedSchemaIndex(null);
                                } else if (kind === "schema") {
                                  // Schema 선택 시 빈 schema 객체로 설정하고 버튼만 보이게 함
                                  updated[index] = {
                                    ...updated[index],
                                    schema: {
                                      ref: undefined,
                                      properties: undefined,
                                    },
                                  };
                                }
                                setStatusCodes(updated);
                              }}
                              disabled={isReadOnly}
                              className={`px-1.5 py-1 text-[11px] border border-gray-300 dark:border-[#2D333B] rounded bg-white dark:bg-[#0D1117] text-gray-700 dark:text-[#E6EDF3] focus:outline-none focus:ring-1 focus:ring-gray-400 dark:focus:ring-gray-500 focus:border-gray-400 dark:focus:border-gray-500 ${
                                isReadOnly
                                  ? "opacity-60 cursor-not-allowed"
                                  : ""
                              }`}
                            >
                              <option value="none">None</option>
                              <option value="schema">Schema</option>
                              <option value="primitive">Primitive</option>
                            </select>

                            {/* Schema 선택 버튼 (Kind가 schema일 때 항상 표시) */}
                            {!statusCode.schema?.type &&
                              (statusCode.schema?.ref ||
                                statusCode.schema?.properties ||
                                statusCode.schema) && (
                                <button
                                  onClick={() => {
                                    if (isReadOnly) return;
                                    setSelectedStatusCodeIndex(index);
                                    setIsResponseSchemaModalOpen(true);
                                  }}
                                  disabled={isReadOnly}
                                  className={`px-1.5 py-1 text-[11px] border border-gray-300 dark:border-[#2D333B] rounded bg-white dark:bg-[#0D1117] text-gray-700 dark:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#161B22] transition-colors flex-1 min-w-0 text-left truncate ${
                                    isReadOnly
                                      ? "opacity-60 cursor-not-allowed"
                                      : ""
                                  }`}
                                  title={statusCode.schema?.ref || "Select Schema..."}
                                >
                                  <span className="truncate block">
                                    {statusCode.schema?.ref || "Select Schema..."}
                                  </span>
                                </button>
                              )}

                            {/* Primitive 타입 선택 */}
                            {statusCode.schema?.type &&
                              !statusCode.schema?.ref &&
                              !statusCode.schema?.properties && (
                                <select
                                  value={statusCode.schema.type}
                                  onChange={(e) => {
                                    if (isReadOnly) return;
                                    const updated = [...statusCodes];
                                    updated[index] = {
                                      ...updated[index],
                                      schema: {
                                        ...updated[index].schema!,
                                        type: e.target.value,
                                      },
                                    };
                                    setStatusCodes(updated);
                                  }}
                                  disabled={isReadOnly}
                                  className={`px-1.5 py-1 text-[11px] border border-gray-300 dark:border-[#2D333B] rounded bg-white dark:bg-[#0D1117] text-gray-700 dark:text-[#E6EDF3] focus:outline-none focus:ring-1 focus:ring-gray-400 dark:focus:ring-gray-500 focus:border-gray-400 dark:focus:border-gray-500 ${
                                    isReadOnly
                                      ? "opacity-60 cursor-not-allowed"
                                      : ""
                                  }`}
                                >
                                  <option value="string">string</option>
                                  <option value="integer">integer</option>
                                  <option value="number">number</option>
                                  <option value="boolean">boolean</option>
                                </select>
                              )}

                            {/* 보조 버튼 그룹: Array, X */}
                            {statusCode.schema && (
                              <div className="flex items-center gap-0.5 ml-auto shrink-0">
                                {/* Array 체크박스 */}
                                <label
                                  className={`flex items-center gap-0.5 text-[10px] text-gray-600 dark:text-gray-400 whitespace-nowrap cursor-pointer ${
                                    isReadOnly
                                      ? "opacity-60 cursor-not-allowed"
                                      : ""
                                  }`}
                                  title="Array"
                                >
                                  <input
                                    type="checkbox"
                                    checked={statusCode.schema.isArray || false}
                                    onChange={(e) => {
                                      if (isReadOnly) return;
                                      const updated = [...statusCodes];
                                      updated[index] = {
                                        ...updated[index],
                                        schema: {
                                          ...updated[index].schema!,
                                          isArray: e.target.checked,
                                        },
                                      };
                                      setStatusCodes(updated);
                                    }}
                                    disabled={isReadOnly}
                                    className="w-2.5 h-2.5"
                                  />
                                  <span className="text-[10px]">Arr</span>
                                </label>

                                {/* X 버튼 */}
                                <button
                                  onClick={() => {
                                    if (isReadOnly) return;
                                    const updated = [...statusCodes];
                                    updated[index] = {
                                      ...updated[index],
                                      schema: undefined,
                                    };
                                    setStatusCodes(updated);
                                    // 필드 정보 제거
                                    setSchemaFieldsMap((prev) => {
                                      const newMap = { ...prev };
                                      delete newMap[index];
                                      return newMap;
                                    });
                                    setSchemaNamesMap((prev) => {
                                      const newMap = { ...prev };
                                      delete newMap[index];
                                      return newMap;
                                    });
                                    setExpandedSchemaIndex(null);
                                  }}
                                  disabled={isReadOnly}
                                  className={`p-0.5 text-red-500 hover:text-red-600 transition-colors ${
                                    isReadOnly
                                      ? "opacity-50 cursor-not-allowed"
                                      : ""
                                  }`}
                                  title="Clear Schema"
                                >
                                  <svg
                                    className="w-3 h-3"
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
                          </div>

                          {/* Array Items 개수 */}
                          {statusCode.schema?.isArray && (
                            <div className="flex items-center gap-2 text-xs">
                              <label className="flex items-center gap-1 text-gray-600 dark:text-gray-400">
                                <span>Min:</span>
                                <input
                                  type="number"
                                  value={statusCode.schema.minItems || ""}
                                  onChange={(e) => {
                                    if (isReadOnly) return;
                                    const updated = [...statusCodes];
                                    updated[index] = {
                                      ...updated[index],
                                      schema: {
                                        ...updated[index].schema!,
                                        minItems: e.target.value
                                          ? parseInt(e.target.value)
                                          : undefined,
                                      },
                                    };
                                    setStatusCodes(updated);
                                  }}
                                  disabled={isReadOnly}
                                  className={`w-16 px-2 py-1 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] ${
                                    isReadOnly
                                      ? "opacity-60 cursor-not-allowed"
                                      : ""
                                  }`}
                                  placeholder="1"
                                  min="0"
                                />
                              </label>
                              <label className="flex items-center gap-1 text-gray-600 dark:text-gray-400">
                                <span>Max:</span>
                                <input
                                  type="number"
                                  value={statusCode.schema.maxItems || ""}
                                  onChange={(e) => {
                                    if (isReadOnly) return;
                                    const updated = [...statusCodes];
                                    updated[index] = {
                                      ...updated[index],
                                      schema: {
                                        ...updated[index].schema!,
                                        maxItems: e.target.value
                                          ? parseInt(e.target.value)
                                          : undefined,
                                      },
                                    };
                                    setStatusCodes(updated);
                                  }}
                                  disabled={isReadOnly}
                                  className={`w-16 px-2 py-1 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] ${
                                    isReadOnly
                                      ? "opacity-60 cursor-not-allowed"
                                      : ""
                                  }`}
                                  placeholder="3"
                                  min="1"
                                />
                              </label>
                            </div>
                          )}
                        </div>
                      </td>
                      <td className="px-2 py-2">
                        <button
                          onClick={() => {
                            if (isReadOnly) return;
                            if (expandedStatusCode === index) {
                              setExpandedStatusCode(null);
                            } else {
                              setExpandedStatusCode(index);
                              // headers 초기화 (없으면 빈 배열)
                              if (!statusCode.headers) {
                                const updated = [...statusCodes];
                                updated[index] = {
                                  ...updated[index],
                                  headers: [
                                    {
                                      key: "Content-Type",
                                      value: "application/json",
                                    },
                                  ],
                                };
                                setStatusCodes(updated);
                              }
                            }
                          }}
                          disabled={isReadOnly}
                          className={`px-2 py-1 text-[11px] border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-700 dark:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#161B22] transition-colors flex items-center gap-1 whitespace-nowrap ${
                            isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                          }`}
                        >
                          {statusCode.headers &&
                          statusCode.headers.length > 0 ? (
                            <span className="text-blue-600 dark:text-blue-400">
                              {statusCode.headers.length} header(s)
                            </span>
                          ) : (
                            <span>+ Headers</span>
                          )}
                          <svg
                            className={`w-3 h-3 transition-transform ${
                              expandedStatusCode === index ? "rotate-180" : ""
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
                      </td>
                      <td className="px-2 py-2">
                        <button
                          onClick={() => removeStatusCode(index)}
                          disabled={isReadOnly}
                          className={`p-1 text-red-500 hover:text-red-600 ${
                            isReadOnly ? "opacity-50 cursor-not-allowed" : ""
                          }`}
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
                              d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                            />
                          </svg>
                        </button>
                      </td>
                    </tr>
                    {/* Schema Fields Row - 스키마가 선택되고 필드가 있을 때 항상 헤더 표시 */}
                    {statusCode.schema?.ref &&
                      schemaFieldsMap[index] &&
                      schemaFieldsMap[index].length > 0 && (
                        <tr className="bg-gray-50 dark:bg-[#161B22]">
                          <td colSpan={6} className="px-4 py-3">
                            <div className="space-y-3">
                              {/* Schema Header with Toggle */}
                              <div className="flex items-center justify-between border-b border-gray-200 dark:border-[#2D333B] pb-2">
                                <div className="flex items-center gap-2">
                                  <span className="px-3 py-1.5 bg-emerald-100 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-300 rounded-md text-sm font-semibold border border-emerald-200 dark:border-emerald-800">
                                    Schema: {schemaNamesMap[index] || statusCode.schema?.ref}
                                  </span>
                                  <span className="text-xs text-gray-500 dark:text-gray-400">
                                    ({schemaFieldsMap[index].length} fields)
                                  </span>
                                </div>
                                <button
                                  onClick={() => {
                                    if (isReadOnly) return;
                                    setExpandedSchemaIndex(
                                      expandedSchemaIndex === index
                                        ? null
                                        : index
                                    );
                                  }}
                                  disabled={isReadOnly}
                                  className={`flex items-center gap-1.5 px-2 py-1 text-xs border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-700 dark:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#161B22] transition-colors ${
                                    isReadOnly
                                      ? "opacity-60 cursor-not-allowed"
                                      : ""
                                  }`}
                                  title={
                                    expandedSchemaIndex === index
                                      ? "Hide Schema Fields"
                                      : "Show Schema Fields"
                                  }
                                >
                                  <span>
                                    {expandedSchemaIndex === index
                                      ? "Hide Fields"
                                      : "Show Fields"}
                                  </span>
                                  <svg
                                    className={`w-4 h-4 transition-transform ${
                                      expandedSchemaIndex === index
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
                              </div>

                              {/* Schema Fields - 펼쳐져 있을 때만 표시 */}
                              {expandedSchemaIndex === index && (
                                <div className="space-y-2 pt-2">
                                  {schemaFieldsMap[index].map((field, fieldIndex) => (
                                    <SchemaFieldEditor
                                      key={fieldIndex}
                                      field={field}
                                      onChange={() => {
                                        // 읽기 전용이므로 변경 불가
                                      }}
                                      isReadOnly={true}
                                      allowFileType={false}
                                      allowMockExpression={false}
                                    />
                                  ))}
                                </div>
                              )}
                            </div>
                          </td>
                        </tr>
                      )}

                    {/* Headers Expandable Row */}
                    {expandedStatusCode === index && statusCode.headers && (
                      <tr className="bg-gray-50 dark:bg-[#161B22]">
                        <td colSpan={6} className="px-4 py-4">
                          <div className="space-y-3">
                            <div className="flex items-center justify-between">
                              <h4 className="text-sm font-semibold text-gray-700 dark:text-gray-300">
                                Response Headers
                              </h4>
                              <button
                                onClick={() => {
                                  if (isReadOnly) return;
                                  const updated = [...statusCodes];
                                  updated[index] = {
                                    ...updated[index],
                                    headers: [
                                      ...(updated[index].headers || []),
                                      { key: "", value: "" },
                                    ],
                                  };
                                  setStatusCodes(updated);
                                }}
                                disabled={isReadOnly}
                                className={`px-3 py-1.5 text-xs text-blue-600 dark:text-blue-400 border border-blue-600 dark:border-blue-400 rounded-md bg-white dark:bg-[#0D1117] hover:bg-blue-50 dark:hover:bg-blue-900/20 transition-colors ${
                                  isReadOnly
                                    ? "opacity-50 cursor-not-allowed"
                                    : ""
                                }`}
                              >
                                + Add Header
                              </button>
                            </div>
                            <div className="space-y-2">
                              {statusCode.headers.map((header, headerIndex) => (
                                <div
                                  key={headerIndex}
                                  className="flex items-center gap-2 p-2 border border-gray-200 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117]"
                                >
                                  <input
                                    type="text"
                                    value={header.key}
                                    onChange={(e) => {
                                      if (isReadOnly) return;
                                      const updated = [...statusCodes];
                                      updated[index].headers![headerIndex].key =
                                        e.target.value;
                                      setStatusCodes(updated);
                                    }}
                                    placeholder="Header Key (e.g., Content-Type)"
                                    disabled={isReadOnly}
                                    className={`flex-1 px-3 py-1.5 text-sm border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-blue-500 ${
                                      isReadOnly
                                        ? "opacity-60 cursor-not-allowed"
                                        : ""
                                    }`}
                                  />
                                  <input
                                    type="text"
                                    value={header.value}
                                    onChange={(e) => {
                                      if (isReadOnly) return;
                                      const updated = [...statusCodes];
                                      updated[index].headers![headerIndex].value =
                                        e.target.value;
                                      setStatusCodes(updated);
                                    }}
                                    placeholder="Header Value (e.g., application/json)"
                                    disabled={isReadOnly}
                                    className={`flex-1 px-3 py-1.5 text-sm border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-blue-500 ${
                                      isReadOnly
                                        ? "opacity-60 cursor-not-allowed"
                                        : ""
                                    }`}
                                  />
                                  <button
                                    onClick={() => {
                                      if (isReadOnly) return;
                                      const updated = [...statusCodes];
                                      updated[index].headers = updated[
                                        index
                                      ].headers!.filter(
                                        (_, i) => i !== headerIndex
                                      );
                                      setStatusCodes(updated);
                                    }}
                                    disabled={isReadOnly}
                                    className={`p-1.5 text-red-500 hover:text-red-600 transition-colors ${
                                      isReadOnly
                                        ? "opacity-50 cursor-not-allowed"
                                        : ""
                                    }`}
                                    title="Remove Header"
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
                              ))}
                            </div>
                          </div>
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Response Schema 선택 모달 */}
      <SchemaModal
        isOpen={isResponseSchemaModalOpen}
        onClose={() => {
          setIsResponseSchemaModalOpen(false);
          setSelectedStatusCodeIndex(null);
        }}
        onSelect={async (schema) => {
          if (selectedStatusCodeIndex !== null) {
            const updated = [...statusCodes];
            // Schema를 ref 형태로 저장 (스키마 이름만 저장)
            updated[selectedStatusCodeIndex] = {
              ...updated[selectedStatusCodeIndex],
              schema: {
                ref: schema.name,
              },
            };
            setStatusCodes(updated);

            // 스키마 필드 정보 로드
            try {
              const schemaResponse = await getSchema(schema.name);
              const schemaData = schemaResponse.data;

              if (schemaData.properties) {
                const fields = Object.entries(schemaData.properties).map(
                  ([key, propSchema]: [string, any]) => {
                    const field = parseOpenAPISchemaToSchemaField(
                      key,
                      propSchema
                    );
                    if (
                      schemaData.required &&
                      schemaData.required.includes(key)
                    ) {
                      field.required = true;
                    }
                    return field;
                  }
                );

                setSchemaFieldsMap((prev) => ({
                  ...prev,
                  [selectedStatusCodeIndex]: fields,
                }));
                setSchemaNamesMap((prev) => ({
                  ...prev,
                  [selectedStatusCodeIndex]: schema.name,
                }));

                // 스키마 선택 시 필드 목록 자동 확장
                setExpandedSchemaIndex(selectedStatusCodeIndex);
              }
            } catch (error) {
              console.error("스키마 필드 로드 실패:", error);
            }
          }
          setIsResponseSchemaModalOpen(false);
          setSelectedStatusCodeIndex(null);
        }}
        schemas={schemas}
        setSchemas={setSchemas}
        protocol="REST"
      />
    </div>
  );
}
