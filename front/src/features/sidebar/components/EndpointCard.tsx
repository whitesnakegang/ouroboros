import { useSidebarStore } from "../store/sidebar.store";

interface Endpoint {
  id: string;
  method: string;
  path: string;
  description: string;
  implementationStatus?: "not-implemented" | "in-progress" | "modifying";
  hasSpecError?: boolean;
  progress?: string;
  tag?: string;
  diff?: string;
}

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
};

// Mock 상태 표시 색상
const mockStatusColors = {
  "not-implemented": "bg-[#8B949E]",
  "in-progress": "bg-blue-500",
  modifying: "bg-orange-500",
};

export function EndpointCard({ endpoint, filterType }: EndpointCardProps) {
  const { setSelectedEndpoint } = useSidebarStore();

  const handleClick = () => {
    setSelectedEndpoint(endpoint);
  };

  const methodColor =
    methodTextColors[endpoint.method as keyof typeof methodTextColors] ||
    "text-[#8B949E]";

  return (
    <div
      onClick={handleClick}
      className="px-3 py-2 mx-2 my-1 border-b border-gray-200 dark:border-[#2D333B] cursor-pointer hover:bg-gray-50 dark:hover:bg-[#161B22] transition-colors"
    >
      <div className="flex items-start gap-3">
        {/* Mock 탭: 구현 상태 표시 점 (왼쪽) */}
        {filterType === "mock" && endpoint.implementationStatus && (
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

        {/* Completed 탭: 오류 또는 불일치 표시 아이콘 (왼쪽) */}
        {filterType === "completed" && (endpoint.hasSpecError || (endpoint.diff && endpoint.diff !== "none")) && (
          <div className="mt-1.5 flex-shrink-0">
            {endpoint.diff && endpoint.diff !== "none" ? (
              <svg
                className="w-3.5 h-3.5 text-amber-500"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
                title="명세와 구현이 일치하지 않습니다"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                />
              </svg>
            ) : (
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
            )}
          </div>
        )}

        <div className="flex-1 min-w-0">
          {/* Method 배지와 Path */}
          <div className="flex items-center gap-2 mb-1">
            <span
              className={`inline-flex items-center rounded-[4px] border border-gray-300 dark:border-[#2D333B] bg-white dark:bg-[#0D1117] px-1.5 py-[2px] text-[10px] font-mono font-semibold ${methodColor}`}
            >
              {endpoint.method}
            </span>
            <span className="text-sm text-gray-900 dark:text-[#E6EDF3] truncate font-mono">
              {endpoint.path}
            </span>
          </div>

          {/* Description */}
          <p className="text-xs text-gray-600 dark:text-[#8B949E] line-clamp-1">
            {endpoint.description}
          </p>
        </div>
      </div>
    </div>
  );
}
