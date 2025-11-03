import { buildOpenApiYaml, downloadYaml } from "../utils/yamlExporter";
import { exportToMarkdown, downloadMarkdown } from "../utils/markdownExporter";

interface KeyValuePair { key: string; value: string }

interface RequestBodyField { key: string; value: string; type: string; description?: string; required?: boolean }
interface RequestBody {
  type: "none" | "form-data" | "x-www-form-urlencoded" | "json" | "xml";
  contentType: string;
  fields: RequestBodyField[];
}

interface StatusCode { code: string; type: "Success" | "Error"; message: string }

interface ApiPreviewCardProps {
  method: string;
  url: string;
  tags: string;
  description: string;
  headers: KeyValuePair[];
  requestBody: RequestBody;
  statusCodes: StatusCode[];
}

export function ApiPreviewCard({
  method,
  url,
  tags,
  description,
  headers,
  requestBody,
  statusCodes,
}: ApiPreviewCardProps) {
  const previewYaml = buildOpenApiYaml({
    method,
    url,
    description,
    tags,
    headers,
    requestBody,
    statusCodes,
  });

  return (
    <div className="bg-white dark:bg-[#161B22] border border-gray-200 dark:border-[#2D333B] rounded-md p-6 shadow-sm">
      {/* Header */}
      <div className="flex items-center gap-3 mb-4">
        <div className="w-10 h-10 rounded-md bg-emerald-50 dark:bg-emerald-900/20 flex items-center justify-center">
          <svg
            className="w-6 h-6 text-emerald-600 dark:text-emerald-400"
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
          <h3 className="text-lg font-semibold text-gray-900 dark:text-[#E6EDF3]">
            Preview / Export
          </h3>
          <p className="text-sm text-gray-600 dark:text-[#8B949E]">
            명세서 미리보기 및 내보내기
          </p>
        </div>
      </div>

      {/* Preview Content */}
      <div className="space-y-4">
        {/* Tabs */}
        <div className="flex gap-1 border-b border-gray-200 dark:border-[#2D333B]">
          {["yaml", "markdown"].map((tab) => (
            <button
              key={tab}
              className={`px-4 py-2 text-sm font-medium transition-colors capitalize border-b-2 ${
                tab === "yaml"
                  ? "text-gray-900 dark:text-[#E6EDF3] border-emerald-500"
                  : "text-gray-500 dark:text-[#8B949E] border-transparent hover:text-gray-900 dark:hover:text-[#E6EDF3]"
              }`}
            >
              {tab === "yaml" && "YAML"}
              {tab === "markdown" && "Markdown"}
            </button>
          ))}
        </div>

        {/* Preview */}
        <div className="relative">
          <pre className="p-4 bg-[#0D1117] dark:bg-[#010409] rounded-md overflow-x-auto text-sm text-emerald-400 font-mono">
            <code>{previewYaml}</code>
          </pre>
        </div>

        {/* Export Buttons */}
        <div className="flex gap-3 pt-4 border-t border-gray-200 dark:border-[#2D333B]">
          <button
            onClick={() => {
              const content = buildOpenApiYaml({
                method,
                url,
                description,
                tags,
                headers,
                requestBody,
                statusCodes,
              });
              const filename = `${method.toUpperCase()}_${url.replace(/\//g, "_")}.yml`;
              downloadYaml(content, filename);
            }}
            className="flex-1 px-4 py-2 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-700 dark:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#161B22] transition-colors font-medium"
          >
            Export YAML
          </button>
          <button
            onClick={() => {
              const md = exportToMarkdown({
                method,
                url,
                description,
                tags,
                headers,
                requestBody,
                statusCodes,
              });
              const filename = `${method.toUpperCase()}_${url.replace(/\//g, "_")}.md`;
              downloadMarkdown(md, filename);
            }}
            className="flex-1 px-4 py-2 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-700 dark:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#161B22] transition-colors font-medium"
          >
            Export Markdown
          </button>
        </div>
      </div>
    </div>
  );
}
