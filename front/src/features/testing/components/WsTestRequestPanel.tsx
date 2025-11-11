import { useState, useEffect, useRef } from "react";
import { useTestingStore } from "../store/testing.store";
import { useSidebarStore } from "@/features/sidebar/store/sidebar.store";

export function WsTestRequestPanel() {
  const {
    wsConnectionStatus,
    setWsConnectionStatus,
    addWsMessage,
    clearWsMessages,
    updateWsStats,
    setWsConnectionStartTime,
    wsConnectionStartTime,
    wsStats,
  } = useTestingStore();
  const { selectedEndpoint } = useSidebarStore();

  const [entryPoint, setEntryPoint] = useState("");
  const [receiverAddress, setReceiverAddress] = useState("");
  const [messageContent, setMessageContent] = useState("");
  const [headers, setHeaders] = useState<Array<{ key: string; value: string }>>([
    { key: "accept-version", value: "1.1" },
  ]);

  const wsRef = useRef<WebSocket | null>(null);
  const stompClientRef = useRef<any>(null);

  // 엔드포인트 선택 시 Entry Point 로드
  useEffect(() => {
    if (selectedEndpoint) {
      // TODO: WebSocket API spec을 가져와서 entryPoint, receiver address 등을 설정
      // 현재는 placeholder로 처리
      setEntryPoint("");
      setReceiverAddress("");
    } else {
      setEntryPoint("");
      setReceiverAddress("");
    }
  }, [selectedEndpoint]);

  // 연결 상태에 따른 통계 업데이트
  useEffect(() => {
    if (wsConnectionStatus === "connected" && wsConnectionStartTime) {
      const interval = setInterval(() => {
        const duration = Date.now() - wsConnectionStartTime;
        updateWsStats({ connectionDuration: duration });
      }, 1000);
      return () => clearInterval(interval);
    }
  }, [wsConnectionStatus, wsConnectionStartTime, updateWsStats]);

  const handleConnect = async () => {
    if (!entryPoint) {
      alert("Entry Point를 입력해주세요.");
      return;
    }

    try {
      setWsConnectionStatus("connecting");
      clearWsMessages();
      updateWsStats({
        totalSent: 0,
        totalReceived: 0,
        averageResponseTime: null,
        connectionDuration: null,
      });

      // WebSocket 연결
      const ws = new WebSocket(entryPoint);
      wsRef.current = ws;

      ws.onopen = () => {
        setWsConnectionStatus("connected");
        setWsConnectionStartTime(Date.now());

        // STOMP 연결 (STOMP 1.1)
        // TODO: 실제 STOMP 클라이언트 라이브러리 사용 (예: @stomp/stompjs)
        // 현재는 기본 WebSocket으로 시뮬레이션
        console.log("WebSocket 연결됨");
      };

      ws.onmessage = (event) => {
        const message = {
          id: `msg-${Date.now()}-${Math.random()}`,
          timestamp: Date.now(),
          direction: "received" as const,
          address: receiverAddress || "/",
          content: event.data,
        };
        addWsMessage(message);
        updateWsStats({
          totalReceived: wsStats.totalReceived + 1,
        });
      };

      ws.onerror = (error) => {
        console.error("WebSocket 에러:", error);
        setWsConnectionStatus("disconnected");
        alert("WebSocket 연결 중 오류가 발생했습니다.");
      };

      ws.onclose = () => {
        setWsConnectionStatus("disconnected");
        setWsConnectionStartTime(null);
        updateWsStats({ connectionDuration: null });
      };
    } catch (error) {
      console.error("연결 실패:", error);
      setWsConnectionStatus("disconnected");
      alert("연결에 실패했습니다.");
    }
  };

  const handleDisconnect = () => {
    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
    }
    if (stompClientRef.current) {
      // TODO: STOMP 클라이언트 disconnect
      stompClientRef.current = null;
    }
    setWsConnectionStatus("disconnected");
    setWsConnectionStartTime(null);
  };

  const handleSendMessage = () => {
    if (!receiverAddress) {
      alert("Receiver Address를 입력해주세요.");
      return;
    }

    if (!messageContent.trim()) {
      alert("메시지 내용을 입력해주세요.");
      return;
    }

    if (wsConnectionStatus !== "connected" || !wsRef.current) {
      alert("먼저 연결을 시도해주세요.");
      return;
    }

    try {
      // STOMP 메시지 전송
      // TODO: 실제 STOMP SEND 프레임 전송
      const sendTime = Date.now();
      wsRef.current.send(
        JSON.stringify({
          command: "SEND",
          destination: receiverAddress,
          headers: Object.fromEntries(
            headers.map((h) => [h.key, h.value])
          ),
          body: messageContent,
        })
      );

      // 전송된 메시지 로그 추가
      const message = {
        id: `msg-${sendTime}-${Math.random()}`,
        timestamp: sendTime,
        direction: "sent" as const,
        address: receiverAddress,
        content: messageContent,
      };
      addWsMessage(message);
      updateWsStats({
        totalSent: wsStats.totalSent + 1,
      });

      setMessageContent("");
    } catch (error) {
      console.error("메시지 전송 실패:", error);
      alert("메시지 전송에 실패했습니다.");
    }
  };

  const addHeader = () => {
    setHeaders([...headers, { key: "", value: "" }]);
  };

  const updateHeader = (index: number, key: string, value: string) => {
    const newHeaders = [...headers];
    newHeaders[index] = { key, value };
    setHeaders(newHeaders);
  };

  const removeHeader = (index: number) => {
    setHeaders(headers.filter((_, i) => i !== index));
  };

  return (
    <div className="rounded-md border border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#161B22] p-4 shadow-sm">
      <div className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3] mb-4 flex items-center gap-2">
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
            d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"
          />
        </svg>
        <span>WebSocket Request</span>
      </div>

      {/* Entry Point */}
      <div className="mb-4">
        <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
          Entry Point
        </label>
        <div className="flex gap-2">
          <input
            type="text"
            value={entryPoint}
            onChange={(e) => setEntryPoint(e.target.value)}
            placeholder="ws://localhost:8080/ws 또는 wss://example.com/ws"
            disabled={wsConnectionStatus === "connected"}
            className="flex-1 px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-500 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm font-mono disabled:opacity-50 disabled:cursor-not-allowed"
          />
          {wsConnectionStatus === "disconnected" ? (
            <button
              onClick={handleConnect}
              className="px-4 py-2 bg-green-500 hover:bg-green-600 text-white rounded-md transition-colors text-sm font-medium flex items-center gap-2"
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
                  d="M5 13l4 4L19 7"
                />
              </svg>
              Connect
            </button>
          ) : (
            <button
              onClick={handleDisconnect}
              className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-md transition-colors text-sm font-medium flex items-center gap-2"
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
              Disconnect
            </button>
          )}
        </div>
      </div>

      {/* Headers */}
      <div className="mb-4">
        <div className="flex items-center justify-between mb-2">
          <label className="text-xs font-medium text-gray-600 dark:text-[#8B949E]">
            Headers
          </label>
          <button
            onClick={addHeader}
            disabled={wsConnectionStatus === "connected"}
            className="text-xs px-2 py-1 bg-[#2563EB] hover:bg-[#1E40AF] disabled:bg-gray-400 disabled:cursor-not-allowed text-white rounded-md transition-colors"
          >
            + Add
          </button>
        </div>
        <div className="space-y-2">
          {headers.map((header, index) => (
            <div key={index} className="flex gap-2">
              <input
                type="text"
                value={header.key}
                onChange={(e) =>
                  updateHeader(index, e.target.value, header.value)
                }
                placeholder="Key"
                disabled={wsConnectionStatus === "connected"}
                className="flex-1 px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-500 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm disabled:opacity-50 disabled:cursor-not-allowed"
              />
              <input
                type="text"
                value={header.value}
                onChange={(e) =>
                  updateHeader(index, header.key, e.target.value)
                }
                placeholder="Value"
                disabled={wsConnectionStatus === "connected"}
                className="flex-1 px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-500 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm disabled:opacity-50 disabled:cursor-not-allowed"
              />
              <button
                onClick={() => removeHeader(index)}
                disabled={wsConnectionStatus === "connected"}
                className="px-3 py-2 bg-transparent border border-gray-300 dark:border-[#2D333B] text-gray-600 dark:text-[#8B949E] hover:text-red-500 hover:border-red-500 rounded-md transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
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
            </div>
          ))}
        </div>
      </div>

      {/* Receiver Address & Message */}
      {wsConnectionStatus === "connected" && (
        <div className="space-y-4">
          <div>
            <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
              Receiver Address
            </label>
            <input
              type="text"
              value={receiverAddress}
              onChange={(e) => setReceiverAddress(e.target.value)}
              placeholder="/chat/message"
              className="w-full px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-500 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm font-mono"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
              Message
            </label>
            <textarea
              value={messageContent}
              onChange={(e) => setMessageContent(e.target.value)}
              placeholder='{"message": "Hello, WebSocket!"}'
              rows={4}
              className="w-full px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-500 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm font-mono resize-none"
            />
          </div>

          <button
            onClick={handleSendMessage}
            className="w-full px-4 py-2 bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded-md transition-colors text-sm font-medium flex items-center justify-center gap-2"
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
                d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"
              />
            </svg>
            Send Message
          </button>
        </div>
      )}
    </div>
  );
}

