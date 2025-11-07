import yaml from 'js-yaml';

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

/**
 * Convert YAML content to Markdown documentation (English, following docs format)
 * Parses OpenAPI YAML and generates documentation similar to backend/docs/endpoints/*.md
 * 
 * Note: This function requires YAML parsing. For full OpenAPI support, 
 * install js-yaml: npm install js-yaml @types/js-yaml
 */
export function convertYamlToMarkdown(yamlContent: string): string {
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

    Object.entries(json.paths || {}).forEach(([path, pathItem]: [string, any]) => {
      const methods = ["get", "post", "put", "delete", "patch", "options", "head", "trace"];
      methods.forEach((method) => {
        if (pathItem[method]) {
          operations.push({
            path,
            method: method.toUpperCase(),
            operation: pathItem[method],
          });
        }
      });
    });

    // Group by tags
    const grouped: Record<string, typeof operations> = {};
    operations.forEach((op) => {
      const tags = op.operation.tags || ["OTHERS"];
      const tag = tags[0] || "OTHERS";
      if (!grouped[tag]) grouped[tag] = [];
      grouped[tag].push(op);
    });

    // Generate documentation for each endpoint
    Object.entries(grouped).forEach(([tag, ops]) => {
      md += `## ${tag}\n\n`;
      
      ops.forEach(({ path, method, operation }) => {
        md += `### ${method} \`${path}\`\n\n`;
        
        // Basic Information
        md += `**HTTP Method**: \`${method}\`\n`;
        md += `**Endpoint**: \`${path}\`\n`;
        
        // Authentication check
        const authInfo = getAuthenticationInfo(operation, json.security);
        md += `**Authentication**: ${authInfo}\n`;
        
        if (operation.deprecated) {
          md += `**Status**: ⚠️ Deprecated\n`;
        }
        md += `\n---\n\n`;

        // Summary/Description
        if (operation.summary) {
          md += `${operation.summary}\n\n`;
        }
        if (operation.description) {
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
            const locationName = location.charAt(0).toUpperCase() + location.slice(1);
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
              md += `| \`${param.name}\` | ${typeDesc} | ${param.required ? "Required" : "Optional"} | ${param.description || "-"} |\n`;
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
            Object.entries(operation.requestBody.content).forEach(([contentType, content]: [string, any]) => {
              md += `**Content-Type**: \`${contentType}\`\n\n`;
              if (content.schema) {
                const schemaMarkdown = formatSchemaForMarkdown(content.schema, json.components?.schemas || {});
                md += schemaMarkdown;
              }
            });
          }
        }

        // Responses
        if (operation.responses) {
          md += `#### Response\n\n`;
          Object.entries(operation.responses).forEach(([code, response]: [string, any]) => {
            md += `##### Status Code: \`${code}\`\n\n`;
            
            const desc = (response as any).description || "-";
            md += `${desc}\n\n`;
            
            // Response headers
            if (response.headers && Object.keys(response.headers).length > 0) {
              md += `**Headers**:\n\n`;
              md += `| Header | Type | Required | Description |\n`;
              md += `|---|---|---|---|\n`;
              Object.entries(response.headers).forEach(([headerName, header]: [string, any]) => {
                const headerType = header.schema?.type || "string";
                md += `| \`${headerName}\` | ${headerType} | ${header.required ? "Required" : "Optional"} | ${header.description || "-"} |\n`;
              });
              md += `\n`;
            }
            
            // Response schema
            if (response.content) {
              Object.entries(response.content).forEach(([contentType, content]: [string, any]) => {
                md += `**Content-Type**: \`${contentType}\`\n\n`;
                
                if (content.schema) {
                  const schemaMarkdown = formatSchemaForMarkdown(content.schema, json.components?.schemas || {});
                  md += schemaMarkdown;
                } else {
                  md += `No schema defined.\n\n`;
                }
              });
            } else {
              md += `No response content defined.\n\n`;
            }
            
            md += `---\n\n`;
          });
        }

        // Example (cURL)
        md += `#### Example\n\n`;
        md += `\`\`\`bash\ncurl -X ${method} http://localhost:8080${path}\n\`\`\`\n\n`;
        
        md += `---\n\n`;
      });
    });

    md += `\n*Generated by Ouroboros* — ${new Date().toLocaleDateString("en-US")}\n`;
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
    throw new Error(`YAML parsing failed: ${error instanceof Error ? error.message : String(error)}`);
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
        if (typeof req === 'object' && req !== null) {
          Object.keys(req).forEach(schemeName => {
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
  if (globalSecurity && Array.isArray(globalSecurity) && globalSecurity.length > 0) {
    const schemes: string[] = [];
    globalSecurity.forEach((req: any) => {
      if (typeof req === 'object' && req !== null) {
        Object.keys(req).forEach(schemeName => {
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
 * Handles $ref references by resolving them from components.schemas
 * @param schema - Schema to format
 * @param allSchemas - All available schemas for resolving references
 * @param depth - Recursion depth (for nested schemas)
 */
function formatSchemaForMarkdown(
  schema: any, 
  allSchemas: Record<string, any>, 
  depth: number = 0
): string {
  if (!schema) return "";
  
  let md = "";
  
  // Handle $ref reference
  if (schema.$ref || schema.ref) {
    const ref = schema.$ref || schema.ref;
    const resolvedSchema = resolveSchemaRef(ref, allSchemas);
    
    if (resolvedSchema) {
      const schemaName = ref.replace("#/components/schemas/", "");
      md += `**Schema Reference**: \`${schemaName}\`\n\n`;
      // Recursively format the resolved schema
      md += formatSchemaForMarkdown(resolvedSchema, allSchemas, depth);
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
      md += formatSchemaForMarkdown(schema.items, allSchemas, depth + 1);
    }
    return md;
  }
  
  // Handle object type with properties
  if (schema.type === "object" && schema.properties) {
    const required = schema.required || [];
    
    md += `| Field | Type | Required | Description |\n`;
    md += `|---|---|---|---|\n`;
    
    // Sort properties by x-ouroboros-orders if available
    const order = schema["x-ouroboros-orders"] || Object.keys(schema.properties);
    const orderedProps = order
      .filter((key: string) => schema.properties[key])
      .concat(Object.keys(schema.properties).filter((key: string) => !order.includes(key)));
    
    orderedProps.forEach((propName: string) => {
      const prop = schema.properties[propName];
      const isRequired = required.includes(propName);
      
      // Get type description
      let typeDesc = prop.type || "object";
      if (prop.$ref || prop.ref) {
        const ref = prop.$ref || prop.ref;
        typeDesc = `ref: ${ref.replace("#/components/schemas/", "")}`;
      } else if (prop.type === "array" && prop.items) {
        const itemType = prop.items.$ref || prop.items.ref
          ? `ref: ${(prop.items.$ref || prop.items.ref).replace("#/components/schemas/", "")}`
          : prop.items.type || "any";
        typeDesc = `array<${itemType}>`;
      } else if (prop.format) {
        typeDesc = `${prop.type} (${prop.format})`;
      }
      
      const description = prop.description || "-";
      md += `| \`${propName}\` | ${typeDesc} | ${isRequired ? "Required" : "Optional"} | ${description} |\n`;
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