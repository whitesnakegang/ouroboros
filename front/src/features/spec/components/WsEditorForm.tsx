import { useState, useEffect } from "react";
import { SchemaFieldEditor } from "./SchemaFieldEditor";
import { SchemaModal } from "./SchemaModal";
import { SchemaCard } from "./SchemaCard";
import { SchemaViewer } from "./SchemaViewer";
import { AlertModal } from "@/ui/AlertModal";
import {
  getAllWebSocketSchemas,
  getAllWebSocketMessages,
  getAllWebSocketChannels,
  createWebSocketMessage,
  type SchemaResponse,
  type MessageResponse,
  type ChannelResponse,
  type CreateMessageRequest,
} from "../services/api";
import type {
  SchemaField,
  RequestBody,
  PrimitiveSchema,
} from "../types/schema.types";
import { createDefaultField } from "../types/schema.types";
import { parseOpenAPISchemaToSchemaType } from "../utils/schemaConverter";

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
  entryPointError?: string; // entryPoint 한글 검증 에러
  setEntryPointError?: (error: string) => void; // entryPoint 에러 설정 함수
  protocol?: "ws" | "wss";
  setProtocol?: (protocol: "ws" | "wss") => void;
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
  wsSpecTab?: "receiver" | "reply";
  setWsSpecTab?: (tab: "receiver" | "reply") => void;
  diff?: string; // 명세 불일치 정보
  operationInfo?: {
    operationName?: string;
    tag?: string;
    progress?: string;
  };
  onSyncToActual?: () => void; // 실제 구현을 명세에 반영하는 콜백
  onProgressUpdate?: (progress: "none" | "completed") => Promise<void>; // progress 수동 업데이트 콜백
}

