import React, { useState, useEffect, useRef } from "react";
import { useTranslation } from "react-i18next";
import { useTestingStore } from "../store/testing.store";
// removed unused useSidebarStore import
import { MessageDetailModal } from "./MessageDetailModal";
import type { WebSocketMessage } from "../store/testing.store";
// JSON 포맷팅을 위한 간단한 컴포넌트
const JsonHighlighter = ({
  code,
  isSent,
  useMonospace = true,
  softWrap = true,
}: {
  code: string;
  isSent: boolean;
  useMonospace?: boolean;
  softWrap?: boolean;
}) => {
  const formatJson = (text: string): string => {
    try {
      return JSON.stringify(JSON.parse(text), null, 2);
    } catch {
      return text;
    }
  };

  const highlightJson = (jsonStr: string): React.ReactElement => {
    const formatted = formatJson(jsonStr);
    const lines = formatted.split("\n");

    return (
      <>
        {lines.map((line, lineIndex) => {
          // 간단한 JSON 하이라이팅
          const parts: (string | React.ReactElement)[] = [];
          let lastIndex = 0;

          // 키 (따옴표로 감싸진 문자열 뒤에 콜론)
          const keyMatch = line.match(/("(?:[^"\\]|\\.)*")\s*:/);
          if (keyMatch && keyMatch.index !== undefined) {
            if (keyMatch.index > lastIndex) {
              parts.push(line.substring(lastIndex, keyMatch.index));
            }
            parts.push(
              <span
                key={`key-${lineIndex}`}
                className={isSent ? "text-blue-300" : "text-blue-400"}
              >
                {keyMatch[0]}
              </span>
            );
            lastIndex = keyMatch.index + keyMatch[0].length;
          }

          // 문자열 값
          const stringValueMatch = line.match(/:\s*("(?:[^"\\]|\\.)*")/);
          if (
            stringValueMatch &&
            stringValueMatch.index !== undefined &&
            stringValueMatch.index >= lastIndex
          ) {
            if (stringValueMatch.index > lastIndex) {
              parts.push(line.substring(lastIndex, stringValueMatch.index));
            }
            parts.push(
              <span
                key={`str-${lineIndex}`}
                className={isSent ? "text-green-300" : "text-green-400"}
              >
                {stringValueMatch[0]}
              </span>
            );
            lastIndex = stringValueMatch.index + stringValueMatch[0].length;
          }

          // 숫자 값
          const numberMatch = line.match(/:\s*(\d+\.?\d*)/);
          if (
            numberMatch &&
            numberMatch.index !== undefined &&
            numberMatch.index >= lastIndex
          ) {
            if (numberMatch.index > lastIndex) {
              parts.push(line.substring(lastIndex, numberMatch.index));
            }
            parts.push(
              <span
                key={`num-${lineIndex}`}
                className={isSent ? "text-yellow-300" : "text-yellow-400"}
              >
                {numberMatch[0]}
              </span>
            );
            lastIndex = numberMatch.index + numberMatch[0].length;
          }

          // boolean/null 값
          const boolMatch = line.match(/:\s*(true|false|null)\b/);
          if (
            boolMatch &&
            boolMatch.index !== undefined &&
            boolMatch.index >= lastIndex
          ) {
            if (boolMatch.index > lastIndex) {
              parts.push(line.substring(lastIndex, boolMatch.index));
            }
            parts.push(
              <span
                key={`bool-${lineIndex}`}
                className={isSent ? "text-green-300" : "text-green-400"}
              >
                {boolMatch[0]}
              </span>
            );
            lastIndex = boolMatch.index + boolMatch[0].length;
          }

          if (lastIndex < line.length) {
            parts.push(line.substring(lastIndex));
          }

          return (
            <span key={lineIndex}>
              {parts.length > 0 ? parts : line}
              {lineIndex < lines.length - 1 && "\n"}
            </span>
          );
        })}
      </>
    );
  };

  const fontClass = useMonospace ? "font-mono" : "font-sans";
  const wrapClass = softWrap
    ? "whitespace-pre-wrap break-words"
    : "whitespace-pre";

  return (
    <pre
      tabIndex={0}
      className={`p-3 text-sm ${fontClass} ${wrapClass} overflow-x-auto rounded-lg ${
        isSent ? "bg-black/30 text-white" : "bg-black/20 text-white/90"
      } focus-visible:outline-none focus-visible:ring-0`}
    >
      <code>{highlightJson(code)}</code>
    </pre>
  );
};

