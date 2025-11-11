import { useState, useEffect } from "react";
import { SchemaFieldEditor } from "./SchemaFieldEditor";
import { SchemaModal } from "./SchemaModal";
import { SchemaCard } from "./SchemaCard";
import { getAllSchemas, createSchema, type SchemaResponse, type CreateSchemaRequest } from "../services/api";
import type { SchemaField, RequestBody } from "../types/schema.types";
import { createDefaultField } from "../types/schema.types";
import { convertSchemaFieldToOpenAPI } from "../utils/schemaConverter";

interface KeyValuePair {
  key: string;
  value: string;
  required?: boolean;
  description?: string;
  type?: string;
}

interface Receiver {
  address: string;
  headers: KeyValuePair[];
  schema: RequestBody;
}

interface Reply {
  address: string;
  schema: RequestBody;
}

interface WsEditorFormProps {
  entryPoint: string;
  setEntryPoint: (entryPoint: string) => void;
  summary: string;
  setSummary: (summary: string) => void;
  description: string;
  setDescription: (description: string) => void;
  tags: string;
  setTags: (tags: string) => void;
  receiver: Receiver | null;
  setReceiver: (receiver: Receiver | null) => void;
  reply: Reply | null;
  setReply: (reply: Reply | null) => void;
  isReadOnly?: boolean;
}

