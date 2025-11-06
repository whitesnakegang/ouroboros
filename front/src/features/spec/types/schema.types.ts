/**
 * Schema Field Types - 재귀적 스키마 구조 정의
 * Request Body와 Response Body 모두에서 사용
 */

// ========== Base Types ==========

export type PrimitiveTypeName = "string" | "integer" | "number" | "boolean" | "file";

export type SchemaKind = "primitive" | "object" | "array" | "ref";

// ========== Schema Type Definitions ==========

export interface PrimitiveSchema {
  kind: "primitive";
  type: PrimitiveTypeName;
  format?: string;
  enum?: string[];
  pattern?: string;
  minLength?: number;
  maxLength?: number;
  minimum?: number;
  maximum?: number;
}

export interface ObjectSchema {
  kind: "object";
  properties: SchemaField[];
}

export interface ArraySchema {
  kind: "array";
  items: SchemaType;
  minItems?: number;
  maxItems?: number;
}

export interface RefSchema {
  kind: "ref";
  schemaName: string;
}

export type SchemaType = PrimitiveSchema | ObjectSchema | ArraySchema | RefSchema;

// ========== Schema Field ==========

export interface SchemaField {
  key: string;
  description?: string;
  required?: boolean;
  mockExpression?: string;
  schemaType: SchemaType;
}

// ========== Request Body ==========

export type RequestBodyType = "none" | "form-data" | "x-www-form-urlencoded" | "json" | "xml";

export interface RequestBody {
  type: RequestBodyType;
  schemaRef?: string;
  fields?: SchemaField[];
  description?: string;
  required?: boolean;
}

// ========== Response Body ==========

export type ResponseContentType = "json" | "xml" | "text" | "html";

export interface ResponseBody {
  description: string;
  contentType: ResponseContentType;
  schemaRef?: string;
  fields?: SchemaField[];
}

export interface StatusCodeDefinition {
  code: string;
  type: "Success" | "Error";
  message: string;
  body?: ResponseBody;
}

// ========== Helper Functions ==========

export function createDefaultField(key: string = ""): SchemaField {
  return {
    key,
    required: false,
    schemaType: {
      kind: "primitive",
      type: "string",
    },
  };
}

export function createPrimitiveField(key: string, type: PrimitiveTypeName): SchemaField {
  return {
    key,
    required: false,
    schemaType: {
      kind: "primitive",
      type,
    },
  };
}

export function createObjectField(key: string): SchemaField {
  return {
    key,
    required: false,
    schemaType: {
      kind: "object",
      properties: [],
    },
  };
}

export function createArrayField(key: string): SchemaField {
  return {
    key,
    required: false,
    schemaType: {
      kind: "array",
      items: {
        kind: "primitive",
        type: "string",
      },
    },
  };
}

export function createRefField(key: string, schemaName: string): SchemaField {
  return {
    key,
    required: false,
    schemaType: {
      kind: "ref",
      schemaName,
    },
  };
}

export function createFileField(key: string): SchemaField {
  return {
    key,
    required: false,
    description: "File upload",
    schemaType: {
      kind: "primitive",
      type: "file",
      format: "binary",
    },
  };
}

export function isPrimitiveSchema(schema: SchemaType): schema is PrimitiveSchema {
  return schema.kind === "primitive";
}

export function isObjectSchema(schema: SchemaType): schema is ObjectSchema {
  return schema.kind === "object";
}

export function isArraySchema(schema: SchemaType): schema is ArraySchema {
  return schema.kind === "array";
}

export function isRefSchema(schema: SchemaType): schema is RefSchema {
  return schema.kind === "ref";
}