export function WsTestResponseTabs() {
  const { t } = useTranslation();
  const { wsMessages, wsConnectionStatus, wsStats, wsConnectionStartTime } =
    useTestingStore();
  // removed unused selectedEndpoint

  const [messageFilter, setMessageFilter] = useState<
    "all" | "sent" | "received"
  >("all");
  const [searchQuery, setSearchQuery] = useState<string>(() => {
    try {
      return localStorage.getItem("ws-log-search") || "";
    } catch {
      return "";
    }
  });
  const [isJsonFormatted, setIsJsonFormatted] = useState(true);
  const [selectedMessage, setSelectedMessage] =
    useState<WebSocketMessage | null>(null);
  const [isMessageModalOpen, setIsMessageModalOpen] = useState(false);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  // 메시지 로그 자동 스크롤
  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [wsMessages]);

  // 검색어 저장 (항상 입력창 유지 및 복원)
  useEffect(() => {
    try {
      localStorage.setItem("ws-log-search", searchQuery);
    } catch {
      // no-op: localStorage not available
    }
  }, [searchQuery]);

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

  // 받은 값과 보낸 값 분리
  const receivedMessages = filteredMessages.filter(
    (msg) => msg.direction === "received"
  );
  const sentMessages = filteredMessages.filter(
    (msg) => msg.direction === "sent"
  );

  const handleMessageClick = (message: WebSocketMessage) => {
    setSelectedMessage(message);
    setIsMessageModalOpen(true);
  };

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

  // JSON 포맷팅은 렌더 단계에서 처리

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
      {/* Log Header */}
      <div className="px-4 py-3 border-b border-gray-200 dark:border-[#2D333B] bg-gray-50 dark:bg-[#0D1117]">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <span className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3]">
              {t("wsTest.log")}
            </span>
            {wsConnectionStatus === "connected" ? (
              <span className="px-2 py-0.5 text-xs font-medium bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400 rounded-full flex items-center gap-1.5">
                <div className="w-1.5 h-1.5 bg-green-500 rounded-full animate-pulse"></div>
                {t("wsTest.connected")}
              </span>
            ) : wsConnectionStatus === "connecting" ? (
              <span className="px-2 py-0.5 text-xs font-medium bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-400 rounded-full flex items-center gap-1.5">
                <div className="w-1.5 h-1.5 bg-yellow-500 rounded-full animate-pulse"></div>
                {t("wsTest.connecting")}
              </span>
            ) : (
              <span className="px-2 py-0.5 text-xs font-medium bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-400 rounded-full">
                {t("wsTest.disconnected")}
              </span>
            )}
            {wsConnectionStatus === "connected" && wsConnectionStartTime && (
              <span className="text-xs text-gray-600 dark:text-[#8B949E]">
                {formatConnectionDuration(wsStats.connectionDuration)}
              </span>
            )}
          </div>
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-3 text-xs text-gray-600 dark:text-[#8B949E]">
              <div className="flex items-center gap-1">
                <svg
                  className="w-3.5 h-3.5 text-blue-500"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M7 11l5-5m0 0l5 5m-5-5v12"
                  />
                </svg>
                <span className="font-medium">{wsStats.totalSent}</span>
              </div>
              <div className="w-px h-4 bg-gray-300 dark:bg-[#2D333B]"></div>
              <div className="flex items-center gap-1">
                <svg
                  className="w-3.5 h-3.5 text-green-500"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M17 13l-5 5m0 0l-5-5m5 5V6"
                  />
                </svg>
                <span className="font-medium">{wsStats.totalReceived}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Log Content */}
      <div className="p-4">
        <ResponseContent
          receivedMessages={receivedMessages}
          sentMessages={sentMessages}
          messageFilter={messageFilter}
          setMessageFilter={setMessageFilter}
          searchQuery={searchQuery}
          setSearchQuery={setSearchQuery}
          isJsonFormatted={isJsonFormatted}
          setIsJsonFormatted={setIsJsonFormatted}
          formatTimestamp={formatTimestamp}
          exportMessages={exportMessages}
          messagesEndRef={messagesEndRef}
          onMessageClick={handleMessageClick}
          t={t}
        />
      </div>

      {/* Message Detail Modal */}
      {selectedMessage && (
        <MessageDetailModal
          isOpen={isMessageModalOpen}
          onClose={() => {
            setIsMessageModalOpen(false);
            setSelectedMessage(null);
          }}
          message={selectedMessage}
        />
      )}
    </div>
  );
}

