import { useState } from "react";
import { useTestingStore } from "../store/testing.store";
import { useSidebarStore } from "@/features/sidebar/store/sidebar.store";
import type { TryMethod, TryMethodParameter } from "@/features/spec/services/api";

export function TestResponseTabs() {
  const { response, methodList, totalDurationMs, useDummyResponse } = useTestingStore();
  const { selectedEndpoint } = useSidebarStore();
  const [activeTab, setActiveTab] = useState<"response" | "test">("response");

  // Mock 엔드포인트인지 확인 (progress가 "mock"이거나 useDummyResponse가 true인 경우)
  const isMockEndpoint = useDummyResponse || (selectedEndpoint?.progress?.toLowerCase() !== "completed");

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
          <ResponseContent response={response} getStatusColor={getStatusColor} isMockEndpoint={isMockEndpoint} />
        ) : (
          <TestContent methodList={methodList} totalDurationMs={totalDurationMs} isMockEndpoint={isMockEndpoint} />
        )}
      </div>
    </div>
  );
}

function ResponseContent({
  response,
  getStatusColor,
  isMockEndpoint,
}: {
  response: NonNullable<ReturnType<typeof useTestingStore>["response"]>;
  getStatusColor: (status: number) => string;
  isMockEndpoint: boolean;
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
          {/* Response Time - Mock 엔드포인트가 아닐 때만 표시 */}
          {!isMockEndpoint && response.responseTime && (
            <div className="px-2 py-1 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B]">
              <span className="text-xs font-medium text-gray-600 dark:text-[#8B949E] font-mono">
                {response.responseTime}ms
              </span>
            </div>
          )}
          {/* Status Code */}
          <div
            className={`px-2 py-1 rounded-md ${getStatusColor(response.status)}`}
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
            {typeof response.body === "string"
              ? response.body
              : JSON.stringify(response.body, null, 2)}
          </pre>
        </div>
      </div>

      {/* Headers */}
      <div>
        <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
          Headers
        </label>
        <div className="bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] rounded-md p-4">
          {Object.entries(response.headers).map(([key, value]: [string, string]) => (
            <div
              key={key}
              className="flex gap-2 py-1 text-sm font-mono text-gray-900 dark:text-[#E6EDF3]"
            >
              <span className="font-semibold text-[#2563EB]">
                {key}:
              </span>
              <span className="text-gray-600 dark:text-[#8B949E]">{value}</span>
            </div>
          ))}
        </div>
      </div>
    </>
  );
}

function TestContent({
  methodList,
  totalDurationMs,
  isMockEndpoint,
}: {
  methodList: ReturnType<typeof useTestingStore>["methodList"];
  totalDurationMs: ReturnType<typeof useTestingStore>["totalDurationMs"];
  isMockEndpoint: boolean;
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
        <p className="text-sm">Mock 엔드포인트는 메서드 실행 정보를 제공하지 않습니다</p>
        <p className="text-xs mt-1 text-gray-500 dark:text-[#6B7280]">
          실제 구현된 엔드포인트(completed)에서만 메서드별 실행 시간을 확인할 수 있습니다
        </p>
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
                      {method.parameters.map((param: TryMethodParameter, paramIndex: number) => (
                        <span
                          key={paramIndex}
                          className="px-2 py-0.5 text-xs bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-300 rounded"
                        >
                          {param.type} {param.name}
                        </span>
                      ))}
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

