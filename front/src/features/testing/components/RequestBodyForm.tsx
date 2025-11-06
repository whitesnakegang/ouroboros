import { useState, useEffect } from "react";
import type { SchemaField, SchemaType } from "@/features/spec/types/schema.types";
import {
  isPrimitiveSchema,
  isObjectSchema,
  isArraySchema,
  isRefSchema,
} from "@/features/spec/types/schema.types";
import type { RequestBody } from "@/features/spec/types/schema.types";

interface RequestBodyFormProps {
  requestBody: RequestBody | null;
  contentType: string;
  value: string;
  onChange: (value: string) => void;
}

// Primitive 타입의 기본값 생성
function getDefaultValue(schemaType: SchemaType): any {
  if (isPrimitiveSchema(schemaType)) {
    switch (schemaType.type) {
      case "string":
        return "string";
      case "integer":
        return 0;
      case "number":
        return 0.0;
      case "boolean":
        return false;
      case "file":
        return "";
      default:
        return "";
    }
  }
  if (isArraySchema(schemaType)) {
    return [];
  }
  if (isObjectSchema(schemaType)) {
    return {};
  }
  return null;
}

// JSON Body Form (기존 textarea)
function JsonBodyForm({ value, onChange }: { value: string; onChange: (value: string) => void }) {
  return (
    <div>
      <div className="flex items-center justify-between mb-2">
        <div className="flex gap-2">
          <button className="px-2 py-1 text-xs bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded">
            Edit Value
          </button>
          <button className="px-2 py-1 text-xs bg-transparent border border-[#2D333B] text-[#8B949E] hover:text-[#E6EDF3] rounded">
            Schema
          </button>
        </div>
      </div>
      <textarea
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder='{\n  "key": "value"\n}'
        className="w-full h-40 px-3 py-2 rounded-md bg-[#0D1117] border border-[#2D333B] text-[#E6EDF3] placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] font-mono text-sm"
      />
    </div>
  );
}

