import { useState } from "react";
// @ts-expect-error - openapi-snippet has no type definitions
import OpenAPISnippet from "openapi-snippet";
import type { RequestBody } from "../types/schema.types";
import { convertRequestBodyToOpenAPI } from "../utils/schemaConverter";

interface KeyValuePair {
  key: string;
  value: string;
  required?: boolean;
  description?: string;
  type?: string;
}

interface CodeSnippetPanelProps {
  isOpen: boolean;
  onClose: () => void;
  method: string;
  url: string;
  headers: KeyValuePair[];
  requestBody?: RequestBody;
}

// openapi-snippet의 target 매핑
const languageTargets: Record<string, string> = {
  javascript: "javascript_fetch",
  typescript: "javascript_fetch", // TypeScript는 JavaScript와 동일한 target 사용
  python: "python_requests",
  curl: "shell_curl",
  shell: "shell_curl",
  java: "java_okhttp",
  swift: "swift_urlsession",
  go: "go_native",
};

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

// OpenAPI 스펙을 구성하는 함수
function buildOpenAPISpec(
  method: string,
  url: string,
  headers: KeyValuePair[],
  requestBody?: RequestBody
): any {
  const openApiSpec: any = {
    openapi: "3.1.0",
    info: {
      title: "API Documentation",
      version: "1.0.0",
    },
    paths: {},
  };

  // Parameters를 OpenAPI 형식으로 변환 (headers)
  const parameters: any[] = [];
  headers.forEach((header) => {
    if (header.key && header.value) {
      parameters.push({
        name: header.key,
        in: "header",
        required: header.required || false,
        schema: {
          type: "string",
        },
        description: header.description || "",
      });
    }
  });

  // RequestBody를 OpenAPI 형식으로 변환
  let openApiRequestBody = requestBody
    ? convertRequestBodyToOpenAPI(requestBody)
    : null;

  // schemaRef가 있는 경우 $ref를 제거 (components 섹션이 없으므로)
  // openapi-snippet은 $ref를 처리하려면 components 섹션이 필요함
  if (openApiRequestBody?.content) {
    const contentTypes = Object.keys(openApiRequestBody.content);
    for (const contentType of contentTypes) {
      const mediaType = openApiRequestBody.content[contentType];
      // $ref나 ref가 있으면 제거 (이미 generateSnippet에서 schemaRef 체크하지만 안전장치)
      if (mediaType?.schema?.$ref || mediaType?.schema?.ref) {
        // schemaRef가 있는 경우 requestBody를 null로 설정하여 fallback 사용
        openApiRequestBody = null;
        break; // null로 설정했으므로 더 이상 처리할 필요 없음
      }
    }
  }

  // Operation 구성
  const operation: any = {
    summary: `${method} ${url}`,
    operationId: `${method.toLowerCase()}_${url
      .replace(/\//g, "_")
      .replace(/[{}]/g, "")}`,
  };

  if (parameters.length > 0) {
    operation.parameters = parameters;
  }

  if (openApiRequestBody) {
    operation.requestBody = openApiRequestBody;
  }

  // Path 구성
  openApiSpec.paths[url] = {
    [method.toLowerCase()]: operation,
  };

  return openApiSpec;
}

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
    const langConfig = languages.find((l) => l.id === lang);
    if (!langConfig) return "";

    // schemaRef가 있는 경우 openapi-snippet을 사용하지 않고 fallback 사용
    // (components 섹션이 없어서 $ref를 처리할 수 없음)
    if (requestBody?.schemaRef) {
      return generateFallbackSnippet(lang, method, url, headers, requestBody);
    }

    const target = languageTargets[lang];
    if (!target) {
      // 지원하지 않는 언어는 fallback 사용
      return generateFallbackSnippet(lang, method, url, headers, requestBody);
    }

    try {
      // OpenAPI 스펙 구성
      const openApiSpec = buildOpenAPISpec(method, url, headers, requestBody);

      // openapi-snippet을 사용하여 코드 생성
      const result = OpenAPISnippet.getEndpointSnippets(
        openApiSpec,
        url,
        method.toLowerCase(),
        [target]
      );

      // 반환값 처리 (제공하신 예제 + 보완)
      if (result) {
        // result.snippets가 있는 경우
        if (
          result.snippets &&
          Array.isArray(result.snippets) &&
          result.snippets.length > 0
        ) {
          const snippet = result.snippets[0];
          return snippet.content || snippet.snippet || "";
        }
        // result 자체가 배열인 경우
        if (Array.isArray(result) && result.length > 0) {
          const snippet = result[0];
          return (
            snippet.content ||
            snippet.snippet ||
            (typeof snippet === "string" ? snippet : "")
          );
        }
        // result에 직접 content나 snippet이 있는 경우
        if (result.content) return result.content;
        if (result.snippet) return result.snippet;
        if (typeof result === "string") return result;
      }
    } catch {
      // 에러 발생 시 fallback 사용
    }

    // openapi-snippet이 실패하거나 지원하지 않는 경우 fallback
    return generateFallbackSnippet(lang, method, url, headers, requestBody);
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

