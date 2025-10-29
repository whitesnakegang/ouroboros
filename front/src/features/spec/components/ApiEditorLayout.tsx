import { useState, useEffect, useCallback } from "react";
import { ApiRequestCard } from "./ApiRequestCard";
import { ApiResponseCard } from "./ApiResponseCard";
import { ApiPreviewCard } from "./ApiPreviewCard";
import { ProtocolTabs } from "./ProtocolTabs";
import { CodeSnippetPanel } from "./CodeSnippetPanel";
import { useSpecStore } from "../store/spec.store";
import { useSidebarStore } from "@/features/sidebar/store/sidebar.store";
import { exportToMarkdown, downloadMarkdown } from "../utils/markdownExporter";

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

  const handleNewForm = useCallback(() => {
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
  }, [setSelectedEndpoint]);

  // 사이드바 Add 버튼 클릭 시 새 폼 초기화
  useEffect(() => {
    if (triggerNewForm) {
      handleNewForm();
      setTriggerNewForm(false);
    }
  }, [triggerNewForm, handleNewForm, setTriggerNewForm]);

  const handleExportMarkdown = () => {
    const markdownContent = exportToMarkdown({
      method,
      url,
      description,
      tags,
      owner,
      headers: requestHeaders,
      requestBody,
      statusCodes,
    });

    const filename = `${method.toUpperCase()}_${url.replace(/\//g, "_")}.md`;
    downloadMarkdown(markdownContent, filename);
    alert("Markdown 파일이 다운로드되었습니다.");
  };

  const handleImportYAML = () => {
    alert("Import YAML 기능은 구현 중입니다.");
  };

  const handleGenerateApiYaml = () => {
    alert("Generate api.yaml 기능은 구현 중입니다.");
  };

  return (
    <div className="h-full flex flex-col bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-800">
      {/* Header Tabs */}
      <div className="border-b border-gray-200 dark:border-gray-700 px-6 py-4 bg-white dark:bg-gray-800 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          {/* Left: Tabs */}
          <div className="flex gap-1 bg-gray-100 dark:bg-gray-700 rounded-xl p-1">
            <button
              onClick={() => setActiveTab("form")}
              className={`px-6 py-3 text-sm font-semibold transition-all duration-200 rounded-lg ${
                activeTab === "form"
                  ? "bg-white dark:bg-gray-600 text-gray-900 dark:text-white shadow-sm"
                  : "text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600"
              }`}
            >
              <div className="flex items-center gap-2">
                <span className="text-lg">📝</span>
              API 생성 폼
              </div>
            </button>
            <button
              onClick={() => setActiveTab("test")}
              className={`px-6 py-3 text-sm font-semibold transition-all duration-200 rounded-lg ${
                activeTab === "test"
                  ? "bg-white dark:bg-gray-600 text-gray-900 dark:text-white shadow-sm"
                  : "text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600"
              }`}
            >
              <div className="flex items-center gap-2">
                <span className="text-lg">🧪</span>
              테스트 폼
              </div>
            </button>
          </div>

          {/* Right: Progress Bar & Actions */}
          <div className="flex flex-col lg:flex-row items-start lg:items-center gap-4 lg:gap-6">
            {/* Progress Bar */}
            <div className="flex items-center gap-3">
              <div className="text-right hidden sm:block">
                <div className="text-sm font-semibold text-gray-700 dark:text-gray-300">
                  진행률
                </div>
                <div className="text-xs text-gray-500 dark:text-gray-400">
                  {completedEndpoints}/{totalEndpoints} 완료
                </div>
              </div>
              <div className="w-24 sm:w-32 h-3 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden shadow-inner">
                <div
                  className="h-full bg-gradient-to-r from-blue-500 to-purple-600 transition-all duration-500 ease-out"
                  style={{ width: `${progressPercentage}%` }}
                />
              </div>
              <span className="text-sm font-bold text-gray-700 dark:text-gray-300 min-w-[3rem]">
                {progressPercentage}%
              </span>
            </div>
            
            {/* Action Buttons */}
            <div className="flex flex-wrap items-center gap-2">
              {/* 선택된 엔드포인트가 있을 때: 수정/삭제 버튼 */}
              {selectedEndpoint && (
                <>
                  <button
                    onClick={handleSave}
                    className="px-3 sm:px-4 py-2 bg-green-600 hover:bg-green-700 dark:bg-green-700 dark:hover:bg-green-600 text-white rounded-xl transition-all duration-200 font-semibold shadow-sm hover:shadow-md flex items-center gap-2 text-sm sm:text-base"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3-3m0 0l-3 3m3-3v12" />
                    </svg>
                    <span className="hidden sm:inline">수정 저장</span>
                    <span className="sm:hidden">저장</span>
                  </button>
                  <button
                    onClick={handleDelete}
                    className="px-3 sm:px-4 py-2 bg-red-600 hover:bg-red-700 dark:bg-red-700 dark:hover:bg-red-600 text-white rounded-xl transition-all duration-200 font-semibold shadow-sm hover:shadow-md flex items-center gap-2 text-sm sm:text-base"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                    <span className="hidden sm:inline">삭제</span>
                  </button>
                  <button
                    onClick={handleReset}
                    className="px-3 sm:px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-all duration-200 font-semibold shadow-sm hover:shadow-md flex items-center gap-2 text-sm sm:text-base"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                    </svg>
                    <span className="hidden sm:inline">초기화</span>
                  </button>
                </>
              )}
              {/* 새 엔드포인트 작성 모드: 생성 버튼 */}
              {!selectedEndpoint && (
                <>
                  <button
                    onClick={handleSave}
                    className="px-3 sm:px-4 py-2 bg-blue-600 hover:bg-blue-700 dark:bg-blue-700 dark:hover:bg-blue-600 text-white rounded-xl transition-all duration-200 font-semibold shadow-sm hover:shadow-md flex items-center gap-2 text-sm sm:text-base"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                    </svg>
                    <span className="hidden sm:inline">생성</span>
                  </button>
                  <button
                    onClick={handleReset}
                    className="px-3 sm:px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-all duration-200 font-semibold shadow-sm hover:shadow-md flex items-center gap-2 text-sm sm:text-base"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                    </svg>
                    <span className="hidden sm:inline">초기화</span>
                  </button>
                </>
              )}
              
              {/* Divider */}
              <div className="w-px h-8 bg-gray-300 dark:bg-gray-600 hidden sm:block"></div>
              
              {/* Utility Buttons */}
              <button
                onClick={handleImportYAML}
                className="px-2 sm:px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-all duration-200 font-medium shadow-sm hover:shadow-md flex items-center gap-2 text-sm sm:text-base"
                title="YAML 파일 가져오기"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M9 19l3 3m0 0l3-3m-3 3V10" />
                </svg>
                <span className="hidden sm:inline">Import</span>
              </button>
              <button
                onClick={handleExportMarkdown}
                className="px-2 sm:px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-all duration-200 font-medium shadow-sm hover:shadow-md flex items-center gap-2 text-sm sm:text-base"
                title="Markdown 파일 내보내기"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                <span className="hidden sm:inline">Export</span>
              </button>
              <button
                onClick={handleGenerateApiYaml}
                className="px-2 sm:px-3 py-2 bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white rounded-xl transition-all duration-200 font-semibold shadow-sm hover:shadow-md flex items-center gap-2 text-sm sm:text-base"
                title="API YAML 파일 생성"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
                <span className="hidden sm:inline">Generate</span>
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
          <div className="h-full flex items-center justify-center">
            <div className="text-center py-12">
              <div className="w-24 h-24 mx-auto mb-6 rounded-full bg-gray-100 dark:bg-gray-700 flex items-center justify-center">
                <span className="text-4xl">🧪</span>
              </div>
              <h3 className="text-xl font-semibold text-gray-700 dark:text-gray-300 mb-2">
                테스트 기능 준비 중
              </h3>
              <p className="text-gray-500 dark:text-gray-400">
                API 테스트 기능이 곧 출시됩니다.
              </p>
            </div>
          </div>
        ) : (
          <div className="max-w-6xl mx-auto px-6 py-8">
            {/* Protocol not supported message */}
            {protocol !== "REST" && (
              <div className="h-full flex items-center justify-center py-12">
                <div className="text-center">
                  <div className="w-24 h-24 mx-auto mb-6 rounded-full bg-gray-100 dark:bg-gray-700 flex items-center justify-center">
                    <span className="text-4xl">🚧</span>
                  </div>
                  <h3 className="text-xl font-semibold text-gray-700 dark:text-gray-300 mb-2">
                    {protocol} 명세서 준비 중
                  </h3>
                  <p className="text-gray-500 dark:text-gray-400 mb-4">
                    현재는 REST API만 지원합니다.
                  </p>
                  <p className="text-sm text-gray-400 dark:text-gray-500">
                    프로토콜 탭을 클릭하여 REST로 전환할 수 있습니다.
                  </p>
                </div>
              </div>
            )}

            {/* Method + URL Card */}
            {protocol === "REST" && (
              <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-2xl p-8 shadow-lg mb-8 hover:shadow-xl transition-shadow duration-300">
                <div className="flex items-center gap-4 mb-6">
                  <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center shadow-lg">
                    <span className="text-white text-xl">🌐</span>
                  </div>
                  <div>
                    <h3 className="text-xl font-bold text-gray-900 dark:text-white">
                      Step 1: Method & URL
                    </h3>
                    <p className="text-sm text-gray-500 dark:text-gray-400">
                      HTTP 메서드와 엔드포인트 URL을 입력하세요
                    </p>
                  </div>
                </div>

                <div className="space-y-6">
                  <div className="flex flex-col sm:flex-row gap-4">
                    <div className="relative sm:w-auto w-full">
                    <select
                      value={method}
                      onChange={(e) => setMethod(e.target.value)}
                        className="appearance-none w-full sm:w-auto px-4 py-3 pr-10 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 font-semibold min-w-[120px]"
                    >
                      {methods.map((m) => (
                        <option key={m} value={m}>
                          {m}
                        </option>
                      ))}
                    </select>
                      <div className="absolute inset-y-0 right-0 flex items-center pr-3 pointer-events-none">
                        <svg className="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                        </svg>
                      </div>
                    </div>
                    <input
                      type="text"
                      value={url}
                      onChange={(e) => setUrl(e.target.value)}
                      placeholder="/api/endpoint"
                      className="flex-1 px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 font-mono"
                    />
                  </div>

                  {/* Method Badge */}
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-medium text-gray-500 dark:text-gray-400">Method:</span>
                    <span className={`px-3 py-1 rounded-lg text-sm font-semibold ${
                      method === 'GET' ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200' :
                      method === 'POST' ? 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200' :
                      method === 'PUT' ? 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200' :
                      method === 'PATCH' ? 'bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200' :
                      'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
                    }`}>
                      {method}
                    </span>
                  </div>

                  <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
                        Tags/Category
                      </label>
                      <input
                        type="text"
                        value={tags}
                        onChange={(e) => setTags(e.target.value)}
                        placeholder="AUTH, USER, PRODUCT, etc."
                        className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200"
                      />
                      <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                        쉼표로 구분하여 여러 태그를 입력할 수 있습니다
                      </p>
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
                        Description
                      </label>
                      <input
                        type="text"
                        value={description}
                        onChange={(e) => setDescription(e.target.value)}
                        placeholder="API의 목적과 기능을 설명하세요"
                        className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
                        Owner
                      </label>
                      <input
                        type="text"
                        value={owner}
                        onChange={(e) => setOwner(e.target.value)}
                        placeholder="팀명 또는 담당자"
                        className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200"
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
              <div className="mt-8">
                <div className="rounded-2xl bg-white dark:bg-gray-800 shadow-lg border border-gray-200 dark:border-gray-700 p-8 hover:shadow-xl transition-shadow duration-300">
                  <div className="flex items-center justify-between mb-6">
                    <div className="flex items-center gap-4">
                      <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-teal-500 to-cyan-600 flex items-center justify-center shadow-lg">
                        <span className="text-white text-xl">📄</span>
                      </div>
                      <div>
                        <h3 className="text-xl font-bold text-gray-900 dark:text-white">
                          API Preview
                    </h3>
                        <p className="text-sm text-gray-500 dark:text-gray-400">
                          작성한 API 명세서의 미리보기입니다
                        </p>
                      </div>
                    </div>
                    <button
                      onClick={() => setIsCodeSnippetOpen(true)}
                      className="px-6 py-3 bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white rounded-xl transition-all duration-200 font-semibold shadow-sm hover:shadow-md flex items-center gap-2"
                    >
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
                      </svg>
                      Code Snippet
                    </button>
                  </div>
                  <div className="bg-gray-50 dark:bg-gray-700 rounded-xl p-6">
                  <ApiPreviewCard
                    method={method}
                    url={url}
                    tags={tags}
                    description={description}
                  />
                  </div>
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
