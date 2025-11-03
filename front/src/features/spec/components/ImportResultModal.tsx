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
        <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-2xl w-full mx-4 max-h-[80vh] overflow-hidden flex flex-col pointer-events-auto border border-gray-200 dark:border-gray-700">
          {/* Header */}
          <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center shadow-lg">
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
                  <h2 className="text-xl font-bold text-gray-900 dark:text-white">
                    Import 완료
                  </h2>
                  <p className="text-sm text-gray-500 dark:text-gray-400">
                    {result.summary}
                  </p>
                </div>
              </div>
              <button
                onClick={onClose}
                className="p-2 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 transition-colors"
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
              <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl p-4">
                <div className="text-sm text-gray-500 dark:text-gray-400 mb-1">
                  Import된 API
                </div>
                <div className="text-2xl font-bold text-gray-900 dark:text-white">
                  {result.imported}
                </div>
              </div>
              <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl p-4">
                <div className="text-sm text-gray-500 dark:text-gray-400 mb-1">
                  이름 변경
                </div>
                <div className="text-2xl font-bold text-gray-900 dark:text-white">
                  {result.renamed}
                </div>
              </div>
            </div>

            {/* Renamed Items */}
            {result.renamed > 0 && result.renamedList.length > 0 && (
              <div>
                <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
                  중복으로 인해 이름이 변경된 항목
                </h3>
                <div className="space-y-2">
                  {result.renamedList.map((item: RenamedItem, index: number) => (
                    <div
                      key={index}
                      className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl p-4"
                    >
                      <div className="flex items-center gap-2 mb-2">
                        <span
                          className={`px-2 py-1 text-xs font-semibold rounded-lg ${
                            item.type === "api"
                              ? "bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-200"
                              : "bg-purple-100 text-purple-700 dark:bg-purple-900 dark:text-purple-200"
                          }`}
                        >
                          {item.type === "api" ? "API" : "Schema"}
                        </span>
                        {item.method && (
                          <span
                            className={`px-2 py-1 text-xs font-semibold rounded-lg ${
                              item.method === "GET"
                                ? "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200"
                                : item.method === "POST"
                                ? "bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200"
                                : item.method === "PUT"
                                ? "bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200"
                                : item.method === "PATCH"
                                ? "bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200"
                                : "bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200"
                            }`}
                          >
                            {item.method}
                          </span>
                        )}
                      </div>
                      <div className="flex items-center gap-2 text-sm">
                        <code className="text-gray-600 dark:text-gray-400 font-mono">
                          {item.original}
                        </code>
                        <svg
                          className="w-4 h-4 text-gray-400"
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
                        <code className="text-gray-900 dark:text-white font-mono font-semibold">
                          {item.renamed}
                        </code>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Footer */}
          <div className="px-6 py-4 border-t border-gray-200 dark:border-gray-700">
            <button
              onClick={onClose}
              className="w-full px-6 py-3 bg-blue-600 hover:bg-blue-700 dark:bg-blue-700 dark:hover:bg-blue-600 text-white rounded-xl transition-all duration-200 font-semibold shadow-sm hover:shadow-md"
            >
              확인
            </button>
          </div>
        </div>
      </div>
    </>
  );
}