// Fallback 함수: openapi-snippet이 실패하거나 지원하지 않는 경우
function generateFallbackSnippet(
  lang: string,
  method: string,
  url: string,
  headers: KeyValuePair[],
  requestBody?: RequestBody
): string {
  const headerString = headers
    .filter((h) => h.key && h.value)
    .map((h) => {
      if (lang === "javascript" || lang === "typescript") {
        return `  "${h.key}": "${h.value}"`;
      } else if (lang === "python") {
        return `    "${h.key}": "${h.value}"`;
      } else if (lang === "curl" || lang === "shell") {
        return `  -H "${h.key}: ${h.value}"`;
      } else if (lang === "java") {
        return `      .header("${h.key}", "${h.value}")`;
      } else if (lang === "swift") {
        return `    "${h.key}": "${h.value}"`;
      } else if (lang === "go") {
        return `    req.Header.Set("${h.key}", "${h.value}")`;
      }
      return "";
    })
    .filter(Boolean)
    .join(lang === "curl" || lang === "shell" ? " \\\n" : "\n");

  // RequestBody 처리 개선
  let bodyString = "";
  let bodyVar = "";
  if (requestBody && method !== "GET" && requestBody.type !== "none") {
    // requestBody에서 실제 값을 추출
    const bodyValue = extractRequestBodyValue(requestBody);

    if (bodyValue !== undefined) {
      if (requestBody.type === "json") {
        const bodyJson = JSON.stringify(bodyValue, null, 2);
        if (lang === "javascript") {
          bodyVar = `\n\nconst body = ${bodyJson};`;
          bodyString = `\n  body: JSON.stringify(body)`;
        } else if (lang === "typescript") {
          bodyVar = `\n\nconst body: Record<string, any> = ${bodyJson};`;
          bodyString = `\n  body: JSON.stringify(body)`;
        } else if (lang === "python") {
          bodyVar = `\n\nbody = ${bodyJson}`;
          bodyString = `\n    json=body`;
        } else if (lang === "curl" || lang === "shell") {
          bodyString = ` -d '${JSON.stringify(bodyValue)}'`;
        } else if (lang === "swift") {
          const swiftDict = Object.entries(bodyValue)
            .map(([k, v]) => `    "${k}": ${JSON.stringify(v)}`)
            .join(",\n");
          bodyVar = `\n\nlet body: [String: Any] = [\n${swiftDict}\n]`;
          bodyString = `\nrequest.httpBody = try? JSONSerialization.data(withJSONObject: body)`;
        } else if (lang === "go") {
          const goMap = Object.entries(bodyValue)
            .map(([k, v]) => `        "${k}": ${JSON.stringify(v)}`)
            .join(",\n");
          bodyVar = `\n\nbody := map[string]interface{}{\n${goMap}\n}`;
          bodyString = `jsonBody, _ := json.Marshal(body)\nreq, _ := http.NewRequest("${method}", "${url}", bytes.NewBuffer(jsonBody))`;
        }
      } else if (requestBody.type === "form-data") {
        // Form-data 처리
        if (lang === "python") {
          bodyVar = `\n\nfiles = ${JSON.stringify(bodyValue)}`;
          bodyString = `\n    files=files`;
        } else if (lang === "curl" || lang === "shell") {
          const formData = Object.entries(bodyValue)
            .map(([k, v]) => `-F "${k}=${v}"`)
            .join(" \\\n  ");
          bodyString = ` \\\n  ${formData}`;
        }
      } else if (requestBody.type === "x-www-form-urlencoded") {
        // URL-encoded 처리
        if (lang === "python") {
          bodyVar = `\n\ndata = ${JSON.stringify(bodyValue)}`;
          bodyString = `\n    data=data`;
        } else if (lang === "curl" || lang === "shell") {
          const formData = Object.entries(bodyValue)
            .map(
              ([k, v]) =>
                `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`
            )
            .join("&");
          bodyString = ` -d '${formData}'`;
        }
      } else if (requestBody.type === "xml") {
        // XML 처리
        if (lang === "curl" || lang === "shell") {
          bodyString = ` -d '${bodyValue}'`;
        }
      }
    }
  }

  switch (lang) {
    case "javascript":
      return `const response = await fetch("${url}", {
  method: "${method}",
  headers: {
${headerString}
  }${bodyString}
});${bodyVar}`;

    case "typescript":
      return `const response: Response = await fetch("${url}", {
  method: "${method}",
  headers: {
${headerString}
  }${bodyString}
});${bodyVar}`;

    case "python":
      return `import requests

response = requests.${method.toLowerCase()}( 
    "${url}",
    headers={
${headerString}
    }${bodyString}
)${bodyVar}`;

    case "curl":
      return `curl -X ${method} "${url}" \\
${headerString}${bodyString ? ` \\` : ""}${bodyString}`;

    case "shell":
      return `curl -X ${method} "${url}" \\\n${headerString}${bodyString}`;

    case "java":
      return `CloseableHttpClient httpClient = HttpClients.createDefault();
Http${method.charAt(0) + method.slice(1).toLowerCase()} request = new Http${
        method.charAt(0) + method.slice(1).toLowerCase()
      }("${url}");

${headerString}

CloseableHttpResponse response = httpClient.execute(request);`;

    case "swift":
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

    case "go": {
      const jsonImport = bodyVar
        ? 'import (\n    "bytes"\n    "encoding/json"\n    "net/http"\n)'
        : 'import "net/http"';
      const reqLine = bodyVar
        ? bodyString
        : `req, _ := http.NewRequest("${method}", "${url}", nil)`;
      return `${jsonImport}

${reqLine}${bodyVar}
${headerString}

client := &http.Client{}
resp, err := client.Do(req)
if err != nil {
    // Handle error
}
defer resp.Body.Close()`;
    }

    default:
      return "";
  }
}

