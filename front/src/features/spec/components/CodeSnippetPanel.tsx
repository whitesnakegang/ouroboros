import { useState } from "react";

interface CodeSnippetPanelProps {
  isOpen: boolean;
  onClose: () => void;
  method: string;
  url: string;
  headers: Array<{ key: string; value: string }>;
  requestBody?: any;
}

const languages = [
  { id: "javascript", name: "JavaScript", icon: "📜" },
  { id: "python", name: "Python", icon: "🐍" },
  { id: "curl", name: "cURL", icon: "📡" },
  { id: "java", name: "Java", icon: "☕" },
];

export function CodeSnippetPanel({
  isOpen,
  onClose,
  method,
  url,
  headers,
  requestBody,
}: CodeSnippetPanelProps) {
  const [selectedLanguage, setSelectedLanguage] = useState("javascript");
  const [copied, setCopied] = useState(false);

  const generateSnippet = (lang: string): string => {
    const authHeader = headers.find(
      (h) => h.key.toLowerCase() === "authorization"
    );

    switch (lang) {
      case "javascript":
        return generateJavaScriptSnippet(method, url, headers, requestBody);
      case "python":
        return generatePythonSnippet(method, url, headers, requestBody);
      case "curl":
        return generateCurlSnippet(method, url, headers, requestBody);
      case "java":
        return generateJavaSnippet(method, url, headers, requestBody);
      default:
        return "";
    }
  };

  const copyToClipboard = () => {
    const code = generateSnippet(selectedLanguage);
    navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  if (!isOpen) return null;

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black bg-opacity-50 z-40"
        onClick={onClose}
      />
      {/* Slide Panel */}
      <div className="fixed right-0 top-0 h-full w-[600px] bg-white dark:bg-gray-800 z-50 shadow-2xl transform transition-transform duration-300">
        {/* Header */}
        <div className="h-16 border-b dark:border-gray-700 flex items-center justify-between px-6">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white flex items-center gap-2">
            <span>💻</span> Code Snippets
          </h2>
          <button
            onClick={onClose}
            className="p-2 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
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
        <div className="border-b dark:border-gray-700 px-6">
          <div className="flex gap-2 overflow-x-auto">
            {languages.map((lang) => (
              <button
                key={lang.id}
                onClick={() => setSelectedLanguage(lang.id)}
                className={`px-4 py-3 text-sm font-medium transition-colors whitespace-nowrap border-b-2 ${
                  selectedLanguage === lang.id
                    ? "text-blue-600 border-blue-600 dark:text-blue-400 dark:border-blue-400"
                    : "text-gray-500 border-transparent hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300"
                }`}
              >
                <span className="mr-1">{lang.icon}</span>
                {lang.name}
              </button>
            ))}
          </div>
        </div>

        {/* Code Display */}
        <div className="flex-1 overflow-auto p-6">
          <div className="bg-gray-50 dark:bg-gray-900 rounded-lg border border-gray-200 dark:border-gray-700 overflow-hidden">
            <div className="flex items-center justify-between bg-gray-100 dark:bg-gray-800 px-4 py-2 border-b dark:border-gray-700">
              <span className="text-sm text-gray-600 dark:text-gray-400">
                {method} {url}
              </span>
              <button
                onClick={copyToClipboard}
                className="px-3 py-1 text-sm bg-blue-600 hover:bg-blue-700 dark:bg-blue-700 dark:hover:bg-blue-600 text-white rounded transition-colors"
              >
                {copied ? "✓ 복사됨" : "📋 복사"}
              </button>
            </div>
            <div className="bg-gray-900 p-4 rounded">
              <pre className="text-sm text-gray-100 whitespace-pre-wrap overflow-x-auto">
                <code>{generateSnippet(selectedLanguage)}</code>
              </pre>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}

function generateJavaScriptSnippet(
  method: string,
  url: string,
  headers: Array<{ key: string; value: string }>,
  requestBody?: any
): string {
  const headerString = headers
    .map((h) => `  "${h.key}": "${h.value}"`)
    .join(",\n");

  let bodyString = "";
  if (requestBody && method !== "GET") {
    if (requestBody.type === "json") {
      const bodyObj = requestBody.fields?.reduce((acc: any, field: any) => {
        acc[field.key] = field.value;
        return acc;
      }, {});
      bodyString = `\n\nconst body = ${JSON.stringify(bodyObj, null, 2)};`;
    }
  }

  return `const response = await fetch("${url}", {
  method: "${method}",
  headers: {
${headerString}
  },${bodyString}
});`;
}

function generatePythonSnippet(
  method: string,
  url: string,
  headers: Array<{ key: string; value: string }>,
  requestBody?: any
): string {
  const headerString = headers
    .map((h) => `    "${h.key}": "${h.value}"`)
    .join(",\n");

  let bodyString = "";
  if (requestBody && method !== "GET") {
    if (requestBody.type === "json") {
      const bodyObj = requestBody.fields?.reduce((acc: any, field: any) => {
        acc[field.key] = field.value;
        return acc;
      }, {});
      bodyString = `\n\nbody = ${JSON.stringify(bodyObj, null, 2)}`;
    }
  }

  return `import requests

response = requests.${method.toLowerCase()}(
    "${url}",
    headers={
${headerString}
    },${bodyString ? `\n    json=body` : ""}
)`;
}

function generateCurlSnippet(
  method: string,
  url: string,
  headers: Array<{ key: string; value: string }>,
  requestBody?: any
): string {
  const headerString = headers
    .map((h) => `  -H "${h.key}: ${h.value}"`)
    .join(" \\\n");

  let bodyString = "";
  if (requestBody && method !== "GET") {
    if (requestBody.type === "json") {
      const bodyObj = requestBody.fields?.reduce((acc: any, field: any) => {
        acc[field.key] = field.value;
        return acc;
      }, {});
      bodyString = ` -d '${JSON.stringify(bodyObj)}'`;
    }
  }

  return `curl -X ${method} "${url}" \\
${headerString}${bodyString ? ` \\` : ""}${bodyString}`;
}

function generateJavaSnippet(
  method: string,
  url: string,
  headers: Array<{ key: string; value: string }>,
  requestBody?: any
): string {
  const headerString = headers
    .map((h) => `      .header("${h.key}", "${h.value}")`)
    .join("\n");

  return `CloseableHttpClient httpClient = HttpClients.createDefault();
Http${method.charAt(0) + method.slice(1).toLowerCase()} request = new Http${
    method.charAt(0) + method.slice(1).toLowerCase()
  }("${url}");

${headerString}

CloseableHttpResponse response = httpClient.execute(request);`;
}
