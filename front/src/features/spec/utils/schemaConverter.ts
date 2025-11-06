/**
 * Schema Converter
 * SchemaField (프론트엔드) ↔ OpenAPI Schema (백엔드)
 */

import type { SchemaField, SchemaType, RequestBody } from "../types/schema.types";
import {
  isPrimitiveSchema,
  isObjectSchema,
  isArraySchema,
  isRefSchema,
} from "../types/schema.types";

// ========== Frontend → OpenAPI ==========

/**
 * SchemaType을 OpenAPI Schema로 변환 (재귀)
 */
export function convertSchemaTypeToOpenAPI(schemaType: SchemaType): any {
  if (isPrimitiveSchema(schemaType)) {
    const schema: any = {
      type: schemaType.type === "file" ? "string" : schemaType.type,
    };

    // file 타입은 format: binary로 표현
    if (schemaType.type === "file") {
      schema.format = "binary";
    } else if (schemaType.format) {
      schema.format = schemaType.format;
    }

    return schema;
  }

  if (isObjectSchema(schemaType)) {
    const properties: Record<string, any> = {};
    const required: string[] = [];

    schemaType.properties.forEach((field) => {
      if (field.key) {
        properties[field.key] = convertSchemaFieldToOpenAPI(field);
        if (field.required) {
          required.push(field.key);
        }
      }
    });

    const schema: any = {
      type: "object",
    };

    // properties가 있을 때만 추가
    if (Object.keys(properties).length > 0) {
      schema.properties = properties;
    }

    if (required.length > 0) {
      schema.required = required;
    }

    return schema;
  }

  if (isArraySchema(schemaType)) {
    const schema: any = {
      type: "array",
      items: convertSchemaTypeToOpenAPI(schemaType.items),
    };
    
    if (schemaType.minItems !== undefined) {
      schema.minItems = schemaType.minItems;
    }
    if (schemaType.maxItems !== undefined) {
      schema.maxItems = schemaType.maxItems;
    }
    
    return schema;
  }

  if (isRefSchema(schemaType)) {
    // schemaName이 비어있으면 기본 타입 반환
    if (!schemaType.schemaName || schemaType.schemaName.trim() === "") {
      return { type: "string" };
    }
    // 백엔드는 ref 필드로 받음 (스키마 이름만)
    return {
      ref: schemaType.schemaName,
    };
  }

  return { type: "string" };
}

/**
 * SchemaField를 OpenAPI Schema로 변환 (재귀)
 */
export function convertSchemaFieldToOpenAPI(field: SchemaField): any {
  const schema = convertSchemaTypeToOpenAPI(field.schemaType);

  // ref가 있는 경우 다른 속성 추가 불가 (OpenAPI 스펙)
  if (schema.$ref || schema.ref) {
    return schema;
  }

  // Description 추가
  if (field.description) {
    schema.description = field.description;
  }

  // Mock Expression 추가 (Primitive 타입만)
  if (field.mockExpression && isPrimitiveSchema(field.schemaType)) {
    schema["x-ouroboros-mock"] = field.mockExpression;
  }

  return schema;
}

/**
 * RequestBody를 OpenAPI RequestBody로 변환
 */
export function convertRequestBodyToOpenAPI(
  body: RequestBody | null
): {
  description: string;
  required: boolean;
  content: Record<string, any>;
} | null {
  if (!body || body.type === "none") {
    return null;
  }

  // Content-Type 결정
  let contentType = "application/json";
  if (body.type === "form-data") {
    contentType = "multipart/form-data";
  } else if (body.type === "x-www-form-urlencoded") {
    contentType = "application/x-www-form-urlencoded";
  } else if (body.type === "xml") {
    contentType = "application/xml";
  }

  // 전체 스키마 참조
  if (body.schemaRef) {
    return {
      description: body.description || "Request body",
      required: body.required !== false,
      content: {
        [contentType]: {
          schema: {
            ref: body.schemaRef,
          },
        },
      },
    };
  }

  // 인라인 스키마
  if (!body.fields || body.fields.length === 0) {
    return null;
  }

  const properties: Record<string, any> = {};
  const required: string[] = [];

  body.fields.forEach((field) => {
    if (field.key) {
      properties[field.key] = convertSchemaFieldToOpenAPI(field);
      if (field.required) {
        required.push(field.key);
      }
    }
  });

  return {
    description: body.description || "Request body",
    required: body.required !== false,
    content: {
      [contentType]: {
        schema: {
          type: "object",
          properties,
          ...(required.length > 0 ? { required } : {}),
        },
      },
    },
  };
}

