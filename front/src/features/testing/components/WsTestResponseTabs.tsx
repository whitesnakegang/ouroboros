import { useState, useEffect, useRef } from "react";
import { useTestingStore } from "../store/testing.store";
import { useSidebarStore } from "@/features/sidebar/store/sidebar.store";
import type { TryMethod, TryTraceData } from "@/features/spec/services/api";
import { getTryMethodList, getTryTrace } from "@/features/spec/services/api";
import { TraceModal } from "./TraceModal";
import type { WebSocketMessage } from "../store/testing.store";

export function WsTestResponseTabs() {
  const {
    wsMessages,
    wsConnectionStatus,
    wsStats,
    wsConnectionStartTime,
    methodList,
    totalDurationMs,
    tryId,
    setMethodList,
    setTotalDurationMs,
  } = useTestingStore();
  const { selectedEndpoint } = useSidebarStore();

  const [activeTab, setActiveTab] = useState<"response" | "test">("response");
  const [messageFilter, setMessageFilter] = useState<"all" | "sent" | "received">("all");
  const [searchQuery, setSearchQuery] = useState("");
  const [isJsonFormatted, setIsJsonFormatted] = useState(true);
  const [isLoadingMethods, setIsLoadingMethods] = useState(false);
  const [isTraceModalOpen, setIsTraceModalOpen] = useState(false);
  const [traceData, setTraceData] = useState<TryTraceData | null>(null);
  const [isLoadingTrace, setIsLoadingTrace] = useState(false);
  const [initialExpandedSpanId, setInitialExpandedSpanId] = useState<string | null>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Mock 엔드포인트인지 확인
  const isMockEndpoint =
    selectedEndpoint?.progress?.toLowerCase() !== "completed";

  // 메시지 로그 자동 스크롤
  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [wsMessages]);

  // Test 탭이 활성화되고 tryId가 있을 때 메서드 리스트 로드
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

  // 필터링된 메시지
  const filteredMessages = wsMessages.filter((msg) => {
    if (messageFilter !== "all" && msg.direction !== messageFilter) {
      return false;
    }
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      return (
        msg.address.toLowerCase().includes(query) ||
        msg.content.toLowerCase().includes(query)
      );
    }
    return true;
  });

  // 연결 지속 시간 포맷팅
  const formatConnectionDuration = (durationMs: number | null): string => {
    if (!durationMs) return "0s";
    const seconds = Math.floor(durationMs / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);

    if (hours > 0) {
      return `${hours}h ${minutes % 60}m ${seconds % 60}s`;
    } else if (minutes > 0) {
      return `${minutes}m ${seconds % 60}s`;
    } else {
      return `${seconds}s`;
    }
  };

  // 타임스탬프 포맷팅
  const formatTimestamp = (timestamp: number): string => {
    const date = new Date(timestamp);
    const hours = date.getHours().toString().padStart(2, "0");
    const minutes = date.getMinutes().toString().padStart(2, "0");
    const seconds = date.getSeconds().toString().padStart(2, "0");
    const milliseconds = date.getMilliseconds().toString().padStart(3, "0");
    return `${hours}:${minutes}:${seconds}.${milliseconds}`;
  };

  // JSON 포맷팅
  const formatContent = (content: string): string => {
    if (!isJsonFormatted) return content;
    try {
      const parsed = JSON.parse(content);
      return JSON.stringify(parsed, null, 2);
    } catch {
      return content;
    }
  };

  // 메시지 로그 내보내기
  const exportMessages = (format: "json" | "csv") => {
    if (format === "json") {
      const dataStr = JSON.stringify(wsMessages, null, 2);
      const dataBlob = new Blob([dataStr], { type: "application/json" });
      const url = URL.createObjectURL(dataBlob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `websocket-messages-${Date.now()}.json`;
      link.click();
      URL.revokeObjectURL(url);
    } else {
      const csvRows = [
        ["Timestamp", "Direction", "Address", "Content", "Try ID"],
        ...wsMessages.map((msg) => [
          new Date(msg.timestamp).toISOString(),
          msg.direction,
          msg.address,
          msg.content.replace(/\n/g, " "),
          msg.tryId || "",
        ]),
      ];
      const csvContent = csvRows.map((row) => row.join(",")).join("\n");
      const dataBlob = new Blob([csvContent], { type: "text/csv" });
      const url = URL.createObjectURL(dataBlob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `websocket-messages-${Date.now()}.csv`;
      link.click();
      URL.revokeObjectURL(url);
    }
  };

  return (
    <div className="rounded-md border border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#161B22] shadow-sm">
      {/* Connection Status */}
      <div className="px-4 py-3 border-b border-gray-200 dark:border-[#2D333B] flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div
            className={`w-3 h-3 rounded-full ${
              wsConnectionStatus === "connected"
                ? "bg-green-500"
                : wsConnectionStatus === "connecting"
                ? "bg-yellow-500"
                : "bg-gray-400"
            }`}
          />
          <span className="text-sm font-medium text-gray-900 dark:text-[#E6EDF3]">
            {wsConnectionStatus === "connected"
              ? "Connected"
              : wsConnectionStatus === "connecting"
              ? "Connecting..."
              : "Disconnected"}
          </span>
          {wsConnectionStatus === "connected" && wsConnectionStartTime && (
            <span className="text-xs text-gray-600 dark:text-[#8B949E]">
              ({formatConnectionDuration(wsStats.connectionDuration)})
            </span>
          )}
        </div>
        <div className="flex items-center gap-2 text-xs text-gray-600 dark:text-[#8B949E]">
          <span>Sent: {wsStats.totalSent}</span>
          <span>|</span>
          <span>Received: {wsStats.totalReceived}</span>
        </div>
      </div>

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
            messages={filteredMessages}
            messageFilter={messageFilter}
            setMessageFilter={setMessageFilter}
            searchQuery={searchQuery}
            setSearchQuery={setSearchQuery}
            isJsonFormatted={isJsonFormatted}
            setIsJsonFormatted={setIsJsonFormatted}
            formatTimestamp={formatTimestamp}
            formatContent={formatContent}
            exportMessages={exportMessages}
            messagesEndRef={messagesEndRef}
          />
        ) : (
          <TestContent
            methodList={methodList}
            totalDurationMs={totalDurationMs}
            isMockEndpoint={isMockEndpoint}
            isLoading={isLoadingMethods}
            tryId={tryId}
            onShowTrace={async (spanId?: string) => {
              if (!tryId) return;
              setIsLoadingTrace(true);
              setIsTraceModalOpen(true);
              setInitialExpandedSpanId(spanId || null);
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
            setInitialExpandedSpanId(null);
          }}
          traceData={traceData}
          initialExpandedSpanId={initialExpandedSpanId}
        />
      )}
    </div>
  );
}

