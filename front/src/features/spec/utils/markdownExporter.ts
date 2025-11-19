import yaml from "js-yaml";
import { getSchema } from "../services/api";
import { parseOpenAPISchemaToSchemaField } from "./schemaConverter";
import type { SchemaField } from "../types/schema.types";
import { isRefSchema, isObjectSchema } from "../types/schema.types";

interface MarkdownExportData {
  method: string;
  url: string;
  description: string;
  tags?: string;
  owner?: string;
  headers?: Array<{ key: string; value: string }>;
  requestBody?: any;
  statusCodes?: Array<{
    code: string;
    type: string;
    message: string;
    schema?: any;
  }>;
}

// Helper function to get fields from schema
async function getSchemaFields(schemaRef: string): Promise<SchemaField[]> {
  if (!schemaRef) {
    return [];
  }

  try {
    const response = await getSchema(schemaRef);
    const schemaData = response?.data;

    if (schemaData?.properties) {
      const requiredFields = schemaData.required || [];
      return Object.entries(schemaData.properties)
        .map(([key, propSchema]: [string, any]) => {
          try {
            const field = parseOpenAPISchemaToSchemaField(key, propSchema);
            field.required = requiredFields.includes(key);
            return field;
          } catch (err) {
            console.error(`Failed to parse field ${key}:`, err);
            return null;
          }
        })
        .filter((field): field is SchemaField => field !== null);
    }
    return [];
  } catch (error) {
    console.error(`Failed to load schema fields for ${schemaRef}:`, error);
    return [];
  }
}

// Helper function to format field type
function getFieldTypeString(field: SchemaField): string {
  if (field.schemaType.kind === "primitive") {
    return field.schemaType.type;
  } else if (field.schemaType.kind === "object") {
    return "object";
  } else if (field.schemaType.kind === "array") {
    const itemType =
      field.schemaType.items.kind === "primitive"
        ? field.schemaType.items.type
        : field.schemaType.items.kind === "object"
        ? "object"
        : field.schemaType.items.kind === "ref"
        ? field.schemaType.items.schemaName
        : "unknown";
    return `${itemType}[]`;
  } else if (field.schemaType.kind === "ref") {
    return field.schemaType.schemaName;
  }
  return "string";
}

