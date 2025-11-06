import { useState, useEffect } from "react";
import { SchemaModal } from "./SchemaModal";
import { SchemaFieldEditor } from "./SchemaFieldEditor";
import { getAllSchemas, createSchema, updateSchema } from "../services/api";
import type {
  SchemaResponse,
  CreateSchemaRequest,
  UpdateSchemaRequest,
} from "../services/api";
import type { SchemaField } from "../types/schema.types";
import { createDefaultField } from "../types/schema.types";
import { convertSchemaFieldToOpenAPI } from "../utils/schemaConverter";

interface SchemaCardProps {
  isReadOnly?: boolean;
}

export function SchemaCard({ isReadOnly = false }: SchemaCardProps) {
  const [schemas, setSchemas] = useState<SchemaResponse[]>([]);
  const [isSchemaModalOpen, setIsSchemaModalOpen] = useState(false);
  const [schemaFields, setSchemaFields] = useState<SchemaField[]>([]);
  const [currentSchemaName, setCurrentSchemaName] = useState("");
  const [currentSchemaDescription, setCurrentSchemaDescription] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Schema Type 상태
  const [schemaType, setSchemaType] = useState<
    "object" | "array" | "string" | "number" | "boolean"
  >("object");
  const [arrayItemType, setArrayItemType] = useState<
    "object" | "string" | "number" | "boolean"
  >("object");

  // 에러 메시지에서 localhost 주소 제거 및 사용자 친화적인 메시지로 변환
  const getErrorMessage = (error: unknown): string => {
    if (error instanceof Error) {
      let message = error.message;
      // localhost 주소 제거
      message = message.replace(/https?:\/\/localhost:\d+/gi, "");
      message = message.replace(/https?:\/\/127\.0\.0\.1:\d+/gi, "");
      // 불필요한 공백 정리
      message = message.trim();
      // 빈 메시지인 경우 기본 메시지 반환
      if (!message) {
        return "알 수 없는 오류가 발생했습니다.";
      }
      return message;
    }
    return "알 수 없는 오류가 발생했습니다.";
  };

  // 컴포넌트 마운트 시 스키마 목록 로드
  useEffect(() => {
    loadSchemas();
  }, []);

  // 스키마 목록 로드
  const loadSchemas = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const response = await getAllSchemas();
      setSchemas(response.data);
    } catch (err) {
      console.error("스키마 로드 실패:", err);
      setError(
        err instanceof Error ? err.message : "스키마를 불러오는데 실패했습니다."
      );
    } finally {
      setIsLoading(false);
    }
  };

  // 스키마 저장 (생성 또는 수정)
  const saveSchema = async () => {
    if (!currentSchemaName.trim()) {
      alert("스키마 이름을 입력해주세요.");
      return;
    }

    // object나 array(object) 타입일 때만 필드 검증
    if (
      (schemaType === "object" ||
        (schemaType === "array" && arrayItemType === "object")) &&
      schemaFields.length === 0
    ) {
      alert("최소 하나의 필드를 추가해주세요.");
      return;
    }

    try {
      setIsLoading(true);
      setError(null);

      let schemaRequest: CreateSchemaRequest & { items?: any };

      if (schemaType === "array") {
        // Array 타입 처리
        if (arrayItemType === "object") {
          // Array of objects: properties 필요 (재귀 지원)
          const properties: Record<string, any> = {};
          const required: string[] = [];

          schemaFields.forEach((field) => {
            if (field.key.trim()) {
              properties[field.key] = convertSchemaFieldToOpenAPI(field);
              if (field.required) {
                required.push(field.key);
              }
            }
          });

          schemaRequest = {
            schemaName: currentSchemaName.trim(),
            type: "array",
            title: `${currentSchemaName} Schema`,
            description:
              currentSchemaDescription.trim() ||
              `${currentSchemaName} 스키마 정의`,
            items: {
              type: "object",
              properties,
              required: required.length > 0 ? required : undefined,
            },
            properties: {}, // array 타입은 properties가 빈 객체
            required: [],
            orders: schemaFields.map((f) => f.key),
          };
        } else {
          // Array of primitives: items만 필요
          schemaRequest = {
            schemaName: currentSchemaName.trim(),
            type: "array",
            title: `${currentSchemaName} Schema`,
            description:
              currentSchemaDescription.trim() ||
              `${currentSchemaName} 스키마 정의`,
            items: {
              type: arrayItemType,
            },
            properties: {},
            required: [],
            orders: [],
          };
        }
      } else if (
        schemaType === "string" ||
        schemaType === "number" ||
        schemaType === "boolean"
      ) {
        // Primitive 타입: properties 불필요
        schemaRequest = {
          schemaName: currentSchemaName.trim(),
          type: schemaType,
          title: `${currentSchemaName} Schema`,
          description:
            currentSchemaDescription.trim() ||
            `${currentSchemaName} 스키마 정의`,
          properties: {},
          required: [],
          orders: [],
        };
      } else {
        // Object 타입: properties 필요 (재귀 지원)
        const properties: Record<string, any> = {};
        const required: string[] = [];

        schemaFields.forEach((field) => {
          if (field.key.trim()) {
            properties[field.key] = convertSchemaFieldToOpenAPI(field);
            if (field.required) {
              required.push(field.key);
            }
          }
        });

        schemaRequest = {
          schemaName: currentSchemaName.trim(),
          type: "object",
          title: `${currentSchemaName} Schema`,
          description:
            currentSchemaDescription.trim() ||
            `${currentSchemaName} 스키마 정의`,
          properties,
          required: required.length > 0 ? required : undefined,
          orders: schemaFields.map((f) => f.key),
        };
      }

      // 기존 스키마가 있는지 확인
      const existingSchema = schemas.find(
        (s) => s.schemaName === currentSchemaName
      );

      console.log("🔍 Schema Request:", JSON.stringify(schemaRequest, null, 2));

      if (existingSchema) {
        // 수정
        const updateRequest: UpdateSchemaRequest & { items?: any } = {
          type: schemaRequest.type,
          title: schemaRequest.title,
          description: schemaRequest.description,
          properties: schemaRequest.properties,
          required: schemaRequest.required,
          orders: schemaRequest.orders,
          // items 필드가 있으면 포함 (array 타입인 경우)
          ...(schemaRequest.items && { items: schemaRequest.items }),
        };
        console.log("🔍 Update Request:", JSON.stringify(updateRequest, null, 2));
        await updateSchema(currentSchemaName, updateRequest);
        alert(`"${currentSchemaName}" 스키마가 수정되었습니다.`);
      } else {
        // 생성
        console.log("🔍 Create Request:", JSON.stringify(schemaRequest, null, 2));
        await createSchema(schemaRequest as CreateSchemaRequest & { items?: any });
        alert(`"${currentSchemaName}" 스키마가 생성되었습니다.`);
      }

      // 스키마 목록 다시 로드
      await loadSchemas();

      // 폼 초기화
      setSchemaFields([]);
      setCurrentSchemaName("");
      setCurrentSchemaDescription("");
    } catch (err) {
      console.error("스키마 저장 실패:", err);
      const errorMessage = getErrorMessage(err);
      alert(`스키마 저장에 실패했습니다: ${errorMessage}`);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="rounded-md border border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#161B22] p-4 shadow-sm">
      {/* Header */}
      <div className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3] mb-2 flex items-center gap-2">
        <svg
          className="h-4 w-4 text-gray-500 dark:text-[#8B949E]"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
          />
        </svg>
        <span>Schema</span>
      </div>
      <p className="text-xs text-gray-600 dark:text-[#8B949E] mb-4">
        Schema 편집 및 관리
      </p>

      {/* Content */}
      <div className="space-y-4">
        <div className="flex items-center justify-between mb-4">
          <div>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              Schema 편집 및 관리
            </p>
            {error && <p className="text-sm text-red-500 mt-1">{error}</p>}
          </div>
          <div className="flex gap-2">
            <button
              onClick={loadSchemas}
              disabled={isLoading}
              className="px-3 py-1 text-sm text-gray-600 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300 font-medium border border-gray-300 dark:border-[#2D333B] rounded-md hover:bg-gray-50 dark:hover:bg-[#161B22] disabled:opacity-50"
            >
              {isLoading ? "로딩..." : "새로고침"}
            </button>
            <button
              onClick={() => setIsSchemaModalOpen(true)}
              disabled={isReadOnly}
              className={`px-4 py-2 bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded-md text-sm font-medium transition-colors ${
                isReadOnly ? "opacity-50 cursor-not-allowed" : ""
              }`}
            >
              Schema 관리
            </button>
          </div>
        </div>

        {/* Schema Fields Table */}
        <div className="space-y-4">
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Schema 이름
            </label>
            <input
              type="text"
              value={currentSchemaName}
              onChange={(e) => setCurrentSchemaName(e.target.value)}
              placeholder="Schema 이름을 입력하세요 (예: UserInfo, ProductData)"
              disabled={isReadOnly}
              className={`w-full px-3 py-2 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] ${
                isReadOnly ? "opacity-60 cursor-not-allowed" : ""
              }`}
            />
          </div>

          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Schema 설명
            </label>
            <textarea
              value={currentSchemaDescription}
              onChange={(e) => setCurrentSchemaDescription(e.target.value)}
              placeholder="Schema에 대한 설명을 입력하세요 (선택사항)"
              rows={3}
              disabled={isReadOnly}
              className={`w-full px-3 py-2 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] resize-none ${
                isReadOnly ? "opacity-60 cursor-not-allowed" : ""
              }`}
            />
          </div>

          {/* Schema Type 선택 */}
          <div className="mb-4 grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Schema Type
              </label>
              <select
                value={schemaType}
                onChange={(e) => setSchemaType(e.target.value as any)}
                disabled={isReadOnly}
                className={`w-full px-3 py-2 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] ${
                  isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                }`}
              >
                <option value="object">Object (객체)</option>
                <option value="array">Array (배열)</option>
                <option value="string">String (문자열)</option>
                <option value="number">Number (숫자)</option>
                <option value="boolean">Boolean (참/거짓)</option>
              </select>
            </div>

            {schemaType === "array" && (
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  Array Item Type
                </label>
                <select
                  value={arrayItemType}
                  onChange={(e) => setArrayItemType(e.target.value as any)}
                  disabled={isReadOnly}
                  className={`w-full px-3 py-2 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] ${
                    isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                  }`}
                >
                  <option value="object">Object</option>
                  <option value="string">String</option>
                  <option value="number">Number</option>
                  <option value="boolean">Boolean</option>
                </select>
              </div>
            )}
          </div>

          {(schemaType === "string" ||
            schemaType === "number" ||
            schemaType === "boolean") && (
            <div className="mb-4">
              <div className="p-4 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-md mb-3">
                <p className="text-sm text-blue-700 dark:text-blue-300">
                  <strong>{schemaType}</strong> 타입은 단일 값을 반환합니다.
                  필드를 추가할 필요가 없습니다.
                </p>
              </div>
              {currentSchemaName && (
                <div className="flex justify-end">
                  <button
                    onClick={saveSchema}
                    disabled={isLoading || isReadOnly}
                    className={`px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-md text-sm font-medium transition-colors disabled:opacity-50 ${
                      isReadOnly ? "opacity-50 cursor-not-allowed" : ""
                    }`}
                  >
                    {isLoading ? "저장 중..." : "Save Schema"}
                  </button>
                </div>
              )}
            </div>
          )}

          {schemaType === "array" && arrayItemType !== "object" && (
            <div className="mb-4">
              <div className="p-4 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-md mb-3">
                <p className="text-sm text-blue-700 dark:text-blue-300">
                  <strong>Array of {arrayItemType}</strong> 타입입니다.
                  필드를 추가할 필요가 없습니다.
                </p>
              </div>
              {currentSchemaName && (
                <div className="flex justify-end">
                  <button
                    onClick={saveSchema}
                    disabled={isLoading || isReadOnly}
                    className={`px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-md text-sm font-medium transition-colors disabled:opacity-50 ${
                      isReadOnly ? "opacity-50 cursor-not-allowed" : ""
                    }`}
                  >
                    {isLoading ? "저장 중..." : "Save Schema"}
                  </button>
                </div>
              )}
            </div>
          )}

          {(schemaType === "object" ||
            (schemaType === "array" && arrayItemType === "object")) && (
            <div className="mb-3 flex items-center justify-between">
              <button
                onClick={() => {
                  setSchemaFields([...schemaFields, createDefaultField()]);
                }}
                disabled={isReadOnly}
                className={`px-3 py-1 text-sm text-[#2563EB] hover:text-[#1E40AF] font-medium ${
                  isReadOnly ? "opacity-50 cursor-not-allowed" : ""
                }`}
              >
                + Add Field
              </button>
              <button
                onClick={saveSchema}
                disabled={isLoading || isReadOnly || !currentSchemaName}
                className={`px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-md text-sm font-medium transition-colors disabled:opacity-50 ${
                  isReadOnly ? "opacity-50 cursor-not-allowed" : ""
                }`}
              >
                {isLoading ? "저장 중..." : "Save Schema"}
              </button>
            </div>
          )}

          {(schemaType === "object" ||
            (schemaType === "array" && arrayItemType === "object")) && (
            <div className="mb-2">
              <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Schema Fields {schemaType === "array" && "(Array Items)"}
              </h4>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                재귀적 스키마 구조 지원 (Object, Array, Reference)
              </p>
            </div>
          )}

          {(schemaType === "object" ||
            (schemaType === "array" && arrayItemType === "object")) && (
            <div className="space-y-2">
              {schemaFields.map((field, index) => (
                <SchemaFieldEditor
                  key={index}
                  field={field}
                  onChange={(newField) => {
                    const updated = [...schemaFields];
                    updated[index] = newField;
                    setSchemaFields(updated);
                  }}
                  onRemove={() => {
                    const updated = schemaFields.filter((_, i) => i !== index);
                    setSchemaFields(updated);
                  }}
                  isReadOnly={isReadOnly}
                  allowFileType={false}
                  allowMockExpression={true}
                />
              ))}
              {schemaFields.length === 0 && (
                <div className="text-center py-8 text-gray-500 dark:text-gray-400 text-sm">
                  <p>No fields yet. Click "+ Add Field" to add one.</p>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Schema Modal (스키마 편집용) */}
      <SchemaModal
        isOpen={isSchemaModalOpen}
        onClose={() => setIsSchemaModalOpen(false)}
        onSelect={(schema) => {
          // SchemaModal에서 재귀적 변환 완료된 필드 사용
          setCurrentSchemaName(schema.name);
          setCurrentSchemaDescription(schema.description || "");
          setSchemaFields(schema.fields);
        }}
        schemas={schemas}
        setSchemas={setSchemas}
      />

    </div>
  );
}

