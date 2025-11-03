interface KeyValuePair { key: string; value: string }

interface RequestBodyField { key: string; value: string; type: string; description?: string; required?: boolean }
interface RequestBody {
  type: "none" | "form-data" | "x-www-form-urlencoded" | "json" | "xml";
  contentType: string;
  fields: RequestBodyField[];
}

interface StatusCode { code: string; type: "Success" | "Error"; message: string }

interface BuildYamlData {
  method: string;
  url: string;
  description: string;
  tags?: string;
  headers?: KeyValuePair[];
  requestBody?: RequestBody;
  statusCodes?: StatusCode[];
}

function indent(lines: string, level: number = 1) {
  const pad = "  ".repeat(level);
  return lines
    .split("\n")
    .map((l) => (l.length ? pad + l : l))
    .join("\n");
}

export function buildOpenApiYaml(data: BuildYamlData): string {
  const { method, url, description, tags, requestBody, statusCodes } = data;
  const methodKey = method.toLowerCase();

  const tagArray = (tags || "")
    .split(",")
    .map((t) => t.trim())
    .filter(Boolean);

  const header = `openapi: 3.1.0
info:
  title: API Documentation
  version: 1.0.0`;

  let pathBlock = `${url}:
  ${methodKey}:
    summary: ${description || ""}
    description: ${description || ""}
    deprecated: false
    tags:
${tagArray.map((t) => `      - ${t || "OTHERS"}`).join("\n")}`;

  // RequestBody (json only for preview)
  if (requestBody && requestBody.type !== "none") {
    if (requestBody.type === "json") {
      const required = requestBody.fields
        .filter((f) => f.required)
        .map((f) => f.key)
        .filter(Boolean);
      const properties = requestBody.fields
        .filter((f) => f.key)
        .map((f) => {
          const mock = f.value ? `\n            x-ouroboros-mock: '${f.value}'` : "";
          return `        ${f.key}:
          type: ${f.type || "string"}
          description: ${f.description || ""}${mock}`;
        })
        .join("\n");

      const reqSchema = `schema:
          type: object
          title: ${url.replace(/\//g, "_")} Request
          description: Request body
          properties:
${properties || "            {}"}`;

      const reqRequired = required.length
        ? `\n          required:\n${required.map((r) => `          - ${r}`).join("\n")}`
        : "";

      pathBlock += `\n    requestBody:
      description: Generated request body
      required: ${required.length > 0 ? "true" : "false"}
      content:
        application/json:
          ${reqSchema}${reqRequired}`;
    } else {
      pathBlock += `\n    requestBody:
      required: false`;
    }
  }

  // Responses
  const codes = statusCodes || [];
  if (codes.length > 0) {
    pathBlock += `\n    responses:`;
    codes.forEach((c) => {
      pathBlock += `\n      '${c.code}':
        description: ${c.message || (c.type === "Success" ? "Success" : "Error")}`;
    });
  } else {
    pathBlock += `\n    responses: {}`;
  }

  // Ouroboros vendor fields (defaults)
  pathBlock += `\n    x-ouroboros-id: ${cryptoRandomUuid()}
    x-ouroboros-progress: mock
    x-ouroboros-tag: none
    x-ouroboros-isvalid: true
    x-ouroboros-diff: none`;

  const yaml = `${header}
paths:
${indent(pathBlock, 1)}`;
  return yaml;
}

