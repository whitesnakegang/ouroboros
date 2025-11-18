import { useState, useEffect, useRef } from "react";
import type {
  SchemaField,
  SchemaType,
  RequestBody,
} from "@/features/spec/types/schema.types";
import {
  isPrimitiveSchema,
  isObjectSchema,
  isArraySchema,
  isRefSchema,
} from "@/features/spec/types/schema.types";
import { JsonEditor } from "@/components/JsonEditor";

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
    // items가 object schema인 경우 properties를 기반으로 객체 생성
    if (isObjectSchema(schemaType.items)) {
      const obj: Record<string, any> = {};
      schemaType.items.properties.forEach((field) => {
        obj[field.key] = getDefaultValue(field.schemaType);
      });
      return [obj];
    }
    // items가 ref인 경우 빈 객체 (이미 변환되어야 하지만 안전장치)
    if (isRefSchema(schemaType.items)) {
      return [{}];
    }
    // primitive나 기타 타입
    const itemValue = getDefaultValue(schemaType.items);
    return itemValue !== undefined ? [itemValue] : [];
  }
  if (isObjectSchema(schemaType)) {
    // object schema인 경우 properties를 기반으로 객체 생성
    const obj: Record<string, any> = {};
    schemaType.properties.forEach((field) => {
      obj[field.key] = getDefaultValue(field.schemaType);
    });
    return obj;
  }
  if (isRefSchema(schemaType)) {
    // Ref는 스키마 참조이므로 빈 객체 반환
    return {};
  }
  return null;
}

