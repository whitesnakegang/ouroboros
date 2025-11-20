import { useState, useRef, useLayoutEffect } from "react";
import { useSidebarStore } from "../store/sidebar.store";
import type { Endpoint } from "../store/sidebar.store";
import { useTranslation } from "react-i18next";

interface EndpointCardProps {
  endpoint: Endpoint;
  filterType: "mock" | "completed" | "all";
}

// HTTP Method 색상 (텍스트 컬러만)
const methodTextColors = {
  GET: "text-[#10B981]",
  POST: "text-[#2563EB]",
  PUT: "text-[#F59E0B]",
  PATCH: "text-[#F59E0B]",
  DELETE: "text-red-500",
  RECEIVE: "text-[#8B5CF6]", // 보라색
  SEND: "text-[#06B6D4]", // 청록색
  DUPLEX: "text-[#EC4899]", // 핑크색 (양방향)
};

// Mock 상태 표시 색상 (육안으로 확실히 구분 가능한 색상)
const mockStatusColors = {
  "not-implemented": "bg-[#b6bdca]", // 회색 (어두운 화면에서도 구분 가능)
  "in-progress": "bg-[#EAB308]", // 노란색 (주의)
  modifying: "bg-[#F97316]", // 주황색 (주의)
};

// Completed 상태 표시 색상 (육안으로 확실히 구분 가능한 색상)
const completedStatusColor = "bg-[#25eb64]"; // 초록색 (어두운 화면에서도 구분 가능)

