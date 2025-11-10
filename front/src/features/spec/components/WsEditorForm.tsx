import { useState } from "react";
import { WsReceiverForm } from "./WsReceiverForm";
import { WsReplyForm } from "./WsReplyForm";
import { SchemaCard } from "./SchemaCard";
import type { RequestBody } from "../types/schema.types";
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
  subprotocol: string;
  setSubprotocol: (subprotocol: string) => void;
  protocol: "ws://" | "wss://";
  setProtocol: (protocol: "ws://" | "wss://") => void;
  receivers: Receiver[];
  setReceivers: (receivers: Receiver[]) => void;
  replies: Reply[];
  setReplies: (replies: Reply[]) => void;
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
  subprotocol,
  setSubprotocol,
  protocol,
  setProtocol,
  receivers,
  setReceivers,
  replies,
  setReplies,
  isReadOnly = false,
}: WsEditorFormProps) {
  // accept-version 헤더 항상 자동 추가 (STOMP 필수 헤더)
  const addReceiver = () => {
    if (isReadOnly) return;
    
    // subprotocol에서 버전 추출 (v11.stomp -> 1.1)
    const version = subprotocol.replace("v", "").replace(".stomp", "");
    const defaultHeaders: KeyValuePair[] = [
      {
        key: "accept-version",
        value: version,
        required: true,
        description: "STOMP 프로토콜 버전 (필수)",
      },
    ];
    
    setReceivers([
      ...receivers,
      {
        address: "",
        headers: defaultHeaders,
        schema: {
          type: "json",
          fields: [],
        },
      },
    ]);
  };

  const removeReceiver = (index: number) => {
    if (isReadOnly) return;
    setReceivers(receivers.filter((_, i) => i !== index));
  };

  const updateReceiver = (index: number, receiver: Receiver) => {
    if (isReadOnly) return;
    const updated = [...receivers];
    updated[index] = receiver;
    setReceivers(updated);
  };

  const addReply = () => {
    if (isReadOnly) return;
    setReplies([
      ...replies,
      {
        address: "",
        schema: {
          type: "json",
          fields: [],
        },
      },
    ]);
  };

  const removeReply = (index: number) => {
    if (isReadOnly) return;
    setReplies(replies.filter((_, i) => i !== index));
  };

  const updateReply = (index: number, reply: Reply) => {
    if (isReadOnly) return;
    const updated = [...replies];
    updated[index] = reply;
    setReplies(updated);
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
              placeholder="예: /ws, /ws/chat, /ws/notifications"
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

      {/* Connection Info (STOMP) */}
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
              d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1"
            />
          </svg>
          <span>Connection Info (STOMP)</span>
        </div>
        <p className="text-xs text-gray-600 dark:text-[#8B949E] mb-4">
          STOMP 프로토콜 연결 정보를 설정하세요
        </p>

        <div className="space-y-4">
          {/* Protocol & Subprotocol */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                Protocol
              </label>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setProtocol("ws://")}
                  disabled={isReadOnly}
                  className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                    protocol === "ws://"
                      ? "bg-[#2563EB] text-white"
                      : "bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-700 dark:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#161B22]"
                  } ${isReadOnly ? "opacity-60 cursor-not-allowed" : ""}`}
                >
                  ws://
                </button>
                <button
                  type="button"
                  onClick={() => setProtocol("wss://")}
                  disabled={isReadOnly}
                  className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                    protocol === "wss://"
                      ? "bg-[#2563EB] text-white"
                      : "bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-700 dark:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#161B22]"
                  } ${isReadOnly ? "opacity-60 cursor-not-allowed" : ""}`}
                >
                  wss://
                </button>
              </div>
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                Subprotocol (STOMP Version)
              </label>
              <select
                value={subprotocol}
                onChange={(e) => setSubprotocol(e.target.value)}
                disabled={isReadOnly}
                className={`w-full px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm ${
                  isReadOnly ? "opacity-60 cursor-not-allowed" : ""
                }`}
              >
                <option value="v10.stomp">v10.stomp (STOMP 1.0)</option>
                <option value="v11.stomp">v11.stomp (STOMP 1.1)</option>
                <option value="v12.stomp">v12.stomp (STOMP 1.2)</option>
              </select>
            </div>
          </div>

          {/* Message Type (Fixed) */}
          <div>
            <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
              Message Type
            </label>
            <div className="px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-sm text-gray-500 dark:text-[#8B949E]">
              text (STOMP는 텍스트 기반 프로토콜입니다)
            </div>
          </div>
        </div>
      </div>

      {/* Receivers & Replies */}
      <div className="mb-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-[#E6EDF3]">
            Receivers & Replies
          </h2>
          {!isReadOnly && (
            <div className="flex items-center gap-2">
              <button
                onClick={addReceiver}
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
                Receiver 추가
              </button>
              <button
                onClick={addReply}
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
                Reply 추가
              </button>
            </div>
          )}
        </div>

        {/* Receivers */}
        <div className="mb-6">
          <h3 className="text-sm font-semibold text-gray-700 dark:text-[#C9D1D9] mb-3">
            Receivers
          </h3>
          <div className="space-y-4">
            {receivers.map((receiver, index) => (
              <WsReceiverForm
                key={index}
                address={receiver.address}
                setAddress={(address) =>
                  updateReceiver(index, { ...receiver, address })
                }
                headers={receiver.headers}
                setHeaders={(headers) =>
                  updateReceiver(index, { ...receiver, headers })
                }
                schema={receiver.schema}
                setSchema={(schema) =>
                  updateReceiver(index, { ...receiver, schema })
                }
                onRemove={() => removeReceiver(index)}
                isReadOnly={isReadOnly}
              />
            ))}
            {receivers.length === 0 && (
              <div className="text-center py-6 text-gray-500 dark:text-gray-400 text-sm border border-gray-200 dark:border-[#2D333B] rounded-md bg-gray-50 dark:bg-[#0D1117]">
                <p>Receiver가 없습니다. "Receiver 추가" 버튼을 클릭하여 추가하세요.</p>
              </div>
            )}
          </div>
        </div>

        {/* Replies */}
        <div>
          <h3 className="text-sm font-semibold text-gray-700 dark:text-[#C9D1D9] mb-3">
            Replies
          </h3>
          <div className="space-y-4">
            {replies.map((reply, index) => (
              <WsReplyForm
                key={index}
                address={reply.address}
                setAddress={(address) =>
                  updateReply(index, { ...reply, address })
                }
                schema={reply.schema}
                setSchema={(schema) =>
                  updateReply(index, { ...reply, schema })
                }
                onRemove={() => removeReply(index)}
                isReadOnly={isReadOnly}
              />
            ))}
            {replies.length === 0 && (
              <div className="text-center py-6 text-gray-500 dark:text-gray-400 text-sm border border-gray-200 dark:border-[#2D333B] rounded-md bg-gray-50 dark:bg-[#0D1117]">
                <p>Reply가 없습니다. "Reply 추가" 버튼을 클릭하여 추가하세요.</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Schema Card */}
      <div className="mt-6">
        <SchemaCard isReadOnly={isReadOnly} />
      </div>
    </div>
  );
}

