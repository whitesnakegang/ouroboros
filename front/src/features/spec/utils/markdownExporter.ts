interface MarkdownExportData {
  method: string;
  url: string;
  description: string;
  tags?: string;
  owner?: string;
  headers?: Array<{ key: string; value: string }>;
  requestBody?: any;
  statusCodes?: Array<{ code: string; type: string; message: string }>;
}

export function exportToMarkdown(data: MarkdownExportData): string {
  const {
    method,
    url,
    description,
    tags,
    owner,
    headers,
    requestBody,
    statusCodes,
  } = data;

  // 헤더: 간결하고 노션/블로그 친화적 형식
  let markdown = `## ${method.toUpperCase()} \`${url}\`\n\n`;
  if (description) markdown += `${description}\n\n`;
  if (tags || owner) {
    markdown += `- Tags: ${tags || "-"}\n`;
    if (owner) markdown += `- Owner: ${owner}\n`;
    markdown += `\n`;
  }

  // Request Headers
  if (headers && headers.length > 0) {
    markdown += `### Request\n\n`;
    markdown += `#### Headers\n\n`;

    headers.forEach((header) => {
      markdown += `**${header.key}**\n`;
      markdown += `\`\`\`\n${header.value}\n\`\`\`\n\n`;
    });
  }

  // Request Body
  if (requestBody && requestBody.type !== "none") {
    markdown += `#### Body\n\n`;
    markdown += `Content-Type: \`${
      requestBody.contentType || requestBody.type
    }\`\n\n`;

    if (
      requestBody.type === "json" ||
      requestBody.type === "form-data" ||
      requestBody.type === "x-www-form-urlencoded"
    ) {
      if (requestBody.fields && requestBody.fields.length > 0) {
        markdown += `| Field | Type | Value | Description |\n`;
        markdown += `|---|---|---|---|\n`;
        requestBody.fields.forEach((field: { key: string; type: string; value?: string; description?: string }) => {
          markdown += `| ${field.key} | ${field.type} | ${
            field.value || "-"
          } | ${field.description || "-"} |\n`;
        });
        markdown += `\n`;
      }
    }
  }

  // Response
  if (statusCodes && statusCodes.length > 0) {
    markdown += `### Response\n\n`;
    markdown += `#### Status Codes\n\n`;

    // Success codes
    const successCodes = statusCodes.filter((s) => s.type === "Success");
    if (successCodes.length > 0) {
      markdown += `- Success\n\n`;
      successCodes.forEach((statusCode) => {
        markdown += `  - ${statusCode.code}: ${statusCode.message}\n`;
      });
      markdown += `\n`;
    }

    // Error codes
    const errorCodes = statusCodes.filter((s) => s.type === "Error");
    if (errorCodes.length > 0) {
      markdown += `- Error\n\n`;
      errorCodes.forEach((statusCode) => {
        markdown += `  - ${statusCode.code}: ${statusCode.message}\n`;
      });
      markdown += `\n`;
    }
  }

  return markdown;
}

export function downloadMarkdown(
  content: string,
  filename: string = "api-spec.md"
) {
  const blob = new Blob([content], { type: "text/markdown" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

// Aggregate export for all endpoints (Notion/블로그 친화 포맷)
type RestApiSpec = {
  id: string;
  path: string;
  method: string;
  summary?: string;
  description?: string;
  tags?: string[];
  progress?: string;
  tag?: string;
  parameters?: unknown[];
  requestBody?: any;
  responses?: Record<string, any>;
};

export function exportAllToMarkdown(specs: RestApiSpec[]): string {
  let md = `# API 문서\n\n`;
  // 전체를 표 형식으로 요약 (노션/블로그 친화)
  md += `| Method | Path | Summary | Tags | Responses |\n`;
  md += `|---|---|---|---|---|\n`;
  specs.forEach((s) => {
    const summary = (s.summary || s.description || "").replace(/\n/g, " ");
    const tags = s.tags && s.tags.length ? s.tags.join(", ") : "-";
    let resp = "-";
    if (s.responses && Object.keys(s.responses).length > 0) {
      const pairs = Object.entries(s.responses).map(([code, val]) => {
        const v = val as any;
        return `${code}${v?.description ? `: ${String(v.description).replace(/\n/g, ' ')}` : ''}`;
      });
      resp = pairs.join("<br>");
    }
    md += `| ${s.method.toUpperCase()} | ` + "`" + `${s.path}` + "`" + ` | ${summary} | ${tags} | ${resp} |\n`;
  });

  // 그룹별 상세 표 (선택적으로 더 자세히 보고 싶을 때)
  const grouped: Record<string, RestApiSpec[]> = {};
  specs.forEach((s) => {
    const group = (s.tags && s.tags[0]) || "OTHERS";
    if (!grouped[group]) grouped[group] = [];
    grouped[group].push(s);
  });
  md += `\n---\n\n`;
  Object.entries(grouped).forEach(([group, items]) => {
    md += `## ${group}\n\n`;
    md += `| Method | Path | Summary | Responses |\n`;
    md += `|---|---|---|---|\n`;
    items.forEach((s) => {
      const summary = (s.summary || s.description || "").replace(/\n/g, " ");
      let resp = "-";
      if (s.responses && Object.keys(s.responses).length > 0) {
        const pairs = Object.entries(s.responses).map(([code, val]) => {
          const v = val as any;
          return `${code}${v?.description ? `: ${String(v.description).replace(/\n/g, ' ')}` : ''}`;
        });
        resp = pairs.join("<br>");
      }
      md += `| ${s.method.toUpperCase()} | ` + "`" + `${s.path}` + "`" + ` | ${summary} | ${resp} |\n`;
    });
    md += `\n`;
  });

  md += `\n*Generated by Ouroboros* — ${new Date().toLocaleDateString("ko-KR")}\n`;
  return md;
}