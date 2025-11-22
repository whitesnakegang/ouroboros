/* eslint-disable @typescript-eslint/no-explicit-any */
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { deleteSchema, deleteWebSocketSchema } from "../services/api";
import type { SchemaResponse } from "../services/api";
import type { SchemaField } from "../types/schema.types";
import { parseOpenAPISchemaToSchemaField } from "../utils/schemaConverter";
import { ConfirmModal } from "@/ui/ConfirmModal";
import { AlertModal } from "@/ui/AlertModal";

interface Schema {
  id: string;
  name: string;
  description?: string;
  type: string;
  fields: SchemaField[];
  items?: any; // array 타입일 경우
}

interface SchemaModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSelect: (schema: Schema) => void;
  schemas: SchemaResponse[];
  setSchemas: (schemas: SchemaResponse[]) => void;
  protocol?: "REST" | "WebSocket";
}

export function SchemaModal({
  isOpen,
  onClose,
  onSelect,
  schemas,
  setSchemas,
  protocol = "REST",
}: SchemaModalProps) {
  const { t } = useTranslation();
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

  if (!isOpen) return null;

  // Schema 이름에서 마지막 부분만 추출 (예: com.example.dto.UserDTO -> UserDTO)
  const getShortSchemaName = (fullName: string): string => {
    const parts = fullName.split(".");
    return parts[parts.length - 1];
  };

  const handleDeleteSchema = (schemaName: string) => {
    setConfirmModal({
      isOpen: true,
      title: t("schema.deleteSchema"),
      message: t("schema.confirmDeleteSchema", { name: schemaName }),
      variant: "danger",
      onConfirm: async () => {
        setConfirmModal((prev) => ({ ...prev, isOpen: false }));
        try {
          if (protocol === "WebSocket") {
            await deleteWebSocketSchema(schemaName);
          } else {
            await deleteSchema(schemaName);
          }
          setSchemas(schemas.filter((s) => s.schemaName !== schemaName));
          setAlertModal({
            isOpen: true,
            title: t("common.delete"),
            message: t("schema.schemaDeletedSuccessfully", {
              name: schemaName,
            }),
            variant: "success",
          });
        } catch (err) {
          console.error("스키마 삭제 실패:", err);
          setAlertModal({
            isOpen: true,
            title: t("modal.saveFailed"),
            message: `${t("schema.deleteSchema")}: ${
              err instanceof Error ? err.message : t("common.error")
            }`,
            variant: "error",
          });
        }
      },
    });
  };

  const handleSelectSchema = (schema: SchemaResponse) => {
    // SchemaResponse를 Schema 형식으로 변환 (재귀적 구조 지원)
    const convertedSchema: Schema = {
      id: schema.schemaName,
      name: schema.schemaName,
      description: schema.description,
      type: schema.type,
      fields: schema.properties
        ? Object.entries(schema.properties).map(([key, propSchema]) => {
            const field = parseOpenAPISchemaToSchemaField(key, propSchema);
            // required 정보 추가
            if (schema.required && schema.required.includes(key)) {
              field.required = true;
            }
            return field;
          })
        : [],
      items: schema.items, // array 타입일 경우
    };
    onSelect(convertedSchema);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
      <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-xl w-full max-w-3xl max-h-[80vh] overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200 dark:border-gray-700">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
            {t("apiCard.schemaList")}
          </h2>
          <div className="flex gap-2">
            <button
              onClick={onClose}
              className="p-2 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
            >
              <svg
                className="w-6 h-6"
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
          </div>
        </div>

        {/* Content */}
        <div className="overflow-y-auto max-h-[calc(80vh-120px)]">
          {/* Schema List */}
          <div className="p-6">
            {schemas.length === 0 ? (
              <div className="text-center py-12 text-gray-500 dark:text-gray-400">
                <p>{t("schema.noSchemasSaved")}</p>
                <p className="text-sm mt-2">
                  {t("schema.createSchemaInResponseTab")}
                </p>
              </div>
            ) : (
              <div className="space-y-3">
                {schemas.map((schema) => (
                  <div
                    key={schema.schemaName}
                    className="border border-gray-200 dark:border-gray-700 rounded-lg p-4 hover:border-purple-500 dark:hover:border-purple-400 transition-colors"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex-1 min-w-0">
                        <h3
                          className="font-medium text-gray-900 dark:text-white truncate"
                          title={schema.schemaName}
                        >
                          {getShortSchemaName(schema.schemaName)}
                        </h3>
                        <div className="flex items-center gap-2 mt-1">
                          <p className="text-sm text-gray-500 dark:text-gray-400">
                            {schema.properties
                              ? Object.keys(schema.properties).length
                              : 0}{" "}
                            {t("apiCard.fields")}
                          </p>
                          {schema.schemaName.includes(".") && (
                            <span
                              className="text-xs text-gray-400 dark:text-gray-500 font-mono truncate"
                              title={schema.schemaName}
                            >
                              (
                              {schema.schemaName
                                .split(".")
                                .slice(0, -1)
                                .join(".")}
                              )
                            </span>
                          )}
                        </div>
                        {schema.description && (
                          <p className="text-xs text-gray-400 dark:text-gray-500 mt-1 truncate">
                            {schema.description}
                          </p>
                        )}
                      </div>
                      <div className="flex gap-2">
                        <button
                          onClick={() => handleSelectSchema(schema)}
                          className="px-3 py-1 text-sm text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300 font-medium"
                        >
                          {t("apiCard.select")}
                        </button>
                        <button
                          onClick={() => handleDeleteSchema(schema.schemaName)}
                          className="px-3 py-1 text-sm text-red-600 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300 font-medium"
                        >
                          {t("apiCard.delete")}
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

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