export async function exportToMarkdown(
  data: MarkdownExportData
): Promise<string> {
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

  // Tags and Owner 정보를 명확하게 표시
  if (tags || owner) {
    if (tags) {
      markdown += `**Tags**: ${tags}\n`;
    }
    if (owner) {
      markdown += `**Owner**: ${owner}\n`;
    }
    markdown += `\n`;
  }

  // Request Section
  const hasRequest =
    (headers && headers.length > 0) ||
    (requestBody && requestBody.type !== "none");
  if (hasRequest) {
    markdown += `### Request\n\n`;

    // Request Headers
    if (headers && headers.length > 0) {
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
        let fieldsToDisplay: Array<{
          key: string;
          type: string;
          value?: string;
          description?: string;
          required?: boolean;
        }> = [];

        // Check for schema reference
        if (requestBody.schemaRef) {
          const schemaFields = await getSchemaFields(requestBody.schemaRef);
          fieldsToDisplay = schemaFields.map((field) => ({
            key: field.key,
            type: getFieldTypeString(field),
            description: field.description || undefined,
            required: field.required,
          }));
        } else if (requestBody.rootSchemaType) {
          // Check if rootSchemaType is ref
          if (
            isRefSchema(requestBody.rootSchemaType) &&
            requestBody.rootSchemaType.schemaName
          ) {
            const schemaFields = await getSchemaFields(
              requestBody.rootSchemaType.schemaName
            );
            fieldsToDisplay = schemaFields.map((field) => ({
              key: field.key,
              type: getFieldTypeString(field),
              description: field.description || undefined,
              required: field.required,
            }));
          } else if (
            isObjectSchema(requestBody.rootSchemaType) &&
            requestBody.rootSchemaType.properties
          ) {
            // Object schema with properties
            fieldsToDisplay = requestBody.rootSchemaType.properties.map(
              (field: SchemaField) => ({
                key: field.key,
                type: getFieldTypeString(field),
                description: field.description || undefined,
                required: field.required,
              })
            );
          }
        } else if (requestBody.fields && requestBody.fields.length > 0) {
          // Use existing fields
          fieldsToDisplay = requestBody.fields.map(
            (field: {
              key: string;
              type?: string;
              description?: string;
              required?: boolean;
              schemaType?: any;
            }) => ({
              key: field.key,
              type:
                field.type ||
                (field.schemaType
                  ? getFieldTypeString(field as SchemaField)
                  : "string"),
              description: field.description || undefined,
              required: field.required,
            })
          );
        }

        if (fieldsToDisplay.length > 0) {
          markdown += `| Field | Type | Required | Description |\n`;
          markdown += `|---|---|---|---|\n`;
          fieldsToDisplay.forEach((field) => {
            markdown += `| ${field.key} | ${field.type} | ${
              field.required ? "Required" : "Optional"
            } | ${field.description || "-"} |\n`;
          });
          markdown += `\n`;
        }
      }
    }
  }

  // Response
  if (statusCodes && statusCodes.length > 0) {
    markdown += `### Response\n\n`;

    // Process each status code
    for (const statusCode of statusCodes) {
      markdown += `#### Status Code: \`${statusCode.code}\` (${statusCode.type})\n\n`;
      markdown += `${statusCode.message}\n\n`;

      // Check for schema in status code
      if (statusCode.schema) {
        let fieldsToDisplay: Array<{
          key: string;
          type: string;
          description?: string;
          required?: boolean;
        }> = [];

        // Check if schema has ref
        if (statusCode.schema.ref) {
          const schemaFields = await getSchemaFields(statusCode.schema.ref);
          fieldsToDisplay = schemaFields.map((field) => ({
            key: field.key,
            type: getFieldTypeString(field),
            description: field.description || undefined,
            required: field.required,
          }));
        } else if (statusCode.schema.isArray && statusCode.schema.ref) {
          // Array of schema reference
          const schemaFields = await getSchemaFields(statusCode.schema.ref);
          fieldsToDisplay = schemaFields.map((field) => ({
            key: field.key,
            type: getFieldTypeString(field),
            description: field.description || undefined,
            required: field.required,
          }));
          markdown += `**Type**: \`array\`\n\n`;
        } else if (statusCode.schema.properties) {
          // Inline schema properties
          const requiredFields = statusCode.schema.required || [];
          fieldsToDisplay = Object.entries(statusCode.schema.properties).map(
            ([key, prop]: [string, any]) => ({
              key,
              type: prop.type || "string",
              description: prop.description || undefined,
              required: requiredFields.includes(key),
            })
          );
        }

        if (fieldsToDisplay.length > 0) {
          markdown += `**Schema Fields**:\n\n`;
          markdown += `| Field | Type | Required | Description |\n`;
          markdown += `|---|---|---|---|\n`;
          fieldsToDisplay.forEach((field) => {
            markdown += `| ${field.key} | ${field.type} | ${
              field.required ? "Required" : "Optional"
            } | ${field.description || "-"} |\n`;
          });
          markdown += `\n`;
        } else if (statusCode.schema.ref) {
          markdown += `**Schema Reference**: \`${statusCode.schema.ref}\`\n\n`;
        }
      }

      markdown += `---\n\n`;
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

/**
 * Convert AsyncAPI (WebSocket) YAML content to Markdown
 * Formats channels, operations (send/receive), and messages with full details.
 */
export function convertWsYamlToMarkdown(yamlContent: string): string {
  let md = `# WebSocket API Documentation\n\n`;

  try {
    const doc = parseSimpleYaml(yamlContent);
    if (!doc) return md + "No AsyncAPI content.\n";

    // Header info
    const info = doc.info || {};
    if (info.title) md += `**Title**: ${info.title}\n`;
    if (info.version) md += `**Version**: ${info.version}\n`;

    // Owner (from x-ouroboros-summary or info.summary)
    const owner = doc["x-ouroboros-summary"] || info.summary;
    if (owner) {
      md += `**Owner**: ${owner}\n`;
    }

    // Entry Point and Protocol (from x-ouroboros metadata or default)
    const entryPoint =
      doc["x-ouroboros-entrypoint"] || doc.servers?.[0]?.url || "/ws";
    const protocol =
      doc["x-ouroboros-protocol"] ||
      (entryPoint.startsWith("wss://") ? "wss" : "ws");
    md += `**Entry Point**: \`${entryPoint}\`\n`;
    md += `**Protocol**: \`${protocol.toUpperCase()}\`\n`;

    md += `\n---\n\n`;

    // Channels overview table
    const channels = doc.channels || {};
    const messages = (doc.components && doc.components.messages) || {};
    const schemas = (doc.components && doc.components.schemas) || {};

    if (Object.keys(channels).length > 0) {
      md += `## Channels Overview\n\n`;
      md += `| Channel | Address | Receiver Messages | Reply Messages |\n`;
      md += `|---|---|---|---|\n`;
      Object.entries(channels).forEach(([channelName, ch]: [string, any]) => {
        const address = (ch as any).address || "-";

        // Collect receiver messages
        const receiverRefs: string[] = [];
        const collectReceiverRefs = (val: any) => {
          if (!val) return;
          if (Array.isArray(val)) {
            val.forEach(collectReceiverRefs);
            return;
          }
          if (val && val["$ref"]) {
            receiverRefs.push(
              val["$ref"].replace("#/components/messages/", "")
            );
          }
        };
        if ((ch as any).messages) {
          Object.values((ch as any).messages).forEach(collectReceiverRefs);
        }

        // Collect reply messages
        const replyRefs: string[] = [];
        if (ch?.reply?.messages) {
          const collectReplyRefs = (val: any) => {
            if (!val) return;
            if (Array.isArray(val)) {
              val.forEach(collectReplyRefs);
              return;
            }
            if (val && val["$ref"]) {
              replyRefs.push(val["$ref"].replace("#/components/messages/", ""));
            }
          };
          const replyMsgs = Array.isArray(ch.reply.messages)
            ? ch.reply.messages
            : [ch.reply.messages];
          replyMsgs.forEach(collectReplyRefs);
        }

        md += `| \`${channelName}\` | \`${address}\` | ${
          receiverRefs.length ? receiverRefs.join(", ") : "-"
        } | ${replyRefs.length ? replyRefs.join(", ") : "-"} |\n`;
      });
      md += `\n---\n\n`;
    }

    // Operations (from AsyncAPI operations or x-ouroboros metadata)
    md += `## Operations\n\n`;
    if (Object.keys(channels).length === 0) {
      md += "No channels defined.\n\n";
    } else {
      Object.entries(channels).forEach(([channelName, ch]: [string, any]) => {
        const address = (ch as any).address || channelName;
        md += `### Channel \`${channelName}\`\n\n`;
        md += `**Address**: \`${address}\`\n\n`;

        // Check for AsyncAPI standard operations
        const operations = ch.operations || {};
        const hasStandardOps = Object.keys(operations).length > 0;

        // Receiver section
        const receiverRefs: string[] = [];
        const collectReceiverRefs = (val: any) => {
          if (!val) return;
          if (Array.isArray(val)) {
            val.forEach(collectReceiverRefs);
            return;
          }
          if (val && val["$ref"]) {
            receiverRefs.push(
              val["$ref"].replace("#/components/messages/", "")
            );
          }
        };
        if ((ch as any).messages) {
          Object.values((ch as any).messages).forEach(collectReceiverRefs);
        }

        // Reply section
        const replyRefs: string[] = [];
        if (ch?.reply?.messages) {
          const collectReplyRefs = (val: any) => {
            if (!val) return;
            if (Array.isArray(val)) {
              val.forEach(collectReplyRefs);
              return;
            }
            if (val && val["$ref"]) {
              replyRefs.push(val["$ref"].replace("#/components/messages/", ""));
            }
          };
          const replyMsgs = Array.isArray(ch.reply.messages)
            ? ch.reply.messages
            : [ch.reply.messages];
          replyMsgs.forEach(collectReplyRefs);
        }

        // Display Receiver
        if (receiverRefs.length > 0) {
          md += `#### Receiver\n\n`;
          md += `**Address**: \`${address}\`\n\n`;
          md += `**Messages**:\n`;
          receiverRefs.forEach((msgName) => {
            md += `- \`${msgName}\`\n`;
          });
          md += `\n`;
        }

        // Display Reply
        if (replyRefs.length > 0) {
          const replyAddress = ch?.reply?.address || address;
          md += `#### Reply\n\n`;
          md += `**Address**: \`${replyAddress}\`\n\n`;
          md += `**Messages**:\n`;
          replyRefs.forEach((msgName) => {
            md += `- \`${msgName}\`\n`;
          });
          md += `\n`;
        }

        // If no receiver or reply found, check standard operations
        if (
          receiverRefs.length === 0 &&
          replyRefs.length === 0 &&
          hasStandardOps
        ) {
          Object.entries(operations).forEach(([_opId, op]: [string, any]) => {
            const action = op.action || "send";
            const title =
              action === "send"
                ? "Send"
                : action === "receive"
                ? "Receive"
                : action;
            md += `#### ${title}\n\n`;
            if (op.title) md += `**Title**: ${op.title}\n\n`;
            if (op.description) md += `${op.description}\n\n`;
            if (op.messages) {
              md += `**Messages**:\n`;
              const opMsgs = Array.isArray(op.messages)
                ? op.messages
                : [op.messages];
              opMsgs.forEach((msg: any) => {
                const msgRef =
                  msg?.$ref?.replace("#/components/messages/", "") || "Unknown";
                md += `- \`${msgRef}\`\n`;
              });
              md += `\n`;
            }
          });
        }

        if (
          receiverRefs.length === 0 &&
          replyRefs.length === 0 &&
          !hasStandardOps
        ) {
          md += `No explicit operations defined.\n\n`;
        }

        md += `---\n\n`;
      });
    }

    // Messages detail
    md += `## Messages\n\n`;
    if (Object.keys(messages).length === 0) {
      md += "No messages defined.\n\n";
    } else {
      Object.entries(messages).forEach(
        ([messageName, message]: [string, any]) => {
          md += `### \`${messageName}\`\n\n`;

          // Basic message info
          if (message?.name) md += `**Name**: ${message.name}\n\n`;
          if (message?.title) md += `**Title**: ${message.title}\n\n`;
          if (message?.summary) md += `**Summary**: ${message.summary}\n\n`;
          if (message?.description) md += `${message.description}\n\n`;

          // Headers
          if (message?.headers) {
            md += `#### Headers\n\n`;
            const headerSchema = message.headers.schema || message.headers;
            md += formatSchemaForMarkdown(headerSchema, schemas);
          }

          // Payload
          if (message?.payload) {
            md += `#### Payload\n\n`;
            const payloadSchema = message.payload.schema || message.payload;
            md += formatSchemaForMarkdown(payloadSchema, schemas);
          }

          // If no headers or payload, indicate that
          if (!message?.headers && !message?.payload) {
            md += `*No headers or payload defined.*\n\n`;
          }

          md += `---\n\n`;
        }
      );
    }

    md += `\n*Generated by Ouroboros* — ${new Date().toLocaleDateString(
      "en-US"
    )}\n`;
  } catch (error) {
    console.error("Error converting WS YAML to Markdown:", error);
    md += `\nError: Failed to parse AsyncAPI YAML.\n`;
  }

  return md;
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
        return `${code}${
          v?.description ? `: ${String(v.description).replace(/\n/g, " ")}` : ""
        }`;
      });
      resp = pairs.join("<br>");
    }
    md +=
      `| ${s.method.toUpperCase()} | ` +
      "`" +
      `${s.path}` +
      "`" +
      ` | ${summary} | ${tags} | ${resp} |\n`;
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
          return `${code}${
            v?.description
              ? `: ${String(v.description).replace(/\n/g, " ")}`
              : ""
          }`;
        });
        resp = pairs.join("<br>");
      }
      md +=
        `| ${s.method.toUpperCase()} | ` +
        "`" +
        `${s.path}` +
        "`" +
        ` | ${summary} | ${resp} |\n`;
    });
    md += `\n`;
  });

  md += `\n*Generated by Ouroboros* — ${new Date().toLocaleDateString(
    "ko-KR"
  )}\n`;
  return md;
}