// JSON Body Form
function JsonBodyForm({
  requestBody,
  value,
  onChange,
}: {
  requestBody?: RequestBody | null;
  value: string;
  onChange: (value: string) => void;
}) {
  const initializedRef = useRef<string>("");
  const [jsonData, setJsonData] = useState<Record<string, any>>({});
  const prevValueRef = useRef<string>("");

  // value prop이 변경될 때만 jsonData 업데이트 (외부에서 변경된 경우)
  useEffect(() => {
    if (value !== prevValueRef.current) {
      prevValueRef.current = value;
      if (value) {
        try {
          const parsed = JSON.parse(value);
          setJsonData(typeof parsed === "object" && parsed !== null ? parsed : {});
        } catch {
          setJsonData({});
        }
      } else {
        setJsonData({});
      }
    }
  }, [value]);

  // rootSchemaType을 기반으로 기본값 생성
  const generateDefaultFromRootSchema = (rootSchemaType: SchemaType): any => {
    return getDefaultValue(rootSchemaType);
  };

  // fields를 기반으로 기본 JSON 객체 생성
  const generateDefaultJson = (fields: SchemaField[]): string => {
    const obj: Record<string, any> = {};
    fields.forEach((field) => {
      obj[field.key] = getDefaultValue(field.schemaType);
    });
    return JSON.stringify(obj, null, 2);
  };

  // 초기값 설정
  useEffect(() => {
    // fields가 있으면 fields를 우선 사용 (스키마 참조로부터 로드된 fields 포함)
    if (requestBody?.fields && requestBody.fields.length > 0) {
      const fieldsKey = JSON.stringify(requestBody.fields.map((f) => f.key));

      if (fieldsKey !== initializedRef.current) {
        initializedRef.current = fieldsKey;

        // fields가 있으면 기본 JSON 생성
        if (!value || value.trim() === "" || value === "{}") {
          const defaultJson = generateDefaultJson(requestBody.fields);
          onChange(defaultJson);
        }
      }
      return;
    }

    // rootSchemaType이 있고 fields가 없는 경우
    if (requestBody?.rootSchemaType) {
      const rootKey = JSON.stringify(requestBody.rootSchemaType);

      if (rootKey !== initializedRef.current) {
        initializedRef.current = rootKey;

        const defaultValue = generateDefaultFromRootSchema(
          requestBody.rootSchemaType
        );
        if (defaultValue !== undefined && defaultValue !== null) {
          const defaultJson = JSON.stringify(defaultValue, null, 2);
          if (
            !value ||
            value.trim() === "" ||
            value === "{}" ||
            value === "[]" ||
            value === '""'
          ) {
            onChange(defaultJson);
          }
        } else {
          // Ref 타입이고 fields도 없으면 빈 값
          if (value && value.trim() !== "") {
            onChange("");
          }
        }
      }
      return;
    }

    // fields도 없고 rootSchemaType도 없으면 빈 값으로 설정 (이전 API의 값 제거)
    if (initializedRef.current !== "") {
      initializedRef.current = "";
      if (value && value.trim() !== "") {
        onChange("");
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [requestBody?.rootSchemaType, requestBody?.fields]);

  // JSON 타입은 항상 JSON 에디터로 표시 (form-data처럼 필드별로 표시하지 않음)
  return (
    <div>
      <JsonEditor
        value={value}
        onChange={onChange}
        placeholder='{\n  "key": "value"\n}'
        height="300px"
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
  const [showSchema, setShowSchema] = useState(false);

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

  // 타입 문자열 생성
  const getTypeString = (schemaType: SchemaType): string => {
    if (isPrimitiveSchema(schemaType)) {
      if (schemaType.type === "file") {
        return "string($binary)";
      }
      if (schemaType.type === "integer") {
        return schemaType.format === "int64" ? "integer(int64)" : "integer";
      }
      if (schemaType.type === "number") {
        return schemaType.format ? `number(${schemaType.format})` : "number";
      }
      return schemaType.type;
    }
    if (isArraySchema(schemaType)) {
      const itemType = getTypeString(schemaType.items);
      return `array[${itemType}]`;
    }
    if (isObjectSchema(schemaType)) {
      return "object";
    }
    return "unknown";
  };

  if (isPrimitiveSchema(field.schemaType)) {
    if (field.schemaType.type === "file") {
      return (
        <div className="space-y-2">
          <div className="flex items-start gap-2">
            <div className="min-w-[120px] pt-2">
              <label className="text-sm text-[#E6EDF3]">
                {field.key}
                {field.required && <span className="text-red-500 ml-1">*</span>}
              </label>
              <p className="text-xs text-[#8B949E] mt-1">string($binary)</p>
            </div>
            <div className="flex-1 space-y-2">
              <input
                type="file"
                onChange={(e) => {
                  const file = e.target.files?.[0];
                  if (file) {
                    handleValueChange(file);
                  }
                }}
                className="w-full px-3 py-2 rounded-md bg-[#0D1117] border border-[#2D333B] text-[#E6EDF3] text-sm"
              />
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
            {canRemove && onRemove && (
              <button
                onClick={onRemove}
                className="px-2 py-1 text-red-500 hover:text-red-700 self-start mt-2"
              >
                -
              </button>
            )}
          </div>
          {field.description && (
            <p className="text-xs text-[#8B949E] ml-[120px]">
              {field.description}
            </p>
          )}
        </div>
      );
    }

    if (field.schemaType.type === "boolean") {
      return (
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <div className="min-w-[120px]">
              <label className="text-sm text-[#E6EDF3]">
                {field.key}
                {field.required && <span className="text-red-500 ml-1">*</span>}
              </label>
              <p className="text-xs text-[#8B949E] mt-1">boolean</p>
            </div>
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
            <p className="text-xs text-[#8B949E] ml-[120px]">
              {field.description}
            </p>
          )}
        </div>
      );
    }

    return (
      <div className="space-y-2">
        <div className="flex items-center gap-2">
          <div className="min-w-[120px]">
            <label className="text-sm text-[#E6EDF3]">
              {field.key}
              {field.required && <span className="text-red-500 ml-1">*</span>}
            </label>
            <p className="text-xs text-[#8B949E] mt-1">
              {getTypeString(field.schemaType)}
            </p>
          </div>
          <input
            type={
              isPrimitiveSchema(field.schemaType) &&
              (field.schemaType.type === "integer" ||
                field.schemaType.type === "number")
                ? "number"
                : "text"
            }
            value={value === undefined ? "" : String(value)}
            onChange={(e) => {
              if (!isPrimitiveSchema(field.schemaType)) return;
              const val =
                field.schemaType.type === "integer"
                  ? parseInt(e.target.value) || 0
                  : field.schemaType.type === "number"
                  ? parseFloat(e.target.value) || 0
                  : e.target.value;
              handleValueChange(val);
            }}
            placeholder={
              isPrimitiveSchema(field.schemaType) &&
              field.schemaType.type === "string"
                ? "string"
                : String(getDefaultValue(field.schemaType))
            }
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
          <p className="text-xs text-[#8B949E] ml-[120px]">
            {field.description}
          </p>
        )}
      </div>
    );
  }

  if (isArraySchema(field.schemaType)) {
    const arrayValue = Array.isArray(value) ? value : [];
    const itemSchema = field.schemaType.items;
    const isPrimitiveItem = isPrimitiveSchema(itemSchema);
    const isStringItem = isPrimitiveItem && itemSchema.type === "string";

    return (
      <div className="space-y-2">
        <div className="flex items-start gap-2">
          <div className="min-w-[120px] pt-2">
            <label className="text-sm text-[#E6EDF3]">
              {field.key}
              {field.required && <span className="text-red-500 ml-1">*</span>}
            </label>
            <p className="text-xs text-[#8B949E] mt-1">
              {getTypeString(field.schemaType)}
            </p>
          </div>
          <div className="flex-1 space-y-2">
            {isStringItem ? (
              // Array of primitive (string) - 각 항목을 입력 필드로
              <>
                {arrayValue.map((item: any, index: number) => (
                  <div key={index} className="flex gap-2 items-center">
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
                    <button
                      onClick={() => {
                        const newArray = arrayValue.filter(
                          (_: any, i: number) => i !== index
                        );
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
                    const newArray = [...arrayValue, ""];
                    onChange(newArray);
                  }}
                  className="px-3 py-1 text-sm bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded"
                >
                  Add string item
                </button>
                <label className="flex items-center gap-1 text-xs text-[#8B949E]">
                  <input
                    type="checkbox"
                    checked={isEmpty}
                    onChange={(e) => handleEmptyToggle(e.target.checked)}
                    className="w-3 h-3"
                  />
                  <span>Send empty value</span>
                </label>
              </>
            ) : (
              // Array of object - Edit Value/Schema 탭
              <>
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
                    {isObjectSchema(itemSchema) &&
                      itemSchema.properties.map((prop) => (
                        <div key={prop.key} className="text-xs">
                          {prop.key}: {prop.schemaType.kind}
                        </div>
                      ))}
                  </div>
                ) : (
                  <>
                    <JsonEditor
                      value={JSON.stringify(arrayValue, null, 2)}
                      onChange={(newValue) => {
                        try {
                          const parsed = JSON.parse(newValue);
                          if (Array.isArray(parsed)) {
                            onChange(parsed);
                          }
                        } catch {
                          // Invalid JSON, keep as is
                        }
                      }}
                      placeholder="[]"
                      height="200px"
                    />
                    <div className="flex gap-2">
                      <button
                        onClick={() => {
                          const newArray = [
                            ...arrayValue,
                            getDefaultValue(itemSchema),
                          ];
                          onChange(newArray);
                        }}
                        className="px-3 py-1 text-sm bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded"
                      >
                        Add object item
                      </button>
                      {arrayValue.length > 0 && (
                        <button
                          onClick={() => {
                            const newArray = arrayValue.slice(0, -1);
                            onChange(newArray);
                          }}
                          className="px-2 py-1 text-red-500 hover:text-red-700"
                        >
                          -
                        </button>
                      )}
                    </div>
                    <label className="flex items-center gap-1 text-xs text-[#8B949E]">
                      <input
                        type="checkbox"
                        checked={isEmpty}
                        onChange={(e) => handleEmptyToggle(e.target.checked)}
                        className="w-3 h-3"
                      />
                      <span>Send empty value</span>
                    </label>
                  </>
                )}
              </>
            )}
          </div>
          {canRemove && onRemove && (
            <button
              onClick={onRemove}
              className="px-2 py-1 text-red-500 hover:text-red-700 self-start mt-2"
            >
              -
            </button>
          )}
        </div>
        {field.description && (
          <p className="text-xs text-[#8B949E] ml-[120px]">
            {field.description}
          </p>
        )}
      </div>
    );
  }

  if (isObjectSchema(field.schemaType)) {
    const objectValue =
      typeof value === "object" && value !== null ? value : {};

    return (
      <div className="space-y-2">
        <div className="flex items-start gap-2">
          <div className="min-w-[120px] pt-2">
            <label className="text-sm text-[#E6EDF3]">
              {field.key}
              {field.required && <span className="text-red-500 ml-1">*</span>}
            </label>
            <p className="text-xs text-[#8B949E] mt-1">object</p>
          </div>
          <div className="flex-1 space-y-2">
            <div className="flex gap-2">
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
              <JsonEditor
                value={JSON.stringify(objectValue, null, 2)}
                onChange={(newValue) => {
                  try {
                    const parsed = JSON.parse(newValue);
                    onChange(parsed);
                  } catch {
                    // Invalid JSON, keep as is
                  }
                }}
                placeholder='{\n  "key": "value"\n}'
                height="200px"
              />
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
          {canRemove && onRemove && (
            <button
              onClick={onRemove}
              className="px-2 py-1 text-red-500 hover:text-red-700 self-start mt-2"
            >
              -
            </button>
          )}
        </div>
        {field.description && (
          <p className="text-xs text-[#8B949E] ml-[120px]">
            {field.description}
          </p>
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
  const prevValueRef = useRef<string>("");

  // value prop이 변경될 때만 formData 업데이트 (외부에서 변경된 경우)
  useEffect(() => {
    if (value !== prevValueRef.current) {
      prevValueRef.current = value;
      if (value) {
        try {
          const parsed = JSON.parse(value);
          setFormData(parsed);
        } catch {
          // Invalid JSON, keep empty
          setFormData({});
        }
      } else {
        setFormData({});
      }
    }
  }, [value]);

  // formData 업데이트 핸들러
  const handleFormDataChange = (key: string, newValue: any) => {
    setFormData((prev) => {
      const updated = { ...prev, [key]: newValue };
      const stringified = JSON.stringify(updated, null, 2);
      prevValueRef.current = stringified;
      onChange(stringified);
      return updated;
    });
  };

  if (!requestBody.fields || requestBody.fields.length === 0) {
    return (
      <div className="text-sm text-[#8B949E]">No fields defined in schema</div>
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
            handleFormDataChange(field.key, newValue);
          }}
        />
      ))}
    </div>
  );
}

// XML Body Form
function XmlBodyForm({
  value,
  onChange,
}: {
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <div>
      <textarea
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder='<?xml version="1.0" encoding="UTF-8"?>\n<root>\n  <key>value</key>\n</root>'
        className="w-full h-40 px-3 py-2 rounded-md bg-[#0D1117] border border-[#2D333B] text-[#E6EDF3] placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-gray-400 dark:focus:ring-gray-500 focus:border-gray-400 dark:focus:border-gray-500 font-mono text-sm"
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
  // requestBody가 없거나 type이 "none"이면 기본 JSON 폼 표시
  if (!requestBody || requestBody.type === "none") {
    return (
      <JsonBodyForm requestBody={null} value={value} onChange={onChange} />
    );
  }

  // Content-Type에 따라 다른 폼 표시
  if (contentType.includes("application/json") || requestBody.type === "json") {
    return (
      <JsonBodyForm
        requestBody={requestBody}
        value={value}
        onChange={onChange}
      />
    );
  }

  if (
    contentType.includes("multipart/form-data") ||
    requestBody.type === "form-data"
  ) {
    return (
      <FormDataBodyForm
        requestBody={requestBody}
        value={value}
        onChange={onChange}
      />
    );
  }

  if (
    contentType.includes("application/x-www-form-urlencoded") ||
    requestBody.type === "x-www-form-urlencoded"
  ) {
    return (
      <FormDataBodyForm
        requestBody={requestBody}
        value={value}
        onChange={onChange}
      />
    );
  }

  if (
    contentType.includes("application/xml") ||
    contentType.includes("text/xml") ||
    requestBody.type === "xml"
  ) {
    return <XmlBodyForm value={value} onChange={onChange} />;
  }

  // Default: JSON
  return (
    <JsonBodyForm requestBody={requestBody} value={value} onChange={onChange} />
  );
}