// requestBody에서 실제 값을 추출하는 함수 (재귀)
function extractRequestBodyValue(field: any): any {
  if (!field) return undefined;

  // SchemaField 타입인 경우
  if (field.schemaType) {
    const schemaType = field.schemaType;

    if (schemaType.kind === "primitive") {
      // Primitive 타입: value가 있으면 사용, 없으면 mockExpression 또는 기본값
      return (
        field.value || field.mockExpression || getDefaultValue(schemaType.type)
      );
    } else if (schemaType.kind === "object") {
      // Object 타입: properties를 재귀적으로 처리
      const obj: any = {};
      if (schemaType.properties) {
        schemaType.properties.forEach((prop: any) => {
          const value = extractRequestBodyValue(prop);
          if (value !== undefined) {
            obj[prop.key] = value;
          }
        });
      }
      return Object.keys(obj).length > 0 ? obj : undefined;
    } else if (schemaType.kind === "array") {
      // Array 타입: items를 재귀적으로 처리
      const itemValue = extractRequestBodyValue({
        schemaType: schemaType.items,
      });
      return itemValue !== undefined ? [itemValue] : undefined;
    } else if (schemaType.kind === "ref") {
      // Ref 타입: 스키마 참조이므로 기본값 반환
      return undefined;
    }
  }

  // 일반 객체인 경우 (fields 배열)
  if (Array.isArray(field.fields)) {
    const obj: any = {};
    field.fields.forEach((f: any) => {
      const value = extractRequestBodyValue(f);
      if (value !== undefined) {
        obj[f.key] = value;
      }
    });
    return Object.keys(obj).length > 0 ? obj : undefined;
  }

  return undefined;
}

function getDefaultValue(type: string): any {
  switch (type) {
    case "string":
      return "";
    case "integer":
    case "number":
      return 0;
    case "boolean":
      return false;
    case "file":
      return undefined;
    default:
      return "";
  }
}
