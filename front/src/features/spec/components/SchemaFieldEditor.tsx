import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import type {
  SchemaField,
  SchemaType,
  PrimitiveTypeName,
} from "../types/schema.types";
import {
  createPrimitiveField,
  createObjectField,
  createArrayField,
  createRefField,
  isPrimitiveSchema,
  isObjectSchema,
  isArraySchema,
  isRefSchema,
} from "../types/schema.types";
import { getAllSchemas, type SchemaResponse } from "../services/api";
import { MockExpressionModal } from "./MockExpressionModal";
import { SchemaModal } from "./SchemaModal";

interface SchemaFieldEditorProps {
  field: SchemaField;
  onChange: (field: SchemaField) => void;
  onRemove?: () => void;
  depth?: number;
  isReadOnly?: boolean;
  allowFileType?: boolean; // multipart/form-data에서만 file 타입 허용
  allowMockExpression?: boolean; // Schema 정의 시 mock expression 허용
}

export function SchemaFieldEditor({
  field,
  onChange,
  onRemove,
  depth = 0,
  isReadOnly = false,
  allowFileType = false,
  allowMockExpression = false,
}: SchemaFieldEditorProps) {
  const { t } = useTranslation();
  const [schemas, setSchemas] = useState<SchemaResponse[]>([]);
  const [isMockModalOpen, setIsMockModalOpen] = useState(false);
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

  const schemaKind = field.schemaType.kind;

  // Kind 변경 핸들러
  const handleKindChange = (newKind: SchemaType["kind"]) => {
    let newField: SchemaField;

    switch (newKind) {
      case "primitive":
        newField = createPrimitiveField(field.key, "string");
        break;
      case "object":
        newField = createObjectField(field.key);
        break;
      case "array":
        newField = createArrayField(field.key);
        break;
      case "ref":
        newField = createRefField(field.key, "");
        break;
      default:
        return;
    }

    // 기존 메타 정보 유지
    newField.description = field.description;
    newField.required = field.required;
    newField.mockExpression = field.mockExpression;

    onChange(newField);
  };

  // Primitive 타입 변경
  const handlePrimitiveTypeChange = (type: PrimitiveTypeName) => {
    if (isPrimitiveSchema(field.schemaType)) {
      onChange({
        ...field,
        schemaType: {
          ...field.schemaType,
          type,
          format: type === "file" ? "binary" : undefined,
        },
      });
    }
  };

  // Object property 추가
  const handleAddProperty = () => {
    if (isObjectSchema(field.schemaType)) {
      onChange({
        ...field,
        schemaType: {
          ...field.schemaType,
          properties: [
            ...field.schemaType.properties,
            createPrimitiveField("", "string"),
          ],
        },
      });
    }
  };

  // Object property 변경
  const handlePropertyChange = (index: number, newProperty: SchemaField) => {
    if (isObjectSchema(field.schemaType)) {
      const newProperties = [...field.schemaType.properties];
      newProperties[index] = newProperty;
      onChange({
        ...field,
        schemaType: {
          ...field.schemaType,
          properties: newProperties,
        },
      });
    }
  };

  // Object property 삭제
  const handlePropertyRemove = (index: number) => {
    if (isObjectSchema(field.schemaType)) {
      onChange({
        ...field,
        schemaType: {
          ...field.schemaType,
          properties: field.schemaType.properties.filter((_, i) => i !== index),
        },
      });
    }
  };

  // Array items 변경
  const handleArrayItemsChange = (newField: SchemaField) => {
    if (isArraySchema(field.schemaType)) {
      onChange({
        ...field,
        schemaType: {
          ...field.schemaType,
          items: newField.schemaType,
          itemsDescription: newField.description,
          itemsRequired: newField.required,
        },
      });
    }
  };

  const indentStyle = { paddingLeft: `${depth * 24}px` };

  const primitiveTypes: PrimitiveTypeName[] = allowFileType
    ? ["string", "integer", "number", "boolean", "file"]
    : ["string", "integer", "number", "boolean"];

  return (
    <div
      className="border-l-2 border-gray-200 dark:border-gray-700 overflow-visible"
      style={indentStyle}
    >
      <div className="flex gap-2 items-start mb-2 p-2 bg-gray-50 dark:bg-gray-800/50 rounded overflow-visible">
        {/* Required Checkbox */}
        <input
          type="checkbox"
          checked={field.required || false}
          onChange={(e) => onChange({ ...field, required: e.target.checked })}
          disabled={isReadOnly}
          className="mt-2 w-4 h-4"
          title="Required"
        />

        {/* Field Name */}
        <div className="flex-1">
          <input
            type="text"
            value={field.key}
            onChange={(e) => onChange({ ...field, key: e.target.value })}
            placeholder={t("apiCard.fieldName")}
            disabled={isReadOnly}
            className="w-full px-3 py-1.5 rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-sm"
          />
        </div>

        {/* Schema Kind Selector */}
        <select
          value={schemaKind}
          onChange={(e) =>
            handleKindChange(e.target.value as SchemaType["kind"])
          }
          disabled={isReadOnly}
          className="px-3 py-1.5 rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-sm relative z-10"
        >
          <option value="primitive">{t("apiCard.primitive")}</option>
          <option value="object">{t("apiCard.object")}</option>
          <option value="array">{t("apiCard.array")}</option>
          <option value="ref">{t("apiCard.schema")}</option>
        </select>

        {/* Type-specific controls */}
        {isPrimitiveSchema(field.schemaType) && (
          <select
            value={field.schemaType.type}
            onChange={(e) =>
              handlePrimitiveTypeChange(e.target.value as PrimitiveTypeName)
            }
            disabled={isReadOnly}
            className="px-3 py-1.5 rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-sm"
          >
            {primitiveTypes.map((type) => (
              <option key={type} value={type}>
                {type}
              </option>
            ))}
          </select>
        )}

        {isRefSchema(field.schemaType) && (
          <button
            onClick={() => setIsSchemaModalOpen(true)}
            disabled={isReadOnly}
            className="px-3 py-1.5 rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-sm min-w-[200px] text-left hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors disabled:opacity-50"
          >
            {field.schemaType.schemaName || t("apiCard.selectSchema")}
          </button>
        )}

        {/* Remove Button */}
        {onRemove && !isReadOnly && (
          <button
            onClick={onRemove}
            className="p-1.5 text-red-500 hover:text-red-700"
            title="Remove field"
          >
            <svg
              className="w-5 h-5"
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

      {/* Description & Mock Expression */}
      <div className="ml-2 mb-2 space-y-1">
        <input
          type="text"
          value={field.description || ""}
          onChange={(e) => onChange({ ...field, description: e.target.value })}
          placeholder={t("apiCard.descriptionOptional")}
          disabled={isReadOnly}
          className="w-full px-2 py-1 text-xs rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900"
        />
        {allowMockExpression && isPrimitiveSchema(field.schemaType) && (
          <button
            type="button"
            onClick={() => setIsMockModalOpen(true)}
            disabled={isReadOnly}
            className="w-full px-2 py-1 text-xs rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-left hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors disabled:opacity-50"
          >
            {field.mockExpression ? (
              <span className="font-mono text-green-600 dark:text-green-400">
                {field.mockExpression}
              </span>
            ) : (
              <span className="text-gray-400 dark:text-gray-500">
                {t("apiCard.clickToSelectMockExpression")}
              </span>
            )}
          </button>
        )}
      </div>

      {/* Object Properties (재귀!) */}
      {isObjectSchema(field.schemaType) && (
        <div className="ml-4 mb-2">
          <div className="mb-2">
            <button
              onClick={handleAddProperty}
              disabled={isReadOnly}
              className="px-3 py-1 text-sm bg-blue-600 hover:bg-blue-700 text-white rounded"
            >
              {t("apiCard.addProperty")}
            </button>
          </div>
          {field.schemaType.properties.map((prop, idx) => (
            <SchemaFieldEditor
              key={idx}
              field={prop}
              onChange={(newProp) => handlePropertyChange(idx, newProp)}
              onRemove={() => handlePropertyRemove(idx)}
              depth={depth + 1}
              isReadOnly={isReadOnly}
              allowFileType={allowFileType}
              allowMockExpression={allowMockExpression}
            />
          ))}
        </div>
      )}

      {/* Array Items (재귀!) */}
      {isArraySchema(field.schemaType) && (
        <div className="ml-4 mb-2">
          <p className="text-xs text-gray-600 dark:text-gray-400 mb-1">
            {t("apiCard.arrayItems")}
          </p>
          <SchemaFieldEditor
            field={{
              key: "items",
              schemaType: field.schemaType.items,
              description: field.schemaType.itemsDescription,
              required: field.schemaType.itemsRequired,
            }}
            onChange={(newField) => handleArrayItemsChange(newField)}
            depth={depth + 1}
            isReadOnly={isReadOnly}
            allowFileType={allowFileType}
            allowMockExpression={allowMockExpression}
          />
          <div className="flex gap-2 mt-2">
            <input
              type="number"
              value={field.schemaType.minItems || ""}
              onChange={(e) => {
                if (isArraySchema(field.schemaType)) {
                  onChange({
                    ...field,
                    schemaType: {
                      ...field.schemaType,
                      minItems: e.target.value
                        ? parseInt(e.target.value)
                        : undefined,
                    },
                  });
                }
              }}
              placeholder="Min items"
              disabled={isReadOnly}
              className="px-2 py-1 text-xs rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900"
            />
            <input
              type="number"
              value={field.schemaType.maxItems || ""}
              onChange={(e) => {
                if (isArraySchema(field.schemaType)) {
                  onChange({
                    ...field,
                    schemaType: {
                      ...field.schemaType,
                      maxItems: e.target.value
                        ? parseInt(e.target.value)
                        : undefined,
                    },
                  });
                }
              }}
              placeholder="Max items"
              disabled={isReadOnly}
              className="px-2 py-1 text-xs rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900"
            />
          </div>
        </div>
      )}

      {/* Mock Expression Modal */}
      {allowMockExpression && (
        <MockExpressionModal
          isOpen={isMockModalOpen}
          onClose={() => setIsMockModalOpen(false)}
          onSelect={(expression) => {
            onChange({ ...field, mockExpression: expression });
            setIsMockModalOpen(false);
          }}
          initialValue={field.mockExpression || ""}
        />
      )}

      {/* Schema Modal */}
      <SchemaModal
        isOpen={isSchemaModalOpen}
        onClose={() => setIsSchemaModalOpen(false)}
        onSelect={(schema) => {
          onChange({
            ...field,
            schemaType: {
              kind: "ref",
              schemaName: schema.name,
            },
          });
          setIsSchemaModalOpen(false);
        }}
        schemas={schemas}
        setSchemas={setSchemas}
      />
    </div>
  );
}
