import { useState } from "react";

interface DiffDetails {
  type: "request" | "response" | "endpoint" | "both";
  fields?: string[];
  description?: string;
}

interface DiffNotificationProps {
  diff: string; // "none", "request", "response", "endpoint", "both" 등
  onSyncToSpec?: () => void; // 명세에 자동 추가 버튼 핸들러
}

export function DiffNotification({
  diff,
  onSyncToSpec,
}: DiffNotificationProps) {
  const [isExpanded, setIsExpanded] = useState(true);

  if (!diff || diff === "none") {
    return null;
  }

  // diff 값 파싱
  const parseDiffType = (diffValue: string): DiffDetails => {
    const lowerDiff = diffValue.toLowerCase();

    if (lowerDiff.includes("both")) {
      return {
        type: "both",
        description:
          "요청(Request)과 응답(Response) 모두 명세와 실제 구현이 다릅니다.",
      };
    } else if (lowerDiff.includes("request")) {
      return {
        type: "request",
        description: "요청(Request) 부분이 명세와 실제 구현이 다릅니다.",
      };
    } else if (lowerDiff.includes("response")) {
      return {
        type: "response",
        description: "응답(Response) 부분이 명세와 실제 구현이 다릅니다.",
      };
    } else if (lowerDiff.includes("endpoint")) {
      return {
        type: "endpoint",
        description: "엔드포인트(Endpoint) 정보가 명세와 실제 구현이 다릅니다.",
      };
    }

    return {
      type: "both",
      description: "명세와 실제 구현이 일치하지 않습니다.",
    };
  };

  const diffDetails = parseDiffType(diff);

  const getTypeLabel = () => {
    switch (diffDetails.type) {
      case "request":
        return "요청 불일치";
      case "response":
        return "응답 불일치";
      case "endpoint":
        return "엔드포인트 불일치";
      case "both":
        return "요청/응답 불일치";
      default:
        return "불일치";
    }
  };

  return (
    <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-2xl p-6 shadow-lg mb-8">
      <div className="flex items-start gap-4">
        <div className="flex-shrink-0">
          <div className="w-10 h-10 rounded-lg bg-orange-100 dark:bg-orange-900/30 flex items-center justify-center">
            <svg
              className="w-5 h-5 text-orange-600 dark:text-orange-400"
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
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-3">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                명세와 실제 구현의 불일치
              </h3>
              <span className="px-2.5 py-1 rounded-lg text-xs font-medium bg-orange-100 text-orange-800 dark:bg-orange-900/50 dark:text-orange-200">
                {getTypeLabel()}
              </span>
            </div>
            <button
              onClick={() => setIsExpanded(!isExpanded)}
              className="p-1.5 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
              aria-label={isExpanded ? "접기" : "펼치기"}
            >
              <svg
                className={`w-4 h-4 text-gray-600 dark:text-gray-400 transition-transform ${
                  isExpanded ? "rotate-180" : ""
                }`}
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M19 9l-7 7-7-7"
                />
              </svg>
            </button>
          </div>

          {isExpanded && (
            <div className="space-y-4">
              <p className="text-sm text-gray-600 dark:text-gray-400">
                {diffDetails.description}
              </p>

              <div className="bg-gray-50 dark:bg-gray-700/50 rounded-xl p-4 border border-gray-200 dark:border-gray-600">
                <h4 className="text-sm font-semibold text-gray-900 dark:text-white mb-3">
                  안내사항
                </h4>
                <ul className="text-sm text-gray-600 dark:text-gray-400 space-y-2.5">
                  <li className="flex items-start gap-3">
                    <svg
                      className="w-4 h-4 text-gray-400 dark:text-gray-500 mt-0.5 flex-shrink-0"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                      />
                    </svg>
                    <span>
                      이 API는{" "}
                      <strong className="text-gray-900 dark:text-white">
                        completed
                      </strong>{" "}
                      상태로 실제 구현이 완료되었습니다.
                    </span>
                  </li>
                  <li className="flex items-start gap-3">
                    <svg
                      className="w-4 h-4 text-gray-400 dark:text-gray-500 mt-0.5 flex-shrink-0"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                      />
                    </svg>
                    <span>
                      백엔드에서{" "}
                      <code className="px-1.5 py-0.5 bg-gray-200 dark:bg-gray-600 rounded text-xs font-mono text-gray-900 dark:text-gray-100">
                        x-ouroboros-diff
                      </code>{" "}
                      어노테이션을 통해 불일치가 감지되었습니다.
                    </span>
                  </li>
                  <li className="flex items-start gap-3">
                    <svg
                      className="w-4 h-4 text-gray-400 dark:text-gray-500 mt-0.5 flex-shrink-0"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                      />
                    </svg>
                    <span>명세서의 수정 및 삭제가 제한됩니다.</span>
                  </li>
                  <li className="flex items-start gap-3">
                    <svg
                      className="w-4 h-4 text-gray-400 dark:text-gray-500 mt-0.5 flex-shrink-0"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                      />
                    </svg>
                    <span>
                      실제 구현에 존재하지만 명세에 없는 필드가 있다면, 아래
                      버튼을 클릭하여 명세에 자동으로 추가할 수 있습니다.
                    </span>
                  </li>
                </ul>
              </div>

              {onSyncToSpec && (
                <div className="flex flex-col sm:flex-row sm:items-center gap-3 pt-2">
                  <button
                    onClick={onSyncToSpec}
                    className="px-6 py-2.5 bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white rounded-xl transition-all duration-200 font-medium shadow-sm hover:shadow-md flex items-center justify-center gap-2"
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
                        d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
                      />
                    </svg>
                    실제 구현을 명세에 반영
                  </button>
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    실제 구현된 필드를 명세서에 추가합니다
                  </p>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
