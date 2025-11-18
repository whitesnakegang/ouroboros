import { useState, useEffect } from "react";
import { SchemaFieldEditor } from "./SchemaFieldEditor";
import { SchemaModal } from "./SchemaModal";
import { getAllSchemas, type SchemaResponse } from "../services/api";
import type { SchemaField, RequestBody } from "../types/schema.types";
import { createDefaultField } from "../types/schema.types";

interface KeyValuePair {
  key: string;
  value: string;
  required?: boolean;
  description?: string;
  type?: string;
}

interface WsReceiverFormProps {
  address: string;
  setAddress: (address: string) => void;
  headers: KeyValuePair[];
  setHeaders: (headers: KeyValuePair[]) => void;
  schema: RequestBody;
  setSchema: (schema: RequestBody) => void;
  onRemove: () => void;
  isReadOnly?: boolean;
}

export function WsReceiverForm({
  address,
  setAddress,
  headers,
  setHeaders,
  schema,
  setSchema,
  onRemove,
  isReadOnly = false,
}: WsReceiverFormProps) {
  const [schemas, setSchemas] = useState<SchemaResponse[]>([]);
  const [isSchemaModalOpen, setIsSchemaModalOpen] = useState(false);

  // 스키마 목록 로드
  useEffect(() => {
    const loadSchemas = async () => {
      try {
        const response = await getAllSchemas();
        setSchemas(response.data);
      } catch (err) {
        console.error("스키마 로드 실패:", err);
      }
    };
    loadSchemas();
  }, []);

  const addHeader = () => {
    if (isReadOnly) return;
    setHeaders([...headers, { key: "", value: "", required: false }]);
  };

  const removeHeader = (index: number) => {
    if (isReadOnly) return;
    setHeaders(headers.filter((_, i) => i !== index));
  };

  const updateHeader = (
    index: number,
    field: "key" | "value",
    value: string
  ) => {
    if (isReadOnly) return;
    const updated = [...headers];
    updated[index] = { ...updated[index], [field]: value };
    setHeaders(updated);
  };

  const addSchemaField = () => {
    if (isReadOnly) return;
    const currentFields = schema.fields || [];
    setSchema({
      ...schema,
      fields: [...currentFields, createDefaultField()],
    });
  };

  const removeSchemaField = (index: number) => {
    if (isReadOnly) return;
    const currentFields = schema.fields || [];
    setSchema({
      ...schema,
      fields: currentFields.filter((_, i) => i !== index),
    });
  };

  const updateSchemaField = (index: number, field: SchemaField) => {
    if (isReadOnly) return;
    const currentFields = schema.fields || [];
    const updated = [...currentFields];
    updated[index] = field;
    setSchema({ ...schema, fields: updated });
  };

  // Schema Reference 선택 시 처리
  const handleSchemaSelect = async (selectedSchema: {
    name: string;
    fields: SchemaField[];
    type: string;
  }) => {
    if (selectedSchema.type === "object") {
      setSchema({
        ...schema,
        schemaRef: selectedSchema.name,
        fields: selectedSchema.fields,
      });
    } else {
      alert("스키마는 object 타입만 지원됩니다.");
    }
  };

  return (
    <div className="rounded-md border border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#161B22] p-4 shadow-sm mb-6">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3]">
          Receiver
        </h3>
        {!isReadOnly && (
          <button
            onClick={onRemove}
            className="text-red-500 hover:text-red-700 text-sm font-medium"
          >
            삭제
          </button>
        )}
      </div>

      {/* 주소 */}
      <div className="mb-4">
        <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
          주소
        </label>
        <input
          type="text"
          value={address}
          onChange={(e) => setAddress(e.target.value)}
          placeholder="예: /chat/message"
          disabled={isReadOnly}
          className={`w-full px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-gray-400 dark:focus:ring-gray-500 focus:border-gray-400 dark:focus:border-gray-500 text-sm ${
            isReadOnly ? "opacity-60 cursor-not-allowed" : ""
          }`}
        />
      </div>

      {/* Header */}
      <div className="mb-4">
        <div className="flex items-center justify-between mb-2">
          <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E]">
            Header
          </label>
          {!isReadOnly && (
            <button
              onClick={addHeader}
              className="text-[#2563EB] hover:text-[#1E40AF] text-xs font-medium"
            >
              + Add Header
            </button>
          )}
        </div>
        <div className="space-y-2">
          {headers.map((header, index) => (
            <div key={index} className="flex gap-2">
              <input
                type="text"
                value={header.key}
                onChange={(e) => updateHeader(index, "key", e.target.value)}
                placeholder="Header 이름"
                disabled={isReadOnly}
                className={`flex-1 px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-gray-400 dark:focus:ring-gray-500 focus:border-gray-400 dark:focus:border-gray-500 text-sm ${
                  isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                }`}
              />
              <input
                type="text"
                value={header.value}
                onChange={(e) => updateHeader(index, "value", e.target.value)}
                placeholder="Header 값"
                disabled={isReadOnly}
                className={`flex-1 px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-gray-400 dark:focus:ring-gray-500 focus:border-gray-400 dark:focus:border-gray-500 text-sm ${
                  isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                }`}
              />
              {!isReadOnly && (
                <button
                  onClick={() => removeHeader(index)}
                  className="px-2 py-2 text-red-500 hover:text-red-700"
                >
                  <svg
                    className="w-4 h-4"
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
              )}
            </div>
          ))}
          {headers.length === 0 && (
            <p className="text-xs text-gray-500 dark:text-gray-400 text-center py-2">
              Header가 없습니다. "+ Add Header"를 클릭하여 추가하세요.
            </p>
          )}
        </div>
      </div>

      {/* Schema */}
      <div className="mt-4">
        <div className="flex items-center justify-between mb-2">
          <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E]">
            Schema
          </label>
          <div className="flex gap-2">
            {!isReadOnly && (
              <>
                <button
                  onClick={() => setIsSchemaModalOpen(true)}
                  className="text-[#2563EB] hover:text-[#1E40AF] text-xs font-medium"
                >
                  Schema 선택
                </button>
                <button
                  onClick={addSchemaField}
                  className="text-[#2563EB] hover:text-[#1E40AF] text-xs font-medium"
                >
                  + Add Field
                </button>
              </>
            )}
          </div>
        </div>

        {/* Schema Reference 표시 */}
        {schema.schemaRef && (
          <div className="mb-2 p-2 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-md">
            <div className="flex items-center justify-between">
              <span className="text-xs text-blue-700 dark:text-blue-300">
                Schema: {schema.schemaRef}
              </span>
              {!isReadOnly && (
                <button
                  onClick={() => {
                    setSchema({ ...schema, schemaRef: undefined });
                  }}
                  className="text-blue-500 hover:text-blue-700 text-xs"
                >
                  제거
                </button>
              )}
            </div>
          </div>
        )}

        {/* Schema Fields */}
        <div className="space-y-2">
          {schema.fields && schema.fields.length > 0 ? (
            schema.fields.map((field, index) => (
              <SchemaFieldEditor
                key={index}
                field={field}
                onChange={(newField) => updateSchemaField(index, newField)}
                onRemove={() => removeSchemaField(index)}
                isReadOnly={isReadOnly}
                allowFileType={false}
                allowMockExpression={true}
              />
            ))
          ) : (
            <p className="text-xs text-gray-500 dark:text-gray-400 text-center py-2">
              Schema 필드가 없습니다. "+ Add Field"를 클릭하여 추가하거나
              Schema를 선택하세요.
            </p>
          )}
        </div>
      </div>

      {/* Schema Modal */}
      <SchemaModal
        isOpen={isSchemaModalOpen}
        onClose={() => setIsSchemaModalOpen(false)}
        onSelect={handleSchemaSelect}
        schemas={schemas}
        setSchemas={setSchemas}
      />
    </div>
  );
}
