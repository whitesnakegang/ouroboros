import { useState } from "react";

interface KeyValuePair {
  key: string;
  value: string;
}

interface RequestBody {
  type: "none" | "raw" | "form-data" | "param";
  content: string;
}

interface StatusCode {
  code: string;
  type: "Success" | "Error";
  message: string;
}

interface SpecFormProps {
  protocol: "REST" | "GraphQL" | "WebSocket";
}

export function SpecForm({ protocol }: SpecFormProps) {
  const [method, setMethod] = useState("POST");
  const [url, setUrl] = useState("/api/auth/login");
  const [tags, setTags] = useState("");
  const [description, setDescription] = useState("사용자 로그인");
  const [isAuthenticated, setIsAuthenticated] = useState(true);
  const [owner, setOwner] = useState("SMART-TEAM");
  const [requestHeaders, setRequestHeaders] = useState<KeyValuePair[]>([
    { key: "Content-Type", value: "application/json" },
  ]);
  const [requestBody, setRequestBody] = useState<RequestBody>({
    type: "raw",
    content: '{\n  "email": "string",\n  "password": "string"\n}',
  });
  const [responseHeaders, setResponseHeaders] = useState<KeyValuePair[]>([
    { key: "Content-Type", value: "application/json" },
  ]);
  const [responseBody, setResponseBody] = useState("json");
  const [responseContent, setResponseContent] = useState(
    '{\n  "token": "string",\n  "user": {\n    "id": "string",\n    "email": "string",\n    "name": "string"\n  }\n}'
  );
  const [statusCodes, setStatusCodes] = useState<StatusCode[]>([]);

  // 기본 상태 코드 템플릿
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

  const methods = ["GET", "POST", "PUT", "PATCH", "DELETE"];
  const bodyTypes = ["none", "raw", "form-data", "param"];
  const responseTypes = ["json", "xml", "text", "html"];

  const addHeader = (
    headers: KeyValuePair[],
    setHeaders: (headers: KeyValuePair[]) => void
  ) => {
    setHeaders([...headers, { key: "", value: "" }]);
  };

  const removeHeader = (
    index: number,
    headers: KeyValuePair[],
    setHeaders: (headers: KeyValuePair[]) => void
  ) => {
    setHeaders(headers.filter((_, i) => i !== index));
  };

  const updateHeader = (
    index: number,
    field: "key" | "value",
    value: string,
    headers: KeyValuePair[],
    setHeaders: (headers: KeyValuePair[]) => void
  ) => {
    const updated = [...headers];
    updated[index] = { ...updated[index], [field]: value };
    setHeaders(updated);
  };

  const addStatusCode = (template?: StatusCode) => {
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
    setStatusCodes(statusCodes.filter((_, i) => i !== index));
  };

  const updateStatusCode = (
    index: number,
    field: "code" | "type" | "message",
    value: string
  ) => {
    const updated = [...statusCodes];
    updated[index] = { ...updated[index], [field]: value };
    setStatusCodes(updated);
  };

  if (protocol !== "REST") {
    return (
      <div className="p-6 text-center text-gray-500 dark:text-gray-400">
        {protocol} 명세서는 준비 중입니다.
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* STEP 1: Protocol Badge */}
      <div className="flex items-center gap-3">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">
          명세서 편집
        </h2>
        <span className="px-3 py-1 bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-200 text-xs font-medium rounded-full">
          {protocol}
        </span>
      </div>

      {/* STEP 2: Method + URL */}
      <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-4">
        <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
          STEP 2: Method + URL
        </h3>
        <div className="flex gap-3">
          <select
            value={method}
            onChange={(e) => setMethod(e.target.value)}
            className="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            {methods.map((m) => (
              <option key={m} value={m}>
                {m}
              </option>
            ))}
          </select>
          <input
            type="text"
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            placeholder="/api/endpoint"
            className="flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
      </div>

      {/* STEP 3: Tags, Description, Auth */}
      <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-4">
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Tags/Category
            </label>
            <input
              type="text"
              value={tags}
              onChange={(e) => setTags(e.target.value)}
              placeholder="AUTH, USER, etc."
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Description
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={2}
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
            />
          </div>
          <div className="flex items-center gap-4">
            <label className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={isAuthenticated}
                onChange={(e) => setIsAuthenticated(e.target.checked)}
                className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
              />
              <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                isAuthenticated
              </span>
            </label>
            <input
              type="text"
              value={owner}
              onChange={(e) => setOwner(e.target.value)}
              placeholder="Owner (optional)"
              className="flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        </div>
      </div>

      {/* Request Header */}
      <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-4">
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300">
            Request Header
          </h3>
          <button
            onClick={() => addHeader(requestHeaders, setRequestHeaders)}
            className="px-3 py-1 text-sm text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300 font-medium"
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
                onChange={(e) =>
                  updateHeader(
                    index,
                    "key",
                    e.target.value,
                    requestHeaders,
                    setRequestHeaders
                  )
                }
                placeholder="Key"
                className="flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <input
                type="text"
                value={header.value}
                onChange={(e) =>
                  updateHeader(
                    index,
                    "value",
                    e.target.value,
                    requestHeaders,
                    setRequestHeaders
                  )
                }
                placeholder="Value"
                className="flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <button
                onClick={() =>
                  removeHeader(index, requestHeaders, setRequestHeaders)
                }
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

      {/* Request Body */}
      <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-4">
        <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
          Request Body
        </h3>
        <div className="flex gap-2 mb-3 border-b border-gray-200 dark:border-gray-700">
          {bodyTypes.map((type) => (
            <button
              key={type}
              onClick={() =>
                setRequestBody({
                  ...requestBody,
                  type: type as "none" | "raw" | "form-data" | "param",
                })
              }
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
        {requestBody.type !== "none" && (
          <textarea
            value={requestBody.content}
            onChange={(e) =>
              setRequestBody({ ...requestBody, content: e.target.value })
            }
            rows={8}
            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-gray-900 text-green-400 font-mono text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
          />
        )}
      </div>

      {/* Response Header */}
      <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-4">
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300">
            Response Header
          </h3>
          <button
            onClick={() => addHeader(responseHeaders, setResponseHeaders)}
            className="px-3 py-1 text-sm text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300 font-medium"
          >
            + Add
          </button>
        </div>
        <div className="space-y-2">
          {responseHeaders.map((header, index) => (
            <div key={index} className="flex gap-2">
              <input
                type="text"
                value={header.key}
                onChange={(e) =>
                  updateHeader(
                    index,
                    "key",
                    e.target.value,
                    responseHeaders,
                    setResponseHeaders
                  )
                }
                placeholder="Key"
                className="flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <input
                type="text"
                value={header.value}
                onChange={(e) =>
                  updateHeader(
                    index,
                    "value",
                    e.target.value,
                    responseHeaders,
                    setResponseHeaders
                  )
                }
                placeholder="Value"
                className="flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <button
                onClick={() =>
                  removeHeader(index, responseHeaders, setResponseHeaders)
                }
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

      {/* Response Body */}
      <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-4">
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300">
            Response Body
          </h3>
          <select
            value={responseBody}
            onChange={(e) => setResponseBody(e.target.value)}
            className="px-3 py-1 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            {responseTypes.map((type) => (
              <option key={type} value={type}>
                {type.toUpperCase()}
              </option>
            ))}
          </select>
        </div>
        <textarea
          value={responseContent}
          onChange={(e) => setResponseContent(e.target.value)}
          rows={10}
          className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-gray-900 text-green-400 font-mono text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
        />
      </div>

      {/* Response Codes */}
      <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-4">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300">
            Response Codes
          </h3>
          <div className="flex gap-2">
            {/* 템플릿 선택 드롭다운 */}
            <select
              onChange={(e) => {
                const selectedCode = e.target.value;
                if (selectedCode) {
                  const template = statusCodeTemplates.find(
                    (t) => t.code === selectedCode
                  );
                  if (template) {
                    addStatusCode(template);
                    e.target.value = ""; // 선택 초기화
                  }
                }
              }}
              className="px-3 py-1 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
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
              className="px-3 py-1 text-sm text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300 font-medium border border-blue-600 dark:border-blue-400 rounded-lg hover:bg-blue-50 dark:hover:bg-blue-900"
            >
              + Add Custom
            </button>
          </div>
        </div>

        {/* Table */}
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
                <th className="px-4 py-3"></th>
              </tr>
            </thead>
            <tbody>
              {statusCodes.map((statusCode, index) => (
                <tr
                  key={index}
                  className="border-b border-gray-100 dark:border-gray-800 hover:bg-gray-50 dark:hover:bg-gray-750"
                >
                  <td className="px-4 py-3">
                    <input
                      type="text"
                      value={statusCode.code}
                      onChange={(e) =>
                        updateStatusCode(index, "code", e.target.value)
                      }
                      placeholder="200"
                      className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                  </td>
                  <td className="px-4 py-3">
                    <select
                      value={statusCode.type}
                      onChange={(e) =>
                        updateStatusCode(index, "type", e.target.value)
                      }
                      className="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
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
                      placeholder="메시지 또는 설명"
                      className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
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
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