export function WsEditorForm({
  entryPoint,
  setEntryPoint,
  summary,
  setSummary,
  description,
  setDescription,
  tags,
  setTags,
  receiver,
  setReceiver,
  reply,
  setReply,
  isReadOnly = false,
}: WsEditorFormProps) {
  const [schemas, setSchemas] = useState<SchemaResponse[]>([]);
  const [isReceiverSchemaModalOpen, setIsReceiverSchemaModalOpen] = useState(false);
  const [isReplySchemaModalOpen, setIsReplySchemaModalOpen] = useState(false);
  const [isCreateSchemaModalOpen, setIsCreateSchemaModalOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  // 스키마 목록 로드 함수
  const loadSchemas = async () => {
    try {
      const response = await getAllSchemas();
      setSchemas(response.data);
    } catch (err) {
      console.error("스키마 로드 실패:", err);
    }
  };

  // 스키마 목록 로드
  useEffect(() => {
    loadSchemas();
  }, []);

  // Receiver 초기화
  const initializeReceiver = () => {
    if (receiver) return;
    setReceiver({
      address: "",
      headers: [
        {
          key: "accept-version",
          value: "1.1",
          required: true,
          description: "STOMP 프로토콜 버전 (필수)",
        },
      ],
      schema: {
        type: "json",
        fields: [],
      },
    });
  };

  // Reply 초기화
  const initializeReply = () => {
    if (reply) return;
    setReply({
      address: "",
      schema: {
        type: "json",
        fields: [],
      },
    });
  };

  // Receiver 헤더 관리
  const addReceiverHeader = () => {
    if (isReadOnly || !receiver) return;
    setReceiver({
      ...receiver,
      headers: [...receiver.headers, { key: "", value: "", required: false }],
    });
  };

  const removeReceiverHeader = (index: number) => {
    if (isReadOnly || !receiver) return;
    setReceiver({
      ...receiver,
      headers: receiver.headers.filter((_, i) => i !== index),
    });
  };

  const updateReceiverHeader = (index: number, field: "key" | "value", value: string) => {
    if (isReadOnly || !receiver) return;
    const updated = [...receiver.headers];
    updated[index] = { ...updated[index], [field]: value };
    setReceiver({ ...receiver, headers: updated });
  };

  // Receiver Schema 관리
  const addReceiverSchemaField = () => {
    if (isReadOnly || !receiver) return;
    const currentFields = receiver.schema.fields || [];
    setReceiver({
      ...receiver,
      schema: {
        ...receiver.schema,
        fields: [...currentFields, createDefaultField()],
      },
    });
  };

  const removeReceiverSchemaField = (index: number) => {
    if (isReadOnly || !receiver) return;
    const currentFields = receiver.schema.fields || [];
    setReceiver({
      ...receiver,
      schema: {
        ...receiver.schema,
        fields: currentFields.filter((_, i) => i !== index),
      },
    });
  };

  const updateReceiverSchemaField = (index: number, field: SchemaField) => {
    if (isReadOnly || !receiver) return;
    const currentFields = receiver.schema.fields || [];
    const updated = [...currentFields];
    updated[index] = field;
    setReceiver({ ...receiver, schema: { ...receiver.schema, fields: updated } });
  };

  // Reply Schema 관리
  const addReplySchemaField = () => {
    if (isReadOnly || !reply) return;
    const currentFields = reply.schema.fields || [];
    setReply({
      ...reply,
      schema: {
        ...reply.schema,
        fields: [...currentFields, createDefaultField()],
      },
    });
  };

  const removeReplySchemaField = (index: number) => {
    if (isReadOnly || !reply) return;
    const currentFields = reply.schema.fields || [];
    setReply({
      ...reply,
      schema: {
        ...reply.schema,
        fields: currentFields.filter((_, i) => i !== index),
      },
    });
  };

  const updateReplySchemaField = (index: number, field: SchemaField) => {
    if (isReadOnly || !reply) return;
    const currentFields = reply.schema.fields || [];
    const updated = [...currentFields];
    updated[index] = field;
    setReply({ ...reply, schema: { ...reply.schema, fields: updated } });
  };

  // Schema 선택 처리
  const handleReceiverSchemaSelect = (selectedSchema: {
    name: string;
    fields: SchemaField[];
    type: string;
  }) => {
    if (!receiver) return;
    if (selectedSchema.type === "object") {
      setReceiver({
        ...receiver,
        schema: {
          ...receiver.schema,
          schemaRef: selectedSchema.name,
          fields: selectedSchema.fields,
        },
      });
    } else {
      alert("스키마는 object 타입만 지원됩니다.");
    }
    setIsReceiverSchemaModalOpen(false);
  };

  const handleReplySchemaSelect = (selectedSchema: {
    name: string;
    fields: SchemaField[];
    type: string;
  }) => {
    if (!reply) return;
    if (selectedSchema.type === "object") {
      setReply({
        ...reply,
        schema: {
          ...reply.schema,
          schemaRef: selectedSchema.name,
          fields: selectedSchema.fields,
        },
      });
    } else {
      alert("스키마는 object 타입만 지원됩니다.");
    }
    setIsReplySchemaModalOpen(false);
  };

  // 새 Schema 생성 (SchemaCard 로직 참고)
  const [newSchemaName, setNewSchemaName] = useState("");
  const [newSchemaDescription, setNewSchemaDescription] = useState("");
  const [newSchemaFields, setNewSchemaFields] = useState<SchemaField[]>([]);

  const saveNewSchema = async () => {
    if (!newSchemaName.trim()) {
      alert("스키마 이름을 입력해주세요.");
      return;
    }

    if (newSchemaFields.length === 0) {
      alert("최소 하나의 필드를 추가해주세요.");
      return;
    }

    try {
      setIsLoading(true);

      const properties: Record<string, any> = {};
      const required: string[] = [];

      newSchemaFields.forEach((field) => {
        if (field.key.trim()) {
          properties[field.key] = convertSchemaFieldToOpenAPI(field);
          if (field.required) {
            required.push(field.key);
          }
        }
      });

      const schemaRequest: CreateSchemaRequest = {
        schemaName: newSchemaName.trim(),
        type: "object",
        title: `${newSchemaName} Schema`,
        description: newSchemaDescription.trim() || `${newSchemaName} 스키마 정의`,
        properties,
        required: required.length > 0 ? required : undefined,
        orders: newSchemaFields.map((f) => f.key),
      };

      await createSchema(schemaRequest);
      alert(`"${newSchemaName}" 스키마가 생성되었습니다.`);

      // 스키마 목록 다시 로드
      await loadSchemas();

      // 폼 초기화
      setNewSchemaName("");
      setNewSchemaDescription("");
      setNewSchemaFields([]);
      setIsCreateSchemaModalOpen(false);
    } catch (err) {
      console.error("스키마 생성 실패:", err);
      alert("스키마 생성에 실패했습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="w-full max-w-6xl mx-auto px-6 py-8">
      {/* Entry Point & Metadata */}
      <div className="rounded-md border border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#161B22] p-4 shadow-sm mb-6">
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
              d="M8 9l3 3-3 3m5 0h3M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
            />
          </svg>
          <span>Entry Point & Metadata</span>
        </div>
        <p className="text-xs text-gray-600 dark:text-[#8B949E] mb-4">
          WebSocket 연결 엔드포인트와 기본 정보를 입력하세요
        </p>

        <div className="space-y-4">
          {/* Entry Point */}
          <div>
            <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
              Entry Point
            </label>
            <input
              type="text"
              value={entryPoint}
              onChange={(e) => setEntryPoint(e.target.value)}
              placeholder="예: ws://localhost:8080/ws, wss://example.com/ws/chat"
              disabled={isReadOnly}
              className={`w-full px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm font-mono ${
                isReadOnly ? "opacity-60 cursor-not-allowed" : ""
              }`}
            />
          </div>

          {/* Tags & Summary */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                Tags/Category
              </label>
              <input
                type="text"
                value={tags}
                onChange={(e) => setTags(e.target.value)}
                placeholder="예: CHAT, NOTIFICATION, REALTIME"
                disabled={isReadOnly}
                className={`w-full px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm ${
                  isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                }`}
              />
            </div>
            <div className="lg:col-span-2">
              <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                Summary
              </label>
              <input
                type="text"
                value={summary}
                onChange={(e) => setSummary(e.target.value)}
                placeholder="예: 실시간 채팅 WebSocket 연결"
                disabled={isReadOnly}
                className={`w-full px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm ${
                  isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                }`}
              />
            </div>
          </div>

          {/* Description */}
          <div>
            <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
              Description
            </label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="예: 실시간 메시지 송수신을 위한 WebSocket 연결 엔드포인트"
              disabled={isReadOnly}
              className={`w-full px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm ${
                isReadOnly ? "opacity-60 cursor-not-allowed" : ""
              }`}
            />
          </div>
        </div>
      </div>

      {/* Receivers & Replies - 이분할 레이아웃 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        {/* Receiver 섹션 */}
        <div className="rounded-md border border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#161B22] p-4 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-[#E6EDF3]">
              Receiver
            </h2>
            {!isReadOnly && (
              <div className="flex gap-2">
                {receiver ? (
                  <button
                    onClick={() => setReceiver(null)}
                    className="text-red-500 hover:text-red-700 text-sm font-medium"
                  >
                    제거
                  </button>
                ) : (
                  <button
                    onClick={initializeReceiver}
                    className="px-3 py-2 bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded-md text-sm font-medium transition-colors flex items-center gap-2"
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
                        d="M12 4v16m8-8H4"
                      />
                    </svg>
                    추가
                  </button>
                )}
              </div>
            )}
          </div>

          {receiver ? (
            <div className="space-y-4">
              {/* 주소 */}
              <div>
                <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                  주소
                </label>
                <input
                  type="text"
                  value={receiver.address}
                  onChange={(e) => setReceiver({ ...receiver, address: e.target.value })}
                  placeholder="예: /chat/message"
                  disabled={isReadOnly}
                  className={`w-full px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm ${
                    isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                  }`}
                />
              </div>

              {/* Header */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E]">
                    Header
                  </label>
                  {!isReadOnly && (
                    <button
                      onClick={addReceiverHeader}
                      className="text-[#2563EB] hover:text-[#1E40AF] text-xs font-medium"
                    >
                      + Add Header
                    </button>
                  )}
                </div>
                <div className="space-y-2">
                  {receiver.headers.map((header, index) => (
                    <div key={index} className="flex gap-2">
                      <input
                        type="text"
                        value={header.key}
                        onChange={(e) => updateReceiverHeader(index, "key", e.target.value)}
                        placeholder="Header 이름"
                        disabled={isReadOnly}
                        className={`flex-1 px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm ${
                          isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                        }`}
                      />
                      <input
                        type="text"
                        value={header.value}
                        onChange={(e) => updateReceiverHeader(index, "value", e.target.value)}
                        placeholder="Header 값"
                        disabled={isReadOnly}
                        className={`flex-1 px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm ${
                          isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                        }`}
                      />
                      {!isReadOnly && (
                        <button
                          onClick={() => removeReceiverHeader(index)}
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
                </div>
              </div>

              {/* Schema */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E]">
                    Schema
                  </label>
                  <div className="flex gap-2">
                    {!isReadOnly && (
                      <>
                        <button
                          onClick={() => setIsReceiverSchemaModalOpen(true)}
                          className="text-[#2563EB] hover:text-[#1E40AF] text-xs font-medium"
                        >
                          Schema 선택
                        </button>
                        <button
                          onClick={addReceiverSchemaField}
                          className="text-[#2563EB] hover:text-[#1E40AF] text-xs font-medium"
                        >
                          + Add Field
                        </button>
                      </>
                    )}
                  </div>
                </div>

                {/* Schema Reference 표시 */}
                {receiver.schema.schemaRef && (
                  <div className="mb-2 p-2 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-md">
                    <div className="flex items-center justify-between">
                      <span className="text-xs text-blue-700 dark:text-blue-300">
                        Schema: {receiver.schema.schemaRef}
                      </span>
                      {!isReadOnly && (
                        <button
                          onClick={() => {
                            setReceiver({
                              ...receiver,
                              schema: { ...receiver.schema, schemaRef: undefined },
                            });
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
                  {receiver.schema.fields && receiver.schema.fields.length > 0 ? (
                    receiver.schema.fields.map((field, index) => (
                      <SchemaFieldEditor
                        key={index}
                        field={field}
                        onChange={(newField) => updateReceiverSchemaField(index, newField)}
                        onRemove={() => removeReceiverSchemaField(index)}
                        isReadOnly={isReadOnly}
                        allowFileType={false}
                        allowMockExpression={true}
                      />
                    ))
                  ) : (
                    <p className="text-xs text-gray-500 dark:text-gray-400 text-center py-2">
                      Schema 필드가 없습니다. "+ Add Field"를 클릭하여 추가하거나 Schema를 선택하세요.
                    </p>
                  )}
                </div>
              </div>
            </div>
          ) : (
            <div className="text-center py-8 text-gray-500 dark:text-gray-400 text-sm">
              <p>Receiver가 없습니다. "추가" 버튼을 클릭하여 추가하세요.</p>
            </div>
          )}
        </div>

        {/* Reply 섹션 */}
        <div className="rounded-md border border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#161B22] p-4 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-[#E6EDF3]">
              Reply
            </h2>
            {!isReadOnly && (
              <div className="flex gap-2">
                {reply ? (
                  <button
                    onClick={() => setReply(null)}
                    className="text-red-500 hover:text-red-700 text-sm font-medium"
                  >
                    제거
                  </button>
                ) : (
                  <button
                    onClick={initializeReply}
                    className="px-3 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-md text-sm font-medium transition-colors flex items-center gap-2"
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
                        d="M12 4v16m8-8H4"
                      />
                    </svg>
                    추가
                  </button>
                )}
              </div>
            )}
          </div>

          {reply ? (
            <div className="space-y-4">
              {/* 주소 */}
              <div>
                <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                  주소
                </label>
                <input
                  type="text"
                  value={reply.address}
                  onChange={(e) => setReply({ ...reply, address: e.target.value })}
                  placeholder="예: /chat/message"
                  disabled={isReadOnly}
                  className={`w-full px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm ${
                    isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                  }`}
                />
              </div>

              {/* Schema */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E]">
                    Schema
                  </label>
                  <div className="flex gap-2">
                    {!isReadOnly && (
                      <>
                        <button
                          onClick={() => setIsReplySchemaModalOpen(true)}
                          className="text-[#2563EB] hover:text-[#1E40AF] text-xs font-medium"
                        >
                          Schema 선택
                        </button>
                        <button
                          onClick={addReplySchemaField}
                          className="text-[#2563EB] hover:text-[#1E40AF] text-xs font-medium"
                        >
                          + Add Field
                        </button>
                      </>
                    )}
                  </div>
                </div>

                {/* Schema Reference 표시 */}
                {reply.schema.schemaRef && (
                  <div className="mb-2 p-2 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-md">
                    <div className="flex items-center justify-between">
                      <span className="text-xs text-blue-700 dark:text-blue-300">
                        Schema: {reply.schema.schemaRef}
                      </span>
                      {!isReadOnly && (
                        <button
                          onClick={() => {
                            setReply({
                              ...reply,
                              schema: { ...reply.schema, schemaRef: undefined },
                            });
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
                  {reply.schema.fields && reply.schema.fields.length > 0 ? (
                    reply.schema.fields.map((field, index) => (
                      <SchemaFieldEditor
                        key={index}
                        field={field}
                        onChange={(newField) => updateReplySchemaField(index, newField)}
                        onRemove={() => removeReplySchemaField(index)}
                        isReadOnly={isReadOnly}
                        allowFileType={false}
                        allowMockExpression={true}
                      />
                    ))
                  ) : (
                    <p className="text-xs text-gray-500 dark:text-gray-400 text-center py-2">
                      Schema 필드가 없습니다. "+ Add Field"를 클릭하여 추가하거나 Schema를 선택하세요.
                    </p>
                  )}
                </div>
              </div>
            </div>
          ) : (
            <div className="text-center py-8 text-gray-500 dark:text-gray-400 text-sm">
              <p>Reply가 없습니다. "추가" 버튼을 클릭하여 추가하세요.</p>
            </div>
          )}
        </div>
      </div>

      {/* Schema 선택 모달들 */}
      <SchemaModal
        isOpen={isReceiverSchemaModalOpen}
        onClose={() => setIsReceiverSchemaModalOpen(false)}
        onSelect={handleReceiverSchemaSelect}
        schemas={schemas}
        setSchemas={setSchemas}
      />

      <SchemaModal
        isOpen={isReplySchemaModalOpen}
        onClose={() => setIsReplySchemaModalOpen(false)}
        onSelect={handleReplySchemaSelect}
        schemas={schemas}
        setSchemas={setSchemas}
      />

      {/* Schema Card - 새 Schema 생성 */}
      <div className="mt-6">
        <div className="rounded-md border border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#161B22] shadow-sm">
          {/* Header with Toggle */}
          <button
            onClick={() => setIsCreateSchemaModalOpen(!isCreateSchemaModalOpen)}
            className="w-full flex items-center justify-between p-4 hover:bg-gray-50 dark:hover:bg-[#0D1117] transition-colors"
            disabled={isReadOnly}
          >
            <div className="flex items-center gap-2">
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
              <span className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3]">
                Schema 관리
              </span>
            </div>
            <svg
              className={`w-5 h-5 text-gray-500 dark:text-[#8B949E] transition-transform ${
                isCreateSchemaModalOpen ? "rotate-180" : ""
              }`}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M19 9l-7 7-7-7"
              />
            </svg>
          </button>

          {/* Content */}
          {isCreateSchemaModalOpen && (
            <div className="border-t border-gray-200 dark:border-[#2D333B] p-4">
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Schema 이름
                  </label>
                  <input
                    type="text"
                    value={newSchemaName}
                    onChange={(e) => setNewSchemaName(e.target.value)}
                    placeholder="예: ChatMessage"
                    disabled={isReadOnly}
                    className={`w-full px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm ${
                      isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                    }`}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    설명 (선택사항)
                  </label>
                  <input
                    type="text"
                    value={newSchemaDescription}
                    onChange={(e) => setNewSchemaDescription(e.target.value)}
                    placeholder="Schema 설명"
                    disabled={isReadOnly}
                    className={`w-full px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm ${
                      isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                    }`}
                  />
                </div>

                <div>
                  <div className="flex items-center justify-between mb-2">
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                      Schema Fields
                    </label>
                    {!isReadOnly && (
                      <button
                        onClick={() => setNewSchemaFields([...newSchemaFields, createDefaultField()])}
                        className="text-[#2563EB] hover:text-[#1E40AF] text-sm font-medium"
                      >
                        + Add Field
                      </button>
                    )}
                  </div>
                  <div className="space-y-2">
                    {newSchemaFields.map((field, index) => (
                      <SchemaFieldEditor
                        key={index}
                        field={field}
                        onChange={(newField) => {
                          const updated = [...newSchemaFields];
                          updated[index] = newField;
                          setNewSchemaFields(updated);
                        }}
                        onRemove={() => {
                          setNewSchemaFields(newSchemaFields.filter((_, i) => i !== index));
                        }}
                        isReadOnly={isReadOnly}
                        allowFileType={false}
                        allowMockExpression={false}
                      />
                    ))}
                    {newSchemaFields.length === 0 && (
                      <p className="text-xs text-gray-500 dark:text-gray-400 text-center py-2">
                        Schema 필드가 없습니다. "+ Add Field"를 클릭하여 추가하세요.
                      </p>
                    )}
                  </div>
                </div>

                {!isReadOnly && (
                  <div className="flex items-center justify-end gap-3 pt-2">
                    <button
                      onClick={() => {
                        setNewSchemaName("");
                        setNewSchemaDescription("");
                        setNewSchemaFields([]);
                      }}
                      className="px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-[#0D1117] rounded-md text-sm font-medium"
                    >
                      초기화
                    </button>
                    <button
                      onClick={saveNewSchema}
                      disabled={isLoading || !newSchemaName.trim()}
                      className="px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-md text-sm font-medium transition-colors disabled:opacity-50"
                    >
                      {isLoading ? "저장 중..." : "Schema 저장"}
                    </button>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Schema Card - 기존 Schema 관리 */}
      <div className="mt-6">
        <SchemaCard isReadOnly={isReadOnly} />
      </div>
    </div>
  );
}
