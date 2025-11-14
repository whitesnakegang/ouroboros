import { useState, useEffect, useRef } from "react";
import { useTestingStore } from "../store/testing.store";
import { useSidebarStore } from "@/features/sidebar/store/sidebar.store";
import { StompClient, buildWebSocketUrl } from "../utils/stompClient";

interface Subscription {
  id: string;
  destination: string;
  subscriptionId: string;
}

interface SavedMessage {
  id: string;
  name: string;
  destination: string;
  headers: Array<{ key: string; value: string }>;
  body: string;
  timestamp: number;
}

type MessageTab = "message" | "saved";
type StompCommand = "SEND" | "SUBSCRIBE" | "UNSUBSCRIBE";
type InputMode = "structured" | "raw";

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
  const [connectHeaders, setConnectHeaders] = useState<Array<{ key: string; value: string }>>([]);
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [newTopic, setNewTopic] = useState("");
  
  // STOMP Frame 구조
  const [stompCommand, setStompCommand] = useState<StompCommand>("SEND");
  const [stompHeaders, setStompHeaders] = useState<Array<{ key: string; value: string }>>([
    { key: "destination", value: "" },
  ]);
  const [stompBody, setStompBody] = useState("");
  
  // UI State
  const [activeTab, setActiveTab] = useState<MessageTab>("message");
  const [inputMode, setInputMode] = useState<InputMode>("structured");
  const [rawStompFrame, setRawStompFrame] = useState("");
  const [savedMessages, setSavedMessages] = useState<SavedMessage[]>([]);

  const stompClientRef = useRef<StompClient | null>(null);

  // 엔드포인트 선택 시 Entry Point 로드
  useEffect(() => {
    if (selectedEndpoint && selectedEndpoint.protocol === "WebSocket") {
      // WebSocket 엔드포인트의 path를 기반으로 Entry Point 생성
      // https 환경에서는 wss://, http 환경에서는 ws:// 사용
      const wsUrl = buildWebSocketUrl(selectedEndpoint.path);
      setEntryPoint(wsUrl);
    } else {
      setEntryPoint("");
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
      setSubscriptions([]);
      updateWsStats({
        totalSent: 0,
        totalReceived: 0,
        averageResponseTime: null,
        connectionDuration: null,
      });

      const headers: Record<string, string> = {};
      connectHeaders.forEach((h) => {
        if (h.key && h.value) {
          headers[h.key] = h.value;
        }
      });

      const client = new StompClient(entryPoint);
      stompClientRef.current = client;

      client.connect(
        headers,
        () => {
          setWsConnectionStatus("connected");
          setWsConnectionStartTime(Date.now());
          
          // CONNECTED 메시지 로그
          const message = {
            id: `msg-${Date.now()}-${Math.random()}`,
            timestamp: Date.now(),
            direction: "received" as const,
            address: "CONNECTED",
            content: JSON.stringify({ status: "Connected to STOMP server" }, null, 2),
          };
          addWsMessage(message);
        },
        (error) => {
          console.error("STOMP 연결 에러:", error);
          setWsConnectionStatus("disconnected");
          alert(`STOMP 연결 중 오류가 발생했습니다: ${error.message}`);
        }
      );
    } catch (error) {
      console.error("연결 실패:", error);
      setWsConnectionStatus("disconnected");
      alert("연결에 실패했습니다.");
    }
  };

  const handleDisconnect = () => {
    if (stompClientRef.current) {
      stompClientRef.current.disconnect(() => {
        setWsConnectionStatus("disconnected");
        setWsConnectionStartTime(null);
        updateWsStats({ connectionDuration: null });
        setSubscriptions([]);
      });
      stompClientRef.current = null;
    }
  };

  const handleSubscribe = (topic?: string) => {
    const destination = topic || newTopic.trim();
    
    if (!destination) {
      alert("Topic을 입력해주세요.");
      return;
    }

    if (!stompClientRef.current || !stompClientRef.current.isConnected()) {
      alert("먼저 연결을 시도해주세요.");
      return;
    }

    try {
      const subscriptionId = stompClientRef.current.subscribe(destination, (frame) => {
        const message = {
          id: `msg-${Date.now()}-${Math.random()}`,
          timestamp: Date.now(),
          direction: "received" as const,
          address: frame.headers.destination || newTopic,
          content: frame.body,
        };
        addWsMessage(message);
        updateWsStats({
          totalReceived: wsStats.totalReceived + 1,
        });
      });

      const subscription: Subscription = {
        id: `sub-${Date.now()}`,
        destination: newTopic,
        subscriptionId,
      };

      setSubscriptions([...subscriptions, subscription]);
      setNewTopic("");

      // 구독 메시지 로그
      const message = {
        id: `msg-${Date.now()}-${Math.random()}`,
        timestamp: Date.now(),
        direction: "sent" as const,
        address: newTopic,
        content: JSON.stringify({ action: "SUBSCRIBE", destination: newTopic }, null, 2),
      };
      addWsMessage(message);
    } catch (error) {
      console.error("구독 실패:", error);
      alert("구독에 실패했습니다.");
    }
  };

  const handleUnsubscribe = (subscription: Subscription) => {
    if (!stompClientRef.current || !stompClientRef.current.isConnected()) {
      return;
    }

    try {
      stompClientRef.current.unsubscribe(subscription.subscriptionId);
      setSubscriptions(subscriptions.filter((s) => s.id !== subscription.id));

      // 구독 해제 메시지 로그
      const message = {
        id: `msg-${Date.now()}-${Math.random()}`,
        timestamp: Date.now(),
        direction: "sent" as const,
        address: subscription.destination,
        content: JSON.stringify({ action: "UNSUBSCRIBE", destination: subscription.destination }, null, 2),
      };
      addWsMessage(message);
    } catch (error) {
      console.error("구독 해제 실패:", error);
    }
  };

  const handleSendMessage = () => {
    if (!stompClientRef.current || !stompClientRef.current.isConnected()) {
      alert("먼저 연결을 시도해주세요.");
      return;
    }

    try {
      // Raw 모드면 먼저 파싱
      if (inputMode === "raw") {
        const parsed = parseRawStompFrame(rawStompFrame);
        if (!parsed) {
          alert("❌ 유효하지 않은 STOMP 프레임입니다.");
          return;
        }
        setStompCommand(parsed.command);
        setStompHeaders(parsed.headers);
        setStompBody(parsed.body);
      }

      // SEND 커맨드 처리
      if (stompCommand === "SEND") {
        const destinationHeader = stompHeaders.find(h => h.key === "destination");
        if (!destinationHeader || !destinationHeader.value) {
          alert("destination 헤더를 입력해주세요.");
          return;
        }

        const headers: Record<string, string> = {};
        stompHeaders.forEach((h) => {
          if (h.key && h.value && h.key !== "destination") {
            headers[h.key] = h.value;
          }
        });

        stompClientRef.current.send(destinationHeader.value, headers, stompBody);

        // 전송된 메시지 로그
        const message = {
          id: `msg-${Date.now()}-${Math.random()}`,
          timestamp: Date.now(),
          direction: "sent" as const,
          address: destinationHeader.value,
          content: stompBody || "(empty)",
        };
        addWsMessage(message);
        updateWsStats({
          totalSent: wsStats.totalSent + 1,
        });

        // Body만 초기화
        if (inputMode === "structured") {
          setStompBody("");
        } else {
          const frameLines = rawStompFrame.split('\n');
          const emptyLineIndex = frameLines.findIndex(line => line.trim() === '');
          if (emptyLineIndex > -1) {
            setRawStompFrame(frameLines.slice(0, emptyLineIndex + 1).join('\n'));
          }
        }
      }
      // SUBSCRIBE 커맨드 처리
      else if (stompCommand === "SUBSCRIBE") {
        const destinationHeader = stompHeaders.find(h => h.key === "destination");
        if (!destinationHeader || !destinationHeader.value) {
          alert("destination 헤더를 입력해주세요.");
          return;
        }
        handleSubscribe(destinationHeader.value);
      }
      // UNSUBSCRIBE 커맨드 처리
      else if (stompCommand === "UNSUBSCRIBE") {
        const idHeader = stompHeaders.find(h => h.key === "id");
        if (idHeader && idHeader.value) {
          const subscription = subscriptions.find(s => s.subscriptionId === idHeader.value);
          if (subscription) {
            handleUnsubscribe(subscription);
          }
        }
      }
    } catch (error) {
      console.error("메시지 전송 실패:", error);
      alert("메시지 전송에 실패했습니다.");
    }
  };

  const addConnectHeader = () => {
    setConnectHeaders([...connectHeaders, { key: "", value: "" }]);
  };

  const updateConnectHeader = (index: number, key: string, value: string) => {
    const newHeaders = [...connectHeaders];
    newHeaders[index] = { key, value };
    setConnectHeaders(newHeaders);
  };

  const removeConnectHeader = (index: number) => {
    setConnectHeaders(connectHeaders.filter((_, i) => i !== index));
  };

  // STOMP Headers 관리
  const addStompHeader = () => {
    setStompHeaders([...stompHeaders, { key: "", value: "" }]);
  };

  const updateStompHeader = (index: number, key: string, value: string) => {
    const newHeaders = [...stompHeaders];
    newHeaders[index] = { key, value };
    setStompHeaders(newHeaders);
  };

  const removeStompHeader = (index: number) => {
    setStompHeaders(stompHeaders.filter((_, i) => i !== index));
  };

  // Raw STOMP 프레임 파싱
  const parseRawStompFrame = (raw: string): { command: StompCommand; headers: Array<{ key: string; value: string }>; body: string } | null => {
    try {
      const lines = raw.trim().split('\n');
      if (lines.length === 0) return null;

      const command = lines[0].trim() as StompCommand;
      if (!["SEND", "SUBSCRIBE", "UNSUBSCRIBE"].includes(command)) {
        return null;
      }

      const headers: Array<{ key: string; value: string }> = [];
      let bodyStartIndex = -1;

      // 헤더 파싱
      for (let i = 1; i < lines.length; i++) {
        const line = lines[i];
        
        // 빈 줄이 나오면 다음부터 body
        if (line.trim() === "") {
          bodyStartIndex = i + 1;
          break;
        }

        const colonIndex = line.indexOf(':');
        if (colonIndex > -1) {
          const key = line.substring(0, colonIndex).trim();
          const value = line.substring(colonIndex + 1).trim();
          headers.push({ key, value });
        }
      }

      // Body 추출
      const body = bodyStartIndex > -1 && bodyStartIndex < lines.length
        ? lines.slice(bodyStartIndex).join('\n').trim()
        : "";

      return { command, headers, body };
    } catch (error) {
      console.error("STOMP 프레임 파싱 실패:", error);
      return null;
    }
  };

  // Structured → Raw 변환
  const generateStompFrameFromStructured = (): string => {
    let frame = `${stompCommand}\n`;
    
    stompHeaders.forEach((h) => {
      if (h.key && h.value) {
        frame += `${h.key}:${h.value}\n`;
      }
    });
    
    frame += `\n${stompBody}`;
    return frame;
  };

  // Raw → Structured 적용
  const applyRawFrame = () => {
    const parsed = parseRawStompFrame(rawStompFrame);
    if (parsed) {
      setStompCommand(parsed.command);
      setStompHeaders(parsed.headers.length > 0 ? parsed.headers : [{ key: "destination", value: "" }]);
      setStompBody(parsed.body);
      alert("✅ STOMP 프레임이 적용되었습니다!");
    } else {
      alert("❌ 유효하지 않은 STOMP 프레임입니다. 형식을 확인해주세요.");
    }
  };

  // Structured → Raw 동기화
  const syncToRawMode = () => {
    setRawStompFrame(generateStompFrameFromStructured());
    setInputMode("raw");
  };

  // 저장된 메시지 관리
  const saveCurrentMessage = () => {
    const name = prompt("메시지 이름을 입력하세요:");
    if (!name) return;

    const newSavedMessage: SavedMessage = {
      id: `saved-${Date.now()}`,
      name,
      destination: stompHeaders.find(h => h.key === "destination")?.value || "",
      headers: stompHeaders,
      body: stompBody,
      timestamp: Date.now(),
    };

    const updated = [...savedMessages, newSavedMessage];
    setSavedMessages(updated);
    localStorage.setItem("stomp-saved-messages", JSON.stringify(updated));
  };

  const loadSavedMessage = (message: SavedMessage) => {
    setStompHeaders(message.headers);
    setStompBody(message.body);
    setActiveTab("message");
    setInputMode("structured");
  };

  const deleteSavedMessage = (id: string) => {
    const updated = savedMessages.filter((m) => m.id !== id);
    setSavedMessages(updated);
    localStorage.setItem("stomp-saved-messages", JSON.stringify(updated));
  };

  // 로컬 스토리지에서 저장된 메시지 불러오기
  useEffect(() => {
    const saved = localStorage.getItem("stomp-saved-messages");
    if (saved) {
      try {
        setSavedMessages(JSON.parse(saved));
      } catch (e) {
        console.error("저장된 메시지 로드 실패:", e);
      }
    }
  }, []);

  // 로컬 스토리지에서 저장된 메시지 불러오기 (deprecated 함수 제거)

  return (
    <div className="rounded-md border border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#161B22] shadow-sm">
      {/* Header with Connection Status */}
      <div className="px-4 py-3 border-b border-gray-200 dark:border-[#2D333B] flex items-center justify-between bg-gray-50 dark:bg-[#0D1117]">
        <div className="flex items-center gap-2">
          <svg
            className="h-5 w-5 text-gray-500 dark:text-[#8B949E]"
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
          <span className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3]">
            STOMP over WebSocket
          </span>
        </div>
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2">
            <div
              className={`w-2 h-2 rounded-full ${
                wsConnectionStatus === "connected"
                  ? "bg-green-500 animate-pulse"
                  : wsConnectionStatus === "connecting"
                  ? "bg-yellow-500 animate-pulse"
                  : "bg-gray-400"
              }`}
            />
            <span className="text-xs font-medium text-gray-600 dark:text-[#8B949E]">
              {wsConnectionStatus === "connected"
                ? "Connected"
                : wsConnectionStatus === "connecting"
                ? "Connecting..."
                : "Disconnected"}
            </span>
          </div>
        </div>
      </div>

      <div className="p-4">
        {/* Connection Section */}
        <div className="mb-4">
          <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
            WebSocket URL
          </label>
          <div className="flex gap-2">
            <input
              type="text"
              value={entryPoint}
              onChange={(e) => setEntryPoint(e.target.value)}
              placeholder="ws://localhost:8080/ws"
              disabled={wsConnectionStatus === "connected"}
              className="flex-1 px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-500 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm font-mono disabled:opacity-50 disabled:cursor-not-allowed"
            />
            {wsConnectionStatus === "disconnected" ? (
              <button
                onClick={handleConnect}
                className="px-6 py-2 bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded-md transition-colors text-sm font-semibold"
              >
                Connect
              </button>
            ) : (
              <button
                onClick={handleDisconnect}
                className="px-6 py-2 bg-red-500 hover:bg-red-600 text-white rounded-md transition-colors text-sm font-semibold"
              >
                Disconnect
              </button>
            )}
          </div>
        </div>

        {/* STOMP CONNECT Headers - Collapsible */}
        {wsConnectionStatus === "disconnected" && (
          <div className="mb-4 border border-gray-200 dark:border-[#2D333B] rounded-md p-3">
            <div className="flex items-center justify-between mb-2">
              <label className="text-xs font-medium text-gray-600 dark:text-[#8B949E]">
                STOMP CONNECT Headers (선택사항)
              </label>
              <button
                onClick={addConnectHeader}
                className="text-xs px-2 py-1 bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded-md transition-colors"
              >
                + Add Header
              </button>
            </div>
            {connectHeaders.length === 0 ? (
              <p className="text-xs text-gray-500 dark:text-[#8B949E] text-center py-2">
                기본 설정으로 연결합니다. 필요시 헤더를 추가하세요.
              </p>
            ) : (
              <div className="space-y-2">
                {connectHeaders.map((header, index) => (
                  <div key={index} className="flex gap-2">
                    <input
                      type="text"
                      value={header.key}
                      onChange={(e) =>
                        updateConnectHeader(index, e.target.value, header.value)
                      }
                      placeholder="Key (예: accept-version)"
                      className="flex-1 px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-500 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm"
                    />
                    <input
                      type="text"
                      value={header.value}
                      onChange={(e) =>
                        updateConnectHeader(index, header.key, e.target.value)
                      }
                      placeholder="Value (예: 1.1,1.2)"
                      className="flex-1 px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-500 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm"
                    />
                    <button
                      onClick={() => removeConnectHeader(index)}
                      className="px-3 py-2 text-red-500 hover:text-red-700"
                    >
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      {/* Main Content - Connected State */}
      {wsConnectionStatus === "connected" && (
        <div className="border-t border-gray-200 dark:border-[#2D333B]">
          {/* Tabs */}
          <div className="flex border-b border-gray-200 dark:border-[#2D333B] bg-gray-50 dark:bg-[#0D1117]">
            <button
              onClick={() => setActiveTab("message")}
              className={`px-4 py-2 text-sm font-medium transition-colors ${
                activeTab === "message"
                  ? "text-[#F97316] border-b-2 border-[#F97316] bg-white dark:bg-[#161B22]"
                  : "text-gray-600 dark:text-[#8B949E] hover:text-gray-900 dark:hover:text-[#E6EDF3]"
              }`}
            >
              STOMP Frame
            </button>
            <button
              onClick={() => setActiveTab("saved")}
              className={`px-4 py-2 text-sm font-medium transition-colors ${
                activeTab === "saved"
                  ? "text-[#F97316] border-b-2 border-[#F97316] bg-white dark:bg-[#161B22]"
                  : "text-gray-600 dark:text-[#8B949E] hover:text-gray-900 dark:hover:text-[#E6EDF3]"
              }`}
            >
              Saved Messages ({savedMessages.length})
            </button>
          </div>

          {/* Tab Content */}
          <div className="p-4">
            {activeTab === "message" && (
              <div className="space-y-4">
                {/* Mode Toggle */}
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 bg-gray-100 dark:bg-[#0D1117] rounded-md p-1 border border-gray-300 dark:border-[#2D333B]">
                    <button
                      onClick={() => setInputMode("structured")}
                      className={`px-3 py-1.5 text-xs font-medium rounded transition-colors flex items-center gap-1.5 ${
                        inputMode === "structured"
                          ? "bg-white dark:bg-[#161B22] text-[#2563EB] shadow-sm"
                          : "text-gray-600 dark:text-[#8B949E]"
                      }`}
                    >
                      <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 10h16M4 14h16M4 18h16" />
                      </svg>
                      Structured
                    </button>
                    <button
                      onClick={syncToRawMode}
                      className={`px-3 py-1.5 text-xs font-medium rounded transition-colors flex items-center gap-1.5 ${
                        inputMode === "raw"
                          ? "bg-white dark:bg-[#161B22] text-[#2563EB] shadow-sm"
                          : "text-gray-600 dark:text-[#8B949E]"
                      }`}
                    >
                      <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
                      </svg>
                      Raw
                    </button>
                  </div>

                  {inputMode === "raw" && (
                    <button
                      onClick={applyRawFrame}
                      className="text-xs px-3 py-1.5 bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded-md transition-colors font-medium flex items-center gap-1.5"
                    >
                      <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                      </svg>
                      Parse & Apply
                    </button>
                  )}
                </div>

                {/* Structured Mode */}
                {inputMode === "structured" && (
                  <div className="space-y-4">
                    <div className="bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] rounded-md overflow-hidden">
                      {/* COMMAND Section */}
                      <div className="border-b border-gray-300 dark:border-[#2D333B] bg-white dark:bg-[#161B22] p-4">
                        <label className="block text-xs font-semibold text-gray-700 dark:text-[#8B949E] mb-2 uppercase tracking-wide">
                          Command
                        </label>
                        <div className="flex gap-2">
                          {(["SEND", "SUBSCRIBE", "UNSUBSCRIBE"] as StompCommand[]).map((cmd) => (
                            <button
                              key={cmd}
                              onClick={() => setStompCommand(cmd)}
                              className={`px-4 py-2 text-sm font-bold rounded-md transition-colors ${
                                stompCommand === cmd
                                  ? "bg-[#F97316] text-white shadow-md"
                                  : "bg-gray-100 dark:bg-[#0D1117] text-gray-600 dark:text-[#8B949E] hover:bg-gray-200 dark:hover:bg-[#161B22]"
                              }`}
                            >
                              {cmd}
                            </button>
                          ))}
                        </div>
                      </div>

                      {/* HEADERS Section */}
                      <div className="border-b border-gray-300 dark:border-[#2D333B] bg-white dark:bg-[#161B22] p-4">
                        <div className="flex items-center justify-between mb-3">
                          <label className="text-xs font-semibold text-gray-700 dark:text-[#8B949E] uppercase tracking-wide">
                            Headers
                          </label>
                          <button
                            onClick={addStompHeader}
                            className="text-xs px-2 py-1 bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded-md transition-colors font-medium"
                          >
                            + Add Header
                          </button>
                        </div>
                        <div className="space-y-2">
                          {stompHeaders.map((header, index) => (
                            <div key={index} className="flex gap-2 font-mono text-sm">
                              <input
                                type="text"
                                value={header.key}
                                onChange={(e) => updateStompHeader(index, e.target.value, header.value)}
                                placeholder="header-name"
                                className="flex-1 px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] text-sm focus:outline-none focus:ring-2 focus:ring-[#2563EB]"
                              />
                              <span className="text-gray-400 flex items-center">:</span>
                              <input
                                type="text"
                                value={header.value}
                                onChange={(e) => updateStompHeader(index, header.key, e.target.value)}
                                placeholder="value"
                                className="flex-1 px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] text-sm focus:outline-none focus:ring-2 focus:ring-[#2563EB]"
                              />
                              <button
                                onClick={() => removeStompHeader(index)}
                                className="px-3 py-2 text-red-500 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-md transition-colors"
                              >
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                </svg>
                              </button>
                            </div>
                          ))}
                        </div>
                      </div>

                      {/* BODY Section */}
                      <div className="bg-white dark:bg-[#161B22] p-4">
                        <label className="block text-xs font-semibold text-gray-700 dark:text-[#8B949E] mb-2 uppercase tracking-wide">
                          Body
                        </label>
                        <textarea
                          value={stompBody}
                          onChange={(e) => setStompBody(e.target.value)}
                          placeholder='{"message": "Hello, STOMP!"}'
                          rows={10}
                          className="w-full px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-500 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-2 focus:ring-[#2563EB] text-sm font-mono resize-none"
                        />
                      </div>
                    </div>
                  </div>
                )}

                {/* Raw Mode */}
                {inputMode === "raw" && (
                  <div className="space-y-3">
                    <div className="relative">
                      <div className="absolute top-3 right-3 text-xs text-green-400 bg-black px-3 py-1.5 rounded font-mono z-10">
                        💡 Paste from Postman
                      </div>
                      <textarea
                        value={rawStompFrame}
                        onChange={(e) => setRawStompFrame(e.target.value)}
                        placeholder="SEND&#10;destination:/app/chat&#10;content-type:application/json&#10;&#10;{&quot;message&quot;:&quot;Hello, STOMP!&quot;}"
                        rows={20}
                        className="w-full px-4 py-3 rounded-md bg-black text-green-400 border-2 border-green-500/30 font-mono text-sm focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 resize-none"
                        style={{ fontFamily: 'Consolas, Monaco, "Courier New", monospace' }}
                      />
                    </div>
                  </div>
                )}

                {/* Action Buttons */}
                <div className="flex gap-2 pt-2">
                  <button
                    onClick={handleSendMessage}
                    className="flex-1 px-4 py-3 bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded-md transition-colors text-sm font-semibold flex items-center justify-center gap-2 shadow-md"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
                    </svg>
                    Send Message
                  </button>
                  <button
                    onClick={saveCurrentMessage}
                    className="px-4 py-3 bg-gray-500 hover:bg-gray-600 text-white rounded-md transition-colors text-sm font-semibold shadow-md"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4" />
                    </svg>
                  </button>
                </div>

                {/* Quick Subscribe */}
                <div className="border-t border-gray-200 dark:border-[#2D333B] pt-4 mt-2">
                  <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                    💡 Quick Subscribe to Topic
                  </label>
                  <div className="flex gap-2">
                    <input
                      type="text"
                      value={newTopic}
                      onChange={(e) => setNewTopic(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === "Enter") {
                          handleSubscribe();
                        }
                      }}
                      placeholder="/topic/chat"
                      className="flex-1 px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-500 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-2 focus:ring-[#2563EB] text-sm font-mono"
                    />
                    <button
                      onClick={() => handleSubscribe()}
                      className="px-4 py-2 bg-green-500 hover:bg-green-600 text-white rounded-md transition-colors text-sm font-medium"
                    >
                      Subscribe
                    </button>
                  </div>
                </div>
              </div>
            )}

            {activeTab === "saved" && (
              <div className="space-y-3">
                {savedMessages.length === 0 ? (
                  <div className="text-center py-8 text-gray-500 dark:text-gray-400">
                    <p className="text-sm">저장된 메시지가 없습니다</p>
                    <p className="text-xs mt-1">Message 탭에서 "Save" 버튼을 클릭하여 메시지를 저장하세요</p>
                  </div>
                ) : (
                  savedMessages.map((msg) => (
                    <div
                      key={msg.id}
                      className="p-3 bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] rounded-md hover:border-[#2563EB] transition-colors cursor-pointer"
                      onClick={() => loadSavedMessage(msg)}
                    >
                      <div className="flex items-center justify-between mb-2">
                        <span className="text-sm font-medium text-gray-900 dark:text-[#E6EDF3]">
                          {msg.name}
                        </span>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            deleteSavedMessage(msg.id);
                          }}
                          className="text-red-500 hover:text-red-700"
                        >
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                          </svg>
                        </button>
                      </div>
                      <div className="text-xs text-gray-600 dark:text-[#8B949E] font-mono">
                        {msg.destination}
                      </div>
                      <div className="text-xs text-gray-500 dark:text-gray-500 mt-1">
                        {new Date(msg.timestamp).toLocaleString()}
                      </div>
                    </div>
                  ))
                )}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Active Subscriptions */}
      {wsConnectionStatus === "connected" && subscriptions.length > 0 && (
        <div className="border-t border-gray-200 dark:border-[#2D333B] p-4">
          <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-3 flex items-center gap-2">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
            </svg>
            Active Subscriptions ({subscriptions.length})
          </label>
          <div className="space-y-2">
            {subscriptions.map((subscription) => (
              <div
                key={subscription.id}
                className="flex items-center justify-between px-3 py-2 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-md"
              >
                <span className="text-sm font-mono text-gray-900 dark:text-[#E6EDF3]">
                  {subscription.destination}
                </span>
                <button
                  onClick={() => handleUnsubscribe(subscription)}
                  className="px-3 py-1 text-xs bg-red-500 hover:bg-red-600 text-white rounded transition-colors font-medium"
                >
                  Unsubscribe
                </button>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