/**
 * Convert YAML content to Markdown documentation (English, following docs format)
 * Parses OpenAPI YAML and generates documentation similar to backend/docs/endpoints/*.md
 *
 * Note: This function requires YAML parsing. For full OpenAPI support,
 * install js-yaml: npm install js-yaml @types/js-yaml
 */
export async function convertYamlToMarkdown(
  yamlContent: string
): Promise<string> {
  let md = `# API Documentation\n\n`;

  try {
    // Parse YAML to JSON using js-yaml
    const json = parseSimpleYaml(yamlContent);

    if (!json || !json.paths) {
      return md + "No API endpoints found in YAML.\n";
    }

    const info = json.info || {};
    md += `**Title**: ${info.title || "API Documentation"}\n`;
    md += `**Version**: ${info.version || "1.0.0"}\n\n`;
    md += `---\n\n`;

    // Extract all operations
    const operations: Array<{
      path: string;
      method: string;
      operation: any;
    }> = [];

    Object.entries(json.paths || {}).forEach(
      ([path, pathItem]: [string, any]) => {
        const methods = [
          "get",
          "post",
          "put",
          "delete",
          "patch",
          "options",
          "head",
          "trace",
        ];
        methods.forEach((method) => {
          if (pathItem[method]) {
            operations.push({
              path,
              method: method.toUpperCase(),
              operation: pathItem[method],
            });
          }
        });
      }
    );

    // Group by tags
    const grouped: Record<string, typeof operations> = {};
    operations.forEach((op) => {
      const tags = op.operation.tags || ["OTHERS"];
      const tag = tags[0] || "OTHERS";
      if (!grouped[tag]) grouped[tag] = [];
      grouped[tag].push(op);
    });

    // Generate documentation for each endpoint
    for (const [tag, ops] of Object.entries(grouped)) {
      md += `## ${tag}\n\n`;

      for (const { path, method, operation } of ops) {
        md += `### ${method} \`${path}\`\n\n`;

        // Basic Information
        md += `**HTTP Method**: \`${method}\`\n`;
        md += `**Endpoint**: \`${path}\`\n`;

        // Owner (from x-ouroboros-summary or operation.summary)
        const owner = operation["x-ouroboros-summary"] || operation.summary;
        if (owner) {
          md += `**Owner**: ${owner}\n`;
        }

        // Authentication check
        const authInfo = getAuthenticationInfo(operation, json.security);
        md += `**Authentication**: ${authInfo}\n`;

        if (operation.deprecated) {
          md += `**Status**: ⚠️ Deprecated\n`;
        }
        md += `\n---\n\n`;

        // Description (summary는 Owner로 표시했으므로 description만 표시)
        if (operation.description && operation.description !== owner) {
          md += `${operation.description}\n\n`;
        }

        // Parameters
        if (operation.parameters && operation.parameters.length > 0) {
          // Group parameters by location
          const paramsByLocation: Record<string, any[]> = {};
          operation.parameters.forEach((param: any) => {
            const location = param.in || "query";
            if (!paramsByLocation[location]) paramsByLocation[location] = [];
            paramsByLocation[location].push(param);
          });

          Object.entries(paramsByLocation).forEach(([location, params]) => {
            const locationName =
              location.charAt(0).toUpperCase() + location.slice(1);
            md += `#### ${locationName} Parameters\n\n`;
            md += `| Parameter | Type | Required | Description |\n`;
            md += `|---|---|---|---|\n`;
            params.forEach((param: any) => {
              let typeDesc = param.schema?.type || "string";
              if (param.schema?.$ref || param.schema?.ref) {
                const ref = param.schema.$ref || param.schema.ref;
                typeDesc = `ref: ${ref.replace("#/components/schemas/", "")}`;
              } else if (param.schema?.format) {
                typeDesc = `${param.schema.type} (${param.schema.format})`;
              }
              md += `| \`${param.name}\` | ${typeDesc} | ${
                param.required ? "Required" : "Optional"
              } | ${param.description || "-"} |\n`;
            });
            md += `\n`;
          });
        }

        // Request Body
        if (operation.requestBody) {
          md += `#### Request Body\n\n`;
          if (operation.requestBody.description) {
            md += `${operation.requestBody.description}\n\n`;
          }
          if (operation.requestBody.content) {
            for (const [contentType, content] of Object.entries(
              operation.requestBody.content
            ) as [string, any][]) {
              md += `**Content-Type**: \`${contentType}\`\n\n`;
              if (content.schema) {
                const schemaMarkdown = await formatSchemaForMarkdown(
                  content.schema,
                  json.components?.schemas || {}
                );
                md += schemaMarkdown;
              }
            }
          }
        }

        // Responses
        if (operation.responses) {
          md += `#### Response\n\n`;
          for (const [code, response] of Object.entries(
            operation.responses
          ) as [string, any][]) {
            md += `##### Status Code: \`${code}\`\n\n`;

            const desc = (response as any).description || "-";
            md += `${desc}\n\n`;

            // Response headers
            if (response.headers && Object.keys(response.headers).length > 0) {
              md += `**Headers**:\n\n`;
              md += `| Header | Type | Required | Description |\n`;
              md += `|---|---|---|---|\n`;
              Object.entries(response.headers).forEach(
                ([headerName, header]: [string, any]) => {
                  const headerType = header.schema?.type || "string";
                  md += `| \`${headerName}\` | ${headerType} | ${
                    header.required ? "Required" : "Optional"
                  } | ${header.description || "-"} |\n`;
                }
              );
              md += `\n`;
            }

            // Response schema
            if (response.content) {
              for (const [contentType, content] of Object.entries(
                response.content
              ) as [string, any][]) {
                md += `**Content-Type**: \`${contentType}\`\n\n`;

                if (content.schema) {
                  const schemaMarkdown = await formatSchemaForMarkdown(
                    content.schema,
                    json.components?.schemas || {}
                  );
                  md += schemaMarkdown;
                } else {
                  md += `No schema defined.\n\n`;
                }
              }
            } else {
              md += `No response content defined.\n\n`;
            }

            md += `---\n\n`;
          }
        }

        // Example (cURL)
        md += `#### Example\n\n`;
        md += `\`\`\`bash\ncurl -X ${method} http://localhost:8080${path}\n\`\`\`\n\n`;

        md += `---\n\n`;
      }
    }

    md += `\n*Generated by Ouroboros* — ${new Date().toLocaleDateString(
      "en-US"
    )}\n`;
  } catch (error) {
    console.error("Error converting YAML to Markdown:", error);
    md += `\nError: Failed to parse YAML content.\n`;
  }

  return md;
}

/**
 * Parse YAML content to JSON object
 * Uses js-yaml library
 */
function parseSimpleYaml(yamlContent: string): any {
  try {
    return yaml.load(yamlContent);
  } catch (error) {
    console.error("Failed to parse YAML:", error);
    throw new Error(
      `YAML parsing failed: ${
        error instanceof Error ? error.message : String(error)
      }`
    );
  }
}

/**
 * Get authentication information from operation security field
 * @param operation - OpenAPI operation object
 * @param globalSecurity - Global security from OpenAPI document (optional)
 * @return Authentication requirement description
 */
function getAuthenticationInfo(operation: any, globalSecurity?: any[]): string {
  // Check operation-level security first
  if (operation.security !== undefined && operation.security !== null) {
    // Empty array means no authentication required
    if (Array.isArray(operation.security) && operation.security.length === 0) {
      return "Not required";
    }

    // Has security requirements
    if (Array.isArray(operation.security) && operation.security.length > 0) {
      const schemes: string[] = [];
      operation.security.forEach((req: any) => {
        if (typeof req === "object" && req !== null) {
          Object.keys(req).forEach((schemeName) => {
            schemes.push(schemeName);
          });
        }
      });
      if (schemes.length > 0) {
        return `Required (${schemes.join(", ")})`;
      }
    }
  }

  // Fall back to global security
  if (
    globalSecurity &&
    Array.isArray(globalSecurity) &&
    globalSecurity.length > 0
  ) {
    const schemes: string[] = [];
    globalSecurity.forEach((req: any) => {
      if (typeof req === "object" && req !== null) {
        Object.keys(req).forEach((schemeName) => {
          schemes.push(schemeName);
        });
      }
    });
    if (schemes.length > 0) {
      return `Required (${schemes.join(", ")}) - Global`;
    }
  }

  // No security defined
  return "Not required";
}