// 메시지 그룹핑 관련 상수
const GAP_THRESHOLD_MS = 30000;
const GROUP_MIN_COUNT = 3;

// Divider 생성 함수
function createDivider(
  message: WebSocketMessage,
  prev: WebSocketMessage,
  formatTimestamp: (timestamp: number) => string
): React.ReactElement {
  const delta = message.timestamp - prev.timestamp;
  const sec = Math.floor(delta / 1000);
  const min = Math.floor(sec / 60);
  const gapLabel = min > 0 ? `+${min}m ${sec % 60}s` : `+${sec}s`;

  return (
    <div key={`divider-${message.id}`} className="flex items-center gap-3 my-2">
      <div className="h-px flex-1 bg-gray-200 dark:bg-[#2D333B]" />
      <span className="text-[10px] text-gray-500 dark:text-[#8B949E] px-2 py-0.5 rounded-full bg-gray-50 dark:bg-[#0D1117] border border-gray-200 dark:border-[#2D333B]">
        {formatTimestamp(message.timestamp)} {gapLabel}
      </span>
      <div className="h-px flex-1 bg-gray-200 dark:bg-[#2D333B]" />
    </div>
  );
}

// Group Header 생성 함수
function createGroupHeader(
  message: WebSocketMessage,
  runCount: number,
  t: (key: string, options?: any) => string
): React.ReactElement {
  return (
    <div
      key={`group-${message.id}`}
      className="mt-2 mb-1 flex items-center gap-2"
    >
      <span
        className={`text-[11px] font-semibold ${
          message.tryId
            ? "text-green-700 dark:text-green-400"
            : message.direction === "sent"
            ? "text-blue-700 dark:text-blue-400"
            : "text-green-700 dark:text-green-400"
        }`}
      >
        {message.direction === "sent" ? t("wsTest.sent") : t("wsTest.received")} · {runCount}
      </span>
      <div className="h-px flex-1 bg-gray-200 dark:bg-[#2D333B]" />
    </div>
  );
}

// Divider 삽입 여부 확인
function shouldInsertDivider(
  current: WebSocketMessage,
  prev: WebSocketMessage
): boolean {
  return current.timestamp - prev.timestamp >= GAP_THRESHOLD_MS;
}

// Group Header 삽입 여부 확인
function shouldInsertGroupHeader(
  index: number,
  messages: WebSocketMessage[]
): boolean {
  if (index === 0) {
    return countGroupRun(index, messages) >= GROUP_MIN_COUNT;
  }
  return (
    messages[index - 1].direction !== messages[index].direction &&
    countGroupRun(index, messages) >= GROUP_MIN_COUNT
  );
}

// 같은 방향으로 연속된 메시지 개수 계산
function countGroupRun(
  startIndex: number,
  messages: WebSocketMessage[]
): number {
  let count = 1;
  const direction = messages[startIndex].direction;
  for (let j = startIndex + 1; j < messages.length; j++) {
    if (messages[j].direction === direction) {
      count++;
    } else {
      break;
    }
  }
  return count;
}

