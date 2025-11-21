import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import type { WebSocketMessage } from "../store/testing.store";
import type { TryMethod, TryTraceData } from "@/features/spec/services/api";
import { getTryMethodList, getTryTrace } from "@/features/spec/services/api";
import { TestContent } from "./WsTestResponseTabs";
import { TraceModal } from "./TraceModal";

interface MessageDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  message: WebSocketMessage;
}

// TryMethod를 TryMethodLite로 변환하는 헬퍼 함수
function convertToTryMethodLite(methods: TryMethod[]): Array<{
  spanId?: string;
  name: string;
  className: string;
  parameters?: Array<{ type: string; name: string }>;
  selfDurationMs: number;
  selfPercentage: number;
  percentage?: number;
}> {
  return methods.map((method) => ({
    spanId: method.spanId,
    name: method.name,
    className: method.className,
    parameters: method.parameters ?? undefined,
    selfDurationMs: method.selfDurationMs,
    selfPercentage: method.selfPercentage,
  }));
}

export function MessageDetailModal({ isOpen, onClose, message }: MessageDetailModalProps) {
  const { t } = useTranslation();
  const [activeTab, setActiveTab] = useState<"message" | "test">("message");
  const [methodList, setMethodList] = useState<TryMethod[] | null>(null);
  const [totalDurationMs, setTotalDurationMs] = useState<number | null>(null);
  const [isLoadingMethods, setIsLoadingMethods] = useState(false);
  const [isTraceModalOpen, setIsTraceModalOpen] = useState(false);
  const [traceData, setTraceData] = useState<TryTraceData | null>(null);
  const [isLoadingTrace, setIsLoadingTrace] = useState(false);
  const [initialExpandedSpanId, setInitialExpandedSpanId] = useState<string | null>(null);

  // Test 탭이 활성화되고 tryId가 있을 때 메서드 리스트 로드
  useEffect(() => {
    const loadTryMethods = async () => {
      if (!message.tryId || activeTab !== "test") return;

      setIsLoadingMethods(true);
      try {
        const response = await getTryMethodList(message.tryId);
        setMethodList(response.data.methods);
        setTotalDurationMs(response.data.totalDurationMs);
            } catch (error) {
              setMethodList(null);
        setTotalDurationMs(null);
      } finally {
        setIsLoadingMethods(false);
      }
    };

    loadTryMethods();
  }, [activeTab, message.tryId]);

  // 모달이 열릴 때 탭 초기화
  useEffect(() => {
    if (isOpen) {
      setActiveTab(message.tryId ? "message" : "message");
    }
  }, [isOpen, message.tryId]);

  if (!isOpen) return null;

  const formatTimestamp = (timestamp: number): string => {
    const date = new Date(timestamp);
    const hours = date.getHours().toString().padStart(2, "0");
    const minutes = date.getMinutes().toString().padStart(2, "0");
    const seconds = date.getSeconds().toString().padStart(2, "0");
    const milliseconds = date.getMilliseconds().toString().padStart(3, "0");
    return `${hours}:${minutes}:${seconds}.${milliseconds}`;
  };

  const formatContent = (content: string): string => {
    try {
      const parsed = JSON.parse(content);
      return JSON.stringify(parsed, null, 2);
    } catch {
      return content;
    }
  };

  const isSent = message.direction === "sent";
  const isJson = (() => {
    try {
      JSON.parse(message.content);
      return true;
    } catch {
      return false;
    }
  })();

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/50 z-50"
        onClick={onClose}
      />
      
      {/* Modal */}
      <div className="fixed right-0 top-0 bottom-0 w-1/2 max-w-2xl bg-white dark:bg-[#161B22] shadow-2xl z-50 flex flex-col">
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-200 dark:border-[#2D333B] flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className={`w-3 h-3 rounded-full ${isSent ? "bg-blue-500" : "bg-green-500"}`} />
            <h2 className="text-lg font-semibold text-gray-900 dark:text-[#E6EDF3]">
              {t("messageDetail.messageDetail")}
            </h2>
          </div>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 dark:hover:bg-[#0D1117] rounded-md transition-colors"
          >
            <svg className="w-5 h-5 text-gray-500 dark:text-[#8B949E]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Tabs */}
        <div className="flex border-b border-gray-200 dark:border-[#2D333B]">
          <button
            onClick={() => setActiveTab("message")}
            className={`flex-1 px-4 py-3 text-sm font-medium transition-colors ${
              activeTab === "message"
                ? "text-[#2563EB] border-b-2 border-[#2563EB] bg-blue-50 dark:bg-blue-900/20"
                : "text-gray-600 dark:text-[#8B949E] hover:text-gray-900 dark:hover:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#0D1117]"
              }`}
          >
            {t("messageDetail.message")}
          </button>
          {message.tryId && (
            <button
              onClick={() => setActiveTab("test")}
              className={`flex-1 px-4 py-3 text-sm font-medium transition-colors ${
                activeTab === "test"
                  ? "text-[#2563EB] border-b-2 border-[#2563EB] bg-blue-50 dark:bg-blue-900/20"
                  : "text-gray-600 dark:text-[#8B949E] hover:text-gray-900 dark:hover:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#0D1117]"
              }`}
            >
              {t("messageDetail.test")}
            </button>
          )}
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6">
          {activeTab === "message" ? (
            <div className="space-y-4">
              {/* Message Info */}
              <div className="space-y-3">
                <div>
                  <label className="text-xs font-semibold text-gray-600 dark:text-[#8B949E] uppercase tracking-wide">
                    {t("messageDetail.direction")}
                  </label>
                  <div className="mt-1">
                    <span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-medium ${
                      isSent
                        ? "bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-400"
                        : "bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400"
                    }`}>
                      {isSent ? t("messageDetail.sent") : t("messageDetail.received")}
                    </span>
                  </div>
                </div>

                <div>
                  <label className="text-xs font-semibold text-gray-600 dark:text-[#8B949E] uppercase tracking-wide">
                    {t("messageDetail.address")}
                  </label>
                  <div className="mt-1 px-3 py-2 bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] rounded-md">
                    <code className="text-sm font-mono text-gray-900 dark:text-[#E6EDF3]">
                      {message.address}
                    </code>
                  </div>
                </div>

                <div>
                  <label className="text-xs font-semibold text-gray-600 dark:text-[#8B949E] uppercase tracking-wide">
                    {t("messageDetail.timestamp")}
                  </label>
                  <div className="mt-1 px-3 py-2 bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] rounded-md">
                    <span className="text-sm text-gray-900 dark:text-[#E6EDF3]">
                      {formatTimestamp(message.timestamp)}
                    </span>
                  </div>
                </div>

                {message.tryId && (
                  <div>
                    <label className="text-xs font-semibold text-gray-600 dark:text-[#8B949E] uppercase tracking-wide">
                      {t("messageDetail.tryId")}
                    </label>
                    <div className="mt-1 px-3 py-2 bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] rounded-md">
                      <code className="text-sm font-mono text-gray-900 dark:text-[#E6EDF3]">
                        {message.tryId}
                      </code>
                    </div>
                  </div>
                )}

                <div>
                  <label className="text-xs font-semibold text-gray-600 dark:text-[#8B949E] uppercase tracking-wide">
                    {t("messageDetail.content")}
                  </label>
                  <div className="mt-1">
                    {isJson ? (
                      <pre className="p-4 bg-gray-900 dark:bg-black text-gray-100 font-mono text-sm rounded-md overflow-x-auto">
                        <code>{formatContent(message.content)}</code>
                      </pre>
                    ) : (
                      <div className="p-4 bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] rounded-md">
                        <pre className="text-sm text-gray-900 dark:text-[#E6EDF3] whitespace-pre-wrap">
                          {message.content}
                        </pre>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          ) : (
            <TestContent
              methodList={methodList ? convertToTryMethodLite(methodList) : null}
              totalDurationMs={totalDurationMs}
              isMockEndpoint={false}
              isLoading={isLoadingMethods}
              tryId={message.tryId || null}
              onShowTrace={async (spanId?: string) => {
                if (!message.tryId) return;
                setIsLoadingTrace(true);
                setIsTraceModalOpen(true);
                setInitialExpandedSpanId(spanId || null);
                try {
                  const response = await getTryTrace(message.tryId);
                  setTraceData(response.data);
                      } catch (error) {
                        setTraceData(null);
                } finally {
                  setIsLoadingTrace(false);
                }
              }}
              isLoadingTrace={isLoadingTrace}
            />
          )}
        </div>
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
    </>
  );
}

