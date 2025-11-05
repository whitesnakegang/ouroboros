import { useState, useEffect } from "react";
import { SchemaModal } from "./SchemaModal";
import { MockExpressionModal } from "./MockExpressionModal";
import { getAllSchemas, createSchema, updateSchema } from "../services/api";
import type {
  SchemaResponse,
  CreateSchemaRequest,
  UpdateSchemaRequest,
} from "../services/api";

interface SchemaField {
  name: string;
  type: string;
  description?: string;
  mockExpression?: string;
  ref?: string;
}

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

  // Mock Expression Modal 상태
  const [isMockModalOpen, setIsMockModalOpen] = useState(false);
  const [currentMockFieldIndex, setCurrentMockFieldIndex] = useState<
    number | null
  >(null);
  const [currentMockValue, setCurrentMockValue] = useState("");

  // Schema Type 상태
  const [schemaType, setSchemaType] = useState<
    "object" | "array" | "string" | "number" | "boolean"
  >("object");
  const [arrayItemType, setArrayItemType] = useState<
    "object" | "string" | "number" | "boolean"
  >("object");

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

    if (schemaFields.length === 0) {
      alert("최소 하나의 필드를 추가해주세요.");
      return;
    }

    try {
      setIsLoading(true);
      setError(null);

      // 필드들을 백엔드 형식으로 변환
      const properties: Record<string, any> = {};
      const required: string[] = [];

      schemaFields.forEach((field) => {
        if (field.name.trim()) {
          properties[field.name] = {
            type: field.type,
            description: field.description,
            mockExpression: field.mockExpression,
            ref: field.ref,
          };
          // 필수 필드는 현재 모든 필드로 설정 (실제로는 UI에서 선택할 수 있도록 개선 가능)
          required.push(field.name);
        }
      });

      const schemaRequest: CreateSchemaRequest = {
        schemaName: currentSchemaName.trim(),
        type: "object",
        title: `${currentSchemaName} Schema`,
        description:
          currentSchemaDescription.trim() || `${currentSchemaName} 스키마 정의`,
        properties,
        required,
        orders: schemaFields.map((f) => f.name),
      };

      // 기존 스키마가 있는지 확인
      const existingSchema = schemas.find(
        (s) => s.schemaName === currentSchemaName
      );

      if (existingSchema) {
        // 수정
        const updateRequest: UpdateSchemaRequest = {
          type: schemaRequest.type,
          title: schemaRequest.title,
          description: schemaRequest.description,
          properties: schemaRequest.properties,
          required: schemaRequest.required,
          orders: schemaRequest.orders,
        };
        await updateSchema(currentSchemaName, updateRequest);
        alert(`"${currentSchemaName}" 스키마가 수정되었습니다.`);
      } else {
        // 생성
        await createSchema(schemaRequest);
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
      alert(err instanceof Error ? err.message : "스키마 저장에 실패했습니다.");
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
            <div className="mb-4 p-4 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-md">
              <p className="text-sm text-blue-700 dark:text-blue-300">
                <strong>{schemaType}</strong> 타입은 단일 값을 반환합니다.
                필드를 추가할 필요가 없습니다.
              </p>
            </div>
          )}

          {(schemaType === "object" ||
            (schemaType === "array" && arrayItemType === "object")) && (
            <div className="mb-3 flex items-center justify-between">
              <button
                onClick={() => {
                  setSchemaFields([
                    ...schemaFields,
                    {
                      name: "",
                      type: "string",
                      description: "",
                      mockExpression: "",
                    },
                  ]);
                }}
                disabled={isReadOnly}
                className={`px-3 py-1 text-sm text-[#2563EB] hover:text-[#1E40AF] font-medium ${
                  isReadOnly ? "opacity-50 cursor-not-allowed" : ""
                }`}
              >
                + Add Field
              </button>
              {currentSchemaName && schemaFields.length > 0 && (
                <button
                  onClick={saveSchema}
                  disabled={isLoading || isReadOnly}
                  className={`px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-md text-sm font-medium transition-colors disabled:opacity-50 ${
                    isReadOnly ? "opacity-50 cursor-not-allowed" : ""
                  }`}
                >
                  {isLoading ? "저장 중..." : "Save Schema"}
                </button>
              )}
            </div>
          )}

          {(schemaType === "object" ||
            (schemaType === "array" && arrayItemType === "object")) && (
            <div className="mb-2">
              <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Schema Fields {schemaType === "array" && "(Array Items)"}
              </h4>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                각 필드의 이름, 타입, mock 값, 설명을 입력하세요
              </p>
            </div>
          )}

          {(schemaType === "object" ||
            (schemaType === "array" && arrayItemType === "object")) && (
            <div className="overflow-x-auto">
              <table className="w-full text-sm border border-gray-200 dark:border-gray-700 rounded-md">
                <thead>
                  <tr className="bg-gray-50 dark:bg-[#161B22]">
                    <th className="px-4 py-3 text-left font-medium text-gray-700 dark:text-gray-300 border-b border-gray-200 dark:border-gray-700 w-1/4">
                      Field Name <span className="text-red-500">*</span>
                    </th>
                    <th className="px-4 py-3 text-left font-medium text-gray-700 dark:text-gray-300 border-b border-gray-200 dark:border-gray-700 w-1/5">
                      Type <span className="text-red-500">*</span>
                    </th>
                    <th className="px-4 py-3 text-left font-medium text-gray-700 dark:text-gray-300 border-b border-gray-200 dark:border-gray-700 w-1/4">
                      Mock Expression
                    </th>
                    <th className="px-4 py-3 text-left font-medium text-gray-700 dark:text-gray-300 border-b border-gray-200 dark:border-gray-700 w-1/3">
                      Description
                    </th>
                    <th className="px-4 py-3 text-center font-medium text-gray-700 dark:text-gray-300 border-b border-gray-200 dark:border-gray-700 w-16"></th>
                  </tr>
                </thead>
                <tbody>
                  {schemaFields.map((field, index) => (
                    <tr
                      key={index}
                      className="border-b border-gray-100 dark:border-gray-800"
                    >
                      <td className="px-4 py-3">
                        <input
                          type="text"
                          value={field.name}
                          onChange={(e) => {
                            const updated = [...schemaFields];
                            updated[index] = {
                              ...updated[index],
                              name: e.target.value,
                            };
                            setSchemaFields(updated);
                          }}
                          placeholder="예: userId, userName, status"
                          disabled={isReadOnly}
                          className={`w-full px-3 py-2 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] ${
                            isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                          }`}
                        />
                      </td>
                      <td className="px-4 py-3">
                        <select
                          value={field.type}
                          onChange={(e) => {
                            const updated = [...schemaFields];
                            updated[index] = {
                              ...updated[index],
                              type: e.target.value,
                            };
                            setSchemaFields(updated);
                          }}
                          disabled={isReadOnly}
                          className={`w-full px-3 py-2 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] ${
                            isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                          }`}
                        >
                          <option value="string">string</option>
                          <option value="integer">integer</option>
                          <option value="number">number</option>
                          <option value="boolean">boolean</option>
                          <option value="object">object</option>
                          <option value="array">array</option>
                        </select>
                      </td>
                      <td className="px-4 py-3">
                        <button
                          type="button"
                          onClick={() => {
                            setCurrentMockFieldIndex(index);
                            setCurrentMockValue(field.mockExpression || "");
                            setIsMockModalOpen(true);
                          }}
                          disabled={isReadOnly}
                          className={`w-full px-3 py-2 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-left hover:bg-gray-50 dark:hover:bg-[#161B22] transition-colors group ${
                            isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                          }`}
                        >
                          {field.mockExpression ? (
                            <span className="font-mono text-sm text-green-600 dark:text-green-400">
                              {field.mockExpression}
                            </span>
                          ) : (
                            <span className="text-gray-400 dark:text-[#8B949E] text-sm">
                              클릭하여 Mock 표현식 선택
                            </span>
                          )}
                        </button>
                      </td>
                      <td className="px-4 py-3">
                        <input
                          type="text"
                          value={field.description}
                          onChange={(e) => {
                            const updated = [...schemaFields];
                            updated[index] = {
                              ...updated[index],
                              description: e.target.value,
                            };
                            setSchemaFields(updated);
                          }}
                          placeholder="필드에 대한 설명을 입력하세요"
                          disabled={isReadOnly}
                          className={`w-full px-3 py-2 border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] ${
                            isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                          }`}
                        />
                      </td>
                      <td className="px-4 py-3 text-center">
                        <button
                          onClick={() => {
                            const updated = schemaFields.filter(
                              (_, i) => i !== index
                            );
                            setSchemaFields(updated);
                          }}
                          disabled={isReadOnly}
                          className={`text-red-500 hover:text-red-600 ${
                            isReadOnly ? "opacity-50 cursor-not-allowed" : ""
                          }`}
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
                              d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                            />
                          </svg>
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Schema Modal (스키마 편집용) */}
      <SchemaModal
        isOpen={isSchemaModalOpen}
        onClose={() => setIsSchemaModalOpen(false)}
        onSelect={(schema) => {
          setCurrentSchemaName(schema.name);
          setCurrentSchemaDescription(schema.description || "");
          setSchemaFields(schema.fields);
        }}
        schemas={schemas}
        setSchemas={setSchemas}
      />

      {/* Mock Expression Modal */}
      <MockExpressionModal
        isOpen={isMockModalOpen}
        onClose={() => {
          setIsMockModalOpen(false);
          setCurrentMockFieldIndex(null);
          setCurrentMockValue("");
        }}
        onSelect={(expression) => {
          if (currentMockFieldIndex !== null) {
            const updated = [...schemaFields];
            updated[currentMockFieldIndex] = {
              ...updated[currentMockFieldIndex],
              mockExpression: expression,
            };
            setSchemaFields(updated);
          }
          setIsMockModalOpen(false);
          setCurrentMockFieldIndex(null);
          setCurrentMockValue("");
        }}
        initialValue={currentMockValue}
      />
    </div>
  );
}

