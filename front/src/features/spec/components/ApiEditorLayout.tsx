import { useState, useEffect, useCallback } from "react";
import { ApiRequestCard } from "./ApiRequestCard";
import { ApiResponseCard } from "./ApiResponseCard";
import { ProtocolTabs } from "./ProtocolTabs";
import { CodeSnippetPanel } from "./CodeSnippetPanel";
import { ImportResultModal } from "./ImportResultModal";
import { useSpecStore } from "../store/spec.store";
import { useSidebarStore } from "@/features/sidebar/store/sidebar.store";
import {
  downloadMarkdown,
  exportAllToMarkdown,
} from "../utils/markdownExporter";
import { buildOpenApiYamlFromSpecs, downloadYaml } from "../utils/yamlExporter";
import {
  getAllRestApiSpecs,
  getAllSchemas,
  type GetAllSchemasResponse,
  importYaml,
  type ImportYamlResponse,
} from "../services/api";
import {
  createRestApiSpec,
  updateRestApiSpec,
  deleteRestApiSpec,
  getRestApiSpec,
} from "../services/api";

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
    deleteEndpoint,
    addEndpoint,
    setSelectedEndpoint,
    triggerNewForm,
    setTriggerNewForm,
    loadEndpoints,
    updateEndpoint,
    endpoints,
  } = useSidebarStore();
  const [activeTab, setActiveTab] = useState<"form" | "test">("form");
  const [isCodeSnippetOpen, setIsCodeSnippetOpen] = useState(false);
  const [isEditMode, setIsEditMode] = useState(false);
  const [importResult, setImportResult] = useState<ImportYamlResponse | null>(
    null
  );
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);

  // Load selected endpoint data when endpoint is clicked
  useEffect(() => {
    if (selectedEndpoint) {
      setIsEditMode(false); // 항목 선택 시 읽기 전용 모드로 시작
      loadEndpointData(selectedEndpoint.id);
    } else {
      setIsEditMode(false);
    }
  }, [selectedEndpoint]);

  // Load endpoint data from backend
  const loadEndpointData = async (id: string) => {
    try {
      const response = await getRestApiSpec(id);
      const spec = response.data;
      setMethod(spec.method);
      setUrl(spec.path);
      setDescription(spec.description || "");
      setSummary(spec.summary || "");
      setTags(spec.tags ? spec.tags.join(", ") : "");
      // 추가 데이터 로드 필요시 여기에 구현
    } catch (error) {
      console.error("API 스펙 로드 실패:", error);
      alert("API 스펙을 불러오는데 실패했습니다.");
    }
  };

  // Form state
  const [method, setMethod] = useState("POST");
  const [url, setUrl] = useState("");
  const [tags, setTags] = useState("");
  const [description, setDescription] = useState("");
  const [summary, setSummary] = useState("");
  const [owner, setOwner] = useState("");

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

  // 진행률: 전체 엔드포인트 대비 completed 비율
  const allEndpoints = Object.values(endpoints || {}).flat();
  const totalEndpoints = allEndpoints.length || 0;
  const completedEndpoints = allEndpoints.filter(
    (ep) => ep.progress === "completed"
  ).length;
  const progressPercentage = totalEndpoints
    ? Math.round((completedEndpoints / totalEndpoints) * 100)
    : 0;

  const handleSave = async () => {
    if (!method || !url) {
      alert("Method와 URL을 입력해주세요.");
      return;
    }

    try {
      if (selectedEndpoint && isEditMode) {
        // 수정 로직 - path와 method도 수정 가능 (백엔드 지원)
        const updateRequest = {
          path: url, // path 수정 가능
          method, // method 수정 가능
          summary,
          description,
          tags: tags ? tags.split(",").map((tag) => tag.trim()) : [],
          progress: "mock",
          tag: "none",
          isValid: true,
          // 추가 필드들도 포함
          parameters: [], // 실제 데이터로 교체 필요
          requestBody: requestBody, // 실제 requestBody 데이터
          responses: statusCodes.reduce((acc, code) => {
            acc[code.code] = {
              description: code.message,
              type: code.type.toLowerCase(),
            };
            return acc;
          }, {} as Record<string, unknown>),
          security: [],
        };

        await updateRestApiSpec(selectedEndpoint.id, updateRequest);

        alert("API 스펙이 수정되었습니다.");
        setIsEditMode(false);

        // 수정된 엔드포인트를 로컬 상태에 반영
        const updatedEndpoint = {
          id: selectedEndpoint.id,
          method, // 수정된 method 반영
          path: url, // 수정된 path 반영
          description,
          implementationStatus: selectedEndpoint.implementationStatus,
          hasSpecError: selectedEndpoint.hasSpecError,
          tags: tags ? tags.split(",").map((tag) => tag.trim()) : [],
          progress: "mock",
        };
        updateEndpoint(updatedEndpoint);
        setSelectedEndpoint(updatedEndpoint);

        // 사이드바 목록 다시 로드 (백그라운드에서)
        loadEndpoints();
      } else {
        // 새 엔드포인트 생성
        const apiRequest = {
          path: url,
          method,
          summary,
          description,
          tags: tags ? tags.split(",").map((tag) => tag.trim()) : [],
          progress: "mock",
          tag: "none",
          isValid: true,
        };

        const response = await createRestApiSpec(apiRequest);

        const newEndpoint = {
          id: response.data.id,
          method,
          path: url,
          description,
          implementationStatus: "not-implemented" as const,
          tags: tags ? tags.split(",").map((tag) => tag.trim()) : [],
          progress: "mock",
        };

        const group = tags ? tags.split(",")[0].trim() : "OTHERS";
        addEndpoint(newEndpoint, group);
        alert(`${method} ${url} API가 생성되었습니다.`);

        // 사이드바 목록 다시 로드
        await loadEndpoints();

        // 수정된 엔드포인트를 다시 선택
        const updatedEndpoint = {
          id: response.data.id,
          method,
          path: url,
          description,
          implementationStatus: "not-implemented" as const,
          tags: tags ? tags.split(",").map((tag) => tag.trim()) : [],
          progress: "mock",
        };
        setSelectedEndpoint(updatedEndpoint);
      }
    } catch (error: unknown) {
      console.error("API 저장 실패:", error);
      const errorMessage =
        error instanceof Error ? error.message : "알 수 없는 오류";
      alert(`API 저장에 실패했습니다: ${errorMessage}`);
    }
  };

  const handleDelete = async () => {
    if (!selectedEndpoint) return;

    if (confirm("이 엔드포인트를 삭제하시겠습니까?")) {
      try {
        await deleteRestApiSpec(selectedEndpoint.id);
        deleteEndpoint(selectedEndpoint.id);
        alert("엔드포인트가 삭제되었습니다.");

        // 폼 초기화
        setSelectedEndpoint(null);
        setMethod("POST");
        setUrl("/api/auth/login");
        setTags("AUTH");
        setDescription("사용자 로그인");
        setOwner("SMART-TEAM");
        setIsEditMode(false);
        loadEndpoints();
      } catch (error: unknown) {
        console.error("API 삭제 실패:", error);
        const errorMessage =
          error instanceof Error ? error.message : "알 수 없는 오류";
        alert(`API 삭제에 실패했습니다: ${errorMessage}`);
      }
    }
  };

  const handleEdit = () => {
    setIsEditMode(true);
  };

  const handleCancelEdit = () => {
    if (selectedEndpoint) {
      loadEndpointData(selectedEndpoint.id);
    }
    setIsEditMode(false);
  };

  const handleReset = () => {
    if (confirm("작성 중인 내용을 초기화하시겠습니까?")) {
      setMethod("POST");
      setUrl("");
      setTags("");
      setDescription("");
      setSummary("");
      setOwner("");
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
    // 값은 비워 placeholder가 보이도록 처리
    setUrl("");
    setTags("");
    setDescription("");
    setSummary("");
    setOwner("");
    setRequestHeaders([]);
    setRequestBody({
      type: "json",
      contentType: "application/json",
      fields: [],
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

  const handleImportYAML = async () => {
    // 파일 선택 input 생성
    const input = document.createElement("input");
    input.type = "file";
    input.accept = ".yml,.yaml";

    input.onchange = async (e) => {
      const file = (e.target as HTMLInputElement).files?.[0];
      if (!file) return;

      // 파일 확장자 검증
      const fileName = file.name.toLowerCase();
      if (!fileName.endsWith(".yml") && !fileName.endsWith(".yaml")) {
        alert("YAML 파일(.yml 또는 .yaml)만 업로드 가능합니다.");
        return;
      }

      try {
        // Import 실행
        const result: ImportYamlResponse = await importYaml(file);

        // 모달로 결과 표시
        setImportResult(result);
        setIsImportModalOpen(true);

        // 사이드바 목록 새로고침
        await loadEndpoints();
      } catch (error) {
        console.error("YAML Import 오류:", error);
        const errorMsg =
          error instanceof Error ? error.message : "알 수 없는 오류";
        alert(`❌ YAML Import 실패\n\n${errorMsg}`);
      }
    };

    // 파일 선택 다이얼로그 열기
    input.click();
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
                <span className="text-lg"></span>
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
                <span className="text-lg"></span>
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

            {/* Action Buttons - Utility만 유지 */}
            <div className="flex flex-wrap items-center gap-2">
              {/* Utility Buttons */}
              <button
                onClick={handleImportYAML}
                className="px-2 sm:px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-all duration-200 font-medium shadow-sm hover:shadow-md flex items-center gap-2 text-sm sm:text-base"
                title="YAML 파일 가져오기"
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
                    d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M9 19l3 3m0 0l3-3m-3 3V10"
                  />
                </svg>
                <span className="hidden sm:inline">Import</span>
              </button>
              <button
                onClick={async () => {
                  try {
                    const res = await getAllRestApiSpecs();
                    const md = exportAllToMarkdown(res.data);
                    downloadMarkdown(md, `ALL_APIS_${new Date().getTime()}.md`);
                    alert("Markdown 파일이 다운로드되었습니다.");
                  } catch (e) {
                    console.error("Markdown 내보내기 오류:", e);
                    const errorMsg =
                      e instanceof Error ? e.message : "알 수 없는 오류";
                    alert(
                      `전체 Markdown 내보내기에 실패했습니다.\n오류: ${errorMsg}`
                    );
                  }
                }}
                className="px-2 sm:px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-all duration-200 font-medium shadow-sm hover:shadow-md flex items-center gap-2 text-sm sm:text-base"
                title="Markdown 파일 내보내기"
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
                    d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                  />
                </svg>
                <span className="hidden sm:inline">Export</span>
              </button>
              <button
                onClick={async () => {
                  try {
                    const [specsRes, schemasRes] = await Promise.all([
                      getAllRestApiSpecs(),
                      getAllSchemas().catch((error) => {
                        console.warn(
                          "Schema 조회 실패, 빈 배열로 계속 진행:",
                          error.message
                        );
                        return {
                          status: 200,
                          data: [],
                          message: "Schema 조회 실패",
                        } as GetAllSchemasResponse;
                      }),
                    ]);
                    const yaml = buildOpenApiYamlFromSpecs(
                      specsRes.data,
                      (schemasRes as GetAllSchemasResponse).data
                    );
                    downloadYaml(yaml, `ALL_APIS_${new Date().getTime()}.yml`);
                    alert("YAML 파일이 다운로드되었습니다.");
                  } catch (e) {
                    console.error("YAML 내보내기 오류:", e);
                    const errorMsg =
                      e instanceof Error ? e.message : "알 수 없는 오류";
                    alert(
                      `전체 YAML 내보내기에 실패했습니다.\n오류: ${errorMsg}`
                    );
                  }
                }}
                className="px-2 sm:px-3 py-2 bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white rounded-xl transition-all duration-200 font-semibold shadow-sm hover:shadow-md flex items-center gap-2 text-sm sm:text-base"
                title="API YAML 파일 생성"
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
                    d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"
                  />
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
                  />
                </svg>
                <span className="hidden sm:inline">Generate</span>
              </button>
              <button
                onClick={() => setIsCodeSnippetOpen(true)}
                className="px-2 sm:px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-all duration-200 font-medium shadow-sm hover:shadow-md flex items-center gap-2 text-sm sm:text-base"
                title="Code Snippet 보기"
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
                    d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4"
                  />
                </svg>
                Code Snippet
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
                      Method & URL
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
                        disabled={!!(selectedEndpoint && !isEditMode)}
                        className={`appearance-none w-full sm:w-auto px-4 py-3 pr-10 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 font-semibold min-w-[120px] ${
                          selectedEndpoint && !isEditMode
                            ? "bg-gray-100 dark:bg-gray-800 cursor-not-allowed opacity-60"
                            : ""
                        }`}
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
                      disabled={!!(selectedEndpoint && !isEditMode)}
                      className={`flex-1 px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 font-mono ${
                        selectedEndpoint && !isEditMode
                          ? "bg-gray-100 dark:bg-gray-800 cursor-not-allowed opacity-60"
                          : ""
                      }`}
                    />
                  </div>

                  {/* Method Badge */}
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-medium text-gray-500 dark:text-gray-400">
                      Method:
                    </span>
                    <span
                      className={`px-3 py-1 rounded-lg text-sm font-semibold ${
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

                  <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
                        Tags/Category
                      </label>
                      <input
                        type="text"
                        value={tags}
                        onChange={(e) => setTags(e.target.value)}
                        placeholder="예: AUTH, USER, PRODUCT, ORDER"
                        disabled={!!(selectedEndpoint && !isEditMode)}
                        className={`w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 ${
                          selectedEndpoint && !isEditMode
                            ? "bg-gray-100 dark:bg-gray-800 cursor-not-allowed opacity-60"
                            : ""
                        }`}
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
                        Summary
                      </label>
                      <input
                        type="text"
                        value={summary}
                        onChange={(e) => setSummary(e.target.value)}
                        placeholder="예: 사용자 로그인 생성"
                        disabled={!!(selectedEndpoint && !isEditMode)}
                        className={`w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 ${
                          selectedEndpoint && !isEditMode
                            ? "bg-gray-100 dark:bg-gray-800 cursor-not-allowed opacity-60"
                            : ""
                        }`}
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
                        placeholder="예: SMART-TEAM, 김개발, 백엔드팀"
                        disabled={!!(selectedEndpoint && !isEditMode)}
                        className={`w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 ${
                          selectedEndpoint && !isEditMode
                            ? "bg-gray-100 dark:bg-gray-800 cursor-not-allowed opacity-60"
                            : ""
                        }`}
                      />
                    </div>
                  </div>

                  {/* Description - place below Tags/Summary/Owner */}
                  <div>
                    <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
                      Description
                    </label>
                    <input
                      type="text"
                      value={description}
                      onChange={(e) => setDescription(e.target.value)}
                      placeholder="예: 사용자 로그인, 상품 목록 조회, 주문 생성"
                      disabled={!!(selectedEndpoint && !isEditMode)}
                      className={`w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 ${
                        selectedEndpoint && !isEditMode
                          ? "bg-gray-100 dark:bg-gray-800 cursor-not-allowed opacity-60"
                          : ""
                      }`}
                    />
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
                isReadOnly={!!(selectedEndpoint && !isEditMode)}
              />
            )}

            {/* Response Card */}
            {protocol === "REST" && (
              <div className="mt-6">
                <ApiResponseCard
                  statusCodes={statusCodes}
                  setStatusCodes={setStatusCodes}
                  isReadOnly={!!(selectedEndpoint && !isEditMode)}
                />
              </div>
            )}

            {/* Preview 제거: 상세 보기에서는 Code Snippet만 노출 */}
          </div>
        )}
      </div>

      {/* 하단 수정/삭제 버튼 - 선택된 엔드포인트가 있을 때만 표시 */}
      {selectedEndpoint && (
        <div className="border-t border-gray-200 dark:border-gray-700 px-6 py-4 bg-white dark:bg-gray-800 shadow-lg">
          <div className="flex items-center justify-end gap-3">
            {isEditMode ? (
              <>
                <button
                  onClick={handleCancelEdit}
                  className="px-6 py-3 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-all duration-200 font-semibold shadow-sm hover:shadow-md flex items-center gap-2"
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
                  취소
                </button>
                <button
                  onClick={handleSave}
                  className="px-6 py-3 bg-green-600 hover:bg-green-700 dark:bg-green-700 dark:hover:bg-green-600 text-white rounded-xl transition-all duration-200 font-semibold shadow-sm hover:shadow-md flex items-center gap-2"
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
                      d="M5 13l4 4L19 7"
                    />
                  </svg>
                  저장
                </button>
              </>
            ) : (
              <>
                <button
                  onClick={handleEdit}
                  className="px-6 py-3 bg-blue-600 hover:bg-blue-700 dark:bg-blue-700 dark:hover:bg-blue-600 text-white rounded-xl transition-all duration-200 font-semibold shadow-sm hover:shadow-md flex items-center gap-2"
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
                      d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
                    />
                  </svg>
                  수정
                </button>
                <button
                  onClick={handleDelete}
                  className="px-6 py-3 bg-red-600 hover:bg-red-700 dark:bg-red-700 dark:hover:bg-red-600 text-white rounded-xl transition-all duration-200 font-semibold shadow-sm hover:shadow-md flex items-center gap-2"
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
                  삭제
                </button>
              </>
            )}
          </div>
        </div>
      )}
      {/* 하단 생성/초기화 버튼 - 새 명세 작성 중일 때 표시 */}
      {!selectedEndpoint && (
        <div className="border-t border-gray-200 dark:border-gray-700 px-6 py-4 bg-white dark:bg-gray-800 shadow-lg">
          <div className="flex items-center justify-end gap-3">
            <button
              onClick={handleReset}
              className="px-6 py-3 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-all duration-200 font-semibold shadow-sm hover:shadow-md flex items-center gap-2"
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
                  d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
                />
              </svg>
              초기화
            </button>
            <button
              onClick={handleSave}
              className="px-6 py-3 bg-blue-600 hover:bg-blue-700 dark:bg-blue-700 dark:hover:bg-blue-600 text-white rounded-xl transition-all duration-200 font-semibold shadow-sm hover:shadow-md flex items-center gap-2"
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
                  d="M12 6v6m0 0v6m0-6h6m-6 0H6"
                />
              </svg>
              생성
            </button>
          </div>
        </div>
      )}

      {/* Code Snippet Panel */}
      <CodeSnippetPanel
        isOpen={isCodeSnippetOpen}
        onClose={() => setIsCodeSnippetOpen(false)}
        method={method}
        url={url}
        headers={requestHeaders}
        requestBody={requestBody}
      />

      {/* Import Result Modal */}
      {importResult && (
        <ImportResultModal
          isOpen={isImportModalOpen}
          onClose={() => {
            setIsImportModalOpen(false);
            setImportResult(null);
          }}
          result={importResult.data}
        />
      )}
    </div>
  );
}
