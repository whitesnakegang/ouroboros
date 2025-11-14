import { useState, useEffect, useRef, useMemo } from "react";
import { useTestingStore } from "../store/testing.store";
import { useSidebarStore } from "@/features/sidebar/store/sidebar.store";
import { StompClient, buildWebSocketUrl } from "../utils/stompClient";

interface Subscription {
  id: string;
  destination: string;
  subscriptionId: string | null; // null이면 구독 해제 상태
}



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
    setTryId,
  } = useTestingStore();
  const { selectedEndpoint, endpoints } = useSidebarStore();

  const [entryPoint, setEntryPoint] = useState("");
  const [roomId, setRoomId] = useState("room1");
  const [connectHeaders, setConnectHeaders] = useState<Array<{ key: string; value: string }>>([]);
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [newTopic, setNewTopic] = useState("");
  
  // 간단한 모드 상태
  const [sender, setSender] = useState("tester");
  const [content, setContent] = useState("");
  const [messageType, setMessageType] = useState<"TALK" | "ENTER" | "LEAVE">("TALK");
  const [enableTryHeader, setEnableTryHeader] = useState(true);

  const stompClientRef = useRef<StompClient | null>(null);

  // 주소에서 도메인 추출 (첫 번째 경로 세그먼트)
  const extractDomainFromAddress = (address: string): string => {
    if (!address || address === "/unknown") {
      return "OTHERS";
    }
    
    // "/"로 시작하는 주소에서 첫 번째 경로 세그먼트 추출
    const parts = address.split("/").filter((part) => part.length > 0);
    
    if (parts.length === 0) {
      return "OTHERS";
    }
    
    // 첫 번째 경로 세그먼트를 도메인으로 사용 (대문자로 변환)
    const domain = parts[0];
    return domain.charAt(0).toUpperCase() + domain.slice(1).toLowerCase();
  };

  // 도메인별 구독 경로 계산
  const domainSubscriptionPaths = useMemo(() => {
    if (!selectedEndpoint || selectedEndpoint.protocol !== "WebSocket") {
      return [];
    }

    // 현재 엔드포인트의 receiver address에서 도메인 추출
    const receiverAddress = selectedEndpoint.tags?.[1] || "";
    if (!receiverAddress) {
      return [];
    }

    const currentDomain = extractDomainFromAddress(receiverAddress);

    // 모든 WebSocket 엔드포인트에서 같은 도메인을 가진 엔드포인트 찾기
    const paths: Array<{ path: string; description: string; isSubscribed: boolean }> = [];
    
    Object.values(endpoints).forEach((groupEndpoints) => {
      groupEndpoints.forEach((endpoint) => {
        if (
          endpoint.protocol === "WebSocket" &&
          endpoint.id !== selectedEndpoint.id
        ) {
          const endpointReceiverAddress = endpoint.tags?.[1] || "";
          if (endpointReceiverAddress) {
            const endpointDomain = extractDomainFromAddress(endpointReceiverAddress);
            if (endpointDomain === currentDomain) {
              // 구독 상태 확인 (subscriptionId가 null이 아니면 활성 상태)
              const subscription = subscriptions.find(
                (sub) => sub.destination === endpointReceiverAddress
              );
              const isSubscribed = subscription?.subscriptionId !== null && subscription?.subscriptionId !== undefined;
              paths.push({
                path: endpointReceiverAddress,
                description: endpoint.description || endpointReceiverAddress,
                isSubscribed,
              });
            }
          }
        }
      });
    });

    return paths;
  }, [selectedEndpoint, endpoints, subscriptions]);

  // 엔드포인트 선택 시 Entry Point 로드
  useEffect(() => {
    if (selectedEndpoint && selectedEndpoint.protocol === "WebSocket") {
      // WebSocket 엔드포인트의 entrypoint는 tags[0]에 저장되어 있음
      // convertOperationToEndpoint에서 tags: [entrypoint, receiverAddress, tag] 형태로 저장
      const entrypoint = selectedEndpoint.tags?.[0] || "/ws";
      const wsUrl = buildWebSocketUrl(entrypoint);
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
      
      // 기본 STOMP 헤더 추가 (없는 경우)
      if (!connectHeaders.some(h => h.key && h.key.toLowerCase() === "accept-version")) {
        headers["accept-version"] = "1.1,1.2";
      }
      
      // 사용자 정의 헤더 추가
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
          if (!stompClientRef.current) return;
          
          setWsConnectionStatus("connected");
          setWsConnectionStartTime(Date.now());
          
          // CONNECTED 메시지 로그
          const message = {
            id: `msg-${Date.now()}-${Math.random()}`,
            timestamp: Date.now(),
            direction: "received" as const,
            address: "CONNECTED",
            content: JSON.stringify({ status: "Connected to STOMP server", url: entryPoint }, null, 2),
          };
          addWsMessage(message);

          // Room ID가 있으면 자동으로 구독
          if (roomId && roomId.trim() !== "") {
            const destination = `/topic/chat/${roomId}`;
            handleSubscribe(destination);
          }

          // Try 알림 구독 (백엔드: /user/queue/ouro/try)
          const tryNotificationDestination = "/user/queue/ouro/try";
          try {
            if (stompClientRef.current) {
              stompClientRef.current.subscribe(tryNotificationDestination, (frame) => {
                // Try 알림 메시지에서 tryId 추출
                const tryIdHeader = frame.headers["X-Ouroboros-Try-Id"] || 
                                   frame.headers["x-ouroboros-try-id"];
                
                if (tryIdHeader) {
                  setTryId(tryIdHeader);
                }
                
                // Try 알림 메시지 본문 파싱 (payload, headers 포함)
                try {
                  const dispatchMessage = JSON.parse(frame.body);
                  
                  // payload가 문자열이면 파싱해서 객체로 변환 (일반 메시지처럼 깔끔하게 표시)
                  let parsedPayload = dispatchMessage.payload;
                  if (typeof dispatchMessage.payload === "string") {
                    try {
                      parsedPayload = JSON.parse(dispatchMessage.payload);
                    } catch (e) {
                      // 파싱 실패 시 원본 문자열 유지
                      parsedPayload = dispatchMessage.payload;
                    }
                  }
                  
                  const tryMessage = {
                    id: `msg-${Date.now()}-${Math.random()}`,
                    timestamp: Date.now(),
                    direction: "received" as const,
                    address: tryNotificationDestination,
                    content: JSON.stringify({
                      tryId: tryIdHeader,
                      payload: parsedPayload,
                      headers: dispatchMessage.headers,
                    }, null, 2),
                    tryId: tryIdHeader,
                  };
                  addWsMessage(tryMessage);
                  updateWsStats((currentStats) => ({
                    totalReceived: currentStats.totalReceived + 1,
                  }));
                } catch (parseError) {
                  // 파싱 실패 시 원본 body 그대로 표시
                  const tryMessage = {
                    id: `msg-${Date.now()}-${Math.random()}`,
                    timestamp: Date.now(),
                    direction: "received" as const,
                    address: tryNotificationDestination,
                    content: frame.body,
                    tryId: tryIdHeader,
                  };
                  addWsMessage(tryMessage);
                  updateWsStats((currentStats) => ({
                    totalReceived: currentStats.totalReceived + 1,
                  }));
                }
              });
            }
          } catch (error) {
            // Try 알림 구독 실패
          }
        },
        (error) => {
          setWsConnectionStatus("disconnected");
          setWsConnectionStartTime(null);
          updateWsStats({ connectionDuration: null });
          const errorMessage = error.message || "알 수 없는 오류";
          alert(`STOMP 연결 중 오류가 발생했습니다:\n\n${errorMessage}\n\nURL: ${entryPoint}`);
          
          // 에러 메시지도 로그에 추가
          const errorMsg = {
            id: `msg-${Date.now()}-${Math.random()}`,
            timestamp: Date.now(),
            direction: "received" as const,
            address: "ERROR",
            content: JSON.stringify({ error: errorMessage, url: entryPoint }, null, 2),
          };
          addWsMessage(errorMsg);
        }
      );
    } catch (error) {
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

    // 이미 구독 해제된 항목이 있는지 확인
    const existingSubscription = subscriptions.find(
      (sub) => sub.destination === destination && sub.subscriptionId === null
    );

    try {
      const subscriptionId = stompClientRef.current.subscribe(destination, (frame) => {
        // 응답 메시지에서 X-Ouroboros-Try-Id 헤더 추출
        const tryIdHeader = frame.headers["X-Ouroboros-Try-Id"] || 
                           frame.headers["x-ouroboros-try-id"];
        
        // tryId가 있으면 store에 저장
        if (tryIdHeader) {
          setTryId(tryIdHeader);
        }

        const message = {
          id: `msg-${Date.now()}-${Math.random()}`,
          timestamp: Date.now(),
          direction: "received" as const,
          address: frame.headers.destination || destination,
          content: frame.body,
          tryId: tryIdHeader || undefined,
        };
        addWsMessage(message);
        updateWsStats((currentStats) => ({
          totalReceived: currentStats.totalReceived + 1,
        }));
      });

      if (existingSubscription) {
        // 기존 구독 해제된 항목을 다시 활성화
        setSubscriptions(subscriptions.map((s) =>
          s.id === existingSubscription.id
            ? { ...s, subscriptionId }
            : s
        ));
      } else {
        // 새로운 구독 추가
        const subscription: Subscription = {
          id: `sub-${Date.now()}`,
          destination: destination,
          subscriptionId,
        };
        setSubscriptions([...subscriptions, subscription]);
      }

      if (!topic) {
        setNewTopic("");
      }

      // 구독 메시지 로그
      const message = {
        id: `msg-${Date.now()}-${Math.random()}`,
        timestamp: Date.now(),
        direction: "sent" as const,
        address: destination,
        content: JSON.stringify({ action: "SUBSCRIBE", destination: destination }, null, 2),
      };
      addWsMessage(message);
      } catch (error) {
        alert("구독에 실패했습니다.");
    }
  };

  const handleUnsubscribe = (subscription: Subscription) => {
    if (!stompClientRef.current || !stompClientRef.current.isConnected()) {
      return;
    }

    if (!subscription.subscriptionId) {
      // 이미 구독 해제된 상태면 아무것도 하지 않음
      return;
    }

    try {
      stompClientRef.current.unsubscribe(subscription.subscriptionId);
      // 구독 해제하되 목록에서 제거하지 않고 subscriptionId를 null로 설정
      setSubscriptions(subscriptions.map((s) => 
        s.id === subscription.id 
          ? { ...s, subscriptionId: null }
          : s
      ));

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
      // 구독 해제 실패
    }
  };

  // 간단한 모드 메시지 전송
  const handleSimpleSend = () => {
    if (wsConnectionStatus !== "connected") {
      alert("먼저 연결을 시도해주세요.");
      return;
    }

    if (!stompClientRef.current) {
      alert("STOMP 클라이언트가 초기화되지 않았습니다.");
      return;
    }

    if (!stompClientRef.current.isConnected()) {
      alert("연결이 끊어진 것 같습니다. 다시 연결해주세요.");
      setWsConnectionStatus("disconnected");
      return;
    }

    if (!roomId || roomId.trim() === "") {
      alert("Room ID를 입력해주세요.");
      return;
    }

    if (!content || content.trim() === "") {
      alert("메시지 내용을 입력해주세요.");
      return;
    }

    try {
      const destination = `/app/chat/${roomId}`;
      const messageBody = JSON.stringify({
        roomId: roomId,
        sender: sender || "tester",
        content: content,
        type: messageType,
        sentAt: new Date().toISOString(),
      });

      const headers: Record<string, string> = {
        "content-type": "application/json",
      };

      // X-Ouroboros-Try 헤더는 체크박스로 선택
      if (enableTryHeader) {
        headers["X-Ouroboros-Try"] = "on";
      }

      stompClientRef.current.send(destination, headers, messageBody);

      // 전송된 메시지 로그
      const message = {
        id: `msg-${Date.now()}-${Math.random()}`,
        timestamp: Date.now(),
        direction: "sent" as const,
        address: destination,
        content: messageBody,
      };
      addWsMessage(message);
      updateWsStats((currentStats) => ({
        totalSent: currentStats.totalSent + 1,
      }));

      // Content 초기화
      setContent("");
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : "알 수 없는 오류";
      alert(`메시지 전송에 실패했습니다:\n\n${errorMessage}`);
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
        {/* 연결 설정 Section */}
        <div className="mb-6 space-y-4">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3]">연결 설정</h3>
          
          <div>
            <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
              WS Endpoint
            </label>
            <input
              type="text"
              value={entryPoint}
              onChange={(e) => setEntryPoint(e.target.value)}
              placeholder="ws://localhost:8080/ws"
              disabled={wsConnectionStatus === "connected"}
              className="w-full px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-500 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm font-mono disabled:opacity-50 disabled:cursor-not-allowed"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
              Room ID
            </label>
            <input
              type="text"
              value={roomId}
              onChange={(e) => setRoomId(e.target.value)}
              placeholder="room1"
              disabled={wsConnectionStatus === "connected"}
              className="w-full px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-500 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm disabled:opacity-50 disabled:cursor-not-allowed"
            />
          </div>

          <div className="flex gap-2">
            {wsConnectionStatus === "disconnected" ? (
              <button
                onClick={handleConnect}
                className="flex-1 px-4 py-2 bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded-md transition-colors text-sm font-semibold"
              >
                Connect
              </button>
            ) : (
              <button
                onClick={handleDisconnect}
                className="flex-1 px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-md transition-colors text-sm font-semibold"
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
        <div className="border-t border-gray-200 dark:border-[#2D333B] p-4">
          <div className="space-y-4">
            <h3 className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3]">메시지 전송</h3>
            
            <div>
              <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                Sender
              </label>
              <input
                type="text"
                value={sender}
                onChange={(e) => setSender(e.target.value)}
                placeholder="tester"
                className="w-full px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-500 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                Content
              </label>
              <input
                type="text"
                value={content}
                onChange={(e) => setContent(e.target.value)}
                placeholder="메시지 내용을 입력하세요"
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    handleSimpleSend();
                  }
                }}
                className="w-full px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-500 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                Type
              </label>
              <select
                value={messageType}
                onChange={(e) => setMessageType(e.target.value as "TALK" | "ENTER" | "LEAVE")}
                className="w-full px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm"
              >
                <option value="TALK">TALK</option>
                <option value="ENTER">ENTER</option>
                <option value="LEAVE">LEAVE</option>
              </select>
            </div>

            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="enableTryHeader"
                checked={enableTryHeader}
                onChange={(e) => setEnableTryHeader(e.target.checked)}
                className="w-4 h-4 text-[#2563EB] bg-gray-100 border-gray-300 rounded focus:ring-[#2563EB]"
              />
              <label htmlFor="enableTryHeader" className="text-xs font-medium text-gray-700 dark:text-[#E6EDF3]">
                X-Ouroboros-Try
              </label>
              <span className="text-xs text-gray-500 dark:text-[#8B949E]">추적 헤더 추가</span>
            </div>

            <button
              onClick={handleSimpleSend}
              className="w-full px-4 py-2 bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded-md transition-colors text-sm font-semibold"
            >
              Send STOMP
            </button>

            {/* 도메인별 구독 경로 섹션 */}
            {domainSubscriptionPaths.length > 0 && (
              <div className="mt-6 pt-6 border-t border-gray-200 dark:border-[#2D333B]">
                <h3 className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3] mb-3">
                  도메인별 구독 경로
                </h3>
                <div className="space-y-2">
                  {domainSubscriptionPaths.map((item, index) => (
                    <div
                      key={index}
                      className="flex items-center justify-between p-3 bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] rounded-md"
                    >
                      <div className="flex-1 min-w-0">
                        <div className="text-sm font-medium text-gray-900 dark:text-[#E6EDF3] truncate">
                          {item.path}
                        </div>
                        {item.description && (
                          <div className="text-xs text-gray-500 dark:text-[#8B949E] truncate mt-1">
                            {item.description}
                          </div>
                        )}
                      </div>
                      <label className="relative inline-flex items-center cursor-pointer ml-4">
                        <input
                          type="checkbox"
                          checked={item.isSubscribed}
                          onChange={() => {
                            if (item.isSubscribed) {
                              // 구독 해제
                              const subscription = subscriptions.find(
                                (sub) => sub.destination === item.path
                              );
                              if (subscription) {
                                handleUnsubscribe(subscription);
                              }
                            } else {
                              // 구독
                              handleSubscribe(item.path);
                            }
                          }}
                          className="sr-only peer"
                        />
                        <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 dark:peer-focus:ring-blue-800 rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-blue-600"></div>
                      </label>
                    </div>
                  ))}
                </div>
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
            Subscriptions ({subscriptions.filter(s => s.subscriptionId !== null).length} active / {subscriptions.length} total)
          </label>
          <div className="space-y-2">
            {subscriptions.map((subscription) => {
              const isActive = subscription.subscriptionId !== null;
              return (
                <div
                  key={subscription.id}
                  className={`flex items-center justify-between p-3 rounded-md ${
                    isActive
                      ? "bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B]"
                      : "bg-gray-100 dark:bg-[#161B22] border border-gray-200 dark:border-[#2D333B] opacity-60"
                  }`}
                >
                  <div className="flex-1 min-w-0">
                    <span className={`text-sm font-mono ${
                      isActive
                        ? "text-gray-900 dark:text-[#E6EDF3]"
                        : "text-gray-500 dark:text-[#8B949E]"
                    }`}>
                      {subscription.destination}
                    </span>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer ml-4">
                    <input
                      type="checkbox"
                      checked={isActive}
                      onChange={() => {
                        if (isActive) {
                          handleUnsubscribe(subscription);
                        } else {
                          handleSubscribe(subscription.destination);
                        }
                      }}
                      className="sr-only peer"
                    />
                    <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 dark:peer-focus:ring-blue-800 rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-blue-600"></div>
                  </label>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
