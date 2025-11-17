import { useState, useEffect } from "react";
import type { RestApiSpecResponse } from "../services/api";
import { getSchema } from "../services/api";

interface CodeSnippetPanelProps {
  isOpen: boolean;
  onClose: () => void;
  spec: RestApiSpecResponse | null;
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

// 스키마에서 기본값 생성 (재귀)
function generateDefaultValue(schema: any, schemas: Record<string, any> = {}): any {
  if (!schema) return undefined;

  // $ref 또는 ref 처리
  if (schema.$ref || schema.ref) {
    const refName = (schema.$ref || schema.ref)
      .replace("#/components/schemas/", "")
      .replace("components/schemas/", "");
    const refSchema = schemas[refName];
    if (refSchema) {
      return generateDefaultValue(refSchema, schemas);
    }
    // 스키마를 찾을 수 없으면 빈 객체
    return {};
  }

  // array 타입
  if (schema.type === "array") {
    if (schema.items) {
      const itemValue = generateDefaultValue(schema.items, schemas);
      return itemValue !== undefined ? [itemValue] : [];
    }
    return [];
  }

  // object 타입
  if (schema.type === "object" && schema.properties) {
    const obj: any = {};
    Object.entries(schema.properties).forEach(([key, propSchema]: [string, any]) => {
      const value = generateDefaultValue(propSchema, schemas);
      if (value !== undefined) {
        obj[key] = value;
      }
    });
    return Object.keys(obj).length > 0 ? obj : undefined;
  }

  // primitive 타입
  if (schema.type === "string") return "string";
  if (schema.type === "integer" || schema.type === "number") return 0;
  if (schema.type === "boolean") return false;

  return undefined;
}

// RequestBody에서 body 값 생성
async function generateBodyValue(requestBody: any): Promise<any> {
  if (!requestBody || !requestBody.content) return undefined;

  const contentType = Object.keys(requestBody.content)[0];
  if (!contentType) return undefined;

  const mediaType = requestBody.content[contentType];
  if (!mediaType || !mediaType.schema) return undefined;

  const schema = mediaType.schema;
  const schemas: Record<string, any> = {};

  // $ref 또는 ref가 있으면 스키마 조회
  if (schema.$ref || schema.ref) {
    const refName = (schema.$ref || schema.ref)
      .replace("#/components/schemas/", "")
      .replace("components/schemas/", "");
    try {
      const response = await getSchema(refName);
      schemas[refName] = response.data;
    } catch {
      // 스키마 조회 실패 시 빈 객체 반환
      return {};
    }
  }

  // array의 items에 ref가 있는 경우
  if (schema.type === "array" && schema.items) {
    if (schema.items.$ref || schema.items.ref) {
      const refName = (schema.items.$ref || schema.items.ref)
        .replace("#/components/schemas/", "")
        .replace("components/schemas/", "");
      try {
        const response = await getSchema(refName);
        const refSchema = response.data;
        schemas[refName] = refSchema;
        // ref 스키마를 items로 사용하여 generateDefaultValue 호출
        const itemValue = generateDefaultValue(refSchema, schemas);
        return itemValue !== undefined ? [itemValue] : [{}];
      } catch {
        return [{}];
      }
    }
  }

  return generateDefaultValue(schema, schemas);
}

export function CodeSnippetPanel({
  isOpen,
  onClose,
  spec,
}: CodeSnippetPanelProps) {
  const [selectedLanguage, setSelectedLanguage] = useState("javascript");
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
        const method = spec.method;
        const url = spec.path;
        const baseUrl = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
        const fullUrl = `${baseUrl}${url}`;

        // Headers 추출
        const headers: Record<string, string> = {};
        if (spec.parameters && Array.isArray(spec.parameters)) {
          spec.parameters.forEach((param: any) => {
            if (param.in === "header" && param.name) {
              headers[param.name] = param.schema?.default || "";
            }
          });
        }

        // RequestBody 처리
        let bodyValue: any = undefined;
        if (spec.requestBody && method !== "GET") {
          bodyValue = await generateBodyValue(spec.requestBody);
        }

        // 스니펫 생성
        const snippet = generateCodeSnippet(
          selectedLanguage,
          method,
          fullUrl,
          headers,
          bodyValue
        );
        setSnippet(snippet);
      } catch (error) {
        console.error("스니펫 생성 실패:", error);
        setSnippet("// 스니펫 생성 중 오류가 발생했습니다.");
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
                {spec.method} {spec.path}
              </span>
              <button
                onClick={copyToClipboard}
                disabled={loading || !snippet}
                className="px-3 py-1 text-sm bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded-md transition-all active:translate-y-[1px] focus:outline-none focus-visible:outline-none disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {copied ? "복사됨" : "복사"}
              </button>
            </div>
            <div className="bg-[#0D1117] dark:bg-[#010409] p-4 rounded-md">
              {loading ? (
                <div className="text-sm text-[#E6EDF3] text-center py-8">
                  스니펫 생성 중...
                </div>
              ) : (
              <pre className="text-sm text-[#E6EDF3] whitespace-pre-wrap overflow-x-auto font-mono">
                  <code>{snippet || "// 스니펫을 생성할 수 없습니다."}</code>
              </pre>
              )}
            </div>
          </div>
        </div>
      </div>
    </>
  );
}