// 메시지 행 빌드 함수
function buildMessageRows(
  allMessages: WebSocketMessage[],
  formatTimestamp: (timestamp: number) => string,
  isJsonFormatted: boolean,
  isCompact: boolean,
  onMessageClick: (message: WebSocketMessage) => void,
  t: (key: string, options?: any) => string
): React.ReactElement[] {
  const rows: React.ReactElement[] = [];

  for (let i = 0; i < allMessages.length; i++) {
    const message = allMessages[i];
    const prev = i > 0 ? allMessages[i - 1] : null;

    // Divider 추가
    if (prev && shouldInsertDivider(message, prev)) {
      rows.push(createDivider(message, prev, formatTimestamp));
    }

    // 그룹 헤더 추가
    if (shouldInsertGroupHeader(i, allMessages)) {
      const runCount = countGroupRun(i, allMessages);
      rows.push(createGroupHeader(message, runCount, t));
    }

    // 메시지 추가
    rows.push(
      <MessageBubble
        key={message.id}
        message={message}
        formatTimestamp={formatTimestamp}
        prevTimestamp={prev?.timestamp ?? null}
        isJsonFormatted={isJsonFormatted}
        compactMode={isCompact}
        onClick={() => onMessageClick(message)}
        t={t}
      />
    );
  }

  return rows;
}

