import { useState, useEffect } from "react";
import { ApiRequestCard } from "./ApiRequestCard";
import { ApiResponseCard } from "./ApiResponseCard";
import { ApiPreviewCard } from "./ApiPreviewCard";
import { ProtocolTabs } from "./ProtocolTabs";
import { CodeSnippetPanel } from "./CodeSnippetPanel";
import { useSpecStore } from "../store/spec.store";
import { useSidebarStore } from "@/features/sidebar/store/sidebar.store";

interface KeyValuePair {
  key: string;
  value: string;
}

// Import RequestBody type from ApiRequestCard
type RequestBody = {
  type: "none" | "form-data" | "x-www-form-urlencoded" | "json" | "xml";
  contentType: string;
  fields: Array<{
    key: string;
    value: string;
    type: string;
    description?: string;
    required?: boolean;
  }>;
};

interface StatusCode {
  code: string;
  type: "Success" | "Error";
  message: string;
}

export function ApiEditorLayout() {
  const { protocol, setProtocol } = useSpecStore();
  const {
    selectedEndpoint,
    updateEndpoint,
    deleteEndpoint,
    addEndpoint,
    setSelectedEndpoint,
    triggerNewForm,
    setTriggerNewForm,
  } = useSidebarStore();
  const [activeTab, setActiveTab] = useState<"form" | "test">("form");
  const [isCodeSnippetOpen, setIsCodeSnippetOpen] = useState(false);

  // 사이드바 Add 버튼 클릭 시 새 폼 초기화
  useEffect(() => {
    if (triggerNewForm) {
      handleNewForm();
      setTriggerNewForm(false);
    }
  }, [triggerNewForm]);

  // Load selected endpoint data when endpoint is clicked
  useEffect(() => {
    if (selectedEndpoint) {
      setMethod(selectedEndpoint.method);
      setUrl(selectedEndpoint.path);
      setDescription(selectedEndpoint.description);
    }
  }, [selectedEndpoint]);

  // Form state
  const [method, setMethod] = useState("POST");
  const [url, setUrl] = useState("/api/auth/login");
  const [tags, setTags] = useState("AUTH");
  const [description, setDescription] = useState("사용자 로그인");
  const [owner, setOwner] = useState("SMART-TEAM");

  // Request state
  const [requestHeaders, setRequestHeaders] = useState<KeyValuePair[]>([
    { key: "Content-Type", value: "application/json" },
  ]);
  const [requestBody, setRequestBody] = useState<RequestBody>({
    type: "json",
    contentType: "application/json",
    fields: [{ key: "email", value: "string", type: "string" }],
  });

  // Response state
  const [statusCodes, setStatusCodes] = useState<StatusCode[]>([]);

  const methods = ["GET", "POST", "PUT", "PATCH", "DELETE"];

  // Mock API endpoints data for progress calculation
  const totalEndpoints = 6;
  const completedEndpoints = 3;
  const progressPercentage = Math.round(
    (completedEndpoints / totalEndpoints) * 100
  );

  const handleSave = () => {
    if (!method || !url) {
      alert("Method와 URL을 입력해주세요.");
      return;
    }
    if (selectedEndpoint) {
      // 기존 엔드포인트 수정
      const updatedEndpoint = {
        ...selectedEndpoint,
        method,
        path: url,
        description,
      };
      updateEndpoint(updatedEndpoint);
      alert(`${method} ${url} API가 수정되었습니다.`);
      setSelectedEndpoint(updatedEndpoint);
    } else {
      // 새 엔드포인트 생성
      const newEndpoint = {
        id: Date.now(),
        method,
        path: url,
        description,
        implementationStatus: "not-implemented" as const,
      };

      addEndpoint(newEndpoint, tags);
      alert(`${method} ${url} API가 생성되었습니다.`);

      // 선택 상태로 만들기
      setSelectedEndpoint(newEndpoint);
    }
  };

  const handleDelete = () => {
    if (!selectedEndpoint) return;

    if (confirm("이 엔드포인트를 삭제하시겠습니까?")) {
      deleteEndpoint(selectedEndpoint.id);
      alert("엔드포인트가 삭제되었습니다.");

      // 폼 초기화
      setMethod("POST");
      setUrl("/api/auth/login");
      setTags("AUTH");
      setDescription("사용자 로그인");
      setOwner("SMART-TEAM");
    }
  };

  const handleReset = () => {
    if (confirm("작성 중인 내용을 초기화하시겠습니까?")) {
      setMethod("POST");
      setUrl("/api/auth/login");
      setTags("AUTH");
      setDescription("사용자 로그인");
      setOwner("SMART-TEAM");
      setRequestBody({
        type: "json",
        contentType: "application/json",
        fields: [{ key: "email", value: "string", type: "string" }],
      });
      setStatusCodes([]);
    }
  };

  const handleNewForm = () => {
    // 새 작성 폼으로 전환
    setSelectedEndpoint(null);
    setMethod("POST");
    setUrl("/api/auth/login");
    setTags("AUTH");
    setDescription("사용자 로그인");
    setOwner("SMART-TEAM");
    setRequestBody({
      type: "json",
      contentType: "application/json",
      fields: [{ key: "email", value: "string", type: "string" }],
    });
    setStatusCodes([]);
  };

  return (
    <div className="h-full flex flex-col bg-gray-50 dark:bg-gray-900">
      {/* Header Tabs */}
      <div className="border-b border-gray-200 dark:border-gray-700 px-6 py-2 bg-white dark:bg-gray-800">
        <div className="flex items-center justify-between mb-3">
          {/* Left: Tabs */}
          <div className="flex gap-8">
            <button
              onClick={() => setActiveTab("form")}
              className={`px-4 py-3 text-sm font-medium transition-colors ${
                activeTab === "form"
                  ? "text-gray-900 dark:text-white border-b-2 border-gray-900 dark:border-white"
                  : "text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300"
              }`}
            >
              API 생성 폼
            </button>
            <button
              onClick={() => setActiveTab("test")}
              className={`px-4 py-3 text-sm font-medium transition-colors ${
                activeTab === "test"
                  ? "text-gray-900 dark:text-white border-b-2 border-gray-900 dark:border-white"
                  : "text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300"
              }`}
            >
              테스트 폼
            </button>
          </div>

          {/* Right: Progress Bar */}
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2">
              <div className="w-48 h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
                <div
                  className="h-full bg-blue-600 dark:bg-blue-500 transition-all duration-300"
                  style={{ width: `${progressPercentage}%` }}
                />
              </div>
              <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                {completedEndpoints}/{totalEndpoints} 완료 ({progressPercentage}
                %)
              </span>
            </div>
            <div className="flex items-center gap-3">
              {/* 선택된 엔드포인트가 있을 때: 수정/삭제 버튼 */}
              {selectedEndpoint && (
                <>
                  <button
                    onClick={handleSave}
                    className="px-4 py-2 bg-green-600 hover:bg-green-700 dark:bg-green-700 dark:hover:bg-green-600 text-white rounded-lg transition-colors font-medium"
                  >
                    💾 수정 저장
                  </button>
                  <button
                    onClick={handleDelete}
                    className="px-4 py-2 bg-red-600 hover:bg-red-700 dark:bg-red-700 dark:hover:bg-red-600 text-white rounded-lg transition-colors font-medium"
                  >
                    🗑️ 삭제
                  </button>
                  <button
                    onClick={handleReset}
                    className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors font-medium"
                  >
                    🔄 초기화
                  </button>
                </>
              )}
              {/* 새 엔드포인트 작성 모드: 생성 버튼 */}
              {!selectedEndpoint && (
                <>
                  <button
                    onClick={handleSave}
                    className="px-4 py-2 bg-blue-600 hover:bg-blue-700 dark:bg-blue-700 dark:hover:bg-blue-600 text-white rounded-lg transition-colors font-medium"
                  >
                    ✅ 생성
                  </button>
                  <button
                    onClick={handleReset}
                    className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors font-medium"
                  >
                    🔄 초기화
                  </button>
                </>
              )}
              <button className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors font-medium">
                Import YAML
              </button>
              <button className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors font-medium">
                Export Markdown
              </button>
              <button className="px-4 py-2 bg-blue-600 hover:bg-blue-700 dark:bg-blue-700 dark:hover:bg-blue-600 text-white rounded-lg transition-colors font-medium">
                Generate api.yaml
              </button>
            </div>
          </div>
        </div>

        {/* Protocol Tabs */}
        <ProtocolTabs
          selectedProtocol={protocol}
          onProtocolChange={setProtocol}
          onNewForm={handleNewForm}
        />
      </div>

      {/* Main Content */}
      <div className="flex-1 overflow-auto">
        {activeTab === "test" ? (
          <div className="p-6 text-center text-gray-500 dark:text-gray-400">
            테스트 기능은 준비 중입니다.
          </div>
        ) : (
          <div className="max-w-5xl mx-auto px-6 py-8">
            {/* Protocol not supported message */}
            {protocol !== "REST" && (
              <div className="h-full flex items-center justify-center py-12">
                <div className="text-center">
                  <p className="text-lg text-gray-500 dark:text-gray-400 mb-2">
                    {protocol} 명세서는 준비 중입니다.
                  </p>
                  <p className="text-sm text-gray-400 dark:text-gray-500">
                    프로토콜 탭을 클릭하여 REST로 전환할 수 있습니다.
                  </p>
                </div>
              </div>
            )}

            {/* Method + URL Card */}
            {protocol === "REST" && (
              <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-2xl p-6 shadow-sm mb-6">
                <div className="flex items-center gap-3 mb-4">
                  <div className="w-10 h-10 rounded-lg bg-gray-100 dark:bg-gray-700 flex items-center justify-center">
                    <span className="text-2xl">📝</span>
                  </div>
                  <div>
                    <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                      Step 1: Method & URL
                    </h3>
                    <p className="text-sm text-gray-500 dark:text-gray-400">
                      HTTP 메서드와 엔드포인트 URL을 입력하세요
                    </p>
                  </div>
                </div>

                <div className="space-y-4">
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

                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
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
                      <input
                        type="text"
                        value={description}
                        onChange={(e) => setDescription(e.target.value)}
                        placeholder="API 설명"
                        className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                        Owner
                      </label>
                      <input
                        type="text"
                        value={owner}
                        onChange={(e) => setOwner(e.target.value)}
                        placeholder="Owner (optional)"
                        className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* Request Card */}
            {protocol === "REST" && (
              <ApiRequestCard
                requestHeaders={requestHeaders}
                setRequestHeaders={setRequestHeaders}
                requestBody={requestBody}
                setRequestBody={setRequestBody}
              />
            )}

            {/* Response Card */}
            {protocol === "REST" && (
              <div className="mt-6">
                <ApiResponseCard
                  statusCodes={statusCodes}
                  setStatusCodes={setStatusCodes}
                />
              </div>
            )}

            {/* Preview Card */}
            {protocol === "REST" && (
              <div className="mt-6">
                <div className="rounded-2xl bg-white dark:bg-gray-800 shadow-sm border border-gray-200 dark:border-gray-700 p-6">
                  <div className="flex items-center justify-between mb-4">
                    <h3 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
                      <span>📄</span> Preview
                    </h3>
                    <button
                      onClick={() => setIsCodeSnippetOpen(true)}
                      className="px-4 py-2 bg-blue-600 hover:bg-blue-700 dark:bg-blue-700 dark:hover:bg-blue-600 text-white rounded-lg transition-colors font-medium flex items-center gap-2"
                    >
                      <span>&lt;/&gt;</span>
                      Code Snippet
                    </button>
                  </div>
                  <ApiPreviewCard
                    method={method}
                    url={url}
                    tags={tags}
                    description={description}
                  />
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Code Snippet Panel */}
      <CodeSnippetPanel
        isOpen={isCodeSnippetOpen}
        onClose={() => setIsCodeSnippetOpen(false)}
        method={method}
        url={url}
        headers={requestHeaders}
        requestBody={requestBody}
      />
    </div>
  );
}
