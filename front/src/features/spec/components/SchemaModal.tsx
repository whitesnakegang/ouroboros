interface SchemaField {
  name: string;
  type: string;
  mock: string;
  description: string;
}

interface Schema {
  id: string;
  name: string;
  fields: SchemaField[];
}

interface SchemaModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSelect: (schema: Schema) => void;
  schemas: Schema[];
  setSchemas: (schemas: Schema[]) => void;
}

export function SchemaModal({
  isOpen,
  onClose,
  onSelect,
  schemas,
  setSchemas,
}: SchemaModalProps) {
  if (!isOpen) return null;

  const handleDeleteSchema = (id: string) => {
    setSchemas(schemas.filter((s) => s.id !== id));
  };

  const handleSelectSchema = (schema: Schema) => {
    onSelect(schema);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
      <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-xl w-full max-w-3xl max-h-[80vh] overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200 dark:border-gray-700">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
            Schema 관리
          </h2>
          <div className="flex gap-2">
            <button
              onClick={() => {
                const newSchema: Schema = {
                  id: Date.now().toString(),
                  name: `New Schema ${schemas.length + 1}`,
                  fields: [],
                };
                setSchemas([...schemas, newSchema]);
              }}
              className="px-4 py-2 bg-blue-600 hover:bg-blue-700 dark:bg-blue-700 dark:hover:bg-blue-600 text-white rounded-lg text-sm font-medium transition-colors"
            >
              + 새 Schema
            </button>
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
                <p>저장된 Schema가 없습니다.</p>
                <p className="text-sm mt-2">
                  "+ 새 Schema" 버튼을 눌러 추가하세요.
                </p>
              </div>
            ) : (
              <div className="space-y-3">
                {schemas.map((schema) => (
                  <div
                    key={schema.id}
                    className="border border-gray-200 dark:border-gray-700 rounded-lg p-4 hover:border-purple-500 dark:hover:border-purple-400 transition-colors"
                  >
                    <div className="flex items-center justify-between">
                      <div>
                        <h3 className="font-medium text-gray-900 dark:text-white">
                          {schema.name}
                        </h3>
                        <p className="text-sm text-gray-500 dark:text-gray-400">
                          {schema.fields.length}개 필드
                        </p>
                      </div>
                      <div className="flex gap-2">
                        <button
                          onClick={() => handleSelectSchema(schema)}
                          className="px-3 py-1 text-sm text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300 font-medium"
                        >
                          선택
                        </button>
                        <button
                          onClick={() => handleDeleteSchema(schema.id)}
                          className="px-3 py-1 text-sm text-red-600 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300 font-medium"
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
