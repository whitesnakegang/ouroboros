import { useState, useEffect } from "react";
import { SchemaModal } from "./SchemaModal";
import { getAllSchemas, createSchema, updateSchema } from "../services/api";
import type {
  SchemaResponse,
  CreateSchemaRequest,
  UpdateSchemaRequest,
} from "../services/api";

interface StatusCode {
  code: string;
  type: "Success" | "Error";
  message: string;
}

interface SchemaField {
  name: string;
  type: string;
  description?: string;
  mockExpression?: string;
  ref?: string;
}

interface ApiResponseCardProps {
  statusCodes: StatusCode[];
  setStatusCodes: (codes: StatusCode[]) => void;
  isReadOnly?: boolean;
}

export function ApiResponseCard({
  statusCodes,
  setStatusCodes,
  isReadOnly = false,
}: ApiResponseCardProps) {
  const statusCodeTemplates: StatusCode[] = [
    { code: "200", type: "Success", message: "요청이 성공적으로 처리됨" },
    { code: "201", type: "Success", message: "리소스가 성공적으로 생성됨" },
    { code: "204", type: "Success", message: "요청 성공 (응답 본문 없음)" },
    { code: "400", type: "Error", message: "잘못된 요청 형식" },
    { code: "401", type: "Error", message: "인증 실패 (유효하지 않은 토큰)" },
    { code: "403", type: "Error", message: "접근 권한이 없음" },
    { code: "404", type: "Error", message: "리소스를 찾을 수 없음" },
    { code: "409", type: "Error", message: "리소스 충돌" },
    { code: "422", type: "Error", message: "유효성 검사 실패" },
    { code: "500", type: "Error", message: "서버 내부 오류" },
    { code: "502", type: "Error", message: "게이트웨이 오류" },
    { code: "503", type: "Error", message: "서비스 사용 불가" },
  ];

  const addStatusCode = (template?: StatusCode) => {
    if (isReadOnly) return;
    if (template) {
      setStatusCodes([...statusCodes, { ...template }]);
    } else {
      setStatusCodes([
        ...statusCodes,
        { code: "", type: "Success", message: "" },
      ]);
    }
  };

  const removeStatusCode = (index: number) => {
    if (isReadOnly) return;
    setStatusCodes(statusCodes.filter((_, i) => i !== index));
  };

  const updateStatusCode = (
    index: number,
    field: "code" | "type" | "message",
    value: string
  ) => {
    if (isReadOnly) return;
    const updated = [...statusCodes];
    updated[index] = { ...updated[index], [field]: value };
    setStatusCodes(updated);
  };

  const [activeTab, setActiveTab] = useState("status");
  const [schemas, setSchemas] = useState<SchemaResponse[]>([]);
  const [isSchemaModalOpen, setIsSchemaModalOpen] = useState(false);
  const [schemaFields, setSchemaFields] = useState<SchemaField[]>([]);
  const [currentSchemaName, setCurrentSchemaName] = useState("");
  const [currentSchemaDescription, setCurrentSchemaDescription] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

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
    <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-2xl p-6 shadow-sm">
      {/* Header */}
      <div className="flex items-center gap-3 mb-4">
        <div className="w-10 h-10 rounded-lg bg-purple-100 dark:bg-purple-900 flex items-center justify-center">
          <svg
            className="w-6 h-6 text-purple-600 dark:text-purple-400"
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
        </div>
        <div>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
            Response
          </h3>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            응답 바디, 헤더 및 상태 코드 설정
          </p>
        </div>
      </div>

      {/* Tabs - Only Status Codes and Schema */}
      <div className="flex gap-1 border-b border-gray-200 dark:border-gray-700 mb-4">
        {["status", "schema"].map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`px-4 py-2 text-sm font-medium transition-colors capitalize ${
              activeTab === tab
                ? "text-purple-600 border-b-2 border-purple-600 dark:text-purple-400"
                : "text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300"
            }`}
          >
            {tab === "status" && "Status Codes"}
            {tab === "schema" && "Schema"}
          </button>
        ))}
      </div>

      {/* Tab Content */}
      <div className="space-y-4">
        {activeTab === "status" && (
          <div>
            <div className="flex items-center justify-between mb-4">
              <p className="text-sm text-gray-600 dark:text-gray-400">
                HTTP 상태 코드 관리
              </p>
              <div className="flex gap-2">
                <select
                  onChange={(e) => {
                    const selectedCode = e.target.value;
                    if (selectedCode) {
                      const template = statusCodeTemplates.find(
                        (t) => t.code === selectedCode
                      );
                      if (template) {
                        addStatusCode(template);
                        e.target.value = "";
                      }
                    }
                  }}
                  className="px-3 py-1 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-purple-500"
                >
                  <option value="">템플릿 선택...</option>
                  <optgroup label="Success">
                    {statusCodeTemplates
                      .filter((t) => t.type === "Success")
                      .map((template) => (
                        <option key={template.code} value={template.code}>
                          {template.code} - {template.message}
                        </option>
                      ))}
                  </optgroup>
                  <optgroup label="Error">
                    {statusCodeTemplates
                      .filter((t) => t.type === "Error")
                      .map((template) => (
                        <option key={template.code} value={template.code}>
                          {template.code} - {template.message}
                        </option>
                      ))}
                  </optgroup>
                </select>
                <button
                  onClick={() => addStatusCode()}
                  className="px-3 py-1 text-sm text-purple-600 hover:text-purple-700 dark:text-purple-400 dark:hover:text-purple-300 font-medium border border-purple-600 dark:border-purple-400 rounded-lg hover:bg-purple-50 dark:hover:bg-purple-900"
                >
                  + Add Custom
                </button>
              </div>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-sm text-left">
                <thead>
                  <tr className="border-b border-gray-200 dark:border-gray-700">
                    <th className="px-4 py-3 font-medium text-gray-700 dark:text-gray-300">
                      Status Code
                    </th>
                    <th className="px-4 py-3 font-medium text-gray-700 dark:text-gray-300">
                      Type
                    </th>
                    <th className="px-4 py-3 font-medium text-gray-700 dark:text-gray-300">
                      Message / Description
                    </th>
                    <th className="px-4 py-3"></th>
                  </tr>
                </thead>
                <tbody>
                  {statusCodes.map((statusCode, index) => (
                    <tr
                      key={index}
                      className="border-b border-gray-100 dark:border-gray-800 hover:bg-gray-50 dark:hover:bg-gray-750"
                    >
                      <td className="px-4 py-3">
                        <input
                          type="text"
                          value={statusCode.code}
                          onChange={(e) =>
                            updateStatusCode(index, "code", e.target.value)
                          }
                          placeholder="200"
                          className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-purple-500"
                        />
                      </td>
                      <td className="px-4 py-3">
                        <select
                          value={statusCode.type}
                          onChange={(e) =>
                            updateStatusCode(index, "type", e.target.value)
                          }
                          className="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-purple-500"
                        >
                          <option value="Success">Success</option>
                          <option value="Error">Error</option>
                        </select>
                      </td>
                      <td className="px-4 py-3">
                        <input
                          type="text"
                          value={statusCode.message}
                          onChange={(e) =>
                            updateStatusCode(index, "message", e.target.value)
                          }
                          placeholder="메시지 또는 설명"
                          className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-purple-500"
                        />
                      </td>
                      <td className="px-4 py-3">
                        <button
                          onClick={() => removeStatusCode(index)}
                          className="p-2 text-red-500 hover:text-red-600"
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
          </div>
        )}

        {activeTab === "schema" && (
          <div>
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
                  className="px-3 py-1 text-sm text-gray-600 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300 font-medium border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50"
                >
                  {isLoading ? "로딩..." : "새로고침"}
                </button>
                <button
                  onClick={() => setIsSchemaModalOpen(true)}
                  className="px-4 py-2 bg-purple-600 hover:bg-purple-700 dark:bg-purple-700 dark:hover:bg-purple-600 text-white rounded-lg text-sm font-medium transition-colors"
                >
                  📦 Schema 관리
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
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-purple-500"
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
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-purple-500 resize-none"
                />
              </div>

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
                  className="px-3 py-1 text-sm text-purple-600 hover:text-purple-700 dark:text-purple-400 dark:hover:text-purple-300 font-medium"
                >
                  + Add Field
                </button>
                {currentSchemaName && schemaFields.length > 0 && (
                  <button
                    onClick={saveSchema}
                    disabled={isLoading}
                    className="px-4 py-2 bg-green-600 hover:bg-green-700 dark:bg-green-700 dark:hover:bg-green-600 text-white rounded-lg text-sm font-medium transition-colors disabled:opacity-50"
                  >
                    {isLoading ? "저장 중..." : "💾 Save Schema"}
                  </button>
                )}
              </div>

              <div className="mb-2">
                <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  Schema Fields
                </h4>
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  각 필드의 이름, 타입, mock 값, 설명을 입력하세요
                </p>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-sm border border-gray-200 dark:border-gray-700 rounded-lg">
                  <thead>
                    <tr className="bg-gray-50 dark:bg-gray-750">
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
                            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-purple-500"
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
                            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-purple-500"
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
                          <input
                            type="text"
                            value={field.mockExpression || ""}
                            onChange={(e) => {
                              const updated = [...schemaFields];
                              updated[index] = {
                                ...updated[index],
                                mockExpression: e.target.value,
                              };
                              setSchemaFields(updated);
                            }}
                            placeholder="예: {{$random.uuid}}, {{$name.fullName}}"
                            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-purple-500"
                          />
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
                            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-purple-500"
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
                            className="text-red-500 hover:text-red-600"
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
            </div>
          </div>
        )}
      </div>

      {/* Schema Modal */}
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
    </div>
  );
}