function ResponseContent({
  receivedMessages,
  sentMessages,
  messageFilter,
  setMessageFilter,
  searchQuery,
  setSearchQuery,
  isJsonFormatted,
  setIsJsonFormatted,
  formatTimestamp,
  exportMessages,
  messagesEndRef,
  onMessageClick,
  t,
}: {
  receivedMessages: WebSocketMessage[];
  sentMessages: WebSocketMessage[];
  messageFilter: "all" | "sent" | "received";
  setMessageFilter: (filter: "all" | "sent" | "received") => void;
  searchQuery: string;
  setSearchQuery: (query: string) => void;
  isJsonFormatted: boolean;
  setIsJsonFormatted: (formatted: boolean) => void;
  formatTimestamp: (timestamp: number) => string;
  exportMessages: (format: "json" | "csv") => void;
  messagesEndRef: React.RefObject<HTMLDivElement | null>;
  onMessageClick: (message: WebSocketMessage) => void;
  t: (key: string, options?: any) => string;
}) {
  const allMessages = [...sentMessages, ...receivedMessages].sort(
    (a, b) => a.timestamp - b.timestamp
  );
  const isCompact = allMessages.length >= 200;

  return (
    <div>
      {/* Filters & Actions Bar (상단 고정 해제) */}
      <div className="mb-4 bg-gray-50 dark:bg-[#0D1117] border border-gray-200 dark:border-[#2D333B] rounded-md p-3">
        <div className="flex flex-wrap items-center gap-3">
          {/* Search Bar */}
          <div className="flex-1 min-w-[200px] relative">
            <svg
              className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 dark:text-[#8B949E]"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
              />
            </svg>
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Escape") setSearchQuery("");
              }}
              placeholder={t("wsTest.searchMessages")}
              className="w-full pl-9 pr-9 py-2 text-sm rounded-md bg-white dark:bg-[#161B22] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-500 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-2 focus:ring-gray-400 dark:focus:ring-gray-500 focus:border-transparent"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery("")}
                className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:text-[#8B949E]"
                title="Clear search (Esc)"
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
                    d="M6 18L18 6M6 6l12 12"
                  />
                </svg>
              </button>
            )}
          </div>

          {/* Filter Buttons */}
          <div className="flex items-center gap-1 bg-white dark:bg-[#161B22] border border-gray-300 dark:border-[#2D333B] rounded-md p-1">
            <button
              onClick={() => setMessageFilter("all")}
              className={`px-3 py-1.5 text-xs font-medium rounded transition-colors ${
                messageFilter === "all"
                  ? "bg-[#2563EB] text-white"
                  : "text-gray-700 dark:text-[#8B949E] hover:bg-gray-100 dark:hover:bg-[#0D1117]"
              }`}
            >
              {t("wsTest.allMessages")}
            </button>
            <button
              onClick={() => setMessageFilter("sent")}
              className={`px-3 py-1.5 text-xs font-medium rounded transition-colors flex items-center gap-1.5 ${
                messageFilter === "sent"
                  ? "bg-[#2563EB] text-white"
                  : "text-gray-700 dark:text-[#8B949E] hover:bg-gray-100 dark:hover:bg-[#0D1117]"
              }`}
            >
              <svg
                className="w-3 h-3"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M7 11l5-5m0 0l5 5m-5-5v12"
                />
              </svg>
              {t("wsTest.sent")}
            </button>
            <button
              onClick={() => setMessageFilter("received")}
              className={`px-3 py-1.5 text-xs font-medium rounded transition-colors flex items-center gap-1.5 ${
                messageFilter === "received"
                  ? "bg-[#2563EB] text-white"
                  : "text-gray-700 dark:text-[#8B949E] hover:bg-gray-100 dark:hover:bg-[#0D1117]"
              }`}
            >
              <svg
                className="w-3 h-3"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M17 13l-5 5m0 0l-5-5m5 5V6"
                />
              </svg>
              {t("wsTest.received")}
            </button>
          </div>

          {/* Options */}
          <div className="flex items-center gap-2 ml-auto">
            <div className="inline-flex rounded-md border border-gray-300 dark:border-[#2D333B] overflow-hidden">
              <button
                onClick={() => setIsJsonFormatted(true)}
                className={`px-3 py-1.5 text-xs ${
                  isJsonFormatted
                    ? "bg-[#2563EB] text-white"
                    : "bg-white dark:bg-[#161B22] text-gray-700 dark:text-[#8B949E]"
                }`}
              >
                {t("wsTest.pretty")}
              </button>
              <button
                onClick={() => setIsJsonFormatted(false)}
                className={`px-3 py-1.5 text-xs ${
                  !isJsonFormatted
                    ? "bg-[#2563EB] text-white"
                    : "bg-white dark:bg-[#161B22] text-gray-700 dark:text-[#8B949E]"
                }`}
              >
                {t("wsTest.raw")}
              </button>
            </div>

            <button
              onClick={() => exportMessages("json")}
              className="px-3 py-1.5 text-xs font-medium bg-white dark:bg-[#161B22] text-gray-700 dark:text-[#8B949E] hover:bg-gray-100 dark:hover:bg-[#0D1117] rounded-md transition-colors border border-gray-300 dark:border-[#2D333B] flex items-center gap-1.5"
            >
              <svg
                className="w-3.5 h-3.5"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"
                />
              </svg>
              {t("wsTest.export")}
            </button>
          </div>
        </div>
      </div>

      {/* Message Log 또는 빈 상태 */}
      {allMessages.length === 0 ? (
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
          <p className="text-sm">{t("wsTest.noMessages")}</p>
          <p className="text-xs mt-1 text-gray-500 dark:text-[#6B7280]">
            {searchQuery ? (
              <>
                {t("wsTest.noMessagesFoundFor", { query: searchQuery })}
              </>
            ) : (
              <>{t("wsTest.messagesWillBeDisplayed")}</>
            )}
          </p>
          {(searchQuery || messageFilter !== "all") && (
            <div className="mt-3 flex items-center justify-center gap-2">
              {searchQuery && (
                <button
                  onClick={() => setSearchQuery("")}
                  className="px-3 py-1.5 text-xs border rounded-md"
                  title={t("wsTest.clearSearch")}
                >
                  {t("wsTest.clearSearch")}
                </button>
              )}
              {messageFilter !== "all" && (
                <button
                  onClick={() => setMessageFilter("all")}
                  className="px-3 py-1.5 text-xs border rounded-md"
                  title={t("wsTest.resetFilter")}
                >
                  {t("wsTest.resetFilter")}
                </button>
              )}
            </div>
          )}
        </div>
      ) : (
        <div className="space-y-2 max-h-[600px] overflow-y-auto px-2">
          {buildMessageRows(
            allMessages,
            formatTimestamp,
            isJsonFormatted,
            isCompact,
            onMessageClick,
            t
          )}
          <div ref={messagesEndRef} />
        </div>
      )}
    </div>
  );
}

