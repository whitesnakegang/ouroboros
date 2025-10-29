import { useSidebarStore } from "../store/sidebar.store";

interface Endpoint {
  id: string;
  method: string;
  path: string;
  description: string;
  implementationStatus?: "not-implemented" | "in-progress" | "modifying";
  hasSpecError?: boolean;
}

interface EndpointCardProps {
  endpoint: Endpoint;
  filterType: "mock" | "completed";
}

// HTTP Method 색상
const methodBadgeColors = {
  GET: "bg-blue-100 text-blue-700 border-blue-200",
  POST: "bg-green-100 text-green-700 border-green-200",
  PUT: "bg-orange-100 text-orange-700 border-orange-200",
  DELETE: "bg-red-100 text-red-700 border-red-200",
  PATCH: "bg-purple-100 text-purple-700 border-purple-200",
};

// Mock 상태 표시 색상
const mockStatusColors = {
  "not-implemented": "bg-gray-500",
  "in-progress": "bg-yellow-500",
  modifying: "bg-orange-500",
};

export function EndpointCard({ endpoint, filterType }: EndpointCardProps) {
  const { setSelectedEndpoint } = useSidebarStore();

  const handleClick = () => {
    setSelectedEndpoint(endpoint);
  };

  const methodColor =
    methodBadgeColors[endpoint.method as keyof typeof methodBadgeColors] ||
    "bg-gray-100 text-gray-700 border-gray-200";

  return (
    <div
      onClick={handleClick}
      className="p-3 mx-2 my-2 rounded-lg bg-white dark:bg-gray-700 cursor-pointer hover:shadow-md transition-shadow border border-gray-200 dark:border-gray-600"
    >
      <div className="flex items-start gap-2">
        {/* Mock 탭: 구현 상태 표시 점 (왼쪽) */}
        {filterType === "mock" && endpoint.implementationStatus && (
          <div
            className={`w-2 h-2 rounded-full mt-2 ${
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

        {/* Completed 탭: 오류 표시 아이콘 (왼쪽) */}
        {filterType === "completed" && endpoint.hasSpecError && (
          <div className="w-2 h-2 rounded-full mt-2 flex-shrink-0">
            <svg
              className="w-4 h-4 text-yellow-500"
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
        )}

        <div className="flex-1 min-w-0">
          {/* Method 배지와 Path */}
          <div className="flex items-center gap-2 mb-1">
            <span
              className={`px-1.5 py-0.5 text-xs font-semibold rounded border ${methodColor} dark:opacity-90`}
            >
              {endpoint.method}
            </span>
            <span className="text-sm text-gray-700 dark:text-gray-300 truncate">
              {endpoint.path}
            </span>
          </div>

          {/* Description */}
          <p className="text-xs text-gray-600 dark:text-gray-400">
            {endpoint.description}
          </p>
        </div>
      </div>
    </div>
  );
}
