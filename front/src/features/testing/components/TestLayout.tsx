import { TestRequestPanel } from "./TestRequestPanel";
import { TestResponsePanel } from "./TestResponsePanel";
import { useTestingStore } from "../store/testing.store";

export function TestLayout() {
  const { protocol } = useTestingStore();

  return (
    <div className="h-full flex flex-col bg-white dark:bg-[#0D1117]">
      {/* Main Content */}
      <div className="flex-1 overflow-auto p-6">
        {protocol === "REST" ? (
          <div className="max-w-6xl mx-auto space-y-6">
            <TestRequestPanel />
            <TestResponsePanel />
          </div>
        ) : (
          <div className="h-full flex items-center justify-center">
            <div className="text-center py-12">
              <div className="w-16 h-16 mx-auto mb-6 rounded-md bg-gray-100 dark:bg-[#161B22] border border-gray-300 dark:border-[#2D333B] flex items-center justify-center">
                <svg
                  className="w-8 h-8 text-gray-500 dark:text-[#8B949E]"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M12 6v6m0 0v6m0-6h6m-6 0H6"
                  />
                </svg>
              </div>
              <h3 className="text-xl font-semibold text-gray-900 dark:text-[#E6EDF3] mb-2">
                {protocol} 테스트 준비 중
              </h3>
              <p className="text-gray-600 dark:text-[#8B949E]">
                현재는 REST 프로토콜만 지원합니다.
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
