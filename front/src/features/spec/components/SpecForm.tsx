import { useState } from "react";
import { JsonEditor } from "@/components/JsonEditor";

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
  protocol: "REST" | "WebSocket";
}

export function SpecForm({ protocol }: SpecFormProps) {
  const [method, setMethod] = useState("POST");
  const [url, setUrl] = useState("");
  const [tags, setTags] = useState("");
  const [description, setDescription] = useState("");
  const [isAuthenticated, setIsAuthenticated] = useState(true);
  const [owner, setOwner] = useState("");
  const [requestHeaders, setRequestHeaders] = useState<KeyValuePair[]>([
    { key: "Content-Type", value: "application/json" },
  ]);
  const [requestBody, setRequestBody] = useState<RequestBody>({
    type: "raw",
    content: "",
  });
  const [responseHeaders, setResponseHeaders] = useState<KeyValuePair[]>([
    { key: "Content-Type", value: "application/json" },
  ]);
  const [responseBody, setResponseBody] = useState("json");
  const [responseContent, setResponseContent] = useState("");
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
    <div className="space-y-8">
      {/* Header Section */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center">
            <span className="text-white text-xl font-bold">
              {protocol.charAt(0)}
            </span>
          </div>
          <div>
            <h2 className="text-3xl font-bold text-gray-900 dark:text-white">
              API 명세서 작성
            </h2>
            <p className="text-gray-600 dark:text-gray-400 mt-1">
              REST API 엔드포인트를 정의하고 문서화하세요
            </p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <span className="px-4 py-2 bg-gradient-to-r from-blue-500 to-purple-600 text-white text-sm font-semibold rounded-full shadow-lg">
            {protocol}
          </span>
          <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
        </div>
      </div>

      {/* Method + URL Section */}
      <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-2xl p-6 shadow-sm hover:shadow-md transition-shadow duration-200">
        <div className="flex items-center gap-3 mb-6">
          <div className="w-10 h-10 rounded-lg bg-blue-100 dark:bg-blue-900 flex items-center justify-center">
            <span className="text-blue-600 dark:text-blue-400 text-lg">🌐</span>
          </div>
          <div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
              Step 1: HTTP Method & URL
            </h3>
            <p className="text-sm text-gray-500 dark:text-gray-400">
              API 엔드포인트의 기본 정보를 설정하세요
            </p>
          </div>
        </div>

        <div className="space-y-4">
          <div className="flex flex-col sm:flex-row gap-4">
            <div className="relative sm:w-auto w-full">
              <select
                value={method}
                onChange={(e) => setMethod(e.target.value)}
                className="appearance-none w-full sm:w-auto px-4 py-3 pr-10 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 font-medium"
              >
                {methods.map((m) => (
                  <option key={m} value={m}>
                    {m}
                  </option>
                ))}
              </select>
              <div className="absolute inset-y-0 right-0 flex items-center pr-3 pointer-events-none">
                <svg
                  className="w-5 h-5 text-gray-400"
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
              </div>
            </div>
            <input
              type="text"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder="예: /api/users, /api/auth/login"
              className="flex-1 px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 font-mono"
            />
          </div>

          {/* Method Badge */}
          <div className="flex items-center gap-2">
            <span className="text-sm text-gray-500 dark:text-gray-400">
              Method:
            </span>
            <span
              className={`px-3 py-1 rounded-lg text-xs font-semibold ${
                method === "GET"
                  ? "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200"
                  : method === "POST"
                  ? "bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200"
                  : method === "PUT"
                  ? "bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200"
                  : method === "PATCH"
                  ? "bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200"
                  : "bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200"
              }`}
            >
              {method}
            </span>
          </div>
        </div>
      </div>

      {/* Metadata Section */}
      <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-2xl p-6 shadow-sm hover:shadow-md transition-shadow duration-200">
        <div className="flex items-center gap-3 mb-6">
          <div className="w-10 h-10 rounded-lg bg-purple-100 dark:bg-purple-900 flex items-center justify-center">
            <span className="text-purple-600 dark:text-purple-400 text-lg">
              📋
            </span>
          </div>
          <div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
              Step 2: API 메타데이터
            </h3>
            <p className="text-sm text-gray-500 dark:text-gray-400">
              API의 분류, 설명, 인증 정보를 설정하세요
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">
                Tags/Category
              </label>
              <input
                type="text"
                value={tags}
                onChange={(e) => setTags(e.target.value)}
                placeholder="예: AUTH, USER, PRODUCT, ORDER"
                className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200"
              />
              <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                쉼표로 구분하여 여러 태그를 입력할 수 있습니다
              </p>
            </div>

            <div>
              <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">
                Description
              </label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={3}
                placeholder="예: 사용자 로그인, 상품 목록 조회, 주문 생성"
                className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 resize-none"
              />
            </div>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">
                Owner
              </label>
              <input
                type="text"
                value={owner}
                onChange={(e) => setOwner(e.target.value)}
                placeholder="예: SMART-TEAM, 김개발, 백엔드팀"
                className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200"
              />
            </div>

            <div className="bg-gray-50 dark:bg-gray-700 rounded-xl p-4">
              <label className="flex items-center gap-3 cursor-pointer">
                <div className="relative">
                  <input
                    type="checkbox"
                    checked={isAuthenticated}
                    onChange={(e) => setIsAuthenticated(e.target.checked)}
                    className="sr-only"
                  />
                  <div
                    className={`w-6 h-6 rounded-lg border-2 transition-all duration-200 ${
                      isAuthenticated
                        ? "bg-blue-500 border-blue-500"
                        : "border-gray-300 dark:border-gray-600"
                    }`}
                  >
                    {isAuthenticated && (
                      <svg
                        className="w-4 h-4 text-white m-0.5"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path
                          fillRule="evenodd"
                          d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                          clipRule="evenodd"
                        />
                      </svg>
                    )}
                  </div>
                </div>
                <div>
                  <span className="text-sm font-semibold text-gray-700 dark:text-gray-300">
                    인증 필요
                  </span>
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    이 API는 인증이 필요한지 여부를 설정합니다
                  </p>
                </div>
              </label>
            </div>
          </div>
        </div>
      </div>

      {/* Request Header */}
      <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-2xl p-6 shadow-sm hover:shadow-md transition-shadow duration-200">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-orange-100 dark:bg-orange-900 flex items-center justify-center">
              <span className="text-orange-600 dark:text-orange-400 text-lg">
                📤
              </span>
            </div>
            <div>
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                Step 3: Request Headers
              </h3>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                HTTP 요청 헤더를 설정하세요
              </p>
            </div>
          </div>
          <button
            onClick={() => addHeader(requestHeaders, setRequestHeaders)}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 dark:bg-blue-700 dark:hover:bg-blue-600 text-white rounded-xl transition-colors font-medium flex items-center gap-2 shadow-sm hover:shadow-md"
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
                d="M12 6v6m0 0v6m0-6h6m-6 0H6"
              />
            </svg>
            Add Header
          </button>
        </div>

        <div className="space-y-3">
          {requestHeaders.length === 0 ? (
            <div className="text-center py-8 text-gray-500 dark:text-gray-400">
              <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-gray-100 dark:bg-gray-700 flex items-center justify-center">
                <svg
                  className="w-8 h-8"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M12 6v6m0 0v6m0-6h6m-6 0H6"
                  />
                </svg>
              </div>
              <p className="text-sm">
                아직 헤더가 없습니다. Add Header 버튼을 클릭하여 추가하세요.
              </p>
            </div>
          ) : (
            requestHeaders.map((header, index) => (
              <div
                key={index}
                className="flex gap-3 p-4 bg-gray-50 dark:bg-gray-700 rounded-xl border border-gray-200 dark:border-gray-600"
              >
                <div className="flex-1">
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
                    placeholder="Header Name (e.g., Content-Type)"
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 font-medium"
                  />
                </div>
                <div className="flex-1">
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
                    placeholder="Header Value (e.g., application/json)"
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200"
                  />
                </div>
                <button
                  onClick={() =>
                    removeHeader(index, requestHeaders, setRequestHeaders)
                  }
                  className="p-2 text-red-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900 rounded-lg transition-colors"
                  title="헤더 삭제"
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
            ))
          )}
        </div>
      </div>

      {/* Request Body */}
      <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-2xl p-6 shadow-sm hover:shadow-md transition-shadow duration-200">
        <div className="flex items-center gap-3 mb-6">
          <div className="w-10 h-10 rounded-lg bg-green-100 dark:bg-green-900 flex items-center justify-center">
            <span className="text-green-600 dark:text-green-400 text-lg">
              📦
            </span>
          </div>
          <div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
              Step 4: Request Body
            </h3>
            <p className="text-sm text-gray-500 dark:text-gray-400">
              요청 본문의 타입과 내용을 설정하세요
            </p>
          </div>
        </div>

        <div className="space-y-4">
          {/* Body Type Tabs */}
          <div className="flex gap-2 p-1 bg-gray-100 dark:bg-gray-700 rounded-xl">
            {bodyTypes.map((type) => (
              <button
                key={type}
                onClick={() =>
                  setRequestBody({
                    ...requestBody,
                    type: type as "none" | "raw" | "form-data" | "param",
                  })
                }
                className={`flex-1 px-4 py-2 text-sm font-medium rounded-lg transition-all duration-200 ${
                  requestBody.type === type
                    ? "bg-white dark:bg-gray-800 text-blue-600 dark:text-blue-400 shadow-sm"
                    : "text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600"
                }`}
              >
                {type === "none"
                  ? "None"
                  : type === "raw"
                  ? "Raw JSON"
                  : type === "form-data"
                  ? "Form Data"
                  : "URL Params"}
              </button>
            ))}
          </div>

          {/* Body Content */}
          {requestBody.type !== "none" && (
            <div className="relative">
              <div className="flex items-center justify-between mb-2">
                <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                  {requestBody.type === "raw"
                    ? "JSON Body"
                    : requestBody.type === "form-data"
                    ? "Form Data"
                    : "URL Parameters"}
                </span>
                <span className="text-xs text-gray-500 dark:text-gray-400">
                  {requestBody.type === "raw"
                    ? "application/json"
                    : "multipart/form-data"}
                </span>
              </div>
              {requestBody.type === "raw" ? (
                <JsonEditor
                  value={requestBody.content}
                  onChange={(value) =>
                    setRequestBody({ ...requestBody, content: value })
                  }
                  placeholder='{\n  "key": "value",\n  "nested": {\n    "property": "value"\n  }\n}'
                  height="300px"
                />
              ) : (
                <textarea
                  value={requestBody.content}
                  onChange={(e) =>
                    setRequestBody({ ...requestBody, content: e.target.value })
                  }
                  rows={8}
                  placeholder={
                    requestBody.type === "form-data"
                      ? "key1=value1\nkey2=value2"
                      : "param1=value1&param2=value2"
                  }
                  className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-xl bg-gray-900 text-green-400 font-mono text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 resize-none"
                />
              )}
              {requestBody.type === "raw" && (
                <div className="absolute top-2 right-2 flex gap-2">
                  <button
                    onClick={() => {
                      try {
                        const formatted = JSON.stringify(
                          JSON.parse(requestBody.content),
                          null,
                          2
                        );
                        setRequestBody({ ...requestBody, content: formatted });
                      } catch {
                        // JSON이 아닌 경우 무시
                      }
                    }}
                  className="px-2 py-1 text-xs bg-gray-700 hover:bg-gray-600 text-gray-300 rounded transition-colors"
                  title="JSON 포맷팅"
                >
                  Format
                </button>
                <button
                  onClick={() =>
                    setRequestBody({ ...requestBody, content: "" })
                  }
                  className="px-2 py-1 text-xs bg-gray-700 hover:bg-gray-600 text-gray-300 rounded transition-colors"
                  title="내용 지우기"
                >
                  Clear
                </button>
              </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Response Header */}
      <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-2xl p-6 shadow-sm hover:shadow-md transition-shadow duration-200">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-indigo-100 dark:bg-indigo-900 flex items-center justify-center">
              <span className="text-indigo-600 dark:text-indigo-400 text-lg">
                📥
              </span>
            </div>
            <div>
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                Step 5: Response Headers
              </h3>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                HTTP 응답 헤더를 설정하세요
              </p>
            </div>
          </div>
          <button
            onClick={() => addHeader(responseHeaders, setResponseHeaders)}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 dark:bg-blue-700 dark:hover:bg-blue-600 text-white rounded-xl transition-colors font-medium flex items-center gap-2 shadow-sm hover:shadow-md"
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
                d="M12 6v6m0 0v6m0-6h6m-6 0H6"
              />
            </svg>
            Add Header
          </button>
        </div>

        <div className="space-y-3">
          {responseHeaders.length === 0 ? (
            <div className="text-center py-8 text-gray-500 dark:text-gray-400">
              <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-gray-100 dark:bg-gray-700 flex items-center justify-center">
                <svg
                  className="w-8 h-8"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M12 6v6m0 0v6m0-6h6m-6 0H6"
                  />
                </svg>
              </div>
              <p className="text-sm">
                아직 응답 헤더가 없습니다. Add Header 버튼을 클릭하여
                추가하세요.
              </p>
            </div>
          ) : (
            responseHeaders.map((header, index) => (
              <div
                key={index}
                className="flex gap-3 p-4 bg-gray-50 dark:bg-gray-700 rounded-xl border border-gray-200 dark:border-gray-600"
              >
                <div className="flex-1">
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
                    placeholder="Header Name (e.g., Content-Type)"
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 font-medium"
                  />
                </div>
                <div className="flex-1">
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
                    placeholder="Header Value (e.g., application/json)"
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200"
                  />
                </div>
                <button
                  onClick={() =>
                    removeHeader(index, responseHeaders, setResponseHeaders)
                  }
                  className="p-2 text-red-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900 rounded-lg transition-colors"
                  title="헤더 삭제"
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
            ))
          )}
        </div>
      </div>

      {/* Response Body */}
      <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-2xl p-6 shadow-sm hover:shadow-md transition-shadow duration-200">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-teal-100 dark:bg-teal-900 flex items-center justify-center">
              <span className="text-teal-600 dark:text-teal-400 text-lg">
                📄
              </span>
            </div>
            <div>
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                Step 6: Response Body
              </h3>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                응답 본문의 형식과 내용을 설정하세요
              </p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <select
              value={responseBody}
              onChange={(e) => setResponseBody(e.target.value)}
              className="px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200"
            >
              {responseTypes.map((type) => (
                <option key={type} value={type}>
                  {type.toUpperCase()}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="relative">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
              Response Content ({responseBody.toUpperCase()})
            </span>
            <div className="flex gap-2">
              <button
                onClick={() => {
                  try {
                    const formatted = JSON.stringify(
                      JSON.parse(responseContent),
                      null,
                      2
                    );
                    setResponseContent(formatted);
                  } catch {
                    // JSON이 아닌 경우 무시
                  }
                }}
                className="px-2 py-1 text-xs bg-gray-700 hover:bg-gray-600 text-gray-300 rounded transition-colors"
                title="JSON 포맷팅"
              >
                Format
              </button>
              <button
                onClick={() => setResponseContent("")}
                className="px-2 py-1 text-xs bg-gray-700 hover:bg-gray-600 text-gray-300 rounded transition-colors"
                title="내용 지우기"
              >
                Clear
              </button>
            </div>
          </div>
          <textarea
            value={responseContent}
            onChange={(e) => setResponseContent(e.target.value)}
            rows={10}
            placeholder={
              responseBody === "json"
                ? '{\n  "success": true,\n  "data": {\n    "id": 1,\n    "name": "example"\n  }\n}'
                : responseBody === "xml"
                ? '<?xml version="1.0" encoding="UTF-8"?>\n<response>\n  <success>true</success>\n  <data>\n    <id>1</id>\n    <name>example</name>\n  </data>\n</response>'
                : "Response content here..."
            }
            className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-xl bg-gray-900 text-green-400 font-mono text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 resize-none"
          />
        </div>
      </div>

      {/* Response Codes */}
      <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-2xl p-6 shadow-sm hover:shadow-md transition-shadow duration-200">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-red-100 dark:bg-red-900 flex items-center justify-center">
              <span className="text-red-600 dark:text-red-400 text-lg">🔢</span>
            </div>
            <div>
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                Step 7: Response Status Codes
              </h3>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                가능한 HTTP 상태 코드와 설명을 추가하세요
              </p>
            </div>
          </div>
          <div className="flex gap-3">
            {/* 템플릿 선택 드롭다운 */}
            <div className="relative">
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
                className="appearance-none px-4 py-2 pr-10 text-sm border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200"
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
              <div className="absolute inset-y-0 right-0 flex items-center pr-3 pointer-events-none">
                <svg
                  className="w-4 h-4 text-gray-400"
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
              </div>
            </div>
            <button
              onClick={() => addStatusCode()}
              className="px-4 py-2 bg-blue-600 hover:bg-blue-700 dark:bg-blue-700 dark:hover:bg-blue-600 text-white rounded-xl transition-colors font-medium flex items-center gap-2 shadow-sm hover:shadow-md"
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
                  d="M12 6v6m0 0v6m0-6h6m-6 0H6"
                />
              </svg>
              Add Custom
            </button>
          </div>
        </div>

        {/* Status Codes Table */}
        <div className="overflow-hidden rounded-xl border border-gray-200 dark:border-gray-600">
          {statusCodes.length === 0 ? (
            <div className="text-center py-12 text-gray-500 dark:text-gray-400">
              <div className="w-20 h-20 mx-auto mb-4 rounded-full bg-gray-100 dark:bg-gray-700 flex items-center justify-center">
                <svg
                  className="w-10 h-10"
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
              </div>
              <p className="text-sm mb-2">아직 상태 코드가 없습니다.</p>
              <p className="text-xs">
                템플릿을 선택하거나 커스텀 코드를 추가하세요.
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-gray-50 dark:bg-gray-700">
                  <tr>
                    <th className="px-6 py-4 text-left font-semibold text-gray-700 dark:text-gray-300">
                      Status Code
                    </th>
                    <th className="px-6 py-4 text-left font-semibold text-gray-700 dark:text-gray-300">
                      Type
                    </th>
                    <th className="px-6 py-4 text-left font-semibold text-gray-700 dark:text-gray-300">
                      Description
                    </th>
                    <th className="px-6 py-4 text-center font-semibold text-gray-700 dark:text-gray-300">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200 dark:divide-gray-600">
                  {statusCodes.map((statusCode, index) => (
                    <tr
                      key={index}
                      className="hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
                    >
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-2">
                          <span
                            className={`px-2 py-1 rounded-lg text-xs font-semibold ${
                              statusCode.type === "Success"
                                ? "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200"
                                : "bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200"
                            }`}
                          >
                            {statusCode.code || "---"}
                          </span>
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <select
                          value={statusCode.type}
                          onChange={(e) =>
                            updateStatusCode(index, "type", e.target.value)
                          }
                          className="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200"
                        >
                          <option value="Success">Success</option>
                          <option value="Error">Error</option>
                        </select>
                      </td>
                      <td className="px-6 py-4">
                        <input
                          type="text"
                          value={statusCode.message}
                          onChange={(e) =>
                            updateStatusCode(index, "message", e.target.value)
                          }
                          placeholder="상태 코드 설명을 입력하세요"
                          className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200"
                        />
                      </td>
                      <td className="px-6 py-4 text-center">
                        <button
                          onClick={() => removeStatusCode(index)}
                          className="p-2 text-red-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900 rounded-lg transition-colors"
                          title="상태 코드 삭제"
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
          )}
        </div>
      </div>
    </div>
  );
}