function MessageBubble({
  message,
  formatTimestamp,
  prevTimestamp = null,
  isJsonFormatted = true,
  useMonospace = true,
  softWrap = true,
  compactMode = false,
  onClick,
  t,
}: {
  message: WebSocketMessage;
  formatTimestamp: (timestamp: number) => string;
  prevTimestamp?: number | null;
  isJsonFormatted?: boolean;
  useMonospace?: boolean;
  softWrap?: boolean;
  compactMode?: boolean;
  onClick?: () => void;
  t: (key: string, options?: any) => string;
}) {
  const isSent = message.direction === "sent";
  const isTryMessage = !!message.tryId; // tryId가 있으면 Try 메시지로 구분
  const isJson = (() => {
    try {
      JSON.parse(message.content);
      return true;
    } catch {
      return false;
    }
  })();

  const relativeBadge = (() => {
    if (!prevTimestamp) return null;
    const deltaMs = message.timestamp - prevTimestamp;
    if (deltaMs < 0) return null;
    const sec = (deltaMs / 1000).toFixed(1);
    return (
      <span className="text-[10px] px-1.5 py-0.5 rounded-full bg-gray-100 dark:bg-[#0D1117] text-gray-600 dark:text-[#8B949E]">
        +{sec}s
      </span>
    );
  })();

  // /queue/ouro/try 또는 /user/queue/ouro/try로 send할 때만 메서드 실행 시간 버튼 표시
  const showMethodButton = 
    isSent && 
    message.tryId && 
    (message.address === "/queue/ouro/try" || message.address === "/user/queue/ouro/try");

  return (
    <div className={`group relative flex ${isSent ? "justify-end" : "justify-start"} mb-3`}>
      <div
        onClick={onClick}
        tabIndex={0}
        onKeyDown={(e) => {
          if (onClick && (e.key === "Enter" || e.key === " ")) {
            e.preventDefault();
            onClick();
          }
        }}
        className={`relative max-w-[85%] ${
          compactMode ? "px-3 py-2" : "px-4 py-3"
        } rounded-2xl border transition-all hover:shadow-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 ${
          onClick ? "cursor-pointer active:scale-[0.98]" : ""
        } ${
          isTryMessage
            ? "bg-green-100 dark:bg-green-900/20 border-green-300 dark:border-green-700/50"
            : isSent
            ? "bg-blue-100 dark:bg-blue-900/20 border-blue-300 dark:border-blue-700/50"
            : "bg-green-100 dark:bg-green-900/20 border-green-300 dark:border-green-700/50"
        }`}
      >
        {/* Header - RECEIVE/SEND 배지와 타임스탬프 */}
        <div className={`flex items-center gap-2 mb-2 ${isSent ? "justify-end" : "justify-start"}`}>
          <span className={`text-[10px] font-semibold px-1.5 py-0.5 rounded ${
            isSent 
              ? "bg-blue-500 text-white"
              : "bg-green-500 text-white"
          }`}>
            {isSent ? t("wsTest.send") : t("wsTest.receive")}
            </span>
          <span className="text-[10px] font-medium text-gray-600 dark:text-[#8B949E]">
              {formatTimestamp(message.timestamp)}
            </span>
            {relativeBadge}
          {showMethodButton && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                if (onClick) onClick();
              }}
              className="text-[10px] px-2 py-0.5 rounded bg-purple-500 hover:bg-purple-600 text-white font-medium transition-colors"
              title="메서드 실행 시간 보기"
            >
              ⏱️
            </button>
            )}
          </div>

        {/* Destination/Address - 작고 간단하게 */}
        {message.address && message.address !== "CONNECTED" && message.address !== "ERROR" && message.address !== "WARNING" && message.address !== "INFO" && (
          <div className={`mb-2 ${isSent ? "text-right" : "text-left"}`}>
            <span className="text-[10px] font-mono text-gray-600 dark:text-[#8B949E]">
              {message.address}
            </span>
          </div>
        )}

          {/* Body */}
          <div className="relative">
            {isJson && isJsonFormatted ? (
            <div className="rounded-lg overflow-hidden bg-white/90 dark:bg-black/40 border border-gray-200 dark:border-[#2D333B]">
                <JsonHighlighter
                  code={message.content}
                  isSent={isSent}
                  useMonospace={useMonospace}
                  softWrap={softWrap}
                />
              </div>
            ) : (
              <div
                className={`${
                compactMode ? "text-xs" : "text-sm"
              } font-mono whitespace-pre-wrap break-words text-gray-900 dark:text-[#E6EDF3]`}
              >
                {message.content || (
                <span className="text-gray-500 dark:text-[#8B949E] italic">(empty)</span>
                )}
              </div>
            )}
        </div>
      </div>
    </div>
  );
}

