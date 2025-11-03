import { useState } from "react";
import { FakerProviderSelect } from "./FakerProviderSelect";

interface KeyValuePair {
  key: string;
  value: string;
}

interface BodyField {
  key: string;
  value: string;
  type: string; // "string" | "integer" | "number" | "boolean" | "object" | "array" | "file"
  description?: string;
  required?: boolean;
}

interface RequestBody {
  type: "none" | "form-data" | "x-www-form-urlencoded" | "json" | "xml";
  contentType: string; // json, xml일 때 사용
  fields: BodyField[]; // 표 형식 데이터
}

interface ApiRequestCardProps {
  requestHeaders: KeyValuePair[];
  setRequestHeaders: (headers: KeyValuePair[]) => void;
  requestBody: RequestBody;
  setRequestBody: (body: RequestBody) => void;
  isReadOnly?: boolean;
}

export function ApiRequestCard({
  requestHeaders,
  setRequestHeaders,
  requestBody,
  setRequestBody,
  isReadOnly = false,
}: ApiRequestCardProps) {
  const bodyTypes: RequestBody["type"][] = [
    "none",
    "form-data",
    "x-www-form-urlencoded",
    "json",
    "xml",
  ];
  const fieldTypes = [
    "string",
    "integer",
    "number",
    "boolean",
    "object",
    "array",
    "file",
  ];

  const addHeader = () => {
    if (isReadOnly) return;
    setRequestHeaders([...requestHeaders, { key: "", value: "" }]);
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

  const [activeTab, setActiveTab] = useState("headers");

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
                <div key={index} className="flex gap-2">
                  <input
                    type="text"
                    value={header.key}
                    onChange={(e) => updateHeader(index, "key", e.target.value)}
                    placeholder="Header Name (e.g., Content-Type)"
                    disabled={isReadOnly}
                    className="flex-1 px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm"
                  />
                  <input
                    type="text"
                    value={header.value}
                    onChange={(e) =>
                      updateHeader(index, "value", e.target.value)
                    }
                    placeholder="Header Value (e.g., application/json)"
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
                      contentType:
                        type === "json"
                          ? "application/json"
                          : type === "xml"
                          ? "application/xml"
                          : "",
                      fields:
                        type === "none"
                          ? []
                          : [{ key: "", value: "", type: "string" }],
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
                <div className="mb-3">
                  <button
                    onClick={() => {
                      setRequestBody({
                        ...requestBody,
                        fields: [
                          ...(requestBody.fields || []),
                          { key: "", value: "", type: "string" },
                        ],
                      });
                    }}
                    className="px-3 py-1 text-sm text-[#2563EB] hover:text-[#1E40AF] font-medium"
                  >
                    + Add Field
                  </button>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full text-sm border border-gray-200 dark:border-gray-700 rounded-md">
                    <thead>
                      <tr className="bg-gray-50 dark:bg-[#161B22]">
                        <th className="px-4 py-3 text-left font-medium text-gray-700 dark:text-gray-300 border-b border-gray-200 dark:border-gray-700">
                          Key
                        </th>
                        <th className="px-4 py-3 text-left font-medium text-gray-700 dark:text-gray-300 border-b border-gray-200 dark:border-gray-700">
                          Value
                        </th>
                        <th className="px-4 py-3 text-left font-medium text-gray-700 dark:text-gray-300 border-b border-gray-200 dark:border-gray-700 w-24">
                          Type
                        </th>
                        <th className="px-4 py-3 text-center font-medium text-gray-700 dark:text-gray-300 border-b border-gray-200 dark:border-gray-700 w-16"></th>
                      </tr>
                    </thead>
                    <tbody>
                      {requestBody.fields.map((item, index) => (
                        <tr
                          key={index}
                          className="border-b border-gray-100 dark:border-gray-800"
                        >
                          <td className="px-4 py-3">
                            <input
                              type="text"
                              value={item.key}
                              onChange={(e) => {
                                const updated = [...requestBody.fields!];
                                updated[index] = {
                                  ...updated[index],
                                  key: e.target.value,
                                };
                                setRequestBody({
                                  ...requestBody,
                                  fields: updated,
                                });
                              }}
                              placeholder="예: username, password"
                              className="w-full px-2 py-1.5 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB]"
                            />
                          </td>
                          <td className="px-4 py-3">
                            <FakerProviderSelect
                              value={item.value}
                              onChange={(newVal) => {
                                const updated = [...requestBody.fields!];
                                updated[index] = {
                                  ...updated[index],
                                  value: newVal,
                                };
                                setRequestBody({
                                  ...requestBody,
                                  fields: updated,
                                });
                              }}
                              placeholder="DataFaker 값만 선택 (예: $internet.email)"
                              disabled={isReadOnly}
                            />
                          </td>
                          <td className="px-4 py-3">
                            <select
                              value={item.type || "text"}
                              onChange={(e) => {
                                const updated = [...requestBody.fields!];
                                updated[index] = {
                                  ...updated[index],
                                  type: e.target.value,
                                };
                                setRequestBody({
                                  ...requestBody,
                                  fields: updated,
                                });
                              }}
                              className="w-full px-2 py-1.5 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB]"
                            >
                              {fieldTypes.map((type) => (
                                <option key={type} value={type}>
                                  {type}
                                </option>
                              ))}
                            </select>
                          </td>
                          <td className="px-4 py-3 text-center">
                            <button
                              onClick={() => {
                                const updated = requestBody.fields!.filter(
                                  (_, i) => i !== index
                                );
                                setRequestBody({
                                  ...requestBody,
                                  fields: updated,
                                });
                              }}
                              className="text-red-500 hover:text-red-600"
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
                      ))}
                    </tbody>
                  </table>
                </div>
                {requestBody.fields.length === 0 && (
                  <div className="text-center py-8 text-gray-500 dark:text-gray-400">
                    <p>No fields yet. Click "Add Field" to add one.</p>
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {activeTab === "params" && (
          <div className="text-center py-8 text-gray-500 dark:text-gray-400">
            <p>Query Parameters 기능은 준비 중입니다.</p>
          </div>
        )}

        {activeTab === "auth" && (
          <div className="text-center py-8 text-gray-500 dark:text-gray-400">
            <p>인증 설정 기능은 준비 중입니다.</p>
          </div>
        )}
      </div>

      {/* Send Button removed: Spec 작성 화면에서는 실제 전송 기능을 제공하지 않음 */}
    </div>
  );
}
