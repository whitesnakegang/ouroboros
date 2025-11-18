import { useState, useEffect, useRef } from "react";
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

export function TraceModal({
  isOpen,
  onClose,
  traceData,
  initialExpandedSpanId,
}: TraceModalProps) {
  const [expandedSpans, setExpandedSpans] = useState<Set<string>>(new Set());
  const [highlightedSpanId, setHighlightedSpanId] = useState<string | null>(
    null
  );
  const highlightedSpanRef = useRef<HTMLDivElement | null>(null);
  const [visiblePathSet, setVisiblePathSet] = useState<Set<string> | null>(
    null
  );

  // spanId로 span을 찾고, 해당 span까지의 부모 path를 반환하는 함수
  const findSpanPath = (
    spans: TryTraceSpan[],
    targetSpanId: string,
    path: string[] = []
  ): string[] | null => {
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

  // (제거) 사용되지 않는 후손 수집 유틸리티 함수들

  // 모달이 닫힐 때 하이라이트 제거
  useEffect(() => {
    if (!isOpen) {
      setHighlightedSpanId(null);
    }
  }, [isOpen]);

  // initialExpandedSpanId 설정 시: 선택한 span까지의 경로만 표시하고,
  // 선택한 span 자체의 토글은 열지 않음(자식 미노출).
  useEffect(() => {
    if (initialExpandedSpanId && traceData.spans.length > 0) {
      const path = findSpanPath(traceData.spans, initialExpandedSpanId);
      if (path) {
        // 해당 span까지의 모든 부모 path (마지막 타겟 제외 = 토글 미오픈)
        const parentOnly = new Set(path.slice(0, -1));
        setExpandedSpans(parentOnly);
        // 화면에 보일 경로(부모 + 타겟)만 허용
        setVisiblePathSet(new Set(path));

        // 해당 span을 하이라이트
        setHighlightedSpanId(initialExpandedSpanId);

        // 하이라이트된 span으로 스크롤 (약간의 지연을 두어 DOM 업데이트 후 실행)
        setTimeout(() => {
          if (highlightedSpanRef.current) {
            highlightedSpanRef.current.scrollIntoView({
              behavior: "smooth",
              block: "center",
            });
          }
        }, 100);

        // 3초 후 하이라이트 제거
        const timer = setTimeout(() => {
          setHighlightedSpanId(null);
        }, 3000);

        return () => clearTimeout(timer);
      }
    } else {
      setHighlightedSpanId(null);
      setVisiblePathSet(null);
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
    // 선택 경로 모드일 때: 경로 외의 span은 렌더하지 않음
    if (visiblePathSet && !visiblePathSet.has(span.spanId)) {
      return null;
    }
    const isExpanded = expandedSpans.has(span.spanId);
    const hasChildren = span.children && span.children.length > 0;
    const isHighlighted = highlightedSpanId === span.spanId;

    return (
      <div key={span.spanId} className="mb-2">
        <div
          ref={
            isHighlighted
              ? (el) => {
                  highlightedSpanRef.current = el;
                }
              : null
          }
          className={`flex items-start gap-2 p-3 rounded-md border transition-all ${
            isHighlighted
              ? "bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800 shadow-lg ring-2 ring-blue-400 dark:ring-blue-600 ring-opacity-50"
              : "bg-gray-50 dark:bg-[#0D1117] border-gray-200 dark:border-[#2D333B]"
          } ${
            hasChildren
              ? "cursor-pointer hover:bg-gray-100 dark:hover:bg-[#161B22] transition-colors"
              : ""
          }`}
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
                  (Self: {span.selfDurationMs.toLocaleString()}ms)
                </div>
                <div className="text-xs text-gray-500 dark:text-[#6B7280] mt-1">
                  {span.percentage.toFixed(1)}% /{" "}
                  {span.selfPercentage.toFixed(1)}%
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
                <span>
                  Try ID: <span className="font-mono">{traceData.tryId}</span>
                </span>
                {traceData.traceId && (
                  <span>
                    Trace ID:{" "}
                    <span className="font-mono">{traceData.traceId}</span>
                  </span>
                )}
                <span>
                  Total Duration:{" "}
                  <span className="font-bold">
                    {traceData.totalDurationMs.toLocaleString()}ms
                  </span>
                </span>
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
                <p className="text-sm">No trace data found.</p>
                <p className="text-xs mt-1 text-gray-500 dark:text-[#6B7280]">
                  Tempo is disabled or trace not found.
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