// Form Data / URL Encoded Field Component
function FormField({
  field,
  value,
  onChange,
  onRemove,
  canRemove,
}: {
  field: SchemaField;
  value: any;
  onChange: (value: any) => void;
  onRemove?: () => void;
  canRemove?: boolean;
}) {
  const [isEmpty, setIsEmpty] = useState(false);

  const handleValueChange = (newValue: any) => {
    onChange(newValue);
    setIsEmpty(false);
  };

  const handleEmptyToggle = (checked: boolean) => {
    setIsEmpty(checked);
    if (checked) {
      onChange(undefined);
    } else {
      onChange(getDefaultValue(field.schemaType));
    }
  };

  if (isPrimitiveSchema(field.schemaType)) {
    if (field.schemaType.type === "file") {
      return (
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <label className="text-sm text-[#E6EDF3] min-w-[120px]">
              {field.key}
              {field.required && <span className="text-red-500 ml-1">*</span>}
            </label>
            <input
              type="file"
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) {
                  onChange(file);
                }
              }}
              className="flex-1 px-3 py-2 rounded-md bg-[#0D1117] border border-[#2D333B] text-[#E6EDF3] text-sm"
            />
            {canRemove && onRemove && (
              <button
                onClick={onRemove}
                className="px-2 py-1 text-red-500 hover:text-red-700"
              >
                -
              </button>
            )}
          </div>
          {field.description && (
            <p className="text-xs text-[#8B949E] ml-[120px]">{field.description}</p>
          )}
        </div>
      );
    }

    if (field.schemaType.type === "boolean") {
      return (
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <label className="text-sm text-[#E6EDF3] min-w-[120px]">
              {field.key}
              {field.required && <span className="text-red-500 ml-1">*</span>}
            </label>
            <select
              value={value === undefined ? "" : String(value)}
              onChange={(e) => handleValueChange(e.target.value === "true")}
              className="flex-1 px-3 py-2 rounded-md bg-[#0D1117] border border-[#2D333B] text-[#E6EDF3] text-sm"
            >
              <option value="">Select...</option>
              <option value="true">true</option>
              <option value="false">false</option>
            </select>
            {canRemove && onRemove && (
              <button
                onClick={onRemove}
                className="px-2 py-1 text-red-500 hover:text-red-700"
              >
                -
              </button>
            )}
          </div>
          {field.description && (
            <p className="text-xs text-[#8B949E] ml-[120px]">{field.description}</p>
          )}
        </div>
      );
    }

    return (
      <div className="space-y-2">
        <div className="flex items-center gap-2">
          <label className="text-sm text-[#E6EDF3] min-w-[120px]">
            {field.key}
            {field.required && <span className="text-red-500 ml-1">*</span>}
          </label>
          <input
            type={field.schemaType.type === "integer" || field.schemaType.type === "number" ? "number" : "text"}
            value={value === undefined ? "" : String(value)}
            onChange={(e) => {
              const val = field.schemaType.type === "integer" 
                ? parseInt(e.target.value) || 0
                : field.schemaType.type === "number"
                ? parseFloat(e.target.value) || 0
                : e.target.value;
              handleValueChange(val);
            }}
            placeholder={field.schemaType.type === "string" ? "string" : String(getDefaultValue(field.schemaType))}
            className="flex-1 px-3 py-2 rounded-md bg-[#0D1117] border border-[#2D333B] text-[#E6EDF3] placeholder:text-[#8B949E] text-sm"
          />
          {canRemove && onRemove && (
            <button
              onClick={onRemove}
              className="px-2 py-1 text-red-500 hover:text-red-700"
            >
              -
            </button>
          )}
          <label className="flex items-center gap-1 text-xs text-[#8B949E]">
            <input
              type="checkbox"
              checked={isEmpty}
              onChange={(e) => handleEmptyToggle(e.target.checked)}
              className="w-3 h-3"
            />
            <span>Send empty value</span>
          </label>
        </div>
        {field.description && (
          <p className="text-xs text-[#8B949E] ml-[120px]">{field.description}</p>
        )}
      </div>
    );
  }

  if (isArraySchema(field.schemaType)) {
    const arrayValue = Array.isArray(value) ? value : [];
    const itemSchema = field.schemaType.items;

    return (
      <div className="space-y-2">
        <div className="flex items-center gap-2">
          <label className="text-sm text-[#E6EDF3] min-w-[120px]">
            {field.key}
            {field.required && <span className="text-red-500 ml-1">*</span>}
          </label>
          <div className="flex-1 space-y-2">
            {arrayValue.map((item: any, index: number) => (
              <div key={index} className="flex gap-2">
                {isPrimitiveSchema(itemSchema) && itemSchema.type === "string" ? (
                  <input
                    type="text"
                    value={item || ""}
                    onChange={(e) => {
                      const newArray = [...arrayValue];
                      newArray[index] = e.target.value;
                      onChange(newArray);
                    }}
                    placeholder="string"
                    className="flex-1 px-3 py-2 rounded-md bg-[#0D1117] border border-[#2D333B] text-[#E6EDF3] placeholder:text-[#8B949E] text-sm"
                  />
                ) : (
                  <textarea
                    value={typeof item === "string" ? item : JSON.stringify(item, null, 2)}
                    onChange={(e) => {
                      try {
                        const parsed = JSON.parse(e.target.value);
                        const newArray = [...arrayValue];
                        newArray[index] = parsed;
                        onChange(newArray);
                      } catch {
                        const newArray = [...arrayValue];
                        newArray[index] = e.target.value;
                        onChange(newArray);
                      }
                    }}
                    placeholder={isPrimitiveSchema(itemSchema) ? String(getDefaultValue(itemSchema)) : "{}"}
                    className="flex-1 px-3 py-2 rounded-md bg-[#0D1117] border border-[#2D333B] text-[#E6EDF3] placeholder:text-[#8B949E] font-mono text-sm min-h-[80px]"
                  />
                )}
                <button
                  onClick={() => {
                    const newArray = arrayValue.filter((_: any, i: number) => i !== index);
                    onChange(newArray);
                  }}
                  className="px-2 py-1 text-red-500 hover:text-red-700"
                >
                  -
                </button>
              </div>
            ))}
            <button
              onClick={() => {
                const newArray = [...arrayValue, getDefaultValue(itemSchema)];
                onChange(newArray);
              }}
              className="px-3 py-1 text-sm bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded"
            >
              {isPrimitiveSchema(itemSchema) && itemSchema.type === "string"
                ? "Add string item"
                : "Add object item"}
            </button>
          </div>
          {canRemove && onRemove && (
            <button
              onClick={onRemove}
              className="px-2 py-1 text-red-500 hover:text-red-700"
            >
              -
            </button>
          )}
        </div>
        {field.description && (
          <p className="text-xs text-[#8B949E] ml-[120px]">{field.description}</p>
        )}
      </div>
    );
  }

  if (isObjectSchema(field.schemaType)) {
    const [showSchema, setShowSchema] = useState(false);
    const objectValue = typeof value === "object" && value !== null ? value : {};

    return (
      <div className="space-y-2">
        <div className="flex items-center gap-2">
          <label className="text-sm text-[#E6EDF3] min-w-[120px]">
            {field.key}
            {field.required && <span className="text-red-500 ml-1">*</span>}
          </label>
          <div className="flex-1">
            <div className="flex gap-2 mb-2">
              <button
                onClick={() => setShowSchema(false)}
                className={`px-2 py-1 text-xs rounded ${
                  !showSchema
                    ? "bg-[#2563EB] text-white"
                    : "bg-transparent border border-[#2D333B] text-[#8B949E]"
                }`}
              >
                Edit Value
              </button>
              <button
                onClick={() => setShowSchema(true)}
                className={`px-2 py-1 text-xs rounded ${
                  showSchema
                    ? "bg-[#2563EB] text-white"
                    : "bg-transparent border border-[#2D333B] text-[#8B949E]"
                }`}
              >
                Schema
              </button>
            </div>
            {showSchema ? (
              <div className="px-3 py-2 rounded-md bg-[#0D1117] border border-[#2D333B] text-sm text-[#8B949E]">
                {field.schemaType.properties.map((prop) => (
                  <div key={prop.key} className="text-xs">
                    {prop.key}: {prop.schemaType.kind}
                  </div>
                ))}
              </div>
            ) : (
              <textarea
                value={JSON.stringify(objectValue, null, 2)}
                onChange={(e) => {
                  try {
                    const parsed = JSON.parse(e.target.value);
                    onChange(parsed);
                  } catch {
                    // Invalid JSON, keep as is
                  }
                }}
                placeholder="{}"
                className="w-full px-3 py-2 rounded-md bg-[#0D1117] border border-[#2D333B] text-[#E6EDF3] placeholder:text-[#8B949E] font-mono text-sm min-h-[80px]"
              />
            )}
          </div>
          {canRemove && onRemove && (
            <button
              onClick={onRemove}
              className="px-2 py-1 text-red-500 hover:text-red-700"
            >
              -
            </button>
          )}
        </div>
        {field.description && (
          <p className="text-xs text-[#8B949E] ml-[120px]">{field.description}</p>
        )}
      </div>
    );
  }

  return null;
}

