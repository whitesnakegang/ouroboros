import { useSidebarStore } from "../store/sidebar.store";
import type { Endpoint } from "../store/sidebar.store";

interface EndpointCardProps {
  endpoint: Endpoint;
  filterType: "mock" | "completed";
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

// Mock 상태 표시 색상
const mockStatusColors = {
  "not-implemented": "bg-[#8B949E]",
  "in-progress": "bg-blue-500",
  modifying: "bg-orange-500",
};

// WebSocket Progress 상태 표시 색상 (점 스타일)
const wsProgressColors = {
  none: "bg-gray-400 dark:bg-gray-600",
  mock: "bg-yellow-500 dark:bg-yellow-400",
  completed: "bg-green-500 dark:bg-green-400",
};

// Progress 상태별 툴팁 텍스트
const wsProgressLabels = {
  none: "미구현",
  mock: "Mock",
  completed: "완료",
};

export function EndpointCard({ endpoint, filterType }: EndpointCardProps) {
  const { setSelectedEndpoint, selectedEndpoint } = useSidebarStore();

  const handleClick = () => {
    setSelectedEndpoint(endpoint);
  };

  const methodColor =
    methodTextColors[endpoint.method as keyof typeof methodTextColors] ||
    "text-[#8B949E]";

  const isSelected = selectedEndpoint?.id === endpoint.id;
  const isWebSocket = endpoint.protocol === "WebSocket";

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
        {!isWebSocket && filterType === "mock" && endpoint.implementationStatus && (
          <div
            className={`w-1.5 h-1.5 rounded-full mt-2 flex-shrink-0 ${
              mockStatusColors[endpoint.implementationStatus]
            }`}
            title={
              endpoint.implementationStatus === "not-implemented"
                ? "미구현"
                : endpoint.implementationStatus === "in-progress"
                ? "구현중"
                : "수정중"
            }
          />
        )}

        {/* WebSocket: Progress 상태 표시 점 */}
        {isWebSocket && endpoint.progress && wsProgressColors[endpoint.progress as keyof typeof wsProgressColors] && (
          <div
            className={`w-1.5 h-1.5 rounded-full mt-2 flex-shrink-0 ${
              wsProgressColors[endpoint.progress as keyof typeof wsProgressColors]
            }`}
            title={wsProgressLabels[endpoint.progress as keyof typeof wsProgressLabels]}
          />
        )}

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
            {(endpoint.diff && endpoint.diff !== "none") || endpoint.hasSpecError ? (
              <div className="flex-shrink-0 ml-1" title="명세와 구현이 일치하지 않습니다">
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

          {/* Summary (두 번째 줄) */}
          <p className="text-xs text-gray-600 dark:text-[#8B949E] line-clamp-1">
            {endpoint.description}
          </p>
        </div>
      </div>
    </div>
  );
}