export function EndpointCard({ endpoint, filterType }: EndpointCardProps) {
  const { setSelectedEndpoint, selectedEndpoint } = useSidebarStore();
  const { t } = useTranslation();
  const [tooltipPosition, setTooltipPosition] = useState({ top: 0, left: 0 });
  const [showTooltip, setShowTooltip] = useState(false);
  const badgeRef = useRef<HTMLDivElement>(null);
  const tooltipRef = useRef<HTMLDivElement>(null);

  // WebSocket 상태별 색상 및 라벨 결정 함수
  const getWebSocketStatus = (tag?: string, progress?: string) => {
    const normalizedProgress = progress?.toLowerCase();
    const normalizedTag = tag?.toLowerCase();

    // tag: receive인 경우
    if (normalizedTag === "receive") {
      if (normalizedProgress === "none" || !normalizedProgress) {
        return {
          color: "bg-[#b6bdca]", // 회색
          label: t("status.notImplemented"),
        };
      } else if (normalizedProgress === "receive") {
        return {
          color: "bg-[#25eb64]", // 초록색
          label: t("status.completed"),
        };
      }
    }

    // tag: duplicate인 경우
    if (normalizedTag === "duplicate") {
      if (normalizedProgress === "none" || !normalizedProgress) {
        return {
          color: "bg-[#b6bdca]", // 회색
          label: t("status.notImplemented"),
        };
      } else if (normalizedProgress === "receive") {
        return {
          color: "bg-[#F97316]", // 주황색
          label: t("status.receiveOnlyVerified"),
        };
      } else if (
        normalizedProgress === "complete" ||
        normalizedProgress === "completed"
      ) {
        return {
          color: "bg-[#25eb64]", // 초록색
          label: t("status.completed"),
        };
      }
    }

    // 기본값 (기타 경우)
    return {
      color: "bg-[#b6bdca]", // 회색
      label: t("status.notImplemented"),
    };
  };

  const handleClick = () => {
    setSelectedEndpoint(endpoint);
  };

  const methodColor =
    methodTextColors[endpoint.method as keyof typeof methodTextColors] ||
    "text-[#8B949E]";

  const isSelected = selectedEndpoint?.id === endpoint.id;
  const isWebSocket = endpoint.protocol === "WebSocket";

  // 툴팁 위치 계산
  useLayoutEffect(() => {
    if (showTooltip && badgeRef.current && tooltipRef.current) {
      const badgeRect = badgeRef.current.getBoundingClientRect();
      const tooltipRect = tooltipRef.current.getBoundingClientRect();
      const viewportHeight = window.innerHeight;
      const viewportWidth = window.innerWidth;

      // 위쪽에 공간이 충분한지 확인
      const spaceAbove = badgeRect.top;
      const spaceBelow = viewportHeight - badgeRect.bottom;

      let top = badgeRect.top - tooltipRect.height - 8;
      let left = badgeRect.left;

      // 위쪽 공간이 부족하면 아래쪽에 표시
      if (
        spaceAbove < tooltipRect.height + 8 &&
        spaceBelow > tooltipRect.height + 8
      ) {
        top = badgeRect.bottom + 8;
      }

      // 오른쪽 경계 체크
      if (left + tooltipRect.width > viewportWidth) {
        left = viewportWidth - tooltipRect.width - 16;
      }

      // 왼쪽 경계 체크
      if (left < 16) {
        left = 16;
      }

      setTooltipPosition({ top, left });
    }
  }, [showTooltip]);

  return (
    <div
      onClick={handleClick}
      className={`px-3 py-2 mx-2 my-1 border-b border-gray-200 dark:border-[#2D333B] cursor-pointer transition-colors ${
        isSelected
          ? "bg-blue-50 dark:bg-blue-900/20 border-l-4 border-l-blue-500"
          : "hover:bg-gray-50 dark:hover:bg-[#161B22]"
      }`}
    >
      <div className="flex items-start gap-3">
        {/* REST: Mock 탭에서만 구현 상태 표시 점 */}
        {!isWebSocket &&
          filterType === "mock" &&
          endpoint.implementationStatus && (
            <div
              className={`w-1.5 h-1.5 rounded-full mt-2 flex-shrink-0 ${
                mockStatusColors[endpoint.implementationStatus]
              }`}
              title={
                endpoint.implementationStatus === "not-implemented"
                  ? t("status.notImplemented")
                  : endpoint.implementationStatus === "in-progress"
                  ? t("status.inProgress")
                  : t("status.modifying")
              }
            />
          )}

        {/* REST: All 탭에서 mock 상태의 tag와 completed 상태를 배지로 표시 */}
        {!isWebSocket && filterType === "all" && (
          <>
            {/* Mock 상태: tag 상태에 따른 배지 */}
            {(endpoint as { progress?: string }).progress?.toLowerCase() !==
              "completed" &&
              endpoint.implementationStatus && (
                <div
                  className={`w-1.5 h-1.5 rounded-full mt-2 flex-shrink-0 ${
                    mockStatusColors[endpoint.implementationStatus]
                  }`}
                  title={
                    endpoint.implementationStatus === "not-implemented"
                      ? t("status.notImplemented")
                      : endpoint.implementationStatus === "in-progress"
                      ? t("status.inProgress")
                      : t("status.modifying")
                  }
                />
              )}
            {/* Completed 상태: 초록색 배지 */}
            {(endpoint as { progress?: string }).progress?.toLowerCase() ===
              "completed" && (
              <div
                className={`w-1.5 h-1.5 rounded-full mt-2 flex-shrink-0 ${completedStatusColor}`}
                title={t("status.completed")}
              />
            )}
          </>
        )}

        {/* WebSocket: tag와 progress 조합에 따른 상태 표시 점 */}
        {isWebSocket &&
          (() => {
            const wsStatus = getWebSocketStatus(
              endpoint.tag,
              endpoint.progress
            );
            return (
              <>
                <div
                  ref={badgeRef}
                  className={`w-1.5 h-1.5 rounded-full mt-2 flex-shrink-0 ${wsStatus.color} cursor-pointer`}
                  title={wsStatus.label}
                  onMouseEnter={() => setShowTooltip(true)}
                  onMouseLeave={() => setShowTooltip(false)}
                />
                {/* 플로팅 툴팁 */}
                {showTooltip && (
                  <div
                    ref={tooltipRef}
                    className="fixed z-[9999] pointer-events-none"
                    style={{
                      top: `${tooltipPosition.top}px`,
                      left: `${tooltipPosition.left}px`,
                    }}
                  >
                    <div className="bg-white dark:bg-[#161B22] border border-gray-300 dark:border-[#2D333B] rounded-md px-3 py-2 shadow-lg min-w-[200px]">
                      <div className="text-xs font-semibold text-gray-900 dark:text-[#E6EDF3] mb-2">
                        {t("badge.badgeStatus")}
                      </div>
                      <div className="space-y-1.5">
                        <div className="flex items-center gap-2">
                          <div className="w-1.5 h-1.5 rounded-full bg-[#b6bdca] flex-shrink-0"></div>
                          <span className="text-xs text-gray-600 dark:text-[#8B949E]">
                            {t("badge.grayDescription")}
                          </span>
                        </div>
                        <div className="flex items-center gap-2">
                          <div className="w-1.5 h-1.5 rounded-full bg-[#F97316] flex-shrink-0"></div>
                          <span className="text-xs text-gray-600 dark:text-[#8B949E]">
                            {t("badge.orangeDescription")}
                          </span>
                        </div>
                        <div className="flex items-center gap-2">
                          <div className="w-1.5 h-1.5 rounded-full bg-[#25eb64] flex-shrink-0"></div>
                          <span className="text-xs text-gray-600 dark:text-[#8B949E]">
                            {t("badge.greenDescription")}
                          </span>
                        </div>
                      </div>
                    </div>
                    {/* 화살표 */}
                    <div className="absolute left-4 top-full w-0 h-0 border-l-4 border-r-4 border-t-4 border-transparent border-t-gray-300 dark:border-t-[#2D333B]"></div>
                  </div>
                )}
              </>
            );
          })()}

        <div className="flex-1 min-w-0">
          {/* Method 배지와 Path */}
          <div className="flex items-center gap-2 mb-1">
            <span
              className={`inline-flex items-center rounded-[4px] border border-gray-300 dark:border-[#2D333B] bg-white dark:bg-[#0D1117] px-1.5 py-[2px] text-[10px] font-mono font-semibold ${methodColor}`}
            >
              {endpoint.method}
            </span>
            <span className="text-sm text-gray-900 dark:text-[#E6EDF3] truncate font-mono flex-1 min-w-0">
              {endpoint.path}
            </span>

            {/* Diff 주의 표시 아이콘 */}
            {(endpoint.diff && endpoint.diff !== "none") ||
            endpoint.hasSpecError ? (
              <div
                className="flex-shrink-0 ml-1"
                title="Spec and implementation do not match"
              >
                <svg
                  className="w-3.5 h-3.5 text-amber-500"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                  />
                </svg>
              </div>
            ) : null}
          </div>

          {/* Owner (두 번째 줄) */}
        </div>
      </div>
    </div>
  );
}
