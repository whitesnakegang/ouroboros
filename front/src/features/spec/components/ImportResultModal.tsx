import { type ImportYamlData, type RenamedItem } from "../services/api";

interface ImportResultModalProps {
  isOpen: boolean;
  onClose: () => void;
  result: ImportYamlData;
}

export function ImportResultModal({
  isOpen,
  onClose,
  result,
}: ImportResultModalProps) {
  if (!isOpen) return null;

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black bg-opacity-50 z-40"
        onClick={onClose}
      />

      {/* Modal */}
      <div className="fixed inset-0 z-50 flex items-center justify-center pointer-events-none">
        <div className="bg-white dark:bg-[#161B22] rounded-md shadow-2xl max-w-2xl w-full mx-4 max-h-[80vh] overflow-hidden flex flex-col pointer-events-auto border border-gray-200 dark:border-[#2D333B]">
          {/* Header */}
          <div className="px-6 py-4 border-b border-gray-200 dark:border-[#2D333B]">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-md bg-emerald-500 flex items-center justify-center">
                  <svg
                    className="w-6 h-6 text-white"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M5 13l4 4L19 7"
                    />
                  </svg>
                </div>
                <div>
                  <h2 className="text-xl font-semibold text-gray-900 dark:text-[#E6EDF3]">
                    Import 완료
                  </h2>
                  <p className="text-sm text-gray-600 dark:text-[#8B949E]">
                    {result.summary}
                  </p>
                </div>
              </div>
              <button
                onClick={onClose}
                className="p-2 text-gray-500 hover:text-gray-700 dark:text-[#8B949E] dark:hover:text-[#E6EDF3] transition-colors"
              >
                <svg
                  className="w-6 h-6"
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
          </div>

          {/* Content */}
          <div className="flex-1 overflow-y-auto p-6">
            {/* Statistics */}
            <div className="grid grid-cols-2 gap-4 mb-6">
              <div className="bg-gray-50 dark:bg-[#0D1117] border border-gray-200 dark:border-[#2D333B] rounded-md p-4">
                <div className="text-sm text-gray-600 dark:text-[#8B949E] mb-1">
                  Import된 API
                </div>
                <div className="text-2xl font-bold text-gray-900 dark:text-[#E6EDF3]">
                  {result.imported}
                </div>
              </div>
              <div className="bg-gray-50 dark:bg-[#0D1117] border border-gray-200 dark:border-[#2D333B] rounded-md p-4">
                <div className="text-sm text-gray-600 dark:text-[#8B949E] mb-1">
                  이름 변경
                </div>
                <div className="text-2xl font-bold text-gray-900 dark:text-[#E6EDF3]">
                  {result.renamed}
                </div>
              </div>
            </div>

            {/* Renamed Items */}
            {result.renamed > 0 && result.renamedList.length > 0 && (
              <div>
                <h3 className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3] mb-3">
                  중복으로 인해 이름이 변경된 항목
                </h3>
                <div className="space-y-2">
                  {result.renamedList.map(
                    (item: RenamedItem, index: number) => (
                      <div
                        key={index}
                        className="bg-gray-50 dark:bg-[#0D1117] border border-gray-200 dark:border-[#2D333B] rounded-md p-4"
                      >
                        <div className="flex items-center gap-2 mb-2">
                          <span
                            className={`px-2 py-1 text-xs font-semibold rounded-[4px] border ${
                              item.type === "api"
                                ? "bg-blue-50 dark:bg-[#0D1117] border-blue-200 dark:border-blue-800 text-blue-700 dark:text-blue-400"
                                : "bg-purple-50 dark:bg-[#0D1117] border-purple-200 dark:border-purple-800 text-purple-700 dark:text-purple-400"
                            }`}
                          >
                            {item.type === "api" ? "API" : "Schema"}
                          </span>
                          {item.method && (
                            <span
                              className={`px-2 py-1 text-xs font-semibold rounded-[4px] border font-mono ${
                                item.method === "GET"
                                  ? "bg-emerald-50 dark:bg-[#0D1117] border-emerald-200 dark:border-emerald-800 text-emerald-700 dark:text-emerald-400"
                                  : item.method === "POST"
                                  ? "bg-blue-50 dark:bg-[#0D1117] border-blue-200 dark:border-blue-800 text-blue-700 dark:text-blue-400"
                                  : item.method === "PUT"
                                  ? "bg-amber-50 dark:bg-[#0D1117] border-amber-200 dark:border-amber-800 text-amber-700 dark:text-amber-400"
                                  : item.method === "PATCH"
                                  ? "bg-amber-50 dark:bg-[#0D1117] border-amber-200 dark:border-amber-800 text-amber-700 dark:text-amber-400"
                                  : "bg-red-50 dark:bg-[#0D1117] border-red-200 dark:border-red-800 text-red-700 dark:text-red-400"
                              }`}
                            >
                              {item.method}
                            </span>
                          )}
                        </div>
                        <div className="flex items-center gap-2 text-sm">
                          <code className="text-gray-600 dark:text-[#8B949E] font-mono">
                            {item.original}
                          </code>
                          <svg
                            className="w-4 h-4 text-gray-400 dark:text-[#8B949E]"
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
                          <code className="text-gray-900 dark:text-[#E6EDF3] font-mono font-semibold">
                            {item.renamed}
                          </code>
                        </div>
                      </div>
                    )
                  )}
                </div>
              </div>
            )}
          </div>

          {/* Footer */}
          <div className="px-6 py-4 border-t border-gray-200 dark:border-[#2D333B]">
            <button
              onClick={onClose}
              className="w-full md:w-auto px-6 py-3 bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded-md transition-all font-medium active:translate-y-[1px] focus:outline-none focus-visible:outline-none"
            >
              확인
            </button>
          </div>
        </div>
      </div>
    </>
  );
}
