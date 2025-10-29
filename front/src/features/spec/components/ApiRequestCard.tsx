import { useState } from "react";

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
    <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-2xl p-6 shadow-sm">
      {/* Header */}
      <div className="flex items-center gap-3 mb-4">
        <div className="w-10 h-10 rounded-lg bg-blue-100 dark:bg-blue-900 flex items-center justify-center">
          <svg
            className="w-6 h-6 text-blue-600 dark:text-blue-400"
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
        </div>
        <div>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
            Request
          </h3>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            요청 파라미터 및 헤더 설정
          </p>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 border-b border-gray-200 dark:border-gray-700 mb-4">
        {["params", "headers", "body", "auth"].map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`px-4 py-2 text-sm font-medium transition-colors capitalize ${
              activeTab === tab
                ? "text-blue-600 border-b-2 border-blue-600 dark:text-blue-400"
                : "text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300"
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
              <p className="text-sm text-gray-600 dark:text-gray-400">
                요청 헤더 설정
              </p>
              <button
                onClick={addHeader}
                disabled={isReadOnly}
                className={`px-3 py-1 text-sm font-medium ${
                  isReadOnly
                    ? "text-gray-400 cursor-not-allowed"
                    : "text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300"
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
                    placeholder="Key"
                    className="flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                  <input
                    type="text"
                    value={header.value}
                    onChange={(e) =>
                      updateHeader(index, "value", e.target.value)
                    }
                    placeholder="Value"
                    className="flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                  <button
                    onClick={() => removeHeader(index)}
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
                </div>
              ))}
            </div>
          </div>
        )}

        {activeTab === "body" && (
          <div>
            <div className="flex gap-2 mb-3 border-b border-gray-200 dark:border-gray-700">
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
                  className={`px-4 py-2 text-sm font-medium transition-colors ${
                    requestBody.type === type
                      ? "text-blue-600 border-b-2 border-blue-600 dark:text-blue-400"
                      : "text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300"
                  }`}
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
                    className="px-3 py-1 text-sm text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300 font-medium"
                  >
                    + Add Field
                  </button>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full text-sm border border-gray-200 dark:border-gray-700 rounded-lg">
                    <thead>
                      <tr className="bg-gray-50 dark:bg-gray-750">
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
                              placeholder="Field name"
                              className="w-full px-2 py-1.5 border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
                            />
                          </td>
                          <td className="px-4 py-3">
                            <input
                              type="text"
                              value={item.value}
                              onChange={(e) => {
                                const updated = [...requestBody.fields!];
                                updated[index] = {
                                  ...updated[index],
                                  value: e.target.value,
                                };
                                setRequestBody({
                                  ...requestBody,
                                  fields: updated,
                                });
                              }}
                              placeholder="Field value"
                              className="w-full px-2 py-1.5 border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
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
                              className="w-full px-2 py-1.5 border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
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

      {/* Send Button */}
      <div className="mt-6 pt-4 border-t border-gray-200 dark:border-gray-700">
        <button className="w-full bg-blue-600 hover:bg-blue-700 dark:bg-blue-700 dark:hover:bg-blue-600 text-white font-medium py-3 px-4 rounded-lg transition-colors flex items-center justify-center gap-2">
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
              d="M14 5l7 7m0 0l-7 7m7-7H3"
            />
          </svg>
          Send Request
        </button>
      </div>
    </div>
  );
}
