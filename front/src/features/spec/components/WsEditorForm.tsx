import { useState, useEffect } from "react";
import { SchemaFieldEditor } from "./SchemaFieldEditor";
import { SchemaModal } from "./SchemaModal";
import { SchemaCard } from "./SchemaCard";
import { SchemaViewer } from "./SchemaViewer";
import { getAllSchemas, getAllWebSocketMessages, type SchemaResponse, type MessageResponse } from "../services/api";
import type { SchemaField, RequestBody } from "../types/schema.types";
import { createDefaultField } from "../types/schema.types";

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
  messages?: string[]; // Message 이름 배열
}

interface Reply {
  address: string;
  schema: RequestBody;
  messages?: string[]; // Message 이름 배열
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
  isDocumentView?: boolean;
  diff?: string; // 명세 불일치 정보
  operationInfo?: {
    operationName?: string;
    tag?: string;
    progress?: string;
  };
  onSyncToActual?: () => void; // 실제 구현을 명세에 반영하는 콜백
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
  isDocumentView = false,
  diff,
  operationInfo,
  onSyncToActual,
}: WsEditorFormProps) {
  const [schemas, setSchemas] = useState<SchemaResponse[]>([]);
  const [messages, setMessages] = useState<MessageResponse[]>([]);
  const [isReceiverSchemaModalOpen, setIsReceiverSchemaModalOpen] = useState(false);
  const [isReplySchemaModalOpen, setIsReplySchemaModalOpen] = useState(false);
  const [wsTab, setWsTab] = useState<"receiver" | "reply" | "schema">("receiver");
  
  // Protocol state (entryPoint에서 분리)
  const [protocol, setProtocol] = useState<"ws" | "wss">("ws");
  const [pathname, setPathname] = useState("/ws");
  
  // Schema 이름에서 마지막 부분만 추출 (예: com.example.dto.UserDTO -> UserDTO)
  const getShortSchemaName = (fullName: string | undefined): string => {
    if (!fullName) return "";
    const parts = fullName.split(".");
    return parts[parts.length - 1];
  };
  
  // entryPoint 파싱 (기존 데이터 로드 시)
  useEffect(() => {
    if (entryPoint && entryPoint.includes("://")) {
      // ws://localhost:8080/ws 형태 파싱
      const match = entryPoint.match(/^(ws|wss):\/\/[^/]+(\/.*)?$/);
      if (match) {
        setProtocol(match[1] as "ws" | "wss");
        setPathname(match[2] || "/ws");
      }
    } else if (entryPoint) {
      // /ws 형태만 있으면 pathname으로
      setPathname(entryPoint);
    }
  }, [entryPoint]);
  
  // Messages 목록 로드
  const loadMessages = async () => {
    try {
      const response = await getAllWebSocketMessages();
      setMessages(response.data);
    } catch (err) {
      console.error("메시지 로드 실패:", err);
    }
  };
  
  useEffect(() => {
    loadMessages();
  }, []);

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

  // removed unused getDiffMessage

  // Diff 타입별 상세 정보 (REST DiffNotification 스타일과 동일)
  const getDiffDetails = (diffType?: string) => {
    const lowerDiff = diffType?.toLowerCase() || "";
    
    if (lowerDiff === "channel") {
      return {
        type: "channel" as const,
        label: "Channel 불일치",
        description: "Channel 정보가 명세와 실제 구현이 다릅니다.",
        canSync: true,
      };
    } else if (lowerDiff === "payload") {
      return {
        type: "payload" as const,
        label: "Payload 불일치",
        description: "메시지 Payload 구조가 명세와 실제 구현이 다릅니다.",
        canSync: false,
      };
    }
    
    return {
      type: "other" as const,
      label: "불일치",
      description: "명세와 실제 구현이 일치하지 않습니다.",
      canSync: false,
    };
  };

  // Diff 알림 렌더링 (문서 뷰와 편집 뷰 모두에서 표시)
  const renderDiffNotification = () => {
    if (!diff || diff === "none") return null;

    const details = getDiffDetails(diff);
    const progressLower = operationInfo?.progress?.toLowerCase() || "none";
    const isCompleted = progressLower === "completed";
    
    return (
      <div className="rounded-md border border-amber-300 dark:border-amber-700 bg-amber-50 dark:bg-amber-900/20 shadow-sm mb-6">
        {/* 헤더 */}
        <div className="p-4 border-b border-amber-200 dark:border-amber-800">
          <div className="flex items-start gap-3">
            <svg
              className="w-5 h-5 text-amber-600 dark:text-amber-400 flex-shrink-0 mt-0.5"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
              />
            </svg>
            <div className="flex-1">
              <div className="flex items-center gap-2 mb-1">
                <h3 className="text-sm font-semibold text-amber-800 dark:text-amber-300">
                  명세와 실제 구현의 불일치
                </h3>
                <span className="px-2 py-0.5 bg-amber-200 dark:bg-amber-800 text-amber-800 dark:text-amber-200 text-xs font-medium rounded">
                  {details.label}
                </span>
              </div>
              <p className="text-sm text-amber-700 dark:text-amber-400">
                {details.description}
              </p>
              <p className="text-xs text-amber-600 dark:text-amber-500 mt-2">
                {isCompleted
                  ? "이 Operation은 completed 상태로 실제 구현이 완료되었습니다."
                  : "이 Operation은 진행 중입니다."}
                {details.canSync && " 아래 버튼으로 명세를 갱신할 수 있습니다."}
              </p>
            </div>
          </div>
        </div>

        {/* 상세 정보 */}
        <div className="p-4 space-y-3">
          <div className="bg-white dark:bg-amber-950/30 rounded-md p-3 border border-amber-200 dark:border-amber-800">
            <h4 className="text-xs font-semibold text-amber-900 dark:text-amber-200 mb-2 flex items-center gap-1">
              <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              안내사항
            </h4>
            <ul className="space-y-2">
              <li className="flex items-start gap-2 text-xs text-amber-700 dark:text-amber-400">
                <svg className="w-3 h-3 text-amber-600 dark:text-amber-500 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <span>
                  백엔드에서{" "}
                  <code className="px-1 py-0.5 bg-amber-100 dark:bg-amber-900 border border-amber-300 dark:border-amber-700 rounded text-[10px] font-mono text-amber-900 dark:text-amber-200">
                    x-ouroboros-diff
                  </code>{" "}
                  필드를 통해 불일치가 감지되었습니다.
                </span>
              </li>
              {details.type === "channel" && (
                <li className="flex items-start gap-2 text-xs text-amber-700 dark:text-amber-400">
                  <svg className="w-3 h-3 text-amber-600 dark:text-amber-500 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <span>
                    실제 구현에 존재하지만 명세에 없는 Channel이 있다면, 아래 버튼을 클릭하여 명세에 자동으로 추가할 수 있습니다.
                  </span>
                </li>
              )}
            </ul>
          </div>

          {details.canSync && onSyncToActual && (
            <button
              onClick={onSyncToActual}
              className="w-full px-4 py-3 bg-amber-600 hover:bg-amber-700 dark:bg-amber-700 dark:hover:bg-amber-800 text-white rounded-md transition-colors text-sm font-semibold flex items-center justify-center gap-2 shadow-md"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              실제 구현을 명세에 반영
            </button>
          )}
        </div>
      </div>
    );
  };

  // 문서 형식 뷰
  if (isDocumentView) {
    return (
      <div className="space-y-6">
        {/* Diff 알림 */}
        {renderDiffNotification()}
        {/* Entry Point */}
        <div>
          <h3 className="text-sm font-semibold text-gray-700 dark:text-[#C9D1D9] mb-2">Entry Point</h3>
          <div className="flex items-start gap-3 text-sm">
            <span className="font-mono text-gray-900 dark:text-[#E6EDF3]">{entryPoint || <span className="text-gray-400 italic">(empty)</span>}</span>
          </div>
        </div>

        {/* Summary */}
        {summary && (
          <div>
            <h3 className="text-sm font-semibold text-gray-700 dark:text-[#C9D1D9] mb-2">Summary</h3>
            <div className="text-sm text-gray-900 dark:text-[#E6EDF3]">{summary}</div>
          </div>
        )}

        {/* Description */}
        {description && (
          <div>
            <h3 className="text-sm font-semibold text-gray-700 dark:text-[#C9D1D9] mb-2">Description</h3>
            <div className="text-sm text-gray-900 dark:text-[#E6EDF3]">{description}</div>
          </div>
        )}

        {/* Tags */}
        {tags && (
          <div>
            <h3 className="text-sm font-semibold text-gray-700 dark:text-[#C9D1D9] mb-2">Tags</h3>
            <div className="flex flex-wrap gap-2">
              {tags.split(",").map((tag, index) => (
                <span
                  key={index}
                  className="px-2 py-1 bg-blue-100 dark:bg-blue-900/20 text-blue-700 dark:text-blue-300 text-xs rounded"
                >
                  {tag.trim()}
                </span>
              ))}
            </div>
          </div>
        )}

        {/* Receiver */}
        {receiver && (
          <div>
            <h3 className="text-sm font-semibold text-gray-700 dark:text-[#C9D1D9] mb-3">Receiver</h3>
            <div className="space-y-4 ml-4">
              <div>
                <h4 className="text-xs font-semibold text-gray-600 dark:text-[#8B949E] mb-2">Address</h4>
                <div className="flex items-start gap-3 text-sm">
                  <span className="font-mono text-gray-900 dark:text-[#E6EDF3]">{receiver.address || <span className="text-gray-400 italic">(empty)</span>}</span>
                </div>
              </div>

              {receiver.headers && receiver.headers.length > 0 && (
                <div>
                  <h4 className="text-xs font-semibold text-gray-600 dark:text-[#8B949E] mb-2">Headers</h4>
                  <div className="space-y-2">
                    {receiver.headers.map((header, index) => (
                      <div key={index} className="flex items-start gap-3 text-sm">
                        <span className="font-mono text-gray-900 dark:text-[#E6EDF3] min-w-[120px]">{header.key}</span>
                        <span className="text-gray-600 dark:text-[#8B949E]">:</span>
                        <span className="text-gray-900 dark:text-[#E6EDF3] flex-1">{header.value || <span className="text-gray-400 italic">(empty)</span>}</span>
                        {header.required && (
                          <span className="px-2 py-0.5 bg-red-100 dark:bg-red-900/20 text-red-700 dark:text-red-300 text-xs rounded">Required</span>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {receiver.schema && (
                <div>
                  <h4 className="text-xs font-semibold text-gray-600 dark:text-[#8B949E] mb-2">Schema</h4>
                  <SchemaViewer
                    schemaType={receiver.schema.rootSchemaType}
                    fields={receiver.schema.fields}
                    schemaRef={receiver.schema.schemaRef}
                    description={receiver.schema.description}
                    contentType="application/json"
                  />
                </div>
              )}
            </div>
          </div>
        )}

        {/* Reply */}
        {reply && (
          <div>
            <h3 className="text-sm font-semibold text-gray-700 dark:text-[#C9D1D9] mb-3">Reply</h3>
            <div className="space-y-4 ml-4">
              <div>
                <h4 className="text-xs font-semibold text-gray-600 dark:text-[#8B949E] mb-2">Address</h4>
                <div className="flex items-start gap-3 text-sm">
                  <span className="font-mono text-gray-900 dark:text-[#E6EDF3]">{reply.address || <span className="text-gray-400 italic">(empty)</span>}</span>
                </div>
              </div>

              {reply.schema && (
                <div>
                  <h4 className="text-xs font-semibold text-gray-600 dark:text-[#8B949E] mb-2">Schema</h4>
                  <SchemaViewer
                    schemaType={reply.schema.rootSchemaType}
                    fields={reply.schema.fields}
                    schemaRef={reply.schema.schemaRef}
                    description={reply.schema.description}
                    contentType="application/json"
                  />
                </div>
              )}
            </div>
          </div>
        )}

        {!receiver && !reply && (
          <div className="text-sm text-gray-500 dark:text-[#8B949E] italic">No WebSocket configuration.</div>
        )}
      </div>
    );
  }

  return (
    <div className="w-full max-w-6xl mx-auto px-6 py-8">
      {/* Diff 알림 */}
      {renderDiffNotification()}

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

        {/* Operation 정보 (읽기 전용 모드) */}
        {isReadOnly && operationInfo && (
          <div className="mb-4 p-3 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-md">
            <div className="text-xs text-blue-600 dark:text-blue-500 font-medium mb-1">Operation Name</div>
            <div className="text-sm text-blue-800 dark:text-blue-400 font-mono">{operationInfo.operationName}</div>
          </div>
        )}

        <div className="space-y-4">
          {/* Protocol & Pathname */}
          <div className="grid grid-cols-4 gap-3">
            {/* Protocol 선택 */}
            <div className="col-span-1">
              <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                Protocol
              </label>
              <select
                value={protocol}
                onChange={(e) => {
                  const newProtocol = e.target.value as "ws" | "wss";
                  setProtocol(newProtocol);
                  setPathname(pathname);
                }}
                disabled={isReadOnly}
                className={`w-full px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm ${
                  isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                }`}
              >
                <option value="ws">ws</option>
                <option value="wss">wss</option>
              </select>
            </div>
            
            {/* Pathname 입력 */}
            <div className="col-span-3">
              <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                Pathname (Entry Point)
              </label>
              <input
                type="text"
                value={pathname}
                onChange={(e) => {
                  const newPathname = e.target.value;
                  setPathname(newPathname);
                  setEntryPoint(newPathname);
                }}
                placeholder="예: /ws, /websocket, /chat"
                disabled={isReadOnly}
                className={`w-full px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm font-mono ${
                  isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                }`}
              />
            </div>
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

      {/* Operation Type 표시 (읽기 전용 모드) */}
      {isReadOnly && operationInfo && (
        <div className="mb-4 p-3 bg-gradient-to-r from-purple-50 to-blue-50 dark:from-purple-900/20 dark:to-blue-900/20 border border-purple-200 dark:border-purple-800 rounded-md">
          <div className="flex items-center gap-2">
            <svg
              className="w-5 h-5 text-purple-600 dark:text-purple-400"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4"
              />
            </svg>
            <div>
              <span className="text-xs text-purple-600 dark:text-purple-500 font-medium mr-2">Operation Type:</span>
              <span className="text-sm font-bold text-purple-800 dark:text-purple-300">
                {operationInfo.tag === "duplicate" ? "DUPLEX (양방향 통신)" : 
                 operationInfo.tag === "receive" ? "RECEIVE (메시지 수신)" : 
                 operationInfo.tag === "sendto" ? "SEND (메시지 송신)" : operationInfo.tag}
              </span>
            </div>
          </div>
        </div>
      )}

      {/* Receiver/Reply/Schema 탭 */}
      <div className="rounded-md border border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#161B22] shadow-sm mb-6 overflow-hidden">
        {/* 탭 헤더 - 폴더 느낌으로 통합 */}
        <div className="bg-gray-50 dark:bg-[#0D1117] border-b border-gray-200 dark:border-[#2D333B] px-4 pt-2">
          <div className="flex gap-0.5 -mb-px">
            <button
              onClick={() => setWsTab("receiver")}
              className={`px-4 py-2 text-sm font-medium transition-all rounded-t-md border border-b-0 ${
                wsTab === "receiver"
                  ? "text-gray-900 dark:text-[#E6EDF3] bg-white dark:bg-[#161B22] border-gray-200 dark:border-[#2D333B] border-b-white dark:border-b-[#161B22] relative z-10"
                  : "text-gray-500 dark:text-[#8B949E] bg-transparent border-transparent hover:text-gray-700 dark:hover:text-[#C9D1D9] hover:bg-gray-100 dark:hover:bg-[#21262D]"
              }`}
            >
              Receiver
            </button>
            <button
              onClick={() => setWsTab("reply")}
              className={`px-4 py-2 text-sm font-medium transition-all rounded-t-md border border-b-0 ${
                wsTab === "reply"
                  ? "text-gray-900 dark:text-[#E6EDF3] bg-white dark:bg-[#161B22] border-gray-200 dark:border-[#2D333B] border-b-white dark:border-b-[#161B22] relative z-10"
                  : "text-gray-500 dark:text-[#8B949E] bg-transparent border-transparent hover:text-gray-700 dark:hover:text-[#C9D1D9] hover:bg-gray-100 dark:hover:bg-[#21262D]"
              }`}
            >
              Reply
            </button>
            <button
              onClick={() => setWsTab("schema")}
              className={`px-4 py-2 text-sm font-medium transition-all rounded-t-md border border-b-0 ${
                wsTab === "schema"
                  ? "text-gray-900 dark:text-[#E6EDF3] bg-white dark:bg-[#161B22] border-gray-200 dark:border-[#2D333B] border-b-white dark:border-b-[#161B22] relative z-10"
                  : "text-gray-500 dark:text-[#8B949E] bg-transparent border-transparent hover:text-gray-700 dark:hover:text-[#C9D1D9] hover:bg-gray-100 dark:hover:bg-[#21262D]"
              }`}
            >
              Schema
            </button>
          </div>
        </div>

        {/* 탭 내용 */}
        <div className="p-4 bg-white dark:bg-[#161B22]">
          {wsTab === "receiver" && (
            <div>
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3]">
                  Receiver
                </h3>
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

              {/* Messages */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E]">
                    📨 Messages {messages.length > 0 && `(${messages.length})`}
                  </label>
                  {!isReadOnly && (
                    <button
                      onClick={loadMessages}
                      className="text-[#2563EB] hover:text-[#1E40AF] text-xs"
                      title="새로고침"
                    >
                      ↻
                    </button>
                  )}
                </div>
                <div className="space-y-2 max-h-48 overflow-y-auto border border-gray-200 dark:border-[#2D333B] rounded-md p-3 bg-gray-50 dark:bg-[#0D1117]">
                  {messages.length > 0 ? (
                    messages.map((msg, idx) => {
                      const messageName = msg.messageName || msg.name || "Unnamed";
                      const isSelected = receiver.messages?.includes(messageName) || false;
                      
                      return (
                        <label
                          key={`${messageName}-${idx}`}
                          className={`flex items-center gap-2 p-2.5 rounded-md cursor-pointer transition-colors ${
                            isSelected
                              ? "bg-blue-100 dark:bg-blue-900/30 border border-blue-300 dark:border-blue-700"
                              : "bg-white dark:bg-[#161B22] border border-gray-200 dark:border-[#2D333B] hover:bg-gray-100 dark:hover:bg-[#21262D]"
                          } ${isReadOnly ? "opacity-60 cursor-not-allowed" : ""}`}
                        >
                          <input
                            type="checkbox"
                            checked={isSelected}
                            onChange={(e) => {
                              if (isReadOnly) return;
                              const currentMessages = receiver.messages || [];
                              const newMessages = e.target.checked
                                ? [...currentMessages, messageName]
                                : currentMessages.filter((m) => m !== messageName);
                              setReceiver({ ...receiver, messages: newMessages });
                            }}
                            disabled={isReadOnly}
                            className="w-4 h-4 text-blue-600 rounded focus:ring-blue-500 flex-shrink-0"
                          />
                          <div className="flex-1 min-w-0">
                            <div className="text-sm text-gray-900 dark:text-[#E6EDF3] font-medium truncate">
                              {messageName}
                            </div>
                            {msg.description && (
                              <div className="text-xs text-gray-500 dark:text-[#8B949E] truncate mt-0.5">
                                {msg.description}
                              </div>
                            )}
                          </div>
                        </label>
                      );
                    })
                  ) : (
                    <div className="text-center py-6">
                      <p className="text-sm text-gray-500 dark:text-[#8B949E] mb-2">
                        사용 가능한 메시지가 없습니다
                      </p>
                      <p className="text-xs text-gray-400 dark:text-[#6E7681]">
                        Message를 먼저 생성해주세요
                      </p>
                    </div>
                  )}
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
                      <span 
                        className="text-xs text-blue-700 dark:text-blue-300 font-medium"
                        title={receiver.schema.schemaRef}
                      >
                        Schema: {getShortSchemaName(receiver.schema.schemaRef)}
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
          )}

          {wsTab === "reply" && (
            <div>
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3]">
                  Reply
                </h3>
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

              {/* Messages */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E]">
                    📨 Messages {messages.length > 0 && `(${messages.length})`}
                  </label>
                  {!isReadOnly && (
                    <button
                      onClick={loadMessages}
                      className="text-[#2563EB] hover:text-[#1E40AF] text-xs"
                      title="새로고침"
                    >
                      ↻
                    </button>
                  )}
                </div>
                <div className="space-y-2 max-h-48 overflow-y-auto border border-gray-200 dark:border-[#2D333B] rounded-md p-3 bg-gray-50 dark:bg-[#0D1117]">
                  {messages.length > 0 ? (
                    messages.map((msg, idx) => {
                      const messageName = msg.messageName || msg.name || "Unnamed";
                      const isSelected = reply.messages?.includes(messageName) || false;
                      
                      return (
                        <label
                          key={`${messageName}-${idx}`}
                          className={`flex items-center gap-2 p-2.5 rounded-md cursor-pointer transition-colors ${
                            isSelected
                              ? "bg-emerald-100 dark:bg-emerald-900/30 border border-emerald-300 dark:border-emerald-700"
                              : "bg-white dark:bg-[#161B22] border border-gray-200 dark:border-[#2D333B] hover:bg-gray-100 dark:hover:bg-[#21262D]"
                          } ${isReadOnly ? "opacity-60 cursor-not-allowed" : ""}`}
                        >
                          <input
                            type="checkbox"
                            checked={isSelected}
                            onChange={(e) => {
                              if (isReadOnly) return;
                              const currentMessages = reply.messages || [];
                              const newMessages = e.target.checked
                                ? [...currentMessages, messageName]
                                : currentMessages.filter((m) => m !== messageName);
                              setReply({ ...reply, messages: newMessages });
                            }}
                            disabled={isReadOnly}
                            className="w-4 h-4 text-emerald-600 rounded focus:ring-emerald-500 flex-shrink-0"
                          />
                          <div className="flex-1 min-w-0">
                            <div className="text-sm text-gray-900 dark:text-[#E6EDF3] font-medium truncate">
                              {messageName}
                            </div>
                            {msg.description && (
                              <div className="text-xs text-gray-500 dark:text-[#8B949E] truncate mt-0.5">
                                {msg.description}
                              </div>
                            )}
                          </div>
                        </label>
                      );
                    })
                  ) : (
                    <div className="text-center py-6">
                      <p className="text-sm text-gray-500 dark:text-[#8B949E] mb-2">
                        사용 가능한 메시지가 없습니다
                      </p>
                      <p className="text-xs text-gray-400 dark:text-[#6E7681]">
                        Message를 먼저 생성해주세요
                      </p>
                    </div>
                  )}
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
                      <span 
                        className="text-xs text-blue-700 dark:text-blue-300 font-medium"
                        title={reply.schema.schemaRef}
                      >
                        Schema: {getShortSchemaName(reply.schema.schemaRef)}
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
          )}

          {wsTab === "schema" && (
            <SchemaCard
              isReadOnly={isReadOnly}
              protocol="WebSocket"
            />
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
    </div>
  );
}
