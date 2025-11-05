import { useState, useEffect } from "react";
import { useTestingStore } from "../store/testing.store";
import { useSidebarStore } from "@/features/sidebar/store/sidebar.store";
import type {
  TryMethod,
  TryMethodParameter,
  TryTraceData,
} from "@/features/spec/services/api";
import type { TestResponse } from "../store/testing.store";
import { getTryMethodList, getTryTrace } from "@/features/spec/services/api";
import { TraceModal } from "./TraceModal";

export function TestResponseTabs() {
  const {
    response,
    methodList,
    totalDurationMs,
    useDummyResponse,
    tryId,
    setMethodList,
    setTotalDurationMs,
  } = useTestingStore();
  const { selectedEndpoint } = useSidebarStore();
  const [activeTab, setActiveTab] = useState<"response" | "test">("response");
  const [isLoadingMethods, setIsLoadingMethods] = useState(false);
  const [isTraceModalOpen, setIsTraceModalOpen] = useState(false);
  const [traceData, setTraceData] = useState<TryTraceData | null>(null);
  const [isLoadingTrace, setIsLoadingTrace] = useState(false);

  // Mock 엔드포인트인지 확인 (progress가 "mock"이거나 useDummyResponse가 true인 경우)
  const isMockEndpoint =
    useDummyResponse ||
    selectedEndpoint?.progress?.toLowerCase() !== "completed";

  // test 탭이 활성화되고 tryId가 있을 때 메서드 리스트 로드
  useEffect(() => {
    const loadTryMethods = async () => {
      if (!tryId || isMockEndpoint) return;

      setIsLoadingMethods(true);
      try {
        const response = await getTryMethodList(tryId);
        setMethodList(response.data.methods);
        setTotalDurationMs(response.data.totalDurationMs);
      } catch (error) {
        console.error("Try 메서드 리스트 로드 실패:", error);
        setMethodList(null);
        setTotalDurationMs(null);
      } finally {
        setIsLoadingMethods(false);
      }
    };

    if (activeTab === "test" && tryId && !isMockEndpoint) {
      loadTryMethods();
    }
  }, [activeTab, tryId, isMockEndpoint, setMethodList, setTotalDurationMs]);

  if (!response) {
    return (
      <div className="rounded-md border border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#161B22] p-4 shadow-sm">
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
        <div className="text-center py-12 text-gray-600 dark:text-[#8B949E]">
          <div className="w-16 h-16 mx-auto mb-4 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] flex items-center justify-center">
            <svg
              className="w-8 h-8 text-gray-500 dark:text-[#8B949E]"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"
              />
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
          </div>
          <p className="text-sm">RUN 버튼을 눌러 테스트를 실행하세요</p>
        </div>
      </div>
    );
  }

  const getStatusColor = (status: number) => {
    if (status >= 200 && status < 300) return "bg-green-500";
    if (status >= 300 && status < 400) return "bg-yellow-500";
    if (status >= 400 && status < 500) return "bg-orange-500";
    return "bg-red-500";
  };

  return (
    <div className="rounded-md border border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#161B22] shadow-sm">
      {/* Tabs */}
      <div className="flex border-b border-gray-200 dark:border-[#2D333B]">
        <button
          onClick={() => setActiveTab("response")}
          className={`flex-1 px-4 py-3 text-sm font-medium transition-colors ${
            activeTab === "response"
              ? "text-[#2563EB] border-b-2 border-[#2563EB] bg-blue-50 dark:bg-blue-900/20"
              : "text-gray-600 dark:text-[#8B949E] hover:text-gray-900 dark:hover:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#0D1117]"
          }`}
        >
          Response
        </button>
        <button
          onClick={() => setActiveTab("test")}
          className={`flex-1 px-4 py-3 text-sm font-medium transition-colors ${
            activeTab === "test"
              ? "text-[#2563EB] border-b-2 border-[#2563EB] bg-blue-50 dark:bg-blue-900/20"
              : "text-gray-600 dark:text-[#8B949E] hover:text-gray-900 dark:hover:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#0D1117]"
          }`}
        >
          Test
        </button>
      </div>

      {/* Tab Content */}
      <div className="p-4">
        {activeTab === "response" ? (
          <ResponseContent
            response={response}
            getStatusColor={getStatusColor}
          />
        ) : (
          <TestContent
            methodList={methodList}
            totalDurationMs={totalDurationMs}
            isMockEndpoint={isMockEndpoint}
            isLoading={isLoadingMethods}
            tryId={tryId}
            onShowTrace={async () => {
              if (!tryId) return;
              setIsLoadingTrace(true);
              setIsTraceModalOpen(true);
              try {
                const response = await getTryTrace(tryId);
                setTraceData(response.data);
              } catch (error) {
                console.error("Trace 조회 실패:", error);
                setTraceData(null);
              } finally {
                setIsLoadingTrace(false);
              }
            }}
            isLoadingTrace={isLoadingTrace}
          />
        )}
      </div>

      {/* Trace Modal */}
      {traceData && (
        <TraceModal
          isOpen={isTraceModalOpen}
          onClose={() => {
            setIsTraceModalOpen(false);
            setTraceData(null);
          }}
          traceData={traceData}
        />
      )}
    </div>
  );
}