export function WsEditorForm({
  entryPoint,
  setEntryPoint,
  entryPointError: externalEntryPointError,
  setEntryPointError: setExternalEntryPointError,
  protocol: externalProtocol,
  setProtocol: setExternalProtocol,
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
  wsSpecTab: externalWsSpecTab,
  setWsSpecTab: setExternalWsSpecTab,
  diff,
  operationInfo,
  onSyncToActual,
  onProgressUpdate,
}: WsEditorFormProps) {
  const [schemas, setSchemas] = useState<SchemaResponse[]>([]);
  const [messages, setMessages] = useState<MessageResponse[]>([]);
  const [channels, setChannels] = useState<ChannelResponse[]>([]);
  const [isReceiverSchemaModalOpen, setIsReceiverSchemaModalOpen] =
    useState(false);
  const [isReplySchemaModalOpen, setIsReplySchemaModalOpen] = useState(false);
  const [isMessageSchemaModalOpen, setIsMessageSchemaModalOpen] =
    useState(false);

  // Modal 상태
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

  // 내부 wsSpecTab state (외부에서 제공되지 않으면 내부에서 관리)
  // SEND 타입의 경우 기본 탭을 reply로 설정
  const getDefaultTab = (): "receiver" | "reply" => {
    if (operationInfo?.tag === "send" || operationInfo?.tag === "sendto") {
      return "reply";
    }
    return "receiver";
  };
  const [internalWsSpecTab, setInternalWsSpecTab] = useState<
    "receiver" | "reply"
  >(getDefaultTab());
  
  // operationInfo.tag가 변경될 때 기본 탭 업데이트 (초기 로드 및 변경 시)
  useEffect(() => {
    if (!externalWsSpecTab && operationInfo?.tag) {
      const defaultTab = getDefaultTab();
      setInternalWsSpecTab(defaultTab);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [operationInfo?.tag, externalWsSpecTab]);
  
  const wsSpecTab = externalWsSpecTab ?? internalWsSpecTab;
  // setWsSpecTab는 외부(ApiEditorLayout)에서 탭 전환 시 사용되므로 유지
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const setWsSpecTab = setExternalWsSpecTab ?? setInternalWsSpecTab;

  // 통합 탭 (편집 모드용)
  const [wsTab, setWsTab] = useState<
    "receiver" | "reply" | "schema" | "message"
  >("receiver");

  // 채널 선택 모드 (기존 채널 선택 vs 새 채널 생성)
  const [receiverChannelMode, setReceiverChannelMode] = useState<
    "select" | "create"
  >("select");
  const [replyChannelMode, setReplyChannelMode] = useState<"select" | "create">(
    "select"
  );

  // 메시지 작성 상태
  const [messageName, setMessageName] = useState("");
  const [messageDescription, setMessageDescription] = useState("");
  const [messageType, setMessageType] = useState<"header" | "schema" | "name">(
    "name"
  );
  const [messageHeaders, setMessageHeaders] = useState<KeyValuePair[]>([]);
  const [messagePayloadSchema, setMessagePayloadSchema] = useState<RequestBody>(
    {
      type: "json",
      fields: [],
    }
  );
  const [selectedMessageSchema, setSelectedMessageSchema] = useState<
    string | null
  >(null);

  // Progress 토글 로컬 상태 (즉시 UI 반영용)
  const [localProgress, setLocalProgress] = useState<string | null>(null);

  // Protocol state (entryPoint에서 분리)
  const [internalProtocol, setInternalProtocol] = useState<"ws" | "wss">("ws");
  const protocol = externalProtocol ?? internalProtocol;
  const setProtocol = setExternalProtocol ?? setInternalProtocol;
  const [pathname, setPathname] = useState("/ws");

  // EntryPoint 에러 state (내부 또는 외부)
  const [internalEntryPointError, setInternalEntryPointError] = useState("");
  const entryPointError = externalEntryPointError ?? internalEntryPointError;
  const setEntryPointError =
    setExternalEntryPointError ?? setInternalEntryPointError;

  // 한글 감지 함수
  const hasKorean = (text: string): boolean => {
    return /[ㄱ-ㅎ|ㅏ-ㅣ|가-힣]/.test(text);
  };

  // Schema 이름에서 마지막 부분만 추출 (예: com.example.dto.UserDTO -> UserDTO)
  const getShortSchemaName = (fullName: string | undefined): string => {
    if (!fullName) return "";
    const parts = fullName.split(".");
    return parts[parts.length - 1];
  };

  // entryPoint 파싱 (기존 데이터 로드 시)
  useEffect(() => {
    // 외부에서 protocol이 전달되면 그것을 우선 사용 (백엔드에서 받은 protocol 값)
    if (externalProtocol && setExternalProtocol) {
      setExternalProtocol(externalProtocol);
    }
    
    if (entryPoint && entryPoint.includes("://")) {
      // ws://localhost:8080/ws 형태 파싱 (외부 protocol이 없을 때만)
      if (!externalProtocol) {
      const match = entryPoint.match(/^(ws|wss):\/\/[^/]+(\/.*)?$/);
      if (match) {
        setProtocol(match[1] as "ws" | "wss");
        setPathname(match[2] || "/ws");
        }
      } else {
        // 외부 protocol이 있으면 pathname만 추출
        const pathMatch = entryPoint.match(/^[^:]+:\/\/[^/]+(\/.*)?$/);
        if (pathMatch) {
          setPathname(pathMatch[1] || "/ws");
        }
      }
    } else if (entryPoint) {
      // /ws 형태만 있으면 pathname으로
      setPathname(entryPoint);
    }
    // entryPoint 변경 시 한글 검증
    if (entryPoint && hasKorean(entryPoint)) {
      setEntryPointError("한글로 생성할 수 없습니다");
    } else {
      setEntryPointError("");
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entryPoint, externalProtocol, setProtocol]);

  // Messages 목록 로드
  const loadMessages = async () => {
    try {
      const response = await getAllWebSocketMessages();
      setMessages(response.data);
    } catch {
      // 메시지 로드 실패 시 무시
    }
  };

  // Channels 목록 로드
  const loadChannels = async () => {
    try {
      const response = await getAllWebSocketChannels();
      setChannels(response.data);
    } catch {
      // 채널 로드 실패 시 무시
    }
  };

  useEffect(() => {
    loadMessages();
    loadChannels();
  }, []);

  // 스키마 목록 로드 함수 (WebSocket 전용)
  const loadSchemas = async () => {
    try {
      const response = await getAllWebSocketSchemas();
      setSchemas(response.data);
    } catch {
      // WebSocket 스키마 로드 실패 시 무시
    }
  };

  // 스키마 목록 로드
  useEffect(() => {
    loadSchemas();
  }, []);

  // operationInfo.progress 변경 시 로컬 상태 동기화
  useEffect(() => {
    if (operationInfo?.progress) {
      setLocalProgress(operationInfo.progress.toLowerCase());
    } else {
      setLocalProgress(null);
    }
  }, [operationInfo?.progress]);

  // Receiver 초기화
  const initializeReceiver = () => {
    if (receiver) return;
    setReceiver({
      address: "",
      headers: [],
      schema: {
        type: "json",
        fields: [],
      },
      messages: [],
    });
    setReceiverChannelMode("select");
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
      messages: [],
    });
    setReplyChannelMode("select");
  };

  // Receiver 헤더 관리 (현재 사용하지 않음 - Header 섹션 제거됨)
  // const addReceiverHeader = () => { ... }
  // const removeReceiverHeader = (index: number) => { ... }
  // const updateReceiverHeader = (index: number, field: "key" | "value", value: string) => { ... }

  // Receiver Schema 관리 (현재 사용하지 않음 - Schema 섹션 제거됨)
  // const addReceiverSchemaField = () => { ... }
  // const removeReceiverSchemaField = (index: number) => { ... }
  // const updateReceiverSchemaField = (index: number, field: SchemaField) => { ... }

  // Reply Schema 관리 (현재 사용하지 않음 - Schema 섹션 제거됨)
  // const addReplySchemaField = () => {
  //   if (isReadOnly || !reply) return;
  //   const currentFields = reply.schema.fields || [];
  //   setReply({
  //     ...reply,
  //     schema: {
  //       ...reply.schema,
  //       fields: [...currentFields, createDefaultField()],
  //     },
  //   });
  // };

  // const removeReplySchemaField = (index: number) => {
  //   if (isReadOnly || !reply) return;
  //   const currentFields = reply.schema.fields || [];
  //   setReply({
  //     ...reply,
  //     schema: {
  //       ...reply.schema,
  //       fields: currentFields.filter((_, i) => i !== index),
  //     },
  //   });
  // };

  // const updateReplySchemaField = (index: number, field: SchemaField) => {
  //   if (isReadOnly || !reply) return;
  //   const currentFields = reply.schema.fields || [];
  //   const updated = [...currentFields];
  //   updated[index] = field;
  //   setReply({ ...reply, schema: { ...reply.schema, fields: updated } });
  // };

  // 메시지 작성 관련 함수들
  const addMessageHeader = () => {
    if (isReadOnly) return;
    setMessageHeaders([
      ...messageHeaders,
      { key: "", value: "", required: false },
    ]);
  };

  const removeMessageHeader = (index: number) => {
    if (isReadOnly) return;
    setMessageHeaders(messageHeaders.filter((_, i) => i !== index));
  };

  const updateMessageHeader = (
    index: number,
    field: "key" | "value",
    value: string
  ) => {
    if (isReadOnly) return;
    const updated = [...messageHeaders];
    updated[index] = { ...updated[index], [field]: value };
    setMessageHeaders(updated);
  };

  const addMessagePayloadField = () => {
    if (isReadOnly) return;
    const currentFields = messagePayloadSchema.fields || [];
    setMessagePayloadSchema({
      ...messagePayloadSchema,
      fields: [...currentFields, createDefaultField()],
    });
  };

  const removeMessagePayloadField = (index: number) => {
    if (isReadOnly) return;
    const currentFields = messagePayloadSchema.fields || [];
    setMessagePayloadSchema({
      ...messagePayloadSchema,
      fields: currentFields.filter((_, i) => i !== index),
    });
  };

  const updateMessagePayloadField = (index: number, field: SchemaField) => {
    if (isReadOnly) return;
    const currentFields = messagePayloadSchema.fields || [];
    const updated = [...currentFields];
    updated[index] = field;
    setMessagePayloadSchema({ ...messagePayloadSchema, fields: updated });
  };

  // 메시지 생성
  const handleCreateMessage = async () => {
    if (!messageName.trim()) {
      setAlertModal({
        isOpen: true,
        title: "입력 오류",
        message: "메시지 이름을 입력해주세요.",
        variant: "warning",
      });
      return;
    }

    try {
      const request: CreateMessageRequest = {
        messageName: messageName.trim(),
        description: messageDescription || undefined,
      };

      if (messageType === "header" && messageHeaders.length > 0) {
        const headersObj: Record<string, string> = {};
        messageHeaders.forEach((h) => {
          if (h.key) {
            headersObj[h.key] = h.value || "";
          }
        });
        request.headers = headersObj;
      } else if (messageType === "schema") {
        if (selectedMessageSchema) {
          request.payload = {
            $ref: `#/components/schemas/${selectedMessageSchema}`,
          };
        } else if (
          messagePayloadSchema.fields &&
          messagePayloadSchema.fields.length > 0
        ) {
          // 인라인 스키마로 변환
          const properties: Record<
            string,
            { type: string; description?: string }
          > = {};
          messagePayloadSchema.fields.forEach((field) => {
            const fieldType =
              field.schemaType.kind === "primitive"
                ? (field.schemaType as PrimitiveSchema).type
                : field.schemaType.kind;
            properties[field.key] = {
              type: fieldType,
              description: field.description,
            };
          });
          request.payload = {
            type: "object",
            properties,
          };
        }
      }

      await createWebSocketMessage(request);
      setAlertModal({
        isOpen: true,
        title: "생성 완료",
        message: "메시지가 생성되었습니다.",
        variant: "success",
      });

      // 폼 초기화
      setMessageName("");
      setMessageDescription("");
      setMessageType("name");
      setMessageHeaders([]);
      setMessagePayloadSchema({ type: "json", fields: [] });
      setSelectedMessageSchema(null);

      // 메시지 목록 새로고침
      await loadMessages();
    } catch (error) {
      setAlertModal({
        isOpen: true,
        title: "생성 실패",
        message: `메시지 생성 실패: ${
          error instanceof Error ? error.message : "알 수 없는 오류"
        }`,
        variant: "error",
      });
    }
  };

  // 기존 채널 선택 핸들러 (토글 방식 - 선택/선택해제)
  const handleSelectExistingChannel = (
    channel: ChannelResponse,
    type: "receiver" | "reply"
  ) => {
    const channelMessageNames = Object.keys(channel.channel.messages || {});

    if (type === "receiver") {
      // 이미 선택된 채널인지 확인 (토글)
      const isAlreadySelected =
        receiver &&
        receiver.address === channel.channel.address &&
        receiver.messages?.length === channelMessageNames.length &&
        receiver.messages.every((msg) => channelMessageNames.includes(msg));

      if (isAlreadySelected) {
        // 선택 해제
        setReceiver({
          address: "",
          headers: [],
          schema: {
            type: "json",
            fields: [],
          },
          messages: [],
        });
      } else {
        // 선택
        if (!receiver) {
          initializeReceiver();
        }
        setReceiver({
          address: channel.channel.address,
          headers: [],
          schema: {
            type: "json",
            fields: [],
          },
          messages: channelMessageNames,
        });
        setReceiverChannelMode("select");
      }
    } else if (type === "reply") {
      // 이미 선택된 채널인지 확인 (토글)
      const isAlreadySelected =
        reply &&
        reply.address === channel.channel.address &&
        reply.messages?.length === channelMessageNames.length &&
        reply.messages.every((msg) => channelMessageNames.includes(msg));

      if (isAlreadySelected) {
        // 선택 해제
        setReply({
          address: "",
          schema: {
            type: "json",
            fields: [],
          },
          messages: [],
        });
      } else {
        // 선택
        if (!reply) {
          initializeReply();
        }
        setReply({
          address: channel.channel.address,
          schema: {
            type: "json",
            fields: [],
          },
          messages: channelMessageNames,
        });
        setReplyChannelMode("select");
      }
    }
  };

  // 채널 선택 또는 생성 (현재 사용하지 않음 - 채널 선택 모드로 대체됨)
  // const handleChannelSelect = async (
  //   address: string,
  //   selectedMessages: string[],
  //   type: "receiver" | "reply"
  // ) => {
  //   if (!address.trim() || selectedMessages.length === 0) {
  //     return;
  //   }

  //   // 기존 채널 중에서 주소와 메시지가 일치하는 채널 찾기
  //   const matchingChannel = channels.find((ch) => {
  //     if (ch.channel.address !== address) return false;
  //     const channelMessageNames = Object.keys(ch.channel.messages || {});
  //     return selectedMessages.every((msg) => channelMessageNames.includes(msg));
  //   });

  //   if (matchingChannel) {
  //     // 기존 채널 사용
  //     if (type === "receiver" && receiver) {
  //       setReceiver({
  //         ...receiver,
  //         address: matchingChannel.channel.address,
  //         messages: selectedMessages,
  //       });
  //     } else if (type === "reply" && reply) {
  //       setReply({
  //         ...reply,
  //         address: matchingChannel.channel.address,
  //         messages: selectedMessages,
  //       });
  //     }
  //     alert(`기존 채널 "${matchingChannel.channelName}"을 사용합니다.`);
  //   } else {
  //     // 새 채널 생성 (현재는 주소와 메시지만 저장)
  //     if (type === "receiver" && receiver) {
  //       setReceiver({
  //         ...receiver,
  //         address: address,
  //         messages: selectedMessages,
  //       });
  //     } else if (type === "reply" && reply) {
  //       setReply({
  //         ...reply,
  //         address: address,
  //         messages: selectedMessages,
  //       });
  //     }
  //     alert("새 채널 정보가 저장되었습니다. (채널 생성 API는 추후 구현 예정)");
  //   }
  // };

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
      setAlertModal({
        isOpen: true,
        title: "타입 오류",
        message: "스키마는 object 타입만 지원됩니다.",
        variant: "warning",
      });
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
      setAlertModal({
        isOpen: true,
        title: "타입 오류",
        message: "스키마는 object 타입만 지원됩니다.",
        variant: "warning",
      });
    }
    setIsReplySchemaModalOpen(false);
  };

  // 메시지 작성용 Schema 선택 처리
  const handleMessageSchemaSelect = (selectedSchema: {
    name: string;
    fields: SchemaField[];
    type: string;
  }) => {
    if (selectedSchema.type === "object") {
      setSelectedMessageSchema(selectedSchema.name);
    } else {
      setAlertModal({
        isOpen: true,
        title: "타입 오류",
        message: "스키마는 object 타입만 지원됩니다.",
        variant: "warning",
      });
    }
    setIsMessageSchemaModalOpen(false);
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

  // Diff 알림 렌더링 (문서 뷰에서만 표시, 편집 모드에서는 숨김)
  const renderDiffNotification = () => {
    // 문서 뷰가 아닐 때는 표시하지 않음
    if (!isDocumentView) return null;
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
              <svg
                className="w-3 h-3"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
              안내사항
            </h4>
            <ul className="space-y-2">
              <li className="flex items-start gap-2 text-xs text-amber-700 dark:text-amber-400">
                <svg
                  className="w-3 h-3 text-amber-600 dark:text-amber-500 mt-0.5 flex-shrink-0"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                  />
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
                  <svg
                    className="w-3 h-3 text-amber-600 dark:text-amber-500 mt-0.5 flex-shrink-0"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                    />
                  </svg>
                  <span>
                    실제 구현에 존재하지만 명세에 없는 Channel이 있다면, 아래
                    버튼을 클릭하여 명세에 자동으로 추가할 수 있습니다.
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
              실제 구현을 명세에 반영
            </button>
          )}
        </div>
      </div>
    );
  };

  // MessagePayloadSchemaViewer 컴포넌트: payload의 schema를 표시 (Schema Reference만 표시)
  const MessagePayloadSchemaViewer = ({ schemaRef }: { schemaRef: string }) => {
    const [schemaName, setSchemaName] = useState<string | null>(null);

    useEffect(() => {
      // $ref에서 schema 이름 추출
      // 예: "#/components/schemas/ChatMessage" -> "ChatMessage"
      // 예: "ChatMessage" -> "ChatMessage"
      if (schemaRef) {
        const extractedName = schemaRef.includes("#/components/schemas/")
          ? schemaRef.replace("#/components/schemas/", "")
          : schemaRef.includes("/")
          ? schemaRef.split("/").pop() || schemaRef
          : schemaRef;
        setSchemaName(extractedName);
      }
    }, [schemaRef]);

    if (!schemaName) {
      return (
        <span className="text-sm text-gray-500 dark:text-[#8B949E] italic">
          (schema 정보 없음)
        </span>
      );
    }

    // Schema Reference만 표시 (상세 필드 목록은 표시하지 않음)
    return (
      <div className="p-3 bg-gray-50 dark:bg-[#0D1117] border border-gray-200 dark:border-[#2D333B] rounded-md">
        <div className="text-sm text-gray-600 dark:text-[#8B949E]">
          <span className="font-medium">Schema Reference:</span>{" "}
          <span className="font-mono text-gray-900 dark:text-[#E6EDF3]">
            {schemaName}
          </span>
        </div>
      </div>
    );
  };

  // 문서 형식 뷰 - REST 스타일로 재구성
  if (isDocumentView) {
    // 메시지 표시 헬퍼 함수
    const renderMessages = (channel: Receiver | Reply | null, channelType: string) => {
      if (!channel) {
        return (
          <div className="text-sm text-gray-500 dark:text-[#8B949E] italic">
            No {channelType.toLowerCase()} configuration.
          </div>
        );
      }

      return (
        <div className="space-y-4">
          {/* Address */}
          <div>
            <h3 className="text-sm font-semibold text-gray-700 dark:text-[#C9D1D9] mb-2">
              Address
            </h3>
            <div className="flex items-start gap-3 text-sm">
              <span className="font-mono text-gray-900 dark:text-[#E6EDF3]">
                {channel.address || (
                  <span className="text-gray-400 italic">(empty)</span>
                )}
              </span>
            </div>
          </div>

          {/* Messages */}
          {channel.messages && channel.messages.length > 0 ? (
            <div>
              <h3 className="text-sm font-semibold text-gray-700 dark:text-[#C9D1D9] mb-2">
                Messages
              </h3>
              <div className="space-y-4">
                {channel.messages.map((messageName, idx) => {
                  // 메시지 정보 찾기
                  const messageInfo = messages.find(
                    (msg) =>
                      msg.messageName === messageName ||
                      msg.name === messageName
                  );

                  return (
                    <div
                      key={`${channelType}-${messageName}-${idx}`}
                      className="border border-gray-200 dark:border-[#2D333B] rounded-md p-3 bg-gray-50 dark:bg-[#0D1117]"
                    >
                      <div className="flex items-center gap-2 mb-2">
                        <span className="font-mono text-sm font-semibold text-gray-900 dark:text-[#E6EDF3]">
                          {messageName}
                        </span>
                        {messageInfo?.description && (
                          <span className="text-xs text-gray-500 dark:text-[#8B949E]">
                            - {messageInfo.description}
                          </span>
                        )}
                      </div>

                      {/* 메시지 구조 표시: name/header/payload 중 실제로 있는 것만 */}
                      <div className="space-y-2 ml-4">
                        {/* Name */}
                        {messageInfo?.name && (
                          <div>
                            <span className="text-xs font-semibold text-gray-600 dark:text-[#8B949E]">
                              Name:
                            </span>
                            <span className="ml-2 text-sm text-gray-900 dark:text-[#E6EDF3]">
                              {messageInfo.name}
                            </span>
                          </div>
                        )}

                        {/* Headers */}
                        {messageInfo?.headers &&
                          Object.keys(messageInfo.headers).length > 0 && (
                            <div>
                              <span className="text-xs font-semibold text-gray-600 dark:text-[#8B949E]">
                                Headers:
                              </span>
                              <div className="mt-1 space-y-1">
                                {Object.entries(messageInfo.headers).map(
                                  ([key, value]) => (
                                    <div
                                      key={key}
                                      className="flex items-start gap-2 text-sm"
                                    >
                                      <span className="font-mono text-gray-900 dark:text-[#E6EDF3] min-w-[100px]">
                                        {key}
                                      </span>
                                      <span className="text-gray-600 dark:text-[#8B949E]">
                                        :
                                      </span>
                                      <span className="text-gray-900 dark:text-[#E6EDF3]">
                                        {String(value)}
                                      </span>
                                    </div>
                                  )
                                )}
                              </div>
                            </div>
                          )}

                        {/* Payload */}
                        {messageInfo?.payload && (() => {
                          // payload가 schema로 감싸져 있으면 사용하고, 아니면 payload 자체를 schema로 사용
                          const schema = messageInfo.payload.schema ?? messageInfo.payload;
                          
                          return (
                            <div>
                              <span className="text-xs font-semibold text-gray-600 dark:text-[#8B949E]">
                                Payload:
                              </span>
                              <div className="mt-2">
                                {schema?.$ref ? (
                                  <MessagePayloadSchemaViewer
                                    schemaRef={schema.$ref}
                                  />
                                ) : schema?.ref ? (
                                  <MessagePayloadSchemaViewer
                                    schemaRef={schema.ref}
                                  />
                                ) : schema?.type ? (
                                  <div>
                                    <span className="text-sm text-gray-900 dark:text-[#E6EDF3]">
                                      type: {schema.type}
                                    </span>
                                    {schema.properties && (
                                      <div className="mt-2">
                                        <SchemaViewer
                                          schemaType={{
                                            kind: "object",
                                            properties: Object.entries(
                                              schema.properties
                                            ).map(
                                              ([key, prop]: [string, unknown]) => {
                                                const propObj = prop as {
                                                  description?: string;
                                                  type?: string;
                                                  [key: string]: unknown;
                                                };
                                                return {
                                                  key,
                                                  description: propObj.description,
                                                  required:
                                                    schema.required?.includes(
                                                      key
                                                    ) || false,
                                                  schemaType:
                                                    parseOpenAPISchemaToSchemaType(
                                                      propObj
                                                    ),
                                                };
                                              }
                                            ),
                                          }}
                                          contentType="application/json"
                                        />
                                      </div>
                                    )}
                                  </div>
                                ) : (
                                  <span className="text-sm text-gray-500 dark:text-[#8B949E] italic">
                                    (schema 정보 없음)
                                  </span>
                                )}
                              </div>
                            </div>
                          );
                        })()}

                        {/* 메시지 정보가 없는 경우 */}
                        {!messageInfo && (
                          <div className="text-xs text-gray-500 dark:text-[#8B949E] italic">
                            메시지 정보를 불러올 수 없습니다.
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          ) : (
            <div className="text-sm text-gray-500 dark:text-[#8B949E] italic">
              No messages configured.
            </div>
          )}
        </div>
      );
    };

    return (
      <div className="w-full max-w-6xl mx-auto px-6 py-8">
        {/* Diff 알림 */}
        {renderDiffNotification()}

        {/* Progress 수동 관리 토글 (읽기 전용 모드에서만 표시, DUPLEX나 SEND인 경우만) */}
        <div className="mb-4 flex items-center justify-end gap-2 h-6">
          {isReadOnly &&
          operationInfo &&
          (operationInfo.tag === "duplex" || operationInfo.tag === "send") &&
          onProgressUpdate && (
            <>
              <span className="text-xs text-gray-600 dark:text-[#8B949E] font-medium">
                작업 완료:
              </span>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  checked={
                    localProgress !== null
                      ? localProgress === "completed"
                      : operationInfo.progress?.toLowerCase() === "completed"
                  }
                  onChange={async (e) => {
                    const newProgress = e.target.checked ? "completed" : "none";
                    // 즉시 로컬 상태 업데이트
                    setLocalProgress(newProgress);
                    try {
                      await onProgressUpdate(
                        newProgress as "none" | "completed"
                      );
                      // 성공 시 로컬 상태 유지 (operationInfo가 업데이트되면 자동으로 동기화됨)
                    } catch (error) {
                      console.error("Progress 업데이트 실패:", error);
                      // 에러 발생 시 이전 상태로 되돌리기
                      setLocalProgress(
                        operationInfo.progress?.toLowerCase() || "none"
                      );
                      setAlertModal({
                        isOpen: true,
                        title: "업데이트 실패",
                        message: `Progress 업데이트에 실패했습니다: ${
                          error instanceof Error
                            ? error.message
                            : "알 수 없는 오류"
                        }`,
                        variant: "error",
                      });
                    }
                  }}
                  className="sr-only"
                />
                <div
                  className={`w-10 h-5 rounded-full transition-colors duration-200 ease-in-out ${
                    localProgress !== null
                      ? localProgress === "completed"
                        ? "bg-[#2563EB]"
                        : "bg-gray-300 dark:bg-gray-600"
                      : operationInfo.progress?.toLowerCase() === "completed"
                      ? "bg-[#2563EB]"
                      : "bg-gray-300 dark:bg-gray-600"
                  }`}
                >
                  <div
                    className={`w-4 h-4 bg-white rounded-full shadow-sm transform transition-transform duration-200 ease-in-out ${
                      localProgress !== null
                        ? localProgress === "completed"
                          ? "translate-x-5"
                          : "translate-x-0.5"
                        : operationInfo.progress?.toLowerCase() === "completed"
                        ? "translate-x-5"
                        : "translate-x-0.5"
                    } translate-y-0.5`}
                  ></div>
                </div>
              </label>
            </>
          )}
        </div>

        {/* Protocol & Entrypoint */}
        <div className="rounded-md border border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#161B22] shadow-sm overflow-hidden mb-4">
          <div className="p-4 bg-white dark:bg-[#161B22]">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <svg
                  className="w-5 h-5 text-gray-500 dark:text-[#8B949E]"
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
                <h2 className="text-base font-semibold text-gray-900 dark:text-[#E6EDF3]">
                  Protocol & Entrypoint
                </h2>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <span
                className={`inline-flex items-center rounded-[4px] border border-gray-300 dark:border-[#2D333B] bg-white dark:bg-[#0D1117] px-2 py-[2px] text-[10px] font-mono font-semibold ${
                  protocol === "wss"
                    ? "text-[#10B981]"
                    : "text-[#2563EB]"
                }`}
              >
                {protocol?.toUpperCase() || "WS"}
              </span>
              <span className="text-sm text-gray-900 dark:text-[#E6EDF3] font-mono">
                {entryPoint || "/ws"}
              </span>
            </div>

            {/* Owner */}
            {summary && (
              <div className="mt-4">
                <h3 className="text-sm font-semibold text-gray-700 dark:text-[#C9D1D9] mb-1">
                  Owner
                </h3>
                <p className="text-sm text-gray-900 dark:text-[#E6EDF3]">
                  {summary}
                </p>
              </div>
            )}

            {/* Description */}
            {description && (
              <div className="mt-4">
                <h3 className="text-sm font-semibold text-gray-700 dark:text-[#C9D1D9] mb-1">
                  Description
                </h3>
                <p className="text-sm text-gray-900 dark:text-[#E6EDF3]">
                  {description}
                </p>
              </div>
            )}

            {/* Tags */}
            {tags && (
              <div className="mt-4">
                <h3 className="text-sm font-semibold text-gray-700 dark:text-[#C9D1D9] mb-1">
                  Tags
                </h3>
                <div className="flex flex-wrap gap-2">
                  {tags.split(",").map((tag, idx) => (
                    <span
                      key={idx}
                      className="px-2 py-1 bg-gray-100 dark:bg-[#21262D] text-gray-700 dark:text-[#C9D1D9] rounded text-xs"
                    >
                      {tag.trim()}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* Tag (operationInfo.tag) */}
            {operationInfo?.tag && !tags && (
              <div className="mt-4">
                <h3 className="text-sm font-semibold text-gray-700 dark:text-[#C9D1D9] mb-1">
                  Tag
                </h3>
                <span className="px-2 py-1 bg-gray-100 dark:bg-[#21262D] text-gray-700 dark:text-[#C9D1D9] rounded text-xs">
                  {operationInfo.tag.toUpperCase()}
                </span>
              </div>
            )}
          </div>
        </div>

        {/* Receiver & Reply 섹션 (탭 전환) */}
        {(receiver || reply) && (
          <div className="rounded-md border border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#161B22] shadow-sm overflow-hidden">
            {/* 탭 헤더 */}
            <div className="bg-gray-50 dark:bg-[#0D1117] border-b border-gray-200 dark:border-[#2D333B] px-4 pt-2">
              <div className="flex gap-0.5 -mb-px">
                <button
                  onClick={() => setWsSpecTab("receiver")}
                  className={`px-4 py-2 text-sm font-medium transition-all rounded-t-md rounded-b-none border border-b-0 focus:outline-none focus-visible:outline-none ${
                    wsSpecTab === "receiver"
                      ? "text-gray-900 dark:text-[#E6EDF3] bg-white dark:bg-[#161B22] border-gray-200 dark:border-[#2D333B] border-b-white dark:border-b-[#161B22] relative z-10"
                      : "text-gray-500 dark:text-[#8B949E] bg-transparent border-transparent hover:text-gray-700 dark:hover:text-[#C9D1D9] hover:bg-gray-100 dark:hover:bg-[#21262D]"
                  }`}
                >
                  Receiver
                </button>
                <button
                  onClick={() => setWsSpecTab("reply")}
                  className={`px-4 py-2 text-sm font-medium transition-all rounded-t-md rounded-b-none border border-b-0 focus:outline-none focus-visible:outline-none ${
                    wsSpecTab === "reply"
                      ? "text-gray-900 dark:text-[#E6EDF3] bg-white dark:bg-[#161B22] border-gray-200 dark:border-[#2D333B] border-b-white dark:border-b-[#161B22] relative z-10"
                      : "text-gray-500 dark:text-[#8B949E] bg-transparent border-transparent hover:text-gray-700 dark:hover:text-[#C9D1D9] hover:bg-gray-100 dark:hover:bg-[#21262D]"
                  }`}
                >
                  Reply
                </button>
              </div>
            </div>

            {/* 탭 내용 */}
            <div className="p-4 bg-white dark:bg-[#161B22]">
              {wsSpecTab === "receiver" ? (
                receiver ? (
                  renderMessages(receiver, "Receiver")
                ) : (
                  <div className="text-sm text-gray-500 dark:text-[#8B949E] italic text-center py-8">
                    No Receiver configuration.
                  </div>
                )
              ) : reply ? (
                renderMessages(reply, "Reply")
              ) : (
                <div className="text-sm text-gray-500 dark:text-[#8B949E] italic text-center py-8">
                  No Reply configuration.
                </div>
              )}
            </div>
          </div>
        )}

        {/* Receiver와 Reply가 모두 없는 경우 */}
        {!receiver && !reply && (
          <div className="rounded-md border border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#161B22] shadow-sm overflow-hidden">
            <div className="p-4 bg-white dark:bg-[#161B22]">
              <div className="text-sm text-gray-500 dark:text-[#8B949E] italic text-center py-8">
                No configuration available.
              </div>
            </div>
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="w-full max-w-6xl mx-auto px-6 py-8">
      {/* Diff 알림 */}
      {renderDiffNotification()}

      {/* Protocol & Pathname */}
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
          <span>Protocol & Pathname</span>
        </div>
        <p className="text-xs text-gray-600 dark:text-[#8B949E] mb-4">
          WebSocket 프로토콜과 엔드포인트 경로를 입력하세요
        </p>

        {/* Operation 정보 (읽기 전용 모드) */}
        {isReadOnly && operationInfo && (
          <div className="mb-4 p-3 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-md">
            <div className="text-xs text-blue-600 dark:text-blue-500 font-medium mb-1">
              Operation Name
            </div>
            <div className="text-sm text-blue-800 dark:text-blue-400 font-mono">
              {operationInfo.operationName}
            </div>
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
                  // 한글 검증
                  if (hasKorean(newPathname)) {
                    setEntryPointError("한글로 생성할 수 없습니다");
                  } else {
                    setEntryPointError("");
                  }
                }}
                onKeyPress={(e) => {
                  // 한글 입력 방지 (IME 입력 차단)
                  const char = e.key;
                  if (/[ㄱ-ㅎ|ㅏ-ㅣ|가-힣]/.test(char)) {
                    e.preventDefault();
                  }
                }}
                placeholder="예: /ws, /websocket, /chat"
                disabled={isReadOnly}
                className={`w-full px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border ${
                  entryPointError
                    ? "border-red-500 dark:border-red-500"
                    : "border-gray-300 dark:border-[#2D333B]"
                } text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 ${
                  entryPointError
                    ? "focus:ring-red-500 focus:border-red-500"
                    : "focus:ring-[#2563EB] focus:border-[#2563EB]"
                } text-sm font-mono ${
                  isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                }`}
              />
              {/* 한글 입력 에러 메시지 */}
              {entryPointError && (
                <p className="mt-1 text-xs text-red-500 dark:text-red-400">
                  {entryPointError}
                </p>
              )}
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
                Owner
              </label>
              <input
                type="text"
                value={summary}
                onChange={(e) => setSummary(e.target.value)}
                placeholder="예: 홍길동"
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

      {/* Progress 수동 관리 토글 (읽기 전용 모드에서만 표시, DUPLEX나 SEND인 경우만) */}
      {isReadOnly &&
        operationInfo &&
        (operationInfo.tag === "duplex" || operationInfo.tag === "send") &&
        onProgressUpdate && (
          <div className="mb-4 flex items-center justify-end gap-2">
            <span className="text-xs text-gray-600 dark:text-[#8B949E] font-medium">
              작업 완료:
              </span>
            <label className="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                checked={
                  localProgress !== null
                    ? localProgress === "completed"
                    : operationInfo.progress?.toLowerCase() === "completed"
                }
                onChange={async (e) => {
                  const newProgress = e.target.checked ? "completed" : "none";
                  // 즉시 로컬 상태 업데이트
                  setLocalProgress(newProgress);
                  try {
                    await onProgressUpdate(newProgress as "none" | "completed");
                    // 성공 시 로컬 상태 유지 (operationInfo가 업데이트되면 자동으로 동기화됨)
                  } catch (error) {
                    console.error("Progress 업데이트 실패:", error);
                    // 에러 발생 시 이전 상태로 되돌리기
                    setLocalProgress(
                      operationInfo.progress?.toLowerCase() || "none"
                    );
                    alert(
                      `Progress 업데이트에 실패했습니다: ${
                        error instanceof Error
                          ? error.message
                          : "알 수 없는 오류"
                      }`
                    );
                  }
                }}
                className="sr-only peer"
              />
              <div
                className={`w-10 h-5 rounded-full transition-colors duration-200 ease-in-out ${
                  localProgress !== null
                    ? localProgress === "completed"
                      ? "bg-[#2563EB]"
                      : "bg-gray-300 dark:bg-gray-600"
                    : operationInfo.progress?.toLowerCase() === "completed"
                    ? "bg-[#2563EB]"
                    : "bg-gray-300 dark:bg-gray-600"
                }`}
              >
                <div
                  className={`w-4 h-4 bg-white rounded-full shadow-sm transform transition-transform duration-200 ease-in-out ${
                    localProgress !== null
                      ? localProgress === "completed"
                        ? "translate-x-5"
                        : "translate-x-0.5"
                      : operationInfo.progress?.toLowerCase() === "completed"
                      ? "translate-x-5"
                      : "translate-x-0.5"
                  } translate-y-0.5`}
                ></div>
            </div>
            </label>
        </div>
      )}

      {/* 통합 탭 박스 */}
      <div className="rounded-md border border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#161B22] shadow-sm mb-6 overflow-hidden">
        {/* 탭 헤더 */}
        <div className="bg-gray-50 dark:bg-[#0D1117] border-b border-gray-200 dark:border-[#2D333B] px-4 pt-2">
          <div className="flex gap-0.5 -mb-px">
            <button
              onClick={() => setWsTab("receiver")}
              className={`px-4 py-2 text-sm font-medium transition-all rounded-t-md rounded-b-none border border-b-0 focus:outline-none focus-visible:outline-none ${
                wsTab === "receiver"
                  ? "text-gray-900 dark:text-[#E6EDF3] bg-white dark:bg-[#161B22] border-gray-200 dark:border-[#2D333B] border-b-white dark:border-b-[#161B22] relative z-10"
                  : "text-gray-500 dark:text-[#8B949E] bg-transparent border-transparent hover:text-gray-700 dark:hover:text-[#C9D1D9] hover:bg-gray-100 dark:hover:bg-[#21262D]"
              }`}
            >
              Receiver
            </button>
            <button
              onClick={() => setWsTab("reply")}
              className={`px-4 py-2 text-sm font-medium transition-all rounded-t-md rounded-b-none border border-b-0 focus:outline-none focus-visible:outline-none ${
                wsTab === "reply"
                  ? "text-gray-900 dark:text-[#E6EDF3] bg-white dark:bg-[#161B22] border-gray-200 dark:border-[#2D333B] border-b-white dark:border-b-[#161B22] relative z-10"
                  : "text-gray-500 dark:text-[#8B949E] bg-transparent border-transparent hover:text-gray-700 dark:hover:text-[#C9D1D9] hover:bg-gray-100 dark:hover:bg-[#21262D]"
              }`}
            >
              Reply
            </button>
            <button
              onClick={() => setWsTab("schema")}
              className={`px-4 py-2 text-sm font-medium transition-all rounded-t-md rounded-b-none border border-b-0 focus:outline-none focus-visible:outline-none ${
                wsTab === "schema"
                  ? "text-gray-900 dark:text-[#E6EDF3] bg-white dark:bg-[#161B22] border-gray-200 dark:border-[#2D333B] border-b-white dark:border-b-[#161B22] relative z-10"
                  : "text-gray-500 dark:text-[#8B949E] bg-transparent border-transparent hover:text-gray-700 dark:hover:text-[#C9D1D9] hover:bg-gray-100 dark:hover:bg-[#21262D]"
              }`}
            >
              Schema
            </button>
            <button
              onClick={() => setWsTab("message")}
              className={`px-4 py-2 text-sm font-medium transition-all rounded-t-md rounded-b-none border border-b-0 focus:outline-none focus-visible:outline-none ${
                wsTab === "message"
                  ? "text-gray-900 dark:text-[#E6EDF3] bg-white dark:bg-[#161B22] border-gray-200 dark:border-[#2D333B] border-b-white dark:border-b-[#161B22] relative z-10"
                  : "text-gray-500 dark:text-[#8B949E] bg-transparent border-transparent hover:text-gray-700 dark:hover:text-[#C9D1D9] hover:bg-gray-100 dark:hover:bg-[#21262D]"
              }`}
            >
              Message
            </button>
          </div>
        </div>

        {/* 탭 내용 */}
        <div className="p-4 bg-white dark:bg-[#161B22]">
          {wsTab === "receiver" && (
            <div>
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h3 className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3]">
                    Receiver
                  </h3>
                  <p className="text-xs text-gray-600 dark:text-[#8B949E] mt-1">
                    메시지와 주소를 입력하면 채널을 생성하거나 기존 채널을
                    선택할 수 있습니다.
                  </p>
                </div>
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
                        className="px-3 py-2 bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded-md text-sm font-medium transition-all active:translate-y-[1px] focus:outline-none focus-visible:outline-none flex items-center gap-2"
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
                  {/* 채널 선택 모드 선택 */}
                  <div>
                    <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                      채널 선택 방식
                    </label>
                    <div className="flex gap-2">
                      <button
                        onClick={() => {
                          setReceiverChannelMode("select");
                          if (receiver) {
                            setReceiver({
                              ...receiver,
                              address: "",
                              messages: [],
                            });
                          }
                        }}
                        disabled={isReadOnly}
                        className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                          receiverChannelMode === "select"
                            ? "bg-[#2563EB] text-white"
                            : "bg-gray-100 dark:bg-[#21262D] text-gray-700 dark:text-[#C9D1D9] hover:bg-gray-200 dark:hover:bg-[#30363D]"
                        } ${isReadOnly ? "opacity-60 cursor-not-allowed" : ""}`}
                      >
                        기존 채널 선택
                      </button>
                      <button
                        onClick={() => {
                          setReceiverChannelMode("create");
                          if (receiver) {
                            setReceiver({
                              ...receiver,
                              address: "",
                              messages: [],
                            });
                          }
                        }}
                        disabled={isReadOnly}
                        className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                          receiverChannelMode === "create"
                            ? "bg-[#2563EB] text-white"
                            : "bg-gray-100 dark:bg-[#21262D] text-gray-700 dark:text-[#C9D1D9] hover:bg-gray-200 dark:hover:bg-[#30363D]"
                        } ${isReadOnly ? "opacity-60 cursor-not-allowed" : ""}`}
                      >
                        새 채널 생성
                      </button>
                    </div>
                  </div>

                  {/* 기존 채널 선택 모드 */}
                  {receiverChannelMode === "select" && (
                    <div>
                      <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                        기존 채널 선택
                      </label>
                      {channels.length > 0 ? (
                        <div className="max-h-64 overflow-y-auto space-y-2 border border-gray-200 dark:border-[#2D333B] rounded-md p-3 bg-gray-50 dark:bg-[#0D1117]">
                          {channels.map((ch) => {
                            const channelMessageNames = Object.keys(
                              ch.channel.messages || {}
                            );
                            const isSelected =
                              receiver &&
                              receiver.address === ch.channel.address &&
                              receiver.messages?.length ===
                                channelMessageNames.length &&
                              receiver.messages.every((msg) =>
                                channelMessageNames.includes(msg)
                              );

                            return (
                              <div
                                key={ch.channelName}
                                onClick={() =>
                                  !isReadOnly &&
                                  handleSelectExistingChannel(ch, "receiver")
                                }
                                className={`text-sm p-3 border rounded cursor-pointer transition-colors ${
                                  isSelected
                                    ? "bg-blue-50 dark:bg-blue-900/20 border-blue-300 dark:border-blue-700"
                                    : "bg-white dark:bg-[#161B22] border-gray-200 dark:border-[#2D333B] hover:bg-gray-100 dark:hover:bg-[#21262D]"
                                } ${
                                  isReadOnly
                                    ? "cursor-not-allowed opacity-60"
                                    : ""
                                }`}
                              >
                                <div className="font-mono text-gray-800 dark:text-[#E6EDF3] font-medium">
                                  {ch.channelName}
                                </div>
                                <div className="text-xs text-gray-600 dark:text-[#8B949E] mt-1">
                                  주소: {ch.channel.address}
                                </div>
                                <div className="text-xs text-gray-600 dark:text-[#8B949E] mt-1">
                                  메시지:{" "}
                                  {channelMessageNames.join(", ") || "없음"}
                                </div>
                                {isSelected && (
                                  <div className="text-xs text-blue-600 dark:text-blue-400 mt-2 font-medium">
                                    ✓ 선택됨
                                  </div>
                                )}
                              </div>
                            );
                          })}
                        </div>
                      ) : (
                        <div className="text-center py-6 border border-gray-200 dark:border-[#2D333B] rounded-md bg-gray-50 dark:bg-[#0D1117]">
                          <p className="text-sm text-gray-500 dark:text-[#8B949E]">
                            기존 채널이 없습니다
                          </p>
                          <p className="text-xs text-gray-400 dark:text-[#6E7681] mt-1">
                            "새 채널 생성"을 선택하여 채널을 생성하세요
                          </p>
                        </div>
                      )}
                    </div>
                  )}

                  {/* 새 채널 생성 모드 */}
                  {receiverChannelMode === "create" && (
                    <>
                      {/* 주소 */}
                      <div>
                        <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                          주소 <span className="text-red-500">*</span>
                        </label>
                        <input
                          type="text"
                          value={receiver.address}
                          onChange={(e) =>
                            setReceiver({
                              ...receiver,
                              address: e.target.value,
                            })
                          }
                          placeholder="예: /chat/{roomId}"
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
                            📨 Messages{" "}
                            {messages.length > 0 && `(${messages.length})`}
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
                              const messageName =
                                msg.messageName || msg.name || "Unnamed";
                              const isSelected =
                                receiver.messages?.length === 1 &&
                                receiver.messages[0] === messageName;

                              return (
                                <label
                                  key={`${messageName}-${idx}`}
                                  className={`flex items-center gap-2 p-2.5 rounded-md cursor-pointer transition-colors ${
                                    isSelected
                                      ? "bg-blue-100 dark:bg-blue-900/30 border border-blue-300 dark:border-blue-700"
                                      : "bg-white dark:bg-[#161B22] border border-gray-200 dark:border-[#2D333B] hover:bg-gray-100 dark:hover:bg-[#21262D]"
                                  } ${
                                    isReadOnly
                                      ? "opacity-60 cursor-not-allowed"
                                      : ""
                                  }`}
                                >
                                  <input
                                    type="radio"
                                    name="receiver-message"
                                    checked={isSelected}
                                    onChange={() => {
                                      if (isReadOnly) return;
                                      setReceiver({
                                        ...receiver,
                                        messages: [messageName],
                                      });
                                    }}
                                    disabled={isReadOnly}
                                    className="w-4 h-4 text-blue-600 focus:ring-blue-500 flex-shrink-0"
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
                                Message 탭에서 먼저 메시지를 생성해주세요
                              </p>
                            </div>
                          )}
                        </div>
                      </div>
                    </>
                  )}
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
                <div>
                  <h3 className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3]">
                    Reply
                  </h3>
                  <p className="text-xs text-gray-600 dark:text-[#8B949E] mt-1">
                    메시지와 주소를 입력하면 채널을 생성하거나 기존 채널을
                    선택할 수 있습니다.
                  </p>
                </div>
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
                  {/* 채널 선택 모드 선택 */}
                  <div>
                    <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                      채널 선택 방식
                    </label>
                    <div className="flex gap-2">
                      <button
                        onClick={() => {
                          setReplyChannelMode("select");
                          if (reply) {
                            setReply({ ...reply, address: "", messages: [] });
                          }
                        }}
                        disabled={isReadOnly}
                        className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                          replyChannelMode === "select"
                            ? "bg-[#2563EB] text-white"
                            : "bg-gray-100 dark:bg-[#21262D] text-gray-700 dark:text-[#C9D1D9] hover:bg-gray-200 dark:hover:bg-[#30363D]"
                        } ${isReadOnly ? "opacity-60 cursor-not-allowed" : ""}`}
                      >
                        기존 채널 선택
                      </button>
                      <button
                        onClick={() => {
                          setReplyChannelMode("create");
                          if (reply) {
                            setReply({ ...reply, address: "", messages: [] });
                          }
                        }}
                        disabled={isReadOnly}
                        className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                          replyChannelMode === "create"
                            ? "bg-[#2563EB] text-white"
                            : "bg-gray-100 dark:bg-[#21262D] text-gray-700 dark:text-[#C9D1D9] hover:bg-gray-200 dark:hover:bg-[#30363D]"
                        } ${isReadOnly ? "opacity-60 cursor-not-allowed" : ""}`}
                      >
                        새 채널 생성
                      </button>
                    </div>
                  </div>

                  {/* 기존 채널 선택 모드 */}
                  {replyChannelMode === "select" && (
                    <div>
                      <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                        기존 채널 선택
                      </label>
                      {channels.length > 0 ? (
                        <div className="max-h-64 overflow-y-auto space-y-2 border border-gray-200 dark:border-[#2D333B] rounded-md p-3 bg-gray-50 dark:bg-[#0D1117]">
                          {channels.map((ch) => {
                            const channelMessageNames = Object.keys(
                              ch.channel.messages || {}
                            );
                            const isSelected =
                              reply &&
                              reply.address === ch.channel.address &&
                              reply.messages?.length ===
                                channelMessageNames.length &&
                              reply.messages.every((msg) =>
                                channelMessageNames.includes(msg)
                              );

                            return (
                              <div
                                key={ch.channelName}
                                onClick={() =>
                                  !isReadOnly &&
                                  handleSelectExistingChannel(ch, "reply")
                                }
                                className={`text-sm p-3 border rounded cursor-pointer transition-colors ${
                                  isSelected
                                    ? "bg-emerald-50 dark:bg-emerald-900/20 border-emerald-300 dark:border-emerald-700"
                                    : "bg-white dark:bg-[#161B22] border-gray-200 dark:border-[#2D333B] hover:bg-gray-100 dark:hover:bg-[#21262D]"
                                } ${
                                  isReadOnly
                                    ? "cursor-not-allowed opacity-60"
                                    : ""
                                }`}
                              >
                                <div className="font-mono text-gray-800 dark:text-[#E6EDF3] font-medium">
                                  {ch.channelName}
                                </div>
                                <div className="text-xs text-gray-600 dark:text-[#8B949E] mt-1">
                                  주소: {ch.channel.address}
                                </div>
                                <div className="text-xs text-gray-600 dark:text-[#8B949E] mt-1">
                                  메시지:{" "}
                                  {channelMessageNames.join(", ") || "없음"}
                                </div>
                                {isSelected && (
                                  <div className="text-xs text-emerald-600 dark:text-emerald-400 mt-2 font-medium">
                                    ✓ 선택됨
                                  </div>
                                )}
                              </div>
                            );
                          })}
                        </div>
                      ) : (
                        <div className="text-center py-6 border border-gray-200 dark:border-[#2D333B] rounded-md bg-gray-50 dark:bg-[#0D1117]">
                          <p className="text-sm text-gray-500 dark:text-[#8B949E]">
                            기존 채널이 없습니다
                          </p>
                          <p className="text-xs text-gray-400 dark:text-[#6E7681] mt-1">
                            "새 채널 생성"을 선택하여 채널을 생성하세요
                          </p>
                        </div>
                      )}
                    </div>
                  )}

                  {/* 새 채널 생성 모드 */}
                  {replyChannelMode === "create" && (
                    <>
                      {/* 주소 */}
                      <div>
                        <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                          주소 <span className="text-red-500">*</span>
                        </label>
                        <input
                          type="text"
                          value={reply.address}
                          onChange={(e) =>
                            setReply({ ...reply, address: e.target.value })
                          }
                          placeholder="예: /chat/{roomId}"
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
                            📨 Messages{" "}
                            {messages.length > 0 && `(${messages.length})`}
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
                              const messageName =
                                msg.messageName || msg.name || "Unnamed";
                              const isSelected =
                                reply.messages?.length === 1 &&
                                reply.messages[0] === messageName;

                              return (
                                <label
                                  key={`${messageName}-${idx}`}
                                  className={`flex items-center gap-2 p-2.5 rounded-md cursor-pointer transition-colors ${
                                    isSelected
                                      ? "bg-emerald-100 dark:bg-emerald-900/30 border border-emerald-300 dark:border-emerald-700"
                                      : "bg-white dark:bg-[#161B22] border border-gray-200 dark:border-[#2D333B] hover:bg-gray-100 dark:hover:bg-[#21262D]"
                                  } ${
                                    isReadOnly
                                      ? "opacity-60 cursor-not-allowed"
                                      : ""
                                  }`}
                                >
                                  <input
                                    type="radio"
                                    name="reply-message"
                                    checked={isSelected}
                                    onChange={() => {
                                      if (isReadOnly) return;
                                      setReply({
                                        ...reply,
                                        messages: [messageName],
                                      });
                                    }}
                                    disabled={isReadOnly}
                                    className="w-4 h-4 text-emerald-600 focus:ring-emerald-500 flex-shrink-0"
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
                                Message 탭에서 먼저 메시지를 생성해주세요
                              </p>
                            </div>
                          )}
                        </div>
                      </div>
                    </>
                  )}
                </div>
              ) : (
                <div className="text-center py-8 text-gray-500 dark:text-gray-400 text-sm">
                  <p>Reply가 없습니다. "추가" 버튼을 클릭하여 추가하세요.</p>
                </div>
              )}
            </div>
          )}

          {wsTab === "schema" && (
            <SchemaCard isReadOnly={isReadOnly} protocol="WebSocket" />
          )}

          {wsTab === "message" && (
            <div className="space-y-4">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3]">
                  메시지 작성
                </h3>
              </div>

              {/* 메시지 이름 */}
              <div>
                <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                  메시지 이름 <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={messageName}
                  onChange={(e) => setMessageName(e.target.value)}
                  placeholder="예: ChatMessage, NotificationMessage"
                  disabled={isReadOnly}
                  className={`w-full px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm ${
                    isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                  }`}
                />
              </div>

              {/* 메시지 설명 */}
              <div>
                <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                  설명
                </label>
                <input
                  type="text"
                  value={messageDescription}
                  onChange={(e) => setMessageDescription(e.target.value)}
                  placeholder="메시지에 대한 설명을 입력하세요"
                  disabled={isReadOnly}
                  className={`w-full px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm ${
                    isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                  }`}
                />
              </div>

              {/* 메시지 타입 선택 */}
              <div>
                <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                  메시지 타입 선택
                </label>
                <div className="flex gap-2">
                  <button
                    onClick={() => setMessageType("name")}
                    disabled={isReadOnly}
                    className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                      messageType === "name"
                        ? "bg-[#2563EB] text-white"
                        : "bg-gray-100 dark:bg-[#21262D] text-gray-700 dark:text-[#C9D1D9] hover:bg-gray-200 dark:hover:bg-[#30363D]"
                    } ${isReadOnly ? "opacity-60 cursor-not-allowed" : ""}`}
                  >
                    Name
                  </button>
                  <button
                    onClick={() => setMessageType("header")}
                    disabled={isReadOnly}
                    className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                      messageType === "header"
                        ? "bg-[#2563EB] text-white"
                        : "bg-gray-100 dark:bg-[#21262D] text-gray-700 dark:text-[#C9D1D9] hover:bg-gray-200 dark:hover:bg-[#30363D]"
                    } ${isReadOnly ? "opacity-60 cursor-not-allowed" : ""}`}
                  >
                    Header
                  </button>
                  <button
                    onClick={() => setMessageType("schema")}
                    disabled={isReadOnly}
                    className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                      messageType === "schema"
                        ? "bg-[#2563EB] text-white"
                        : "bg-gray-100 dark:bg-[#21262D] text-gray-700 dark:text-[#C9D1D9] hover:bg-gray-200 dark:hover:bg-[#30363D]"
                    } ${isReadOnly ? "opacity-60 cursor-not-allowed" : ""}`}
                  >
                    Payload
                  </button>
                </div>
              </div>

              {/* Header 타입 선택 시 */}
              {messageType === "header" && (
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E]">
                      Headers
                    </label>
                    {!isReadOnly && (
                      <button
                        onClick={addMessageHeader}
                        className="text-[#2563EB] hover:text-[#1E40AF] text-xs font-medium"
                      >
                        + Add Header
                      </button>
                    )}
                  </div>
                  <div className="space-y-2">
                    {messageHeaders.map((header, index) => (
                      <div key={index} className="flex gap-2">
                        <input
                          type="text"
                          value={header.key}
                          onChange={(e) =>
                            updateMessageHeader(index, "key", e.target.value)
                          }
                          placeholder="Header 이름"
                          disabled={isReadOnly}
                          className={`flex-1 px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm ${
                            isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                          }`}
                        />
                        <input
                          type="text"
                          value={header.value}
                          onChange={(e) =>
                            updateMessageHeader(index, "value", e.target.value)
                          }
                          placeholder="Header 값"
                          disabled={isReadOnly}
                          className={`flex-1 px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm ${
                            isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                          }`}
                        />
                        {!isReadOnly && (
                          <button
                            onClick={() => removeMessageHeader(index)}
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
              )}

              {/* Schema 타입 선택 시 */}
              {messageType === "schema" && (
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E]">
                      Payload Schema
                    </label>
                    <div className="flex gap-2">
                      {!isReadOnly && (
                        <>
                          <button
                            onClick={() => setIsMessageSchemaModalOpen(true)}
                            className="text-[#2563EB] hover:text-[#1E40AF] text-xs font-medium"
                          >
                            Schema 선택
                          </button>
                          <button
                            onClick={addMessagePayloadField}
                            className="text-[#2563EB] hover:text-[#1E40AF] text-xs font-medium"
                          >
                            + Add Field
                          </button>
                        </>
                      )}
                    </div>
                  </div>

                  {selectedMessageSchema && (
                    <div className="mb-2 p-2 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-md">
                      <div className="flex items-center justify-between">
                        <span className="text-xs text-blue-700 dark:text-blue-300 font-medium">
                          Schema: {getShortSchemaName(selectedMessageSchema)}
                        </span>
                        {!isReadOnly && (
                          <button
                            onClick={() => setSelectedMessageSchema(null)}
                            className="text-blue-500 hover:text-blue-700 text-xs"
                          >
                            제거
                          </button>
                        )}
                      </div>
                    </div>
                  )}

                  {!selectedMessageSchema && (
                    <div className="space-y-2">
                      {messagePayloadSchema.fields &&
                      messagePayloadSchema.fields.length > 0 ? (
                        messagePayloadSchema.fields.map((field, index) => (
                          <SchemaFieldEditor
                            key={index}
                            field={field}
                            onChange={(newField) =>
                              updateMessagePayloadField(index, newField)
                            }
                            onRemove={() => removeMessagePayloadField(index)}
                            isReadOnly={isReadOnly}
                            allowFileType={false}
                            allowMockExpression={true}
                          />
                        ))
                      ) : (
                        <p className="text-xs text-gray-500 dark:text-gray-400 text-center py-2">
                          Schema 필드가 없습니다. "+ Add Field"를 클릭하여
                          추가하거나 Schema를 선택하세요.
                        </p>
                      )}
                    </div>
                  )}
                </div>
              )}

              {/* 메시지 생성 버튼 */}
              {!isReadOnly && (
                <div className="pt-4">
                  <button
                    onClick={handleCreateMessage}
                    className="w-full px-4 py-2 bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded-md text-sm font-medium transition-colors"
                  >
                    메시지 생성
                  </button>
                </div>
              )}

              {/* 생성된 메시지 목록 */}
              <div className="mt-6">
                <h4 className="text-xs font-semibold text-gray-700 dark:text-[#C9D1D9] mb-2">
                  생성된 메시지 목록 ({messages.length})
                </h4>
                <div className="space-y-2 max-h-48 overflow-y-auto border border-gray-200 dark:border-[#2D333B] rounded-md p-3 bg-gray-50 dark:bg-[#0D1117]">
                  {messages.length > 0 ? (
                    messages.map((msg, idx) => {
                      const messageName =
                        msg.messageName || msg.name || "Unnamed";
                      return (
                        <div
                          key={`${messageName}-${idx}`}
                          className="p-2.5 rounded-md bg-white dark:bg-[#161B22] border border-gray-200 dark:border-[#2D333B]"
                        >
                          <div className="text-sm text-gray-900 dark:text-[#E6EDF3] font-medium">
                            {messageName}
                          </div>
                          {msg.description && (
                            <div className="text-xs text-gray-500 dark:text-[#8B949E] mt-0.5">
                              {msg.description}
                            </div>
                          )}
                        </div>
                      );
                    })
                  ) : (
                    <div className="text-center py-6">
                      <p className="text-sm text-gray-500 dark:text-[#8B949E]">
                        생성된 메시지가 없습니다
                      </p>
                    </div>
                  )}
                </div>
              </div>
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

      {/* 메시지 작성용 Schema 선택 모달 */}
      <SchemaModal
        isOpen={isMessageSchemaModalOpen}
        onClose={() => setIsMessageSchemaModalOpen(false)}
        onSelect={handleMessageSchemaSelect}
        schemas={schemas}
        setSchemas={setSchemas}
        protocol="WebSocket"
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
