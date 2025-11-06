import { useState, useEffect } from "react";
import React from "react";
import { SchemaModal } from "./SchemaModal";
import { getAllSchemas, type SchemaResponse } from "../services/api";

interface StatusCode {
  code: string;
  type: "Success" | "Error";
  message: string;
  headers?: Array<{ key: string; value: string }>; // Response headers
  schema?: {
    ref?: string; // 스키마 참조 (예: "User")
    properties?: Record<string, any>; // 인라인 스키마
  };
}

interface ApiResponseCardProps {
  statusCodes: StatusCode[];
  setStatusCodes: (codes: StatusCode[]) => void;
  isReadOnly?: boolean;
}

export function ApiResponseCard({
  statusCodes,
  setStatusCodes,
  isReadOnly = false,
}: ApiResponseCardProps) {
  const statusCodeTemplates: StatusCode[] = [
    { code: "200", type: "Success", message: "요청이 성공적으로 처리됨" },
    { code: "201", type: "Success", message: "리소스가 성공적으로 생성됨" },
    { code: "204", type: "Success", message: "요청 성공 (응답 본문 없음)" },
    { code: "400", type: "Error", message: "잘못된 요청 형식" },
    { code: "401", type: "Error", message: "인증 실패 (유효하지 않은 토큰)" },
    { code: "403", type: "Error", message: "접근 권한이 없음" },
    { code: "404", type: "Error", message: "리소스를 찾을 수 없음" },
    { code: "409", type: "Error", message: "리소스 충돌" },
    { code: "422", type: "Error", message: "유효성 검사 실패" },
    { code: "500", type: "Error", message: "서버 내부 오류" },
    { code: "502", type: "Error", message: "게이트웨이 오류" },
    { code: "503", type: "Error", message: "서비스 사용 불가" },
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
    if (isResponseSchemaModalOpen) {
      loadSchemas();
    }
  }, [isResponseSchemaModalOpen]);

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
      <p className="text-xs text-gray-600 dark:text-[#8B949E] mb-4">
        응답 바디, 헤더 및 상태 코드 설정
      </p>

      {/* Content */}
      <div className="space-y-4">
        <div>
          <div className="flex items-center justify-between mb-4">
            <p className="text-sm text-gray-600 dark:text-gray-400">
              HTTP 상태 코드 관리
            </p>
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
                className="px-3 py-1 text-sm border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB]"
              >
                <option value="">템플릿 선택...</option>
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
                className="px-3 py-1 text-sm text-[#2563EB] font-medium border border-[#2563EB] rounded-md hover:bg-[#2563EB] hover:text-white transition-colors"
              >
                + Add Custom
              </button>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead>
                <tr className="border-b border-gray-200 dark:border-gray-700">
                  <th className="px-4 py-3 font-medium text-gray-700 dark:text-gray-300">
                    Status Code
                  </th>
                  <th className="px-4 py-3 font-medium text-gray-700 dark:text-gray-300">
                    Type
                  </th>
                  <th className="px-4 py-3 font-medium text-gray-700 dark:text-gray-300">
                    Message / Description
                  </th>
                  <th className="px-4 py-3 font-medium text-gray-700 dark:text-gray-300">
                    Schema
                  </th>
                  <th className="px-4 py-3 font-medium text-gray-700 dark:text-gray-300">
                    Headers
                  </th>
                  <th className="px-4 py-3"></th>
                </tr>
              </thead>
              <tbody>
                {statusCodes.map((statusCode, index) => (
                  <React.Fragment key={index}>
                    <tr className="border-b border-gray-100 dark:border-gray-800 hover:bg-gray-50 dark:hover:bg-[#161B22]">
                      <td className="px-4 py-3">
                        <input
                          type="text"
                          value={statusCode.code}
                          onChange={(e) =>
                            updateStatusCode(index, "code", e.target.value)
                          }
                          placeholder="200"
                          className="w-full px-3 py-2 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB]"
                        />
                      </td>
                      <td className="px-4 py-3">
                        <select
                          value={statusCode.type}
                          onChange={(e) =>
                            updateStatusCode(index, "type", e.target.value)
                          }
                          className="px-3 py-2 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB]"
                        >
                          <option value="Success">Success</option>
                          <option value="Error">Error</option>
                        </select>
                      </td>
                      <td className="px-4 py-3">
                        <input
                          type="text"
                          value={statusCode.message}
                          onChange={(e) =>
                            updateStatusCode(index, "message", e.target.value)
                          }
                          placeholder="예: 요청이 성공적으로 처리됨"
                          className="w-full px-3 py-2 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB]"
                        />
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => {
                              setSelectedStatusCodeIndex(index);
                              setIsResponseSchemaModalOpen(true);
                            }}
                            className="px-3 py-1.5 text-xs border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-700 dark:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#161B22] transition-colors"
                          >
                            {statusCode.schema?.ref
                              ? `Schema: ${statusCode.schema.ref}`
                              : statusCode.schema?.properties
                              ? "Inline Schema"
                              : "Schema 선택"}
                          </button>
                          {statusCode.schema && (
                            <button
                              onClick={() => {
                                const updated = [...statusCodes];
                                updated[index] = {
                                  ...updated[index],
                                  schema: undefined,
                                };
                                setStatusCodes(updated);
                              }}
                              className="p-1 text-red-500 hover:text-red-600"
                              title="Schema 제거"
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
                          )}
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        <button
                          onClick={() => {
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
                          className="px-3 py-1.5 text-xs border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-700 dark:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#161B22] transition-colors flex items-center gap-1"
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
                            className={`w-4 h-4 transition-transform ${
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
                      <td className="px-4 py-3">
                        <button
                          onClick={() => removeStatusCode(index)}
                          className="p-2 text-red-500 hover:text-red-600"
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
                      </td>
                    </tr>
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
                                className="px-3 py-1 text-xs text-blue-600 dark:text-blue-400 border border-blue-600 dark:border-blue-400 rounded-md hover:bg-blue-50 dark:hover:bg-blue-900/20"
                              >
                                + Add Header
                              </button>
                            </div>
                            {statusCode.headers.map((header, headerIndex) => (
                              <div
                                key={headerIndex}
                                className="flex items-center gap-3"
                              >
                                <input
                                  type="text"
                                  value={header.key}
                                  onChange={(e) => {
                                    const updated = [...statusCodes];
                                    updated[index].headers![headerIndex].key =
                                      e.target.value;
                                    setStatusCodes(updated);
                                  }}
                                  placeholder="Header Key (e.g., Content-Type)"
                                  className="flex-1 px-3 py-2 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-blue-500"
                                />
                                <input
                                  type="text"
                                  value={header.value}
                                  onChange={(e) => {
                                    const updated = [...statusCodes];
                                    updated[index].headers![headerIndex].value =
                                      e.target.value;
                                    setStatusCodes(updated);
                                  }}
                                  placeholder="Header Value (e.g., application/json)"
                                  className="flex-1 px-3 py-2 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-blue-500"
                                />
                                <button
                                  onClick={() => {
                                    const updated = [...statusCodes];
                                    updated[index].headers = updated[
                                      index
                                    ].headers!.filter(
                                      (_, i) => i !== headerIndex
                                    );
                                    setStatusCodes(updated);
                                  }}
                                  className="p-2 text-red-500 hover:text-red-600"
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
                                      d="M6 18L18 6M6 6l12 12"
                                    />
                                  </svg>
                                </button>
                              </div>
                            ))}
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
        onSelect={(schema) => {
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
          }
          setIsResponseSchemaModalOpen(false);
          setSelectedStatusCodeIndex(null);
        }}
        schemas={schemas}
        setSchemas={setSchemas}
      />
    </div>
  );
}