/**
 * Resolve schema reference ($ref) to actual schema definition
 */
function resolveSchemaRef(ref: string, schemas: Record<string, any>): any {
  if (!ref || !ref.startsWith("#/components/schemas/")) {
    return null;
  }

  const schemaName = ref.replace("#/components/schemas/", "");
  return schemas[schemaName] || null;
}

/**
 * Format schema for markdown documentation
 * Handles $ref references by resolving them from components.schemas or API
 * @param schema - Schema to format
 * @param allSchemas - All available schemas for resolving references
 * @param depth - Recursion depth (for nested schemas)
 */
async function formatSchemaForMarkdown(
  schema: any,
  allSchemas: Record<string, any>,
  depth: number = 0
): Promise<string> {
  if (!schema) return "";

  let md = "";

  // Handle $ref reference
  if (schema.$ref || schema.ref) {
    const ref = schema.$ref || schema.ref;
    const schemaName = ref.replace("#/components/schemas/", "");

    // Try to resolve from YAML first
    let resolvedSchema = resolveSchemaRef(ref, allSchemas);

    // If not found in YAML, try to fetch from API
    if (!resolvedSchema && schemaName) {
      try {
        const schemaResponse = await getSchema(schemaName);
        resolvedSchema = schemaResponse?.data;
      } catch (error) {
        console.error(`Failed to load schema ${schemaName}:`, error);
        // Continue with just the reference name
      }
    }

    if (resolvedSchema) {
      md += `**Schema Reference**: \`${schemaName}\`\n\n`;
      // If resolved schema has properties, ensure it's treated as object
      if (resolvedSchema.properties && !resolvedSchema.type) {
        resolvedSchema = { ...resolvedSchema, type: "object" };
      }
      // Always try to display properties if they exist, before recursive call
      // This ensures properties are shown even if the schema structure is complex

      if (
        resolvedSchema.properties &&
        Object.keys(resolvedSchema.properties).length > 0
      ) {
        // Display properties table directly
        const required = resolvedSchema.required || [];
        md += `| Field | Type | Required | Description |\n`;
        md += `|---|---|---|---|\n`;

        const order =
          resolvedSchema["x-ouroboros-orders"] ||
          Object.keys(resolvedSchema.properties);
        const orderedProps = order
          .filter((key: string) => resolvedSchema.properties[key])
          .concat(
            Object.keys(resolvedSchema.properties).filter(
              (key: string) => !order.includes(key)
            )
          );

        for (const propName of orderedProps) {
          const prop = resolvedSchema.properties[propName];
          const isRequired = required.includes(propName);

          // Get type description - handle API SchemaField format (name, type) and OpenAPI format
          let typeDesc = "object";
          let description = "-";

          // Check if prop is in frontend SchemaField format (has schemaType)
          if (prop.schemaType) {
            // Frontend SchemaField format - use getFieldTypeString helper
            try {
              const field = prop as SchemaField;
              typeDesc = getFieldTypeString(field);
              description = field.description || "-";
            } catch (err) {
              console.error(`Failed to format field ${propName}:`, err);
              typeDesc = "unknown";
            }
          }
          // API SchemaField format: { name, type, description, mockExpression, ref }
          // or OpenAPI format: { type, description, $ref, items, format, ... }
          else {
            typeDesc = prop.type || "object";
            description = prop.description || "-";

            // Handle ref (API SchemaField format uses 'ref', OpenAPI uses '$ref')
            if (prop.ref && !prop.$ref) {
              // API SchemaField format
              typeDesc = `ref: ${prop.ref.replace(
                "#/components/schemas/",
                ""
              )}`;
            } else if (prop.$ref || prop.ref) {
              // OpenAPI format
              const propRef = prop.$ref || prop.ref;
              typeDesc = `ref: ${propRef.replace("#/components/schemas/", "")}`;
            } else if (prop.type === "array" && prop.items) {
              const itemType =
                prop.items.$ref || prop.items.ref
                  ? `ref: ${(prop.items.$ref || prop.items.ref).replace(
                      "#/components/schemas/",
                      ""
                    )}`
                  : prop.items.type || "any";
              typeDesc = `array<${itemType}>`;
            } else if (prop.format) {
              typeDesc = `${prop.type} (${prop.format})`;
            }
          }

          md += `| \`${propName}\` | ${typeDesc} | ${
            isRequired ? "Required" : "Optional"
          } | ${description} |\n`;
        }
        md += `\n`;
        return md;
      }

      // If no properties, recursively format the resolved schema
      // This handles cases where the schema might have nested structures
      md += await formatSchemaForMarkdown(resolvedSchema, allSchemas, depth);
      return md;
    } else {
      // Reference not found, show the ref
      md += `**Reference**: \`${ref}\`\n\n`;
      return md;
    }
  }

  // Handle array type
  if (schema.type === "array") {
    md += `**Type**: \`array\`\n\n`;
    if (schema.items) {
      md += `**Items**:\n\n`;
      md += await formatSchemaForMarkdown(schema.items, allSchemas, depth + 1);
    }
    return md;
  }

  // Handle object type with properties (or just properties without explicit type)
  if (schema.properties && (schema.type === "object" || !schema.type)) {
    const required = schema.required || [];

    md += `| Field | Type | Required | Description |\n`;
    md += `|---|---|---|---|\n`;

    // Sort properties by x-ouroboros-orders if available
    const order =
      schema["x-ouroboros-orders"] || Object.keys(schema.properties);
    const orderedProps = order
      .filter((key: string) => schema.properties[key])
      .concat(
        Object.keys(schema.properties).filter(
          (key: string) => !order.includes(key)
        )
      );

    orderedProps.forEach((propName: string) => {
      const prop = schema.properties[propName];
      const isRequired = required.includes(propName);

      // Get type description
      let typeDesc = prop.type || "object";
      if (prop.$ref || prop.ref) {
        const ref = prop.$ref || prop.ref;
        typeDesc = `ref: ${ref.replace("#/components/schemas/", "")}`;
      } else if (prop.type === "array" && prop.items) {
        const itemType =
          prop.items.$ref || prop.items.ref
            ? `ref: ${(prop.items.$ref || prop.items.ref).replace(
                "#/components/schemas/",
                ""
              )}`
            : prop.items.type || "any";
        typeDesc = `array<${itemType}>`;
      } else if (prop.format) {
        typeDesc = `${prop.type} (${prop.format})`;
      }

      const description = prop.description || "-";
      md += `| \`${propName}\` | ${typeDesc} | ${
        isRequired ? "Required" : "Optional"
      } | ${description} |\n`;
    });

    md += `\n`;
    return md;
  }

  // Handle primitive types
  if (schema.type) {
    md += `**Type**: \`${schema.type}\`\n`;
    if (schema.format) {
      md += `**Format**: \`${schema.format}\`\n`;
    }
    if (schema.description) {
      md += `**Description**: ${schema.description}\n`;
    }
    md += `\n`;
    return md;
  }

  // Fallback: show as JSON
  md += `\`\`\`json\n${JSON.stringify(schema, null, 2)}\n\`\`\`\n\n`;
  return md;
}