function cryptoRandomUuid() {
  // Lightweight UUID v4 fallback for preview/export (not cryptographically strong)
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function (c) {
    const r = (Math.random() * 16) | 0;
    const v = c === "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

export function downloadYaml(content: string, filename: string = "api.yaml") {
  const blob = new Blob([content], { type: "text/yaml" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

// Build from backend specs (aggregate)
type RestApiSpec = {
  id: string;
  path: string;
  method: string;
  summary?: string;
  description?: string;
  deprecated?: boolean;
  tags?: string[];
  parameters?: any[];
  requestBody?: any;
  responses?: Record<string, any>;
  security?: any[];
  progress?: string;
  tag?: string;
  isValid?: boolean;
  diff?: string;
};

type SchemaResponse = {
  schemaName: string;
  type: string;
  title?: string;
  description?: string;
  properties?: Record<string, any>;
  required?: string[];
  orders?: string[];
  xmlName?: string;
};

export function buildOpenApiYamlFromSpecs(
  specs: RestApiSpec[],
  schemas?: SchemaResponse[]
): string {
  const header = `openapi: 3.1.0\ninfo:\n  title: API Documentation\n  version: 1.0.0`;

  // group by path
  const pathMap: Record<string, string[]> = {};
  specs.forEach((s) => {
    const methodKey = (s.method || "get").toLowerCase();
    const tagsBlock = (s.tags || []).map((t) => `      - ${t}`).join("\n");
    let op = `    ${methodKey}:\n      summary: ${s.summary || s.description || ""}\n      description: ${s.description || s.summary || ""}\n      deprecated: ${s.deprecated ? "true" : "false"}\n      tags:\n${tagsBlock || "      - OTHERS"}`;

    // RequestBody if exists
    if (s.requestBody !== undefined) {
      // pass-through (assume already OpenAPI-like structure)
      const rb = JSON.stringify(s.requestBody, null, 2)
        .split("\n")
        .map((l) => (l ? "      " + l : l))
        .join("\n");
      op += `\n      requestBody: ${rb.startsWith("      {") ? "|" : ""}\n${rb}`;
    }
    // Responses
    if (s.responses && Object.keys(s.responses).length > 0) {
      op += `\n      responses:`;
      Object.entries(s.responses).forEach(([code, resp]) => {
        const desc = (resp as any).description || "";
        op += `\n        '${code}':\n          description: ${desc}`;
      });
    } else {
      op += `\n      responses: {}`;
    }

    // vendor fields
    op += `\n      x-ouroboros-id: ${s.id}\n      x-ouroboros-progress: ${s.progress || "mock"}\n      x-ouroboros-tag: ${s.tag || "none"}\n      x-ouroboros-isvalid: ${s.isValid === false ? "false" : "true"}\n      x-ouroboros-diff: ${s.diff || "none"}`;

    const block = op;
    if (!pathMap[s.path]) pathMap[s.path] = [];
    pathMap[s.path].push(block);
  });

  let pathsSection = "paths:\n";
  Object.entries(pathMap).forEach(([path, ops]) => {
    pathsSection += `  ${path}:\n` + ops.join("\n");
    pathsSection += "\n";
  });

  // components.schemas
  let componentsSection = "";
  if (schemas && schemas.length > 0) {
    componentsSection += "components:\n  schemas:\n";
    schemas.forEach((s) => {
      componentsSection += `    ${s.schemaName}:\n      type: ${s.type || "object"}\n`;
      if (s.title) componentsSection += `      title: ${s.title}\n`;
      if (s.description) componentsSection += `      description: ${s.description}\n`;
      if (s.properties && Object.keys(s.properties).length > 0) {
        componentsSection += `      properties:\n`;
        Object.entries(s.properties).forEach(([name, def]) => {
          const d = def as any;
          componentsSection += `        ${name}:\n          type: ${d.type || "string"}\n`;
          if (d.description) componentsSection += `          description: ${d.description}\n`;
          if (d.mockExpression)
            componentsSection += `          x-ouroboros-mock: '${d.mockExpression}'\n`;
          if (d.ref) componentsSection += `          $ref: '${d.ref}'\n`;
        });
      }
      if (s.required && s.required.length > 0) {
        componentsSection += `      required:\n`;
        s.required.forEach((r) => {
          componentsSection += `      - ${r}\n`;
        });
      }
      if (s.orders && s.orders.length > 0) {
        componentsSection += `      x-ouroboros-orders:\n`;
        s.orders.forEach((o) => {
          componentsSection += `      - ${o}\n`;
        });
      }
    });
  }

  return [header, pathsSection, componentsSection].filter(Boolean).join("\n");
}