// 코드 스니펫 생성 함수
function generateCodeSnippet(
  lang: string,
  method: string,
  url: string,
  headers: Record<string, string>,
  bodyValue?: any
): string {
  const headerEntries = Object.entries(headers).filter(([_, v]) => v);
  const hasBody = bodyValue !== undefined && method !== "GET";

  switch (lang) {
    case "javascript": {
      const headerStr = headerEntries
        .map(([k, v]) => `    "${k}": "${v}"`)
        .join(",\n");
      const headersPart = headerStr
        ? `  headers: {\n${headerStr}\n  },`
        : "";
      const bodyPart = hasBody
        ? `\n  body: JSON.stringify(${JSON.stringify(bodyValue, null, 2)})`
        : "";

      return `const options = {
  method: '${method}'${headersPart ? `,\n${headersPart}` : ""}${bodyPart}
};

fetch('${url}', options)
  .then(response => response.json())
  .then(data => console.log(data))
  .catch(err => console.error(err));`;
    }

    case "typescript": {
      const headerStr = headerEntries
        .map(([k, v]) => `    "${k}": "${v}"`)
        .join(",\n");
      const headersPart = headerStr
        ? `  headers: {\n${headerStr}\n  },`
        : "";
      const bodyPart = hasBody
        ? `\n  body: JSON.stringify(${JSON.stringify(bodyValue, null, 2)})`
        : "";

      return `const options: RequestInit = {
  method: '${method}'${headersPart ? `,\n${headersPart}` : ""}${bodyPart}
};

fetch('${url}', options)
  .then((response: Response) => response.json())
  .then((data: any) => console.log(data))
  .catch((err: Error) => console.error(err));`;
    }

    case "python": {
      const headerStr = headerEntries
        .map(([k, v]) => `    "${k}": "${v}"`)
        .join(",\n");
      const headersPart = headerStr ? `    headers={\n${headerStr}\n    },\n` : "";
      const bodyPart = hasBody
        ? `    json=${JSON.stringify(bodyValue, null, 2)},\n`
        : "";

      return `import requests

response = requests.${method.toLowerCase()}( 
    '${url}',
${headersPart}${bodyPart})
print(response.json())`;
    }

    case "curl": {
      const headerParts = headerEntries.map(([k, v]) => `  -H "${k}: ${v}"`);
      const bodyPart = hasBody
        ? `  -d '${JSON.stringify(bodyValue)}'`
        : "";

      return `curl -X ${method} '${url}' \\
${headerParts.join(" \\\n")}${bodyPart ? ` \\\n${bodyPart}` : ""}`;
    }

    case "shell": {
      const headerParts = headerEntries.map(([k, v]) => `  -H "${k}: ${v}"`);
      const bodyPart = hasBody
        ? `  -d '${JSON.stringify(bodyValue)}'`
        : "";

      return `curl -X ${method} '${url}' \\
${headerParts.join(" \\\n")}${bodyPart ? ` \\\n${bodyPart}` : ""}`;
    }

    case "java": {
      const headerParts = headerEntries
        .map(([k, v]) => `      .header("${k}", "${v}")`)
        .join("\n");
      const bodyPart = hasBody
        ? `      .body(JSON.stringify(${JSON.stringify(bodyValue)}))`
        : "";

      return `import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create("${url}"))
      .method("${method}", ${bodyPart ? `HttpRequest.BodyPublishers.ofString(${JSON.stringify(JSON.stringify(bodyValue))})` : "HttpRequest.BodyPublishers.noBody()"})
${headerParts}

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());`;
    }

    case "swift": {
      const headerStr = headerEntries
        .map(([k, v]) => `    "${k}": "${v}"`)
        .join(",\n");
      const headersPart = headerStr ? `    ${headerStr}\n  ]` : "  ]";
      const bodyPart = hasBody
        ? `\n    request.httpBody = try? JSONSerialization.data(withJSONObject: ${JSON.stringify(bodyValue)})`
        : "";

      return `import Foundation

let url = URL(string: "${url}")!
var request = URLRequest(url: url)
request.httpMethod = "${method}"
request.setValue("application/json", forHTTPHeaderField: "Content-Type")
request.allHTTPHeaderFields = [
${headersPart}${bodyPart}

let task = URLSession.shared.dataTask(with: request) { data, response, error in
    if let data = data {
        let json = try? JSONSerialization.jsonObject(with: data)
        print(json ?? "No data")
    }
}
task.resume()`;
    }

    case "go": {
      const headerParts = headerEntries
        .map(([k, v]) => `    req.Header.Set("${k}", "${v}")`)
        .join("\n");
      const bodyPart = hasBody
        ? `\n    jsonBody, _ := json.Marshal(${JSON.stringify(bodyValue)})\n    req, _ = http.NewRequest("${method}", "${url}", bytes.NewBuffer(jsonBody))`
        : `\n    req, _ := http.NewRequest("${method}", "${url}", nil)`;

      return `package main

import (
    "bytes"
    "encoding/json"
    "net/http"
)

${bodyPart}
${headerParts}

client := &http.Client{}
resp, err := client.Do(req)
if err != nil {
    panic(err)
}
defer resp.Body.Close()`;
    }

    default:
      return `// ${lang} 언어는 아직 지원되지 않습니다.`;
  }
}
