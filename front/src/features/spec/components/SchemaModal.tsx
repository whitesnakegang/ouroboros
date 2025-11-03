import type { SchemaResponse } from "../services/api";

interface SchemaField {
  name: string;
  type: string;
  description?: string;
  mockExpression?: string;
  ref?: string;
}

interface Schema {
  id: string;
  name: string;
  description?: string;
  fields: SchemaField[];
}

interface SchemaModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSelect: (schema: Schema) => void;
  schemas: SchemaResponse[];
  setSchemas: (schemas: SchemaResponse[]) => void;
}

export function SchemaModal({
  isOpen,
  onClose,
  onSelect,
  schemas,
  setSchemas,
}: SchemaModalProps) {
  if (!isOpen) return null;

  const handleDeleteSchema = (schemaName: string) => {
    setSchemas(schemas.filter((s) => s.schemaName !== schemaName));
  };

  const handleSelectSchema = (schema: SchemaResponse) => {
    // SchemaResponse를 Schema 형식으로 변환
    const convertedSchema: Schema = {
      id: schema.schemaName,
      name: schema.schemaName,
      description: schema.description,
      fields: schema.properties
        ? Object.entries(schema.properties).map(([name, prop]) => ({
            name,
            type: prop.type || "string",
            description: prop.description,
            mockExpression: prop.mockExpression,
            ref: prop.ref,
          }))
        : [],
    };
    onSelect(convertedSchema);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
      <div className="bg-white dark:bg-[#161B22] rounded-md shadow-xl w-full max-w-3xl max-h-[80vh] overflow-hidden border border-gray-200 dark:border-[#2D333B]">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200 dark:border-[#2D333B]">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-[#E6EDF3]">
            Schema 관리
          </h2>
          <div className="flex gap-2">
            <button
              onClick={onClose}
              className="p-2 text-gray-500 hover:text-gray-700 dark:text-[#8B949E] dark:hover:text-[#E6EDF3]"
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
              <div className="text-center py-12 text-gray-600 dark:text-[#8B949E]">
                <p>저장된 Schema가 없습니다.</p>
                <p className="text-sm mt-2">
                  Response 탭에서 새 Schema를 생성하세요.
                </p>
              </div>
            ) : (
              <div className="space-y-3">
                {schemas.map((schema) => (
                  <div
                    key={schema.schemaName}
                    className="border border-gray-200 dark:border-[#2D333B] rounded-md p-4 hover:border-[#2563EB] transition-colors bg-gray-50 dark:bg-[#0D1117]"
                  >
                    <div className="flex items-center justify-between">
                      <div>
                        <h3 className="font-medium text-gray-900 dark:text-[#E6EDF3]">
                          {schema.schemaName}
                        </h3>
                        <p className="text-sm text-gray-600 dark:text-[#8B949E]">
                          {schema.properties
                            ? Object.keys(schema.properties).length
                            : 0}
                          개 필드
                        </p>
                        {schema.description && (
                          <p className="text-xs text-gray-500 dark:text-[#8B949E] mt-1">
                            {schema.description}
                          </p>
                        )}
                      </div>
                      <div className="flex gap-2">
                        <button
                          onClick={() => handleSelectSchema(schema)}
                          className="px-3 py-1 text-sm text-[#2563EB] hover:text-[#1E40AF] font-medium"
                        >
                          선택
                        </button>
                        <button
                          onClick={() => handleDeleteSchema(schema.schemaName)}
                          className="px-3 py-1 text-sm text-red-500 hover:text-red-600 font-medium"
                        >
                          삭제
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
    </div>
  );
}
