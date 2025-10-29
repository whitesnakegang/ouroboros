interface ApiPreviewCardProps {
  method: string;
  url: string;
  tags: string;
  description: string;
}

export function ApiPreviewCard({
  method,
  url,
  tags,
  description,
}: ApiPreviewCardProps) {
  const previewYaml = `openapi: 3.0.0
info:
  title: ${url}
  version: 1.0.0
  
paths:
  ${url}:
    ${method.toLowerCase()}:
      summary: ${description}
      tags: [${tags}]
      # ... additional spec
`;

  return (
    <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-2xl p-6 shadow-sm">
      {/* Header */}
      <div className="flex items-center gap-3 mb-4">
        <div className="w-10 h-10 rounded-lg bg-green-100 dark:bg-green-900 flex items-center justify-center">
          <svg
            className="w-6 h-6 text-green-600 dark:text-green-400"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
            />
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"
            />
          </svg>
        </div>
        <div>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
            Preview / Export
          </h3>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            명세서 미리보기 및 내보내기
          </p>
        </div>
      </div>

      {/* Preview Content */}
      <div className="space-y-4">
        {/* Tabs */}
        <div className="flex gap-1 border-b border-gray-200 dark:border-gray-700">
          {["yaml", "markdown"].map((tab) => (
            <button
              key={tab}
              className={`px-4 py-2 text-sm font-medium transition-colors capitalize ${
                tab === "yaml"
                  ? "text-green-600 border-b-2 border-green-600 dark:text-green-400"
                  : "text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300"
              }`}
            >
              {tab === "yaml" && "YAML"}
              {tab === "markdown" && "Markdown"}
            </button>
          ))}
        </div>

        {/* Preview */}
        <div className="relative">
          <pre className="p-4 bg-gray-900 dark:bg-gray-950 rounded-lg overflow-x-auto text-sm text-green-400 font-mono">
            <code>{previewYaml}</code>
          </pre>
        </div>

        {/* Export Buttons */}
        <div className="flex gap-3 pt-4 border-t border-gray-200 dark:border-gray-700">
          <button className="flex-1 px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors font-medium">
            Export YAML
          </button>
          <button className="flex-1 px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors font-medium">
            Export Markdown
          </button>
        </div>
      </div>
    </div>
  );
}
