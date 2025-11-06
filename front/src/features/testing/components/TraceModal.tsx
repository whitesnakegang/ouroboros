import { useState, useEffect } from "react";
import type { TryTraceSpan } from "@/features/spec/services/api";

interface TraceModalProps {
  isOpen: boolean;
  onClose: () => void;
  traceData: {
    tryId: string;
    traceId: string | null;
    totalDurationMs: number;
    spans: TryTraceSpan[];
  };
  initialExpandedSpanId?: string | null;
}

export function TraceModal({ isOpen, onClose, traceData, initialExpandedSpanId }: TraceModalProps) {
  const [expandedSpans, setExpandedSpans] = useState<Set<string>>(new Set());

  // spanId로 span을 찾고, 해당 span까지의 부모 path를 반환하는 함수
  const findSpanPath = (spans: TryTraceSpan[], targetSpanId: string, path: string[] = []): string[] | null => {
    for (const span of spans) {
      const currentPath = [...path, span.spanId];
      if (span.spanId === targetSpanId) {
        return currentPath;
      }
      if (span.children && span.children.length > 0) {
        const found = findSpanPath(span.children, targetSpanId, currentPath);
        if (found) return found;
      }
    }
    return null;
  };

  // initialExpandedSpanId가 변경되면 해당 span과 부모들을 확장
  useEffect(() => {
    if (initialExpandedSpanId && traceData.spans.length > 0) {
      const path = findSpanPath(traceData.spans, initialExpandedSpanId);
      if (path) {
        // 해당 span까지의 모든 부모를 확장
        setExpandedSpans(new Set(path));
      }
    }
  }, [initialExpandedSpanId, traceData.spans]);

  if (!isOpen) return null;

  const toggleSpan = (spanId: string) => {
    const newExpanded = new Set(expandedSpans);
    if (newExpanded.has(spanId)) {
      newExpanded.delete(spanId);
    } else {
      newExpanded.add(spanId);
    }
    setExpandedSpans(newExpanded);
  };

  const renderSpan = (span: TryTraceSpan, depth: number = 0) => {
    const isExpanded = expandedSpans.has(span.spanId);
    const hasChildren = span.children && span.children.length > 0;

    return (
      <div key={span.spanId} className="mb-2">
        <div
          className={`flex items-start gap-2 p-3 rounded-md border ${
            depth === 0
              ? "bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800"
              : "bg-gray-50 dark:bg-[#0D1117] border-gray-200 dark:border-[#2D333B]"
          } ${hasChildren ? "cursor-pointer hover:bg-gray-100 dark:hover:bg-[#161B22] transition-colors" : ""}`}
          style={{ marginLeft: `${depth * 24}px` }}
          onClick={hasChildren ? () => toggleSpan(span.spanId) : undefined}
        >
          {/* Expand/Collapse Button */}
          {hasChildren && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                toggleSpan(span.spanId);
              }}
              className="mt-1 p-1 hover:bg-gray-200 dark:hover:bg-[#161B22] rounded transition-colors"
            >
              <svg
                className={`w-4 h-4 text-gray-600 dark:text-[#8B949E] transition-transform ${
                  isExpanded ? "rotate-90" : ""
                }`}
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M9 5l7 7-7 7"
                />
              </svg>
            </button>
          )}
          {!hasChildren && <div className="w-6" />}

          {/* Span Content */}
          <div className="flex-1 min-w-0">
            <div className="flex items-start justify-between gap-4 mb-2">
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3]">
                    {span.name}
                  </span>
                  <span className="px-2 py-0.5 text-xs bg-gray-200 dark:bg-[#2D333B] text-gray-700 dark:text-[#8B949E] rounded">
                    {span.kind}
                  </span>
                </div>
                <div className="text-xs text-gray-600 dark:text-[#8B949E] font-mono mb-2">
                  {span.className}
                </div>
                {span.parameters && span.parameters.length > 0 && (
                  <div className="flex flex-wrap gap-1 mb-2">
                    {span.parameters.map((param, idx) => (
                      <span
                        key={idx}
                        className="px-2 py-0.5 text-xs bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-300 rounded"
                      >
                        {param.type} {param.name}
                      </span>
                    ))}
                  </div>
                )}
              </div>

              {/* Duration Info */}
              <div className="text-right flex-shrink-0">
                <div className="text-sm font-bold text-gray-900 dark:text-[#E6EDF3]">
                  {span.durationMs.toLocaleString()}ms
                </div>
                <div className="text-xs text-gray-600 dark:text-[#8B949E]">
                  (자체: {span.selfDurationMs.toLocaleString()}ms)
                </div>
                <div className="text-xs text-gray-500 dark:text-[#6B7280] mt-1">
                  {span.percentage.toFixed(1)}% / {span.selfPercentage.toFixed(1)}%
                </div>
              </div>
            </div>

            {/* Progress Bar */}
            <div className="mt-2">
              <div className="h-2 bg-gray-200 dark:bg-[#2D333B] rounded-full overflow-hidden">
                <div
                  className="h-full bg-blue-500 dark:bg-blue-600 transition-all"
                  style={{ width: `${span.percentage}%` }}
                />
              </div>
            </div>
          </div>
        </div>

        {/* Children */}
        {hasChildren && isExpanded && (
          <div className="mt-2">
            {span.children.map((child) => renderSpan(child, depth + 1))}
          </div>
        )}
      </div>
    );
  };

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black bg-opacity-50 z-50"
        onClick={onClose}
      />

      {/* Modal */}
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div
          className="bg-white dark:bg-[#0D1117] rounded-lg shadow-2xl w-full max-w-4xl max-h-[90vh] flex flex-col"
          onClick={(e) => e.stopPropagation()}
        >
          {/* Header */}
          <div className="flex items-center justify-between p-6 border-b border-gray-200 dark:border-[#2D333B]">
            <div>
              <h2 className="text-xl font-semibold text-gray-900 dark:text-[#E6EDF3] flex items-center gap-2">
                <svg
                  className="w-6 h-6 text-blue-500"
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
                Call Trace
              </h2>
              <div className="mt-2 flex items-center gap-4 text-sm text-gray-600 dark:text-[#8B949E]">
                <span>Try ID: <span className="font-mono">{traceData.tryId}</span></span>
                {traceData.traceId && (
                  <span>Trace ID: <span className="font-mono">{traceData.traceId}</span></span>
                )}
                <span>전체 시간: <span className="font-bold">{traceData.totalDurationMs.toLocaleString()}ms</span></span>
              </div>
            </div>
            <button
              onClick={onClose}
              className="p-2 hover:bg-gray-100 dark:hover:bg-[#161B22] rounded-md transition-colors"
            >
              <svg
                className="w-6 h-6 text-gray-500 dark:text-[#8B949E]"
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
          </div>

          {/* Content */}
          <div className="flex-1 overflow-auto p-6">
            {traceData.spans.length === 0 ? (
              <div className="text-center py-12 text-gray-600 dark:text-[#8B949E]">
                <p className="text-sm">Trace 데이터가 없습니다</p>
                <p className="text-xs mt-1 text-gray-500 dark:text-[#6B7280]">
                  Tempo가 비활성화되었거나 trace를 찾지 못한 경우입니다
                </p>
              </div>
            ) : (
              <div className="space-y-2">
                {traceData.spans.map((span) => renderSpan(span, 0))}
              </div>
            )}
          </div>
        </div>
      </div>
    </>
  );
}