// Form Data / URL Encoded Form
function FormDataBodyForm({
  requestBody,
  value,
  onChange,
}: {
  requestBody: RequestBody;
  value: string;
  onChange: (value: string) => void;
}) {
  const [formData, setFormData] = useState<Record<string, any>>({});

  useEffect(() => {
    // Parse existing value
    if (value) {
      try {
        const parsed = JSON.parse(value);
        setFormData(parsed);
      } catch {
        // Invalid JSON, keep empty
      }
    }
  }, []);

  useEffect(() => {
    // Update parent value when formData changes
    onChange(JSON.stringify(formData, null, 2));
  }, [formData, onChange]);

  if (!requestBody.fields || requestBody.fields.length === 0) {
    return (
      <div className="text-sm text-[#8B949E]">
        No fields defined in schema
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {requestBody.fields.map((field) => (
        <FormField
          key={field.key}
          field={field}
          value={formData[field.key]}
          onChange={(newValue) => {
            setFormData((prev) => ({
              ...prev,
              [field.key]: newValue,
            }));
          }}
        />
      ))}
    </div>
  );
}

// XML Body Form
function XmlBodyForm({ value, onChange }: { value: string; onChange: (value: string) => void }) {
  return (
    <div>
      <div className="flex items-center justify-between mb-2">
        <div className="flex gap-2">
          <button className="px-2 py-1 text-xs bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded">
            Edit Value
          </button>
          <button className="px-2 py-1 text-xs bg-transparent border border-[#2D333B] text-[#8B949E] hover:text-[#E6EDF3] rounded">
            Schema
          </button>
        </div>
      </div>
      <textarea
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder='<?xml version="1.0" encoding="UTF-8"?>\n<root>\n  <key>value</key>\n</root>'
        className="w-full h-40 px-3 py-2 rounded-md bg-[#0D1117] border border-[#2D333B] text-[#E6EDF3] placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] font-mono text-sm"
      />
    </div>
  );
}

export function RequestBodyForm({
  requestBody,
  contentType,
  value,
  onChange,
}: RequestBodyFormProps) {
  if (!requestBody || requestBody.type === "none") {
    return null;
  }

  // Content-Type에 따라 다른 폼 표시
  if (contentType.includes("application/json") || requestBody.type === "json") {
    return <JsonBodyForm value={value} onChange={onChange} />;
  }

  if (
    contentType.includes("multipart/form-data") ||
    requestBody.type === "form-data"
  ) {
    return <FormDataBodyForm requestBody={requestBody} value={value} onChange={onChange} />;
  }

  if (
    contentType.includes("application/x-www-form-urlencoded") ||
    requestBody.type === "x-www-form-urlencoded"
  ) {
    return <FormDataBodyForm requestBody={requestBody} value={value} onChange={onChange} />;
  }

  if (contentType.includes("application/xml") || contentType.includes("text/xml") || requestBody.type === "xml") {
    return <XmlBodyForm value={value} onChange={onChange} />;
  }

  // Default: JSON
  return <JsonBodyForm value={value} onChange={onChange} />;
}