interface TryMethodLite {
  spanId?: string;
  name: string;
  className: string;
  parameters?: Array<{ type: string; name: string }>;
  selfDurationMs: number;
  selfPercentage: number;
  percentage?: number;
}

export function TestContent({
  methodList,
  totalDurationMs,
  isMockEndpoint,
  isLoading,
  tryId,
  onShowTrace,
  isLoadingTrace,
}: {
  methodList: TryMethodLite[] | null;
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
          Mock endpoints do not provide method execution information
        </p>
        <p className="text-xs mt-1 text-gray-500 dark:text-[#6B7280]">
          Method execution time can only be checked for actually implemented endpoints (completed)
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
          Please send a message first to load method execution information
        </p>
        <p className="text-xs mt-1 text-gray-500 dark:text-[#6B7280]">
          You can check method execution time by sending a message
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
        <p className="text-sm">Loading method execution information...</p>
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
        <p className="text-sm">No method execution information found</p>
        <p className="text-xs mt-1 text-gray-500 dark:text-[#6B7280]">
          You can check method execution time by running in Trace mode
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
                Total Duration
              </div>
              <div className="text-2xl font-bold text-gray-900 dark:text-[#E6EDF3]">
                {totalDurationMs.toLocaleString()}ms
              </div>
            </div>
            <div className="flex items-center gap-3">
              <button
                onClick={() => onShowTrace()}
                disabled={isLoadingTrace || !tryId}
                className="px-4 py-2 bg-gray-600 hover:bg-gray-700 disabled:bg-gray-300 dark:disabled:bg-[#2D333B] text-white rounded-md transition-all active:translate-y-[1px] focus:outline-none focus-visible:outline-none focus:ring-0 text-sm font-medium flex items-center gap-2 disabled:cursor-not-allowed"
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
                    Loading...
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
                    View Call Trace
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
          Method Execution Time ({methodList.length} methods)
        </div>
        <div className="space-y-2">
          {methodList.map((method: TryMethodLite, index: number) => (
            <div
              key={method.spanId || index}
              onClick={() => onShowTrace(method.spanId)}
              className="p-4 bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] rounded-md hover:border-gray-400 dark:hover:border-gray-600 transition-colors cursor-pointer focus:outline-none focus:ring-0"
            >
              <div className="flex items-start justify-between mb-2 gap-4">
                <div className="flex-1 min-w-0">
                  <div className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3] mb-1 break-words">
                    {method.name}
                  </div>
                  <div className="text-xs text-gray-600 dark:text-[#8B949E] font-mono break-words">
                    {method.className}
                  </div>
                  {method.parameters && method.parameters.length > 0 && (
                    <div className="mt-2 flex flex-wrap gap-1">
                      {method.parameters.map(
                        (
                          param: { type: string; name: string },
                          paramIndex: number
                        ) => (
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
                <div className="ml-4 text-right flex-shrink-0 whitespace-nowrap">
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