function ResponseContent({
  response,
  getStatusColor,
}: {
  response: TestResponse;
  getStatusColor: (status: number) => string;
}) {
  return (
    <>
      <div className="flex items-center justify-between mb-4">
        <div className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3] flex items-center gap-2">
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
        <div className="flex items-center gap-3">
          {/* Status Code */}
          <div
            className={`px-2 py-1 rounded-md ${getStatusColor(
              response.status
            )}`}
          >
            <span className="text-sm font-bold text-white">
              {response.status} {response.statusText}
            </span>
          </div>
        </div>
      </div>

      {/* Response Body */}
      <div className="mb-6">
        <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
          Response Body
        </label>
        <div className="bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] rounded-md p-4 overflow-auto max-h-64">
          <pre className="text-sm text-gray-900 dark:text-[#E6EDF3] font-mono whitespace-pre-wrap">
            {(() => {
              try {
                // body가 문자열인 경우 JSON 파싱 시도
                if (typeof response.body === "string") {
                  const parsed = JSON.parse(response.body);
                  return JSON.stringify(parsed, null, 2);
                }
                // body가 객체인 경우 직접 stringify
                return JSON.stringify(response.body, null, 2);
              } catch {
                // JSON 파싱 실패 시 원본 그대로 표시 (텍스트 응답 등)
                return response.body;
              }
            })()}
          </pre>
        </div>
      </div>

      {/* Headers */}
      <div>
        <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
          Headers
        </label>
        <div className="bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] rounded-md p-4">
          {Object.entries(response.headers as Record<string, string>).map(
            ([key, value]) => (
              <div
                key={key}
                className="flex gap-2 py-1 text-sm font-mono text-gray-900 dark:text-[#E6EDF3]"
              >
                <span className="font-semibold text-[#2563EB]">{key}:</span>
                <span className="text-gray-600 dark:text-[#8B949E]">
                  {value}
                </span>
              </div>
            )
          )}
        </div>
      </div>
    </>
  );
}

