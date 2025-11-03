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
  { id: "javascript", name: "JavaScript" },
  { id: "typescript", name: "TypeScript" },
  { id: "python", name: "Python" },
  { id: "curl", name: "cURL" },
  { id: "java", name: "Java" },
  { id: "shell", name: "Shell" },
  { id: "swift", name: "Swift" },
  { id: "go", name: "Go" },
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
      case "typescript":
        return generateTypeScriptSnippet(method, url, headers, requestBody);
      case "python":
        return generatePythonSnippet(method, url, headers, requestBody);
      case "curl":
        return generateCurlSnippet(method, url, headers, requestBody);
      case "java":
        return generateJavaSnippet(method, url, headers, requestBody);
      case "shell":
        return generateShellSnippet(method, url, headers, requestBody);
      case "swift":
        return generateSwiftSnippet(method, url, headers, requestBody);
      case "go":
        return generateGoSnippet(method, url, headers, requestBody);
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
      <div className="fixed right-0 top-0 h-full w-[600px] bg-white dark:bg-[#0D1117] z-50 shadow-2xl transform transition-transform duration-300">
        {/* Header */}
        <div className="h-16 border-b border-gray-200 dark:border-[#2D333B] flex items-center justify-between px-6">
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
            Code Snippets
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
        <div className="border-b border-gray-200 dark:border-[#2D333B] px-6">
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
        <div className="flex-1 overflow-auto p-6">
          <div className="bg-gray-50 dark:bg-[#0D1117] rounded-md border border-gray-200 dark:border-[#2D333B] overflow-hidden">
            <div className="flex items-center justify-between bg-white dark:bg-[#161B22] px-4 py-2 border-b border-gray-200 dark:border-[#2D333B]">
              <span className="text-sm text-gray-600 dark:text-[#8B949E] font-mono">
                {method} {url}
              </span>
              <button
                onClick={copyToClipboard}
                className="px-3 py-1 text-sm bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded-md transition-colors"
              >
                {copied ? "복사됨" : "복사"}
              </button>
            </div>
            <div className="bg-[#0D1117] dark:bg-[#010409] p-4 rounded-md">
              <pre className="text-sm text-[#E6EDF3] whitespace-pre-wrap overflow-x-auto font-mono">
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

function generateTypeScriptSnippet(
  method: string,
  url: string,
  headers: Array<{ key: string; value: string }>,
  requestBody?: any
): string {
  const headerString = headers
    .map((h) => `  "${h.key}": "${h.value}"`)
    .join(",\n");

  let bodyString = "";
  let bodyVar = "";
  if (requestBody && method !== "GET") {
    if (requestBody.type === "json") {
      const bodyObj = requestBody.fields?.reduce((acc: any, field: any) => {
        acc[field.key] = field.value;
        return acc;
      }, {});
      bodyVar = `\n\nconst body: Record<string, any> = ${JSON.stringify(bodyObj, null, 2)};`;
      bodyString = `\n  body: JSON.stringify(body)`;
    }
  }

  return `const response: Response = await fetch("${url}", {
  method: "${method}",
  headers: {
${headerString}
  }${bodyString}
});${bodyVar}`;
}

function generateShellSnippet(
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
      bodyString = ` \\\n  -d '${JSON.stringify(bodyObj)}'`;
    }
  }

  return `curl -X ${method} "${url}" \\\n${headerString}${bodyString}`;
}

function generateSwiftSnippet(
  method: string,
  url: string,
  headers: Array<{ key: string; value: string }>,
  requestBody?: any
): string {
  const headerString = headers
    .map((h) => `    "${h.key}": "${h.value}"`)
    .join(",\n");

  let bodyString = "";
  let bodyVar = "";
  if (requestBody && method !== "GET") {
    if (requestBody.type === "json") {
      // Swift 딕셔너리 형식으로 변환
      const fields = requestBody.fields || [];
      const swiftDictEntries = fields
        .map((field: any) => `    "${field.key}": "${field.value}"`)
        .join(",\n");
      if (swiftDictEntries) {
        bodyVar = `\n\nlet body: [String: Any] = [\n${swiftDictEntries}\n]`;
        bodyString = `\nrequest.httpBody = try? JSONSerialization.data(withJSONObject: body)`;
      }
    }
  }

  return `import Foundation

let url = URL(string: "${url}")!
var request = URLRequest(url: url)
request.httpMethod = "${method}"
request.allHTTPHeaderFields = [
${headerString}
]${bodyString}${bodyVar}

let task = URLSession.shared.dataTask(with: request) { data, response, error in
    // Handle response
}
task.resume()`;
}

function generateGoSnippet(
  method: string,
  url: string,
  headers: Array<{ key: string; value: string }>,
  requestBody?: any
): string {
  const headerString = headers
    .map((h) => `    req.Header.Set("${h.key}", "${h.value}")`)
    .join("\n");

  let bodyString = "";
  let jsonImport = "";
  let bodyVar = "";
  if (requestBody && method !== "GET") {
    if (requestBody.type === "json") {
      jsonImport = 'import (\n    "bytes"\n    "encoding/json"\n    "net/http"\n)';
      // Go map 형식으로 변환
      const fields = requestBody.fields || [];
      const goMapEntries = fields
        .map((field: any) => `        "${field.key}": "${field.value}"`)
        .join(",\n");
      if (goMapEntries) {
        bodyVar = `\n\nbody := map[string]interface{}{\n${goMapEntries}\n}`;
        bodyString = `jsonBody, _ := json.Marshal(body)
req, _ := http.NewRequest("${method}", "${url}", bytes.NewBuffer(jsonBody))`;
      }
    }
  } else {
    jsonImport = 'import "net/http"';
    bodyString = `req, _ := http.NewRequest("${method}", "${url}", nil)`;
  }

  return `${jsonImport}

${bodyString}${bodyVar}
${headerString}

client := &http.Client{}
resp, err := client.Do(req)
if err != nil {
    // Handle error
}
defer resp.Body.Close()`;
}
