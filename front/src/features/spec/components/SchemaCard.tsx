import { useState, useEffect } from "react";
import { SchemaModal } from "./SchemaModal";
import { SchemaFieldEditor } from "./SchemaFieldEditor";
import { ConfirmModal } from "@/ui/ConfirmModal";
import { AlertModal } from "@/ui/AlertModal";
import {
  getAllSchemas,
  createSchema,
  updateSchema,
  deleteSchema,
  getAllWebSocketSchemas,
  createWebSocketSchema,
  updateWebSocketSchema,
  deleteWebSocketSchema,
} from "../services/api";
import type {
  SchemaResponse,
  CreateSchemaRequest,
  UpdateSchemaRequest,
} from "../services/api";
import type { SchemaField } from "../types/schema.types";
import { createDefaultField } from "../types/schema.types";
import {
  convertSchemaFieldToOpenAPI,
  parseOpenAPISchemaToSchemaField,
} from "../utils/schemaConverter";

interface SchemaCardProps {
  isReadOnly?: boolean;
  protocol?: "REST" | "WebSocket";
  isDocumentView?: boolean;
}

// Schema 이름에서 마지막 부분만 추출 (예: com.example.dto.UserDTO -> UserDTO)
const getShortSchemaName = (fullName: string): string => {
  const parts = fullName.split(".");
  return parts[parts.length - 1];
};

export function SchemaCard({
  isReadOnly = false,
  protocol = "REST",
  isDocumentView = false,
}: SchemaCardProps) {
  const [schemas, setSchemas] = useState<SchemaResponse[]>([]);
  const [isSchemaModalOpen, setIsSchemaModalOpen] = useState(false);
  const [selectedSchemaName, setSelectedSchemaName] = useState<string | null>(
    null
  );
  const [schemaFields, setSchemaFields] = useState<SchemaField[]>([]);
  const [currentSchemaName, setCurrentSchemaName] = useState("");
  const [currentSchemaDescription, setCurrentSchemaDescription] = useState("");
  const [originalSchemaName, setOriginalSchemaName] = useState<string | null>(
    null
  );
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Modal 상태
  const [confirmModal, setConfirmModal] = useState<{
    isOpen: boolean;
    title: string;
    message: string;
    onConfirm: () => void;
    variant?: "danger" | "warning" | "info";
  }>({
    isOpen: false,
    title: "",
    message: "",
    onConfirm: () => {},
  });

  const [alertModal, setAlertModal] = useState<{
    isOpen: boolean;
    title: string;
    message: string;
    variant?: "success" | "error" | "warning" | "info";
  }>({
    isOpen: false,
    title: "",
    message: "",
  });

  // Schema Type 상태 (object만 허용)
  const schemaType = "object" as const;

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

  // 컴포넌트 마운트 시 및 프로토콜 변경 시 스키마 목록 로드
  useEffect(() => {
    loadSchemas();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [protocol]);

  // 스키마 목록 로드
  const loadSchemas = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const response =
        protocol === "WebSocket"
          ? await getAllWebSocketSchemas()
          : await getAllSchemas();
      setSchemas(response.data);
    } catch (err) {
      console.error("스키마 로드 실패:", err);
      setError(err instanceof Error ? err.message : "Failed to load schema.");
    } finally {
      setIsLoading(false);
    }
  };

  // 스키마 선택 핸들러
  const handleSelectSchema = (schema: SchemaResponse) => {
    setSelectedSchemaName(schema.schemaName);
    setCurrentSchemaName(schema.schemaName);
    setOriginalSchemaName(schema.schemaName);
    setCurrentSchemaDescription(schema.description || "");

    // SchemaResponse를 SchemaField 배열로 변환
    if (schema.properties) {
      const fields = Object.entries(schema.properties).map(
        ([key, propSchema]) => {
          const field = parseOpenAPISchemaToSchemaField(key, propSchema);
          if (schema.required && schema.required.includes(key)) {
            field.required = true;
          }
          return field;
        }
      );
      setSchemaFields(fields);
    } else {
      setSchemaFields([]);
    }
  };

  // 새 스키마 생성 모드로 전환
  const handleNewSchema = () => {
    setSelectedSchemaName(null);
    setCurrentSchemaName("");
    setOriginalSchemaName(null);
    setCurrentSchemaDescription("");
    setSchemaFields([]);
  };

  // 스키마 삭제 핸들러
  const handleDeleteSchema = (schemaName: string) => {
    setConfirmModal({
      isOpen: true,
      title: "Delete Schema",
      message: `Are you sure you want to delete the schema "${schemaName}"?`,
      variant: "danger",
      onConfirm: async () => {
        setConfirmModal((prev) => ({ ...prev, isOpen: false }));
        try {
          if (protocol === "WebSocket") {
            await deleteWebSocketSchema(schemaName);
          } else {
            await deleteSchema(schemaName);
          }
          // 함수형 업데이터를 사용하여 최신 schemas 상태 참조
          setSchemas((prev) => prev.filter((s) => s.schemaName !== schemaName));

          // 함수형 업데이터를 사용하여 최신 selectedSchemaName 상태 참조
          setSelectedSchemaName((prev) => {
            if (prev === schemaName) {
              handleNewSchema();
              return null;
            }
            return prev;
          });

          setAlertModal({
            isOpen: true,
            title: "Deleted",
            message: `Schema "${schemaName}" has been deleted successfully.`,
            variant: "success",
          });
        } catch (err) {
          console.error("스키마 삭제 실패:", err);
          setAlertModal({
            isOpen: true,
            title: "Delete Failed",
            message: `Failed to delete schema: ${
              err instanceof Error ? err.message : "Unknown error"
            }`,
            variant: "error",
          });
        }
      },
    });
  };

  // 스키마 저장 (생성 또는 수정)
  const saveSchema = async () => {
    if (!currentSchemaName.trim()) {
      setAlertModal({
        isOpen: true,
        title: "Input Error",
        message: "Please enter a schema name.",
        variant: "warning",
      });
      return;
    }

    // object 타입은 필드 검증
    if (schemaFields.length === 0) {
      setAlertModal({
        isOpen: true,
        title: "Input Error",
        message: "Please add at least one field.",
        variant: "warning",
      });
      return;
    }

    try {
      setIsLoading(true);
      setError(null);

      // Object 타입만 처리 (재귀 지원)
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
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

      const schemaRequest: CreateSchemaRequest = {
        schemaName: currentSchemaName.trim(),
        type: "object",
        title: `${currentSchemaName} Schema`,
        description:
          currentSchemaDescription.trim() ||
          `${currentSchemaName} schema definition`,
        properties,
        required: required.length > 0 ? required : undefined,
        orders: schemaFields.map((f) => f.key),
      };

      // 기존 스키마가 있는지 확인 (원래 이름 또는 현재 이름으로 확인)
      const schemaNameToCheck = originalSchemaName || currentSchemaName;
      const existingSchema = schemas.find(
        (s) => s.schemaName === schemaNameToCheck
      );

      if (existingSchema && originalSchemaName) {
        // 수정 모드: 원래 이름으로 업데이트 (originalSchemaName 사용)
        const updateRequest: UpdateSchemaRequest = {
          type: schemaRequest.type,
          title: schemaRequest.title,
          description: schemaRequest.description,
          properties: schemaRequest.properties,
          required: schemaRequest.required,
          orders: schemaRequest.orders,
        };
        if (protocol === "WebSocket") {
          await updateWebSocketSchema(originalSchemaName, updateRequest);
        } else {
          await updateSchema(originalSchemaName, updateRequest);
        }
        setAlertModal({
          isOpen: true,
          title: "Updated",
          message: `Schema "${originalSchemaName}" has been updated successfully.`,
          variant: "success",
        });
      } else {
        // 생성 모드
        if (protocol === "WebSocket") {
          await createWebSocketSchema(schemaRequest);
        } else {
          await createSchema(schemaRequest);
        }
        setAlertModal({
          isOpen: true,
          title: "Created",
          message: `Schema "${currentSchemaName}" has been created successfully.`,
          variant: "success",
        });
      }

      // 스키마 목록 다시 로드
      await loadSchemas();

      // 선택된 스키마 업데이트 (수정 모드인 경우 originalSchemaName 유지)
      if (originalSchemaName) {
        setSelectedSchemaName(originalSchemaName);
        // 수정 후에도 편집 모드 유지
      } else {
        setSelectedSchemaName(currentSchemaName.trim());
        setOriginalSchemaName(currentSchemaName.trim());
      }
    } catch (err) {
      console.error("스키마 저장 실패:", err);
      const errorMessage = getErrorMessage(err);
      setAlertModal({
        isOpen: true,
        title: "Save Failed",
        message: `Failed to save schema: ${errorMessage}`,
        variant: "error",
      });
    } finally {
      setIsLoading(false);
    }
  };

  // 문서 형식 뷰
  if (isDocumentView) {
    return (
      <div className="space-y-4">
        {schemas.length > 0 ? (
          schemas.map((schema) => (
            <div
              key={schema.schemaName}
              className="border-b border-gray-200 dark:border-[#2D333B] pb-4 last:border-b-0 last:pb-0"
            >
              <div className="flex items-start gap-3">
                <h3 className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3] min-w-[150px]">
                  {schema.schemaName}
                </h3>
                {schema.description && (
                  <p className="text-sm text-gray-600 dark:text-[#8B949E] flex-1">
                    {schema.description}
                  </p>
                )}
              </div>
              {schema.properties &&
                Object.keys(schema.properties).length > 0 && (
                  <div className="mt-2 ml-5 space-y-1">
                    <div className="text-xs font-semibold text-gray-700 dark:text-[#C9D1D9]">
                      Properties:
                    </div>
                    {Object.entries(schema.properties).map(
                      ([key, prop]: [string, unknown]) => {
                        const propObj = prop as { type?: string };
                        return (
                          <div
                            key={key}
                            className="text-xs text-gray-600 dark:text-[#8B949E] ml-2"
                          >
                            • {key}: {propObj.type || "object"}
                            {schema.required &&
                              schema.required.includes(key) && (
                                <span className="text-red-600 dark:text-red-400 ml-1">
                                  *
                                </span>
                              )}
                          </div>
                        );
                      }
                    )}
                  </div>
                )}
            </div>
          ))
        ) : (
          <div className="text-sm text-gray-500 dark:text-[#8B949E] italic">
            No schemas available.
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="flex gap-4 h-[600px]">
      {/* 사이드바: 스키마 목록 */}
      <div className="w-64 border-r border-gray-200 dark:border-[#2D333B] flex flex-col">
        <div className="p-4 border-b border-gray-200 dark:border-[#2D333B]">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3]">
              Schema List
            </h3>
            <button
              onClick={loadSchemas}
              disabled={isLoading}
              className="px-2 py-1 text-xs text-gray-600 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300 font-medium border border-gray-300 dark:border-[#2D333B] rounded-md hover:bg-gray-50 dark:hover:bg-[#161B22] disabled:opacity-50"
              title="Refresh"
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
                  d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
                />
              </svg>
            </button>
          </div>
          <button
            onClick={handleNewSchema}
            disabled={isReadOnly}
            className={`w-full px-2 py-1.5 bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded-md text-xs font-medium transition-colors flex items-center justify-center gap-1.5 ${
              isReadOnly ? "opacity-50 cursor-not-allowed" : ""
            }`}
          >
            <svg
              className="w-3.5 h-3.5"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 4v16m8-8H4"
              />
            </svg>
            New Schema
          </button>
        </div>

        {/* 스키마 목록 */}
        <div className="flex-1 overflow-y-auto p-2">
          {isLoading && schemas.length === 0 ? (
            <div className="text-center py-8 text-gray-500 dark:text-gray-400 text-sm">
              Loading...
            </div>
          ) : schemas.length === 0 ? (
            <div className="text-center py-8 text-gray-500 dark:text-gray-400 text-sm">
              <p>No schemas saved.</p>
            </div>
          ) : (
            <div className="space-y-2">
              {schemas.map((schema) => (
                <div
                  key={schema.schemaName}
                  onClick={() => handleSelectSchema(schema)}
                  className={`p-3 rounded-md border cursor-pointer transition-colors ${
                    selectedSchemaName === schema.schemaName
                      ? "border-[#2563EB] bg-blue-50 dark:bg-blue-900/20"
                      : "border-gray-200 dark:border-[#2D333B] hover:border-gray-300 dark:hover:border-[#404850] hover:bg-gray-50 dark:hover:bg-[#161B22]"
                  }`}
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex-1 min-w-0">
                      <h4
                        className="font-medium text-sm text-gray-900 dark:text-[#E6EDF3] truncate"
                        title={schema.schemaName}
                      >
                        {getShortSchemaName(schema.schemaName)}
                      </h4>
                      <div className="flex items-center gap-2 mt-1">
                        <span className="text-xs text-gray-500 dark:text-[#8B949E]">
                          {schema.properties
                            ? Object.keys(schema.properties).length
                            : 0}
                          fields
                        </span>
                      </div>
                      {schema.description && (
                        <p className="text-xs text-gray-400 dark:text-[#6E7681] mt-1 line-clamp-2">
                          {schema.description}
                        </p>
                      )}
                    </div>
                    {!isReadOnly && (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDeleteSchema(schema.schemaName);
                        }}
                        className="p-1 text-red-500 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300 flex-shrink-0"
                        title="Delete"
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
                            d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                          />
                        </svg>
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* 본문: 스키마 편집 폼 */}
      <div className="flex-1 overflow-y-auto">
        <div className="p-4 flex flex-col h-full">
          {error && (
            <div className="p-2 mb-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-md">
              <p className="text-xs text-red-600 dark:text-red-400">{error}</p>
            </div>
          )}

          {/* Schema 이름/설명 입력란 (작게) */}
          <div className="space-y-2 mb-4 flex-shrink-0">
            <div>
              <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">
                Schema Name
              </label>
              <input
                type="text"
                value={currentSchemaName}
                onChange={(e) => {
                  const newName = e.target.value;
                  setCurrentSchemaName(newName);
                  // 이름이 비어있으면 편집 모드 해제 (새 스키마 생성 모드)
                  if (!newName.trim() && originalSchemaName !== null) {
                    setOriginalSchemaName(null);
                    setSchemaFields([]);
                    setCurrentSchemaDescription("");
                  }
                }}
                placeholder="Schema Name"
                disabled={isReadOnly || originalSchemaName !== null}
                className={`w-full px-2 py-1.5 text-sm border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-gray-400 dark:focus:ring-gray-500 focus:border-gray-400 dark:focus:border-gray-500 ${
                  isReadOnly || originalSchemaName !== null
                    ? "opacity-60 cursor-not-allowed"
                    : ""
                }`}
              />
              {originalSchemaName !== null && (
                <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
                  editing mode: "{originalSchemaName}" schema is being edited.
                  The name cannot be changed.
                </p>
              )}
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">
                Schema Description
              </label>
              <textarea
                value={currentSchemaDescription}
                onChange={(e) => setCurrentSchemaDescription(e.target.value)}
                placeholder="Description (optional)"
                rows={2}
                disabled={isReadOnly}
                className={`w-full px-2 py-1.5 text-sm border border-gray-300 dark:border-[#2D333B] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-gray-400 dark:focus:ring-gray-500 focus:border-gray-400 dark:focus:border-gray-500 resize-none ${
                  isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                }`}
              />
            </div>
          </div>

          {/* Schema Fields 영역 (강조) */}
          {schemaType === "object" && (
            <div className="flex-1 flex flex-col min-h-0 border-t border-gray-200 dark:border-[#2D333B] pt-4">
              <div className="flex items-center justify-between mb-3 flex-shrink-0">
                <div>
                  <h4 className="text-base font-semibold text-gray-900 dark:text-[#E6EDF3] mb-1">
                    Schema Fields
                    {originalSchemaName && (
                      <span className="ml-2 text-xs font-normal text-gray-500 dark:text-gray-400">
                        (editing mode)
                      </span>
                    )}
                  </h4>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => {
                      setSchemaFields([...schemaFields, createDefaultField()]);
                    }}
                    disabled={isReadOnly}
                    className={`px-3 py-1.5 text-sm text-[#2563EB] hover:text-[#1E40AF] font-medium border border-[#2563EB] hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-md transition-all active:translate-y-[1px] focus:outline-none focus-visible:outline-none ${
                      isReadOnly ? "opacity-50 cursor-not-allowed" : ""
                    }`}
                  >
                    + Add Field
                  </button>
                  <button
                    onClick={saveSchema}
                    disabled={isLoading || isReadOnly || !currentSchemaName}
                    className={`px-4 py-1.5 ${
                      originalSchemaName
                        ? "bg-blue-500 hover:bg-blue-600"
                        : "bg-emerald-500 hover:bg-emerald-600"
                    } text-white rounded-md text-sm font-medium transition-all active:translate-y-[1px] focus:outline-none focus-visible:outline-none disabled:opacity-50 ${
                      isReadOnly ? "opacity-50 cursor-not-allowed" : ""
                    }`}
                  >
                    {isLoading
                      ? "Saving..."
                      : originalSchemaName
                      ? "Save Changes"
                      : "Create Schema"}
                  </button>
                </div>
              </div>
              <div className="flex-1 overflow-y-auto space-y-2 pr-2">
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
                      const updated = schemaFields.filter(
                        (_, i) => i !== index
                      );
                      setSchemaFields(updated);
                    }}
                    isReadOnly={isReadOnly}
                    allowFileType={false}
                    allowMockExpression={protocol !== "WebSocket"}
                  />
                ))}
                {schemaFields.length === 0 && (
                  <div className="text-center py-12 text-gray-500 dark:text-gray-400 text-sm border-2 border-dashed border-gray-300 dark:border-[#2D333B] rounded-md bg-gray-50 dark:bg-[#0D1117]">
                    <p>No fields yet. Click "+ Add Field" to add one.</p>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Schema Modal (스키마 관리용 - 삭제 기능 포함) */}
      <SchemaModal
        isOpen={isSchemaModalOpen}
        onClose={() => setIsSchemaModalOpen(false)}
        onSelect={(schema) => {
          // SchemaModal에서 재귀적 변환 완료된 필드 사용 (object 타입만)
          if (schema.type === "object") {
            handleSelectSchema(
              schemas.find((s) => s.schemaName === schema.name)!
            );
            setIsSchemaModalOpen(false);
          } else {
            setAlertModal({
              isOpen: true,
              title: "Type Error",
              message: "Only object type schemas are supported.",
              variant: "warning",
            });
          }
        }}
        schemas={schemas}
        setSchemas={setSchemas}
        protocol={protocol}
      />

      {/* Confirm Modal */}
      <ConfirmModal
        isOpen={confirmModal.isOpen}
        onClose={() => setConfirmModal((prev) => ({ ...prev, isOpen: false }))}
        onConfirm={confirmModal.onConfirm}
        title={confirmModal.title}
        message={confirmModal.message}
        variant={confirmModal.variant}
      />

      {/* Alert Modal */}
      <AlertModal
        isOpen={alertModal.isOpen}
        onClose={() => setAlertModal((prev) => ({ ...prev, isOpen: false }))}
        title={alertModal.title}
        message={alertModal.message}
        variant={alertModal.variant}
      />
    </div>
  );
}