function TestContent({
  methodList,
  totalDurationMs,
  isMockEndpoint,
  isLoading,
  tryId,
  onShowTrace,
  isLoadingTrace,
}: {
  methodList: TryMethod[] | null;
  totalDurationMs: number | null;
  isMockEndpoint: boolean;
  isLoading: boolean;
  tryId: string | null;
  onShowTrace: () => void;
  isLoadingTrace: boolean;
}) {
  // Mock 엔드포인트인 경우 메서드 실행 정보가 없음을 표시
  if (isMockEndpoint) {
    return (
      <div className="text-center py-12 text-gray-600 dark:text-[#8B949E]">
        <div className="w-16 h-16 mx-auto mb-4 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] flex items-center justify-center">
          <svg
            className="w-8 h-8 text-gray-500 dark:text-[#8B949E]"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
            />
          </svg>
        </div>
        <p className="text-sm">
          Mock 엔드포인트는 메서드 실행 정보를 제공하지 않습니다
        </p>
        <p className="text-xs mt-1 text-gray-500 dark:text-[#6B7280]">
          실제 구현된 엔드포인트(completed)에서만 메서드별 실행 시간을 확인할 수
          있습니다
        </p>
      </div>
    );
  }

  // tryId가 없는 경우 안내 메시지 표시
  if (!tryId) {
    return (
      <div className="text-center py-12 text-gray-600 dark:text-[#8B949E]">
        <div className="w-16 h-16 mx-auto mb-4 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] flex items-center justify-center">
          <svg
            className="w-8 h-8 text-gray-500 dark:text-[#8B949E]"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"
            />
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
        </div>
        <p className="text-sm">
          메서드 실행 정보를 불러오려면 먼저 API를 실행하세요
        </p>
        <p className="text-xs mt-1 text-gray-500 dark:text-[#6B7280]">
          RUN 버튼을 눌러 테스트를 실행하면 메서드별 실행 시간을 확인할 수
          있습니다
        </p>
      </div>
    );
  }

  // 로딩 중일 때
  if (isLoading) {
    return (
      <div className="text-center py-12 text-gray-600 dark:text-[#8B949E]">
        <div className="w-16 h-16 mx-auto mb-4 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] flex items-center justify-center">
          <svg
            className="animate-spin h-8 w-8 text-blue-500"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
          >
            <circle
              className="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              strokeWidth="4"
            />
            <path
              className="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            />
          </svg>
        </div>
        <p className="text-sm">메서드 실행 정보를 불러오는 중...</p>
      </div>
    );
  }

  if (!methodList || methodList.length === 0) {
    return (
      <div className="text-center py-12 text-gray-600 dark:text-[#8B949E]">
        <div className="w-16 h-16 mx-auto mb-4 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] flex items-center justify-center">
          <svg
            className="w-8 h-8 text-gray-500 dark:text-[#8B949E]"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
            />
          </svg>
        </div>
        <p className="text-sm">메서드 실행 정보가 없습니다</p>
        <p className="text-xs mt-1 text-gray-500 dark:text-[#6B7280]">
          Trace 모드로 실행하면 메서드별 실행 시간을 확인할 수 있습니다
        </p>
      </div>
    );
  }

  return (
    <div>
      {/* Total Duration */}
      {totalDurationMs !== null && (
        <div className="mb-6 p-4 bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] rounded-md">
          <div className="flex items-center justify-between">
            <div>
              <div className="text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-1">
                전체 소요 시간
              </div>
              <div className="text-2xl font-bold text-gray-900 dark:text-[#E6EDF3]">
                {totalDurationMs.toLocaleString()}ms
              </div>
            </div>
            <div className="flex items-center gap-3">
              <button
                onClick={onShowTrace}
                disabled={isLoadingTrace || !tryId}
                className="px-4 py-2 bg-blue-500 hover:bg-blue-600 disabled:bg-gray-300 dark:disabled:bg-[#2D333B] disabled:text-gray-500 dark:disabled:text-[#8B949E] text-white rounded-md transition-colors text-sm font-medium flex items-center gap-2 disabled:cursor-not-allowed"
              >
                {isLoadingTrace ? (
                  <>
                    <svg
                      className="animate-spin h-4 w-4"
                      xmlns="http://www.w3.org/2000/svg"
                      fill="none"
                      viewBox="0 0 24 24"
                    >
                      <circle
                        className="opacity-25"
                        cx="12"
                        cy="12"
                        r="10"
                        stroke="currentColor"
                        strokeWidth="4"
                      />
                      <path
                        className="opacity-75"
                        fill="currentColor"
                        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                      />
                    </svg>
                    로딩 중...
                  </>
                ) : (
                  <>
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
                        d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
                      />
                    </svg>
                    Call Trace 보기
                  </>
                )}
              </button>
              <div className="w-16 h-16 rounded-full bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center">
                <svg
                  className="w-8 h-8 text-blue-600 dark:text-blue-400"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
                  />
                </svg>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Method List */}
      <div>
        <div className="text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-3">
          메서드별 실행 시간 ({methodList.length}개)
        </div>
        <div className="space-y-2">
          {methodList.map((method: TryMethod, index: number) => (
            <div
              key={method.spanId || index}
              className="p-4 bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] rounded-md hover:border-blue-300 dark:hover:border-blue-700 transition-colors"
            >
              <div className="flex items-start justify-between mb-2">
                <div className="flex-1">
                  <div className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3] mb-1">
                    {method.name}
                  </div>
                  <div className="text-xs text-gray-600 dark:text-[#8B949E] font-mono">
                    {method.className}
                  </div>
                  {method.parameters && method.parameters.length > 0 && (
                    <div className="mt-2 flex flex-wrap gap-1">
                      {method.parameters.map(
                        (param: TryMethodParameter, paramIndex: number) => (
                          <span
                            key={paramIndex}
                            className="px-2 py-0.5 text-xs bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-300 rounded"
                          >
                            {param.type} {param.name}
                          </span>
                        )
                      )}
                    </div>
                  )}
                </div>
                <div className="ml-4 text-right">
                  <div className="text-lg font-bold text-gray-900 dark:text-[#E6EDF3]">
                    {method.selfDurationMs.toLocaleString()}ms
                  </div>
                  <div className="text-xs text-gray-600 dark:text-[#8B949E]">
                    {method.selfPercentage.toFixed(1)}%
                  </div>
                </div>
              </div>
              {/* Progress Bar */}
              {totalDurationMs && totalDurationMs > 0 && (
                <div className="mt-2">
                  <div className="h-2 bg-gray-200 dark:bg-[#2D333B] rounded-full overflow-hidden">
                    <div
                      className="h-full bg-blue-500 dark:bg-blue-600 transition-all"
                      style={{
                        width: `${method.selfPercentage}%`,
                      }}
                    />
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