// ========== OpenAPI → Frontend ==========

/**
 * OpenAPI Schema를 SchemaType으로 변환 (재귀)
 */
export function parseOpenAPISchemaToSchemaType(schema: any): SchemaType {
  // Reference ($ref 또는 ref 모두 지원)
  if (schema.$ref || schema.ref) {
    const refValue = schema.$ref || schema.ref;
    const schemaName = refValue.includes("#/components/schemas/")
      ? refValue.replace("#/components/schemas/", "")
      : refValue;
    return {
      kind: "ref",
      schemaName,
    };
  }

  // Array
  if (schema.type === "array") {
    return {
      kind: "array",
      items: schema.items
        ? parseOpenAPISchemaToSchemaType(schema.items)
        : { kind: "primitive", type: "string" },
      minItems: schema.minItems,
      maxItems: schema.maxItems,
    };
  }

  // Object
  if (schema.type === "object") {
    const properties: SchemaField[] = [];

    if (schema.properties) {
      Object.entries(schema.properties).forEach(([key, propSchema]: [string, any]) => {
        properties.push({
          key,
          description: propSchema.description,
          required: schema.required?.includes(key) || false,
          mockExpression: propSchema["x-ouroboros-mock"],
          schemaType: parseOpenAPISchemaToSchemaType(propSchema),
        });
      });
    }

    return {
      kind: "object",
      properties,
    };
  }

  // Primitive (including file)
  const primitiveType =
    schema.format === "binary" ? "file" : (schema.type as any) || "string";

  return {
    kind: "primitive",
    type: primitiveType,
    format: schema.format,
  };
}

/**
 * OpenAPI Schema를 SchemaField로 변환 (재귀)
 */
export function parseOpenAPISchemaToSchemaField(
  key: string,
  schema: any
): SchemaField {
  return {
    key,
    description: schema.description,
    mockExpression: schema["x-ouroboros-mock"],
    required: false, // required는 상위 레벨에서 처리
    schemaType: parseOpenAPISchemaToSchemaType(schema),
  };
}

/**
 * OpenAPI RequestBody를 Frontend RequestBody로 변환
 */
export function parseOpenAPIRequestBody(
  openApiBody: any,
  contentType: string
): RequestBody | null {
  if (!openApiBody || !openApiBody.content) {
    return null;
  }

  // type 결정
  let type: RequestBody["type"] = "json";
  if (contentType.includes("multipart/form-data")) {
    type = "form-data";
  } else if (contentType.includes("x-www-form-urlencoded")) {
    type = "x-www-form-urlencoded";
  } else if (contentType.includes("xml")) {
    type = "xml";
  }

  const mediaType = openApiBody.content[contentType];
  if (!mediaType || !mediaType.schema) {
    return null;
  }

  const schema = mediaType.schema;

  // 전체 스키마 참조 ($ref 또는 ref 모두 지원)
  if (schema.$ref || schema.ref) {
    const refValue = schema.$ref || schema.ref;
    const schemaName = refValue.includes("#/components/schemas/")
      ? refValue.replace("#/components/schemas/", "")
      : refValue;
    return {
      type,
      schemaRef: schemaName,
      description: openApiBody.description,
      required: openApiBody.required !== false,
    };
  }

  // 인라인 스키마
  if (schema.type === "object" && schema.properties) {
    const fields: SchemaField[] = [];

    Object.entries(schema.properties).forEach(([key, propSchema]: [string, any]) => {
      fields.push({
        key,
        description: propSchema.description,
        required: schema.required?.includes(key) || false,
        mockExpression: propSchema["x-ouroboros-mock"],
        schemaType: parseOpenAPISchemaToSchemaType(propSchema),
      });
    });

    return {
      type,
      fields,
      description: openApiBody.description,
      required: openApiBody.required !== false,
    };
  }

  return null;
}