function ResponseContent({
  messages,
  messageFilter,
  setMessageFilter,
  searchQuery,
  setSearchQuery,
  isJsonFormatted,
  setIsJsonFormatted,
  formatTimestamp,
  formatContent,
  exportMessages,
  messagesEndRef,
}: {
  messages: WebSocketMessage[];
  messageFilter: "all" | "sent" | "received";
  setMessageFilter: (filter: "all" | "sent" | "received") => void;
  searchQuery: string;
  setSearchQuery: (query: string) => void;
  isJsonFormatted: boolean;
  setIsJsonFormatted: (formatted: boolean) => void;
  formatTimestamp: (timestamp: number) => string;
  formatContent: (content: string) => string;
  exportMessages: (format: "json" | "csv") => void;
  messagesEndRef: React.RefObject<HTMLDivElement>;
}) {
  if (messages.length === 0) {
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
              d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"
            />
          </svg>
        </div>
        <p className="text-sm">메시지가 없습니다</p>
        <p className="text-xs mt-1 text-gray-500 dark:text-[#6B7280]">
          연결 후 메시지를 전송하거나 수신하면 여기에 표시됩니다
        </p>
      </div>
    );
  }

  return (
    <div>
      {/* Filters & Actions */}
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <div className="flex items-center gap-2">
          <button
            onClick={() => setMessageFilter("all")}
            className={`px-3 py-1 text-xs rounded-md transition-colors ${
              messageFilter === "all"
                ? "bg-[#2563EB] text-white"
                : "bg-gray-100 dark:bg-[#0D1117] text-gray-700 dark:text-[#8B949E] hover:bg-gray-200 dark:hover:bg-[#161B22]"
            }`}
          >
            All
          </button>
          <button
            onClick={() => setMessageFilter("sent")}
            className={`px-3 py-1 text-xs rounded-md transition-colors ${
              messageFilter === "sent"
                ? "bg-[#2563EB] text-white"
                : "bg-gray-100 dark:bg-[#0D1117] text-gray-700 dark:text-[#8B949E] hover:bg-gray-200 dark:hover:bg-[#161B22]"
            }`}
          >
            Sent
          </button>
          <button
            onClick={() => setMessageFilter("received")}
            className={`px-3 py-1 text-xs rounded-md transition-colors ${
              messageFilter === "received"
                ? "bg-[#2563EB] text-white"
                : "bg-gray-100 dark:bg-[#0D1117] text-gray-700 dark:text-[#8B949E] hover:bg-gray-200 dark:hover:bg-[#161B22]"
            }`}
          >
            Received
          </button>
        </div>

        <div className="flex-1 min-w-[200px]">
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="메시지 검색..."
            className="w-full px-3 py-1 text-xs rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-500 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB]"
          />
        </div>

        <div className="flex items-center gap-2">
          <label className="flex items-center gap-2 text-xs text-gray-600 dark:text-[#8B949E]">
            <input
              type="checkbox"
              checked={isJsonFormatted}
              onChange={(e) => setIsJsonFormatted(e.target.checked)}
              className="rounded"
            />
            JSON 포맷팅
          </label>

          <div className="relative">
            <button
              onClick={() => exportMessages("json")}
              className="px-3 py-1 text-xs bg-gray-100 dark:bg-[#0D1117] text-gray-700 dark:text-[#8B949E] hover:bg-gray-200 dark:hover:bg-[#161B22] rounded-md transition-colors border border-gray-300 dark:border-[#2D333B]"
            >
              Export JSON
            </button>
          </div>
        </div>
      </div>

      {/* Message Log */}
      <div className="space-y-3 max-h-[600px] overflow-y-auto">
        {messages.map((message) => (
          <MessageBubble
            key={message.id}
            message={message}
            formatTimestamp={formatTimestamp}
            formatContent={formatContent}
          />
        ))}
        <div ref={messagesEndRef} />
      </div>
    </div>
  );
}

function MessageBubble({
  message,
  formatTimestamp,
  formatContent,
}: {
  message: WebSocketMessage;
  formatTimestamp: (timestamp: number) => string;
  formatContent: (content: string) => string;
}) {
  const isSent = message.direction === "sent";

  return (
    <div
      className={`flex ${isSent ? "justify-end" : "justify-start"}`}
    >
      <div
        className={`max-w-[70%] rounded-lg p-3 ${
          isSent
            ? "bg-blue-500 text-white"
            : "bg-gray-100 dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] border border-gray-300 dark:border-[#2D333B]"
        }`}
      >
        <div className="flex items-center gap-2 mb-2">
          <span
            className={`text-xs font-semibold px-2 py-0.5 rounded ${
              isSent
                ? "bg-blue-600 text-white"
                : "bg-gray-200 dark:bg-[#161B22] text-gray-700 dark:text-[#8B949E]"
            }`}
          >
            {isSent ? "SENT" : "RECEIVED"}
          </span>
          <span className="text-xs opacity-75">
            {formatTimestamp(message.timestamp)}
          </span>
        </div>

        <div className="text-xs font-mono mb-1 text-gray-600 dark:text-gray-400">
          {message.address}
        </div>

        <div className="text-sm whitespace-pre-wrap break-words">
          {formatContent(message.content)}
        </div>

        {message.tryId && (
          <div className="mt-2 text-xs opacity-75">
            Try ID: {message.tryId}
          </div>
        )}
      </div>
    </div>
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
  onShowTrace: (spanId?: string) => void;
  isLoadingTrace: boolean;
}) {
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
          메서드 실행 정보를 불러오려면 먼저 메시지를 전송하세요
        </p>
        <p className="text-xs mt-1 text-gray-500 dark:text-[#6B7280]">
          메시지를 전송하면 메서드별 실행 시간을 확인할 수 있습니다
        </p>
      </div>
    );
  }

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
                onClick={() => onShowTrace()}
                disabled={isLoadingTrace || !tryId}
                className="px-4 py-2 bg-blue-500 hover:bg-blue-600 disabled:bg-gray-300 dark:disabled:bg-[#2D333B] text-white rounded-md transition-colors text-sm font-medium flex items-center gap-2 disabled:cursor-not-allowed"
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
              onClick={() => onShowTrace(method.spanId)}
              className="p-4 bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] rounded-md hover:border-blue-300 dark:hover:border-blue-700 transition-colors cursor-pointer"
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
                        (param: any, paramIndex: number) => (
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

