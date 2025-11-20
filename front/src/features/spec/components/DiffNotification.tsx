// removed unused UI imports
import { useTranslation } from "react-i18next";

interface DiffDetails {
  type: "request" | "response" | "endpoint" | "both";
  fields?: string[];
  description?: string;
}

interface DiffNotificationProps {
  diff: string; // "none", "request", "response", "endpoint", "both" 등
  progress?: string; // "mock", "completed" 등
  onSyncToSpec?: () => void; // 명세에 자동 추가 버튼 핸들러
  reqLog?: string; // x-ouroboros-req-log 헤더 값
  resLog?: string; // x-ouroboros-res-log 헤더 값
}

export function DiffNotification({
  diff,
  progress,
  onSyncToSpec,
  reqLog,
  resLog,
}: DiffNotificationProps) {
  const { t } = useTranslation();
  
  if (!diff || diff === "none") {
    return null;
  }

  // diff 값 파싱
  const parseDiffType = (diffValue: string): DiffDetails => {
    const lowerDiff = diffValue.toLowerCase();

    if (lowerDiff.includes("both")) {
      return {
        type: "both",
        description: t("diffNotification.bothDescription"),
      };
    } else if (lowerDiff.includes("request")) {
      return {
        type: "request",
        description: t("diffNotification.requestDescription"),
      };
    } else if (lowerDiff.includes("response")) {
      return {
        type: "response",
        description: t("diffNotification.responseDescription"),
      };
    } else if (lowerDiff.includes("endpoint")) {
      return {
        type: "endpoint",
        description: t("diffNotification.endpointDescription"),
      };
    }

    return {
      type: "both",
      description: t("diffNotification.defaultDescription"),
    };
  };

  const diffDetails = parseDiffType(diff);

  // progress 상태 확인 (기본값: "mock")
  const progressLower = progress?.toLowerCase() || "mock";
  const isCompleted = progressLower === "completed";

  const getTypeLabel = () => {
    switch (diffDetails.type) {
      case "request":
        return t("diffNotification.requestDiff");
      case "response":
        return t("diffNotification.responseDiff");
      case "endpoint":
        return t("diffNotification.endpointDiff");
      case "both":
        return t("diffNotification.bothDiff");
      default:
        return t("diffNotification.diff");
    }
  };

  return (
    <div className="rounded-md border border-amber-300 dark:border-amber-700 bg-amber-50 dark:bg-amber-900/20 shadow-sm mb-6">
      {/* 헤더 */}
      <div className="p-4 border-b border-amber-200 dark:border-amber-800">
        <div className="flex items-start gap-3">
          <svg
            className="w-5 h-5 text-amber-600 dark:text-amber-400 flex-shrink-0 mt-0.5"
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
          <div className="flex-1">
            <div className="flex items-center gap-2 mb-1">
              <h3 className="text-sm font-semibold text-amber-800 dark:text-amber-300">
                {t("diffNotification.title")}
              </h3>
              <span className="px-2 py-0.5 bg-amber-200 dark:bg-amber-800 text-amber-800 dark:text-amber-200 text-xs font-medium rounded">
                {getTypeLabel()}
              </span>
            </div>
            <p className="text-sm text-amber-700 dark:text-amber-400">
              {diffDetails.description}
            </p>
            <p className="text-xs text-amber-600 dark:text-amber-500 mt-2">
              {isCompleted
                ? t("diffNotification.completedMessage")
                : t("diffNotification.mockMessage")}
              {diffDetails.type === "endpoint" &&
                " " + t("diffNotification.updateSpecMessage")}
            </p>
          </div>
        </div>
      </div>

      {/* 상세 정보 */}
      <div className="p-4 space-y-3">
        {/* 상세 로그 정보 */}
        {diffDetails.type === "request" && reqLog && reqLog.trim() !== "" && (
          <div className="bg-white dark:bg-amber-950/30 rounded-md p-3 border border-amber-200 dark:border-amber-800">
            <h4 className="text-xs font-semibold text-amber-900 dark:text-amber-200 mb-2 flex items-center gap-1">
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
                  d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                />
              </svg>
              {t("diffNotification.requestDiffDetails")}
            </h4>
            <div className="mt-2 p-2 bg-amber-100 dark:bg-amber-900/50 rounded border border-amber-300 dark:border-amber-700">
              <pre className="text-xs text-amber-900 dark:text-amber-200 whitespace-pre-wrap break-words font-mono">
                {reqLog}
              </pre>
            </div>
          </div>
        )}

        {diffDetails.type === "response" && resLog && resLog.trim() !== "" && (
          <div className="bg-white dark:bg-amber-950/30 rounded-md p-3 border border-amber-200 dark:border-amber-800">
            <h4 className="text-xs font-semibold text-amber-900 dark:text-amber-200 mb-2 flex items-center gap-1">
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
                  d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                />
              </svg>
              {t("diffNotification.responseDiffDetails")}
            </h4>
            <div className="mt-2 p-2 bg-amber-100 dark:bg-amber-900/50 rounded border border-amber-300 dark:border-amber-700">
              <pre className="text-xs text-amber-900 dark:text-amber-200 whitespace-pre-wrap break-words font-mono">
                {resLog}
              </pre>
            </div>
          </div>
        )}

        {diffDetails.type === "both" &&
          ((reqLog && reqLog.trim() !== "") ||
            (resLog && resLog.trim() !== "")) && (
            <div className="bg-white dark:bg-amber-950/30 rounded-md p-3 border border-amber-200 dark:border-amber-800">
              <h4 className="text-xs font-semibold text-amber-900 dark:text-amber-200 mb-2 flex items-center gap-1">
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
                    d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                  />
                </svg>
                {t("diffNotification.diffDetails")}
              </h4>
              <div className="space-y-3 mt-2">
                {reqLog && (
                  <div>
                    <h5 className="text-xs font-medium text-amber-800 dark:text-amber-300 mb-1">
                      {t("diffNotification.requestDiffLabel")}
                    </h5>
                    <div className="p-2 bg-amber-100 dark:bg-amber-900/50 rounded border border-amber-300 dark:border-amber-700">
                      <pre className="text-xs text-amber-900 dark:text-amber-200 whitespace-pre-wrap break-words font-mono">
                        {reqLog}
                      </pre>
                    </div>
                  </div>
                )}
                {resLog && (
                  <div>
                    <h5 className="text-xs font-medium text-amber-800 dark:text-amber-300 mb-1">
                      {t("diffNotification.responseDiffLabel")}
                    </h5>
                    <div className="p-2 bg-amber-100 dark:bg-amber-900/50 rounded border border-amber-300 dark:border-amber-700">
                      <pre className="text-xs text-amber-900 dark:text-amber-200 whitespace-pre-wrap break-words font-mono">
                        {resLog}
                      </pre>
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}

        {onSyncToSpec && diffDetails.type === "endpoint" && (
          <button
            onClick={onSyncToSpec}
            className="w-full px-4 py-3 bg-amber-600 hover:bg-amber-700 dark:bg-amber-700 dark:hover:bg-amber-800 text-white rounded-md transition-colors text-sm font-semibold flex items-center justify-center gap-2 shadow-md"
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
            {t("diffNotification.syncToSpec")}
          </button>
        )}
      </div>
    </div>
  );
}
