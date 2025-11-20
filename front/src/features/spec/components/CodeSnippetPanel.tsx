import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import type { RestApiSpecResponse } from "../services/api";
import { exportYaml } from "../services/api";
import { getEndpointSnippets } from "openapi-snippet";
import yaml from "js-yaml";

interface CodeSnippetPanelProps {
  isOpen: boolean;
  onClose: () => void;
  spec: RestApiSpecResponse | null;
}

const languages = [
  { id: "node_native", name: "JavaScript" },
  { id: "javascript_xhr", name: "TypeScript" },
  { id: "python_python3", name: "Python" },
  { id: "shell_curl", name: "cURL" },
  { id: "java_okhttp", name: "Java" },
  { id: "swift_nsurlsession", name: "Swift" },
  { id: "go_native", name: "Go" },
];

export function CodeSnippetPanel({
  isOpen,
  onClose,
  spec,
}: CodeSnippetPanelProps) {
  const { t } = useTranslation();
  const [selectedLanguage, setSelectedLanguage] = useState("node_native");
  const [copied, setCopied] = useState(false);
  const [snippet, setSnippet] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!isOpen || !spec) {
      setSnippet("");
      return;
    }

    const generateSnippet = async () => {
      setLoading(true);
      try {
        // 전체 OpenAPI YAML 가져오기
        const yamlContent = await exportYaml();

        // YAML을 객체로 변환
        const openApiSpec = yaml.load(yamlContent) as any;

        if (
          !openApiSpec ||
          !openApiSpec.paths ||
          !openApiSpec.paths[spec.path]
        ) {
          throw new Error(t("apiCard.pathNotFoundInSpec"));
        }

        // openapi-snippet으로 스니펫 생성
        // getEndpointSnippets(openApi, path, method, targets, values)
        const targets = languages.map((lang) => lang.id);
        const result = getEndpointSnippets(
          openApiSpec,
          spec.path,
          spec.method.toLowerCase(),
          targets
        );

        // 선택된 언어의 스니펫 찾기
        const selectedSnippet = result.snippets.find(
          (snippet: any) => snippet.id === selectedLanguage
        );

        if (selectedSnippet && selectedSnippet.content) {
          setSnippet(selectedSnippet.content);
        } else {
          // fallback: 지원되지 않는 언어인 경우
          const availableLanguages = result.snippets
            .map((s: any) => s.id)
            .join(", ");
          setSnippet(
            `// ${t("apiCard.languageNotSupportedYet", {
              language: selectedLanguage,
            })}\n// ${t("apiCard.availableLanguages", {
              languages: availableLanguages || "none",
            })}`
          );
        }
      } catch (error) {
        console.error("Failed to generate snippet:", error);
        const errorMessage =
          error instanceof Error ? error.message : String(error);
        setSnippet(
          `// ${errorMessage}\n// ${t("apiCard.tryAgainAfterUpdatingSpec")}`
        );
      } finally {
        setLoading(false);
      }
    };

    generateSnippet();
  }, [isOpen, spec, selectedLanguage]);

  const copyToClipboard = () => {
    navigator.clipboard.writeText(snippet);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  if (!isOpen || !spec) return null;

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black bg-opacity-50 z-40"
        onClick={onClose}
      />
      {/* Slide Panel */}
      <div className="fixed right-0 top-0 h-full w-[600px] bg-white dark:bg-[#0D1117] z-50 shadow-2xl transform transition-transform duration-300 flex flex-col">
        {/* Header */}
        <div className="h-16 border-b border-gray-200 dark:border-[#2D333B] flex items-center justify-between px-6 flex-shrink-0">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-[#E6EDF3] flex items-center gap-2">
            <svg
              className="w-5 h-5 text-gray-500 dark:text-[#8B949E]"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4"
              />
            </svg>
            {t("apiCard.codeSnippets")}
          </h2>
          <button
            onClick={onClose}
            className="p-2 text-gray-500 hover:text-gray-700 dark:text-[#8B949E] dark:hover:text-[#E6EDF3]"
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

        {/* Language Tabs */}
        <div className="border-b border-gray-200 dark:border-[#2D333B] px-6 flex-shrink-0">
          <div className="flex gap-2 overflow-x-auto">
            {languages.map((lang) => (
              <button
                key={lang.id}
                onClick={() => setSelectedLanguage(lang.id)}
                className={`px-4 py-3 text-sm font-medium transition-colors whitespace-nowrap border-b-2 ${
                  selectedLanguage === lang.id
                    ? "text-gray-900 dark:text-[#E6EDF3] border-[#2563EB]"
                    : "text-gray-500 dark:text-[#8B949E] border-transparent hover:text-gray-900 dark:hover:text-[#E6EDF3]"
                }`}
              >
                {lang.name}
              </button>
            ))}
          </div>
        </div>

        {/* Code Display */}
        <div className="flex-1 overflow-y-auto p-6 min-h-0">
          <div className="bg-gray-50 dark:bg-[#0D1117] rounded-md border border-gray-200 dark:border-[#2D333B] overflow-hidden">
            <div className="flex items-center justify-between bg-white dark:bg-[#161B22] px-4 py-2 border-b border-gray-200 dark:border-[#2D333B] flex-shrink-0">
              <span className="text-sm text-gray-600 dark:text-[#8B949E] font-mono">
                {spec.method} {spec.path}
              </span>
              <button
                onClick={copyToClipboard}
                disabled={loading || !snippet}
                className="px-3 py-1 text-sm bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded-md transition-all active:translate-y-[1px] focus:outline-none focus-visible:outline-none disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {copied ? t("apiCard.copied") : t("apiCard.copy")}
              </button>
            </div>
            <div className="bg-[#0D1117] dark:bg-[#010409] p-4 rounded-md max-h-[calc(100vh-250px)] overflow-y-auto">
              {loading ? (
                <div className="text-sm text-[#E6EDF3] text-center py-8">
                  {t("apiCard.generatingSnippet")}
                </div>
              ) : (
                <pre className="text-sm text-[#E6EDF3] whitespace-pre-wrap overflow-x-auto font-mono">
                  <code>
                    {snippet || `// ${t("apiCard.cannotGenerateSnippet")}`}
                  </code>
                </pre>
              )}
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
