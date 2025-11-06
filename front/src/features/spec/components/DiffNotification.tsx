import { Button } from "@/ui/Button";
import { Badge } from "@/ui/Badge";

interface DiffDetails {
  type: "request" | "response" | "endpoint" | "both";
  fields?: string[];
  description?: string;
}

interface DiffNotificationProps {
  diff: string; // "none", "request", "response", "endpoint", "both" 등
  progress?: string; // "mock", "completed" 등
  onSyncToSpec?: () => void; // 명세에 자동 추가 버튼 핸들러
}

export function DiffNotification({
  diff,
  progress,
  onSyncToSpec,
}: DiffNotificationProps) {
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
  
  // progress 상태 확인 (기본값: "mock")
  const progressLower = progress?.toLowerCase() || "mock";
  const isCompleted = progressLower === "completed";
  const isMock = !isCompleted;

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
    <div className="rounded-md border border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#161B22] p-4 shadow-sm mb-6 space-y-3">
      <div className="flex items-start gap-2">
        <svg
          className="h-4 w-4 text-amber-500 flex-shrink-0 mt-0.5"
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
        <div className="flex-1 flex flex-col">
          <div className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3] flex items-center gap-2">
            명세와 실제 구현의 불일치
            <Badge className="text-amber-500">{getTypeLabel()}</Badge>
          </div>
          <div className="text-xs text-gray-600 dark:text-[#8B949E] leading-relaxed mt-1">
            {isCompleted
              ? "이 API는 completed 상태로 실제 구현이 완료되었습니다. 백엔드에서 diff가 감지되었습니다."
              : "이 API는 mock 상태입니다. 백엔드에서 diff가 감지되었습니다."}
            {diffDetails.type === "endpoint" && " 아래 버튼으로 명세를 갱신할 수 있습니다."}
          </div>
        </div>
      </div>

      <div className="space-y-3 pt-2 border-t border-gray-200 dark:border-[#2D333B]">
        <div className="text-xs text-gray-600 dark:text-[#8B949E] leading-relaxed">
          {diffDetails.description}
        </div>

        <div className="bg-gray-50 dark:bg-[#0D1117] rounded-md p-3 border border-gray-200 dark:border-[#2D333B]">
          <h4 className="text-xs font-semibold text-gray-900 dark:text-[#E6EDF3] mb-2">
            안내사항
          </h4>
          <ul className="text-xs text-gray-600 dark:text-[#8B949E] space-y-2">
            <li className="flex items-start gap-2">
              <svg
                className="w-3 h-3 text-gray-500 dark:text-[#8B949E] mt-0.5 flex-shrink-0"
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
                <strong className="text-gray-900 dark:text-[#E6EDF3]">
                  {isCompleted ? "completed" : "mock"}
                </strong>{" "}
                상태{isCompleted ? "로 실제 구현이 완료되었습니다" : "입니다"}.
              </span>
            </li>
            <li className="flex items-start gap-2">
              <svg
                className="w-3 h-3 text-gray-500 dark:text-[#8B949E] mt-0.5 flex-shrink-0"
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
                <code className="px-1 py-0.5 bg-white dark:bg-[#161B22] border border-gray-300 dark:border-[#2D333B] rounded text-[10px] font-mono text-gray-900 dark:text-[#E6EDF3]">
                  x-ouroboros-diff
                </code>{" "}
                어노테이션을 통해 불일치가 감지되었습니다.
              </span>
            </li>
            <li className="flex items-start gap-2">
              <svg
                className="w-3 h-3 text-gray-500 dark:text-[#8B949E] mt-0.5 flex-shrink-0"
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
            {diffDetails.type === "endpoint" && (
              <li className="flex items-start gap-2">
                <svg
                  className="w-3 h-3 text-gray-500 dark:text-[#8B949E] mt-0.5 flex-shrink-0"
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
                  실제 구현에 존재하지만 명세에 없는 필드가 있다면, 아래 버튼을
                  클릭하여 명세에 자동으로 추가할 수 있습니다.
                </span>
              </li>
            )}
          </ul>
        </div>

        {onSyncToSpec && diffDetails.type === "endpoint" && (
          <div className="flex flex-wrap gap-2 pt-2">
            <Button
              variant="primary"
              onClick={onSyncToSpec}
              className="text-xs"
            >
              실제 구현을 명세에 반영
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}
