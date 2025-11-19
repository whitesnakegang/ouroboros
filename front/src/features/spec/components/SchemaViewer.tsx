import React, { useState, useEffect } from "react";
import type { SchemaField, SchemaType } from "../types/schema.types";
import {
  isPrimitiveSchema,
  isObjectSchema,
  isArraySchema,
  isRefSchema,
} from "../types/schema.types";
import { getSchema } from "../services/api";
import { parseOpenAPISchemaToSchemaField } from "../utils/schemaConverter";

interface SchemaViewerProps {
  schemaType?: SchemaType;
  fields?: SchemaField[];
  schemaRef?: string;
  schemaName?: string | null; // 스키마 이름 (표시용)
  description?: string;
  contentType?: string;
}

export function SchemaViewer({
  schemaType,
  fields,
  schemaRef,
  schemaName,
  description,
  contentType = "application/json",
}: SchemaViewerProps) {
  const [loadedFields, setLoadedFields] = useState<SchemaField[]>([]);
  const [isLoadingSchema, setIsLoadingSchema] = useState(false);

  // schemaRef 또는 rootSchemaType이 ref인 경우 스키마를 조회하여 필드들을 로드
  useEffect(() => {
    const loadSchemaFields = async () => {
      // schemaRef가 있으면 사용
      let schemaNameToLoad: string | undefined = schemaRef;

      // schemaRef가 없고 rootSchemaType이 ref인 경우 사용
      if (!schemaNameToLoad && schemaType && isRefSchema(schemaType)) {
        schemaNameToLoad = schemaType.schemaName;
      }

      if (!schemaNameToLoad) {
        setLoadedFields([]);
        return;
      }

      setIsLoadingSchema(true);
      try {
        const response = await getSchema(schemaNameToLoad);
        const schemaData = response.data;

        if (schemaData.properties) {
          const requiredFields = schemaData.required || [];
          const schemaFields: SchemaField[] = Object.entries(
            schemaData.properties
          ).map(([key, propSchema]: [string, unknown]) => {
            const field = parseOpenAPISchemaToSchemaField(key, propSchema);
            // required 필드 설정
            field.required = requiredFields.includes(key);
            return field;
          });
          setLoadedFields(schemaFields);
        } else {
          setLoadedFields([]);
        }
      } catch (error) {
        // 스키마 조회 실패 시 빈 배열
        setLoadedFields([]);
      } finally {
        setIsLoadingSchema(false);
      }
    };

    loadSchemaFields();
  }, [schemaRef, schemaType]);
  // SchemaField를 재귀적으로 렌더링하는 함수
  const renderField = (
    field: SchemaField,
    depth: number = 0
  ): React.ReactElement => {
    const getTypeString = (schemaType: SchemaType): string => {
      if (isPrimitiveSchema(schemaType)) {
        return schemaType.type;
      } else if (isObjectSchema(schemaType)) {
        return "object";
      } else if (isArraySchema(schemaType)) {
        const itemType = getTypeString(schemaType.items);
        return `${itemType}[]`;
      } else if (isRefSchema(schemaType)) {
        return schemaType.schemaName;
      }
      return "string";
    };

    const typeString = getTypeString(field.schemaType);

    return (
      <div key={field.key} className="relative">
        <div className="flex items-start py-2">
          {/* 트리 세로선 및 들여쓰기 */}
          <div
            className="flex-shrink-0 relative"
            style={{ width: `${depth * 20 + 20}px` }}
          >
            {/* 왼쪽 세로선 (전체 높이) */}
            <div className="absolute left-0 top-0 bottom-0 w-px bg-gray-300 dark:bg-[#2D333B]" />
            {/* 각 depth의 가로선 */}
            {depth > 0 && (
              <div
                className="absolute top-3 w-4 h-px bg-gray-300 dark:bg-[#2D333B]"
                style={{ left: `${depth * 20}px` }}
              />
            )}
          </div>

          {/* 필드 내용 */}
          <div className="flex-1 min-w-0">
            <div className="flex items-start justify-between gap-4">
              <div className="flex-1 min-w-0">
                <div className="flex items-baseline gap-2">
                  <span className="font-semibold text-gray-900 dark:text-[#E6EDF3] text-sm">
                    {field.key}
                  </span>
                  <span className="text-xs text-gray-500 dark:text-[#8B949E] font-mono">
                    {typeString}
                  </span>
                </div>
                {field.description && (
                  <div className="mt-1 text-xs text-gray-600 dark:text-[#8B949E]">
                    {field.description}
                  </div>
                )}
              </div>
              {field.required && (
                <div className="flex-shrink-0">
                  <span className="px-2 py-0.5 text-xs font-medium bg-orange-100 dark:bg-orange-900/30 text-orange-700 dark:text-orange-400 rounded">
                    required
                  </span>
                </div>
              )}
            </div>

            {/* Nested fields for object */}
            {isObjectSchema(field.schemaType) &&
              field.schemaType.properties &&
              field.schemaType.properties.length > 0 && (
                <div className="mt-2">
                  {field.schemaType.properties.map((prop) =>
                    renderField(prop, depth + 1)
                  )}
                </div>
              )}
            {/* Array items */}
            {isArraySchema(field.schemaType) && (
              <div className="mt-2">
                {isObjectSchema(field.schemaType.items) &&
                field.schemaType.items.properties ? (
                  field.schemaType.items.properties.map((prop) =>
                    renderField(prop, depth + 1)
                  )
                ) : (
                  <div className="py-2 text-xs text-gray-600 dark:text-[#8B949E]">
                    <span className="font-medium">Type:</span>{" "}
                    <span className="font-mono">
                      {getTypeString(field.schemaType.items)}
                    </span>
                    {field.schemaType.itemsDescription && (
                      <div className="mt-1">
                        {field.schemaType.itemsDescription}
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    );
  };

  // Root schema type에서 fields 추출
  const getFieldsFromSchemaType = (): SchemaField[] => {
    // schemaRef 또는 rootSchemaType이 ref인 경우 로드된 필드가 있으면 우선 사용
    if (loadedFields.length > 0) {
      return loadedFields;
    }
    if (fields && fields.length > 0) {
      return fields;
    }
    if (schemaType && isObjectSchema(schemaType)) {
      return schemaType.properties || [];
    }
    return [];
  };

  const displayFields = getFieldsFromSchemaType();

  if (
    !schemaRef &&
    !schemaType &&
    (!displayFields || displayFields.length === 0)
  ) {
    return null;
  }

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <h3 className="text-lg font-bold text-gray-900 dark:text-[#E6EDF3]">
            Body
          </h3>
          {description && (
            <p className="mt-1 text-sm text-gray-600 dark:text-[#8B949E]">
              {description}
            </p>
          )}
        </div>
        <div className="px-3 py-1.5 text-sm font-medium text-gray-700 dark:text-[#8B949E] bg-gray-100 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] rounded-md">
          {contentType}
        </div>
      </div>

      {/* Schema Reference */}
      {(schemaRef || (schemaType && isRefSchema(schemaType))) && (
        <div className="p-3 bg-gray-50 dark:bg-[#0D1117] border border-gray-200 dark:border-[#2D333B] rounded-md">
          <div className="text-sm text-gray-600 dark:text-[#8B949E]">
            <span className="font-medium">Schema Reference:</span>{" "}
            <span className="font-mono text-gray-900 dark:text-[#E6EDF3]">
              {schemaName ||
                (schemaType && isRefSchema(schemaType)
                  ? schemaType.schemaName
                  : null) ||
                schemaRef}
            </span>
            {isLoadingSchema && (
              <span className="ml-2 text-xs text-gray-500 dark:text-[#8B949E]">
                (로딩 중...)
              </span>
            )}
          </div>
        </div>
      )}

      {/* Fields */}
      {isLoadingSchema ? (
        <div className="px-6 py-4">
          <div className="text-sm text-gray-600 dark:text-[#8B949E] text-center">
            스키마를 불러오는 중...
          </div>
        </div>
      ) : displayFields.length > 0 ? (
        <div className="px-6 py-4 relative">
          {displayFields.map((field) => renderField(field, 0))}
        </div>
      ) : schemaRef ? (
        <div className="px-6 py-4">
          <div className="text-sm text-gray-600 dark:text-[#8B949E] text-center">
            스키마 필드를 불러올 수 없습니다.
          </div>
        </div>
      ) : null}

      {/* Primitive type (no fields) */}
      {schemaType && isPrimitiveSchema(schemaType) && !schemaRef && (
        <div className="px-6 py-4">
          <div className="text-sm text-gray-600 dark:text-[#8B949E]">
            <span className="font-medium">Type:</span>{" "}
            <span className="font-mono text-gray-900 dark:text-[#E6EDF3]">
              {schemaType.type}
            </span>
          </div>
        </div>
      )}

      {/* Array of primitive */}
      {schemaType &&
        isArraySchema(schemaType) &&
        isPrimitiveSchema(schemaType.items) && (
          <div className="px-6 py-4">
            <div className="text-sm text-gray-600 dark:text-[#8B949E]">
              <span className="font-medium">Type:</span>{" "}
              <span className="font-mono text-gray-900 dark:text-[#E6EDF3]">
                {schemaType.items.type}[]
              </span>
            </div>
            {schemaType.itemsDescription && (
              <div className="mt-2 text-sm text-gray-600 dark:text-[#8B949E]">
                {schemaType.itemsDescription}
              </div>
            )}
          </div>
        )}
    </div>
  );
}
