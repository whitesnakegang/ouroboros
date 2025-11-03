import { useTestingStore } from "../store/testing.store";

export function TestResponsePanel() {
  const { response } = useTestingStore();

  if (!response) {
    return (
      <div className="rounded-md border border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#161B22] p-4 shadow-sm">
        <div className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3] mb-2 flex items-center gap-2">
          <svg
            className="h-4 w-4 text-gray-500 dark:text-[#8B949E]"
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
          <span>Response</span>
        </div>
        <div className="text-center py-12 text-gray-600 dark:text-[#8B949E]">
          <div className="w-16 h-16 mx-auto mb-4 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] flex items-center justify-center">
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
                d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"
              />
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
          </div>
          <p className="text-sm">RUN 버튼을 눌러 테스트를 실행하세요</p>
        </div>
      </div>
    );
  }

  const getStatusColor = (status: number) => {
    if (status >= 200 && status < 300) return "bg-green-500";
    if (status >= 300 && status < 400) return "bg-yellow-500";
    if (status >= 400 && status < 500) return "bg-orange-500";
    return "bg-red-500";
  };

  return (
    <div className="rounded-md border border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#161B22] p-4 shadow-sm">
      <div className="flex items-center justify-between mb-4">
        <div className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3] flex items-center gap-2">
          <svg
            className="h-4 w-4 text-gray-500 dark:text-[#8B949E]"
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
          <span>Response</span>
        </div>
        <div className="flex items-center gap-3">
          {/* Response Time */}
          <div className="px-2 py-1 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B]">
            <span className="text-xs font-medium text-gray-600 dark:text-[#8B949E] font-mono">
              {response.responseTime}ms
            </span>
          </div>
          {/* Status Code */}
          <div
            className={`px-2 py-1 rounded-md ${getStatusColor(response.status)}`}
          >
            <span className="text-sm font-bold text-white">
              {response.status} {response.statusText}
            </span>
          </div>
        </div>
      </div>

      {/* Response Body */}
      <div className="mb-6">
        <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
          Response Body
        </label>
        <div className="bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] rounded-md p-4 overflow-auto max-h-64">
          <pre className="text-sm text-gray-900 dark:text-[#E6EDF3] font-mono whitespace-pre-wrap">
            {typeof response.body === "string"
              ? response.body
              : JSON.stringify(response.body, null, 2)}
          </pre>
        </div>
      </div>

      {/* Headers */}
      <div>
        <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
          Headers
        </label>
        <div className="bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] rounded-md p-4">
          {Object.entries(response.headers).map(([key, value]) => (
            <div
              key={key}
              className="flex gap-2 py-1 text-sm font-mono text-gray-900 dark:text-[#E6EDF3]"
            >
              <span className="font-semibold text-[#2563EB]">
                {key}:
              </span>
              <span className="text-gray-600 dark:text-[#8B949E]">{value}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

