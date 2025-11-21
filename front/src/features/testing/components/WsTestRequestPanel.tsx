import { useState, useEffect, useRef, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useTestingStore } from "../store/testing.store";
import { useSidebarStore } from "@/features/sidebar/store/sidebar.store";
import { StompClient, buildWebSocketUrl } from "../utils/stompClient";
import {
  getWebSocketOperation,
  getWebSocketChannel,
  getWebSocketSchema,
} from "@/features/spec/services/api";
import type { SchemaField } from "@/features/spec/types/schema.types";
import { parseOpenAPISchemaToSchemaField } from "@/features/spec/utils/schemaConverter";
import { isPrimitiveSchema } from "@/features/spec/types/schema.types";

interface Subscription {
  id: string;
  destination: string;
  subscriptionId: string | null; // null이면 구독 해제 상태
}

export function WsTestRequestPanel() {
  const { t } = useTranslation();
  const {
    wsConnectionStatus,
    setWsConnectionStatus,
    addWsMessage,
    clearWsMessages,
    updateWsStats,
    setWsConnectionStartTime,
    wsConnectionStartTime,
    setTryId,
  } = useTestingStore();
  const { selectedEndpoint, endpoints } = useSidebarStore();

  const [entryPoint, setEntryPoint] = useState("");
  const [receiveAddress, setReceiveAddress] = useState("");
  const [replyAddress, setReplyAddress] = useState("");
  const [operationAction, setOperationAction] = useState<string | null>(null);
  const [connectHeaders, setConnectHeaders] = useState<
    Array<{ key: string; value: string }>
  >([]);
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [newTopic, setNewTopic] = useState("");

  // Schema 기반 메시지 입력 상태
  const [messageSchemaFields, setMessageSchemaFields] = useState<SchemaField[]>(
    []
  );
  const [messageFormData, setMessageFormData] = useState<
    Record<string, unknown>
  >({});
  const [enableTryHeader, setEnableTryHeader] = useState(true);

  const stompClientRef = useRef<StompClient | null>(null);

  // 패턴 파라미터 입력 상태 (예: {roomId: "room1"})
  const [pathParameters, setPathParameters] = useState<Record<string, string>>(
    {}
  );

  // 패턴 감지 함수
  const hasPathParameter = (address: string): boolean => {
    if (!address) return false;
    return /\{[^}]+\}/.test(address);
  };

  // 패턴에서 파라미터 이름 추출
  const extractPathParameters = (address: string): string[] => {
    if (!address) return [];
    const matches = address.match(/\{([^}]+)\}/g);
    return matches ? matches.map((m) => m.slice(1, -1)) : [];
  };

  // 패턴을 실제 값으로 치환
  const replacePathParameters = (
    address: string,
    params: Record<string, string>
  ): string => {
    if (!address) return address;
    let result = address;
    Object.entries(params).forEach(([key, value]) => {
      if (value) {
        result = result.replace(`{${key}}`, value);
      }
    });
    return result;
  };

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

  // CONNECT 버튼 활성화 여부 계산 (초록색 배지 조건)
  const isConnectEnabled = useMemo(() => {
    if (!selectedEndpoint || selectedEndpoint.protocol !== "WebSocket") {
      return false;
    }
    const normalizedProgress = selectedEndpoint.progress?.toLowerCase();
    const normalizedTag = selectedEndpoint.tag?.toLowerCase();
    return (
      normalizedProgress === "completed" ||
      normalizedProgress === "complete" ||
      (normalizedTag === "receive" && normalizedProgress === "receive")
    );
  }, [selectedEndpoint]);

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
    const paths: Array<{
      path: string;
      description: string;
      isSubscribed: boolean;
    }> = [];

    Object.values(endpoints).forEach((groupEndpoints) => {
      groupEndpoints.forEach((endpoint) => {
        if (
          endpoint.protocol === "WebSocket" &&
          endpoint.id !== selectedEndpoint.id
        ) {
          const endpointReceiverAddress = endpoint.tags?.[1] || "";
          if (endpointReceiverAddress) {
            const endpointDomain = extractDomainFromAddress(
              endpointReceiverAddress
            );
            if (endpointDomain === currentDomain) {
              // 구독 상태 확인 (subscriptionId가 null이 아니면 활성 상태)
              const subscription = subscriptions.find(
                (sub) => sub.destination === endpointReceiverAddress
              );
              const isSubscribed =
                subscription?.subscriptionId !== null &&
                subscription?.subscriptionId !== undefined;
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

  // 엔드포인트 선택 시 Entry Point 및 Receive/Reply Address 로드
  useEffect(() => {
    // selectedEndpoint가 변경되면 기존 연결 끊기
    if (wsConnectionStatus === "connected" && stompClientRef.current) {
      const client = stompClientRef.current;
      client.disconnect(() => {
        setWsConnectionStatus("disconnected");
        setWsConnectionStartTime(null);
        updateWsStats({ connectionDuration: null });
        setSubscriptions([]);
        stompClientRef.current = null;
      });
    }

    const loadWebSocketInfo = async () => {
      if (selectedEndpoint && selectedEndpoint.protocol === "WebSocket") {
        // WebSocket 엔드포인트의 entrypoint는 entrypoint 필드에 저장되어 있음
        const entrypoint = selectedEndpoint.entrypoint || "/ws";
        // protocol 정보 사용 (ws/wss/null, null이면 기본값 "ws")
        const protocol = selectedEndpoint.wsProtocol || "ws";
        const wsUrl = buildWebSocketUrl(entrypoint, protocol);
        setEntryPoint(wsUrl);

        // Operation 정보를 가져와서 receive와 reply address 추출
        try {
          const operationId = selectedEndpoint.id;
          const operationResponse = await getWebSocketOperation(operationId);
          const operation = operationResponse.data.operation;

          // Operation action 저장 (입력창 표시 제어용)
          // progress가 "completed"이고 action이 "receive"인 경우 "duplex"로 처리
          const normalizedProgress = selectedEndpoint?.progress?.toLowerCase();
          const isProgressCompleted =
            normalizedProgress === "completed" ||
            normalizedProgress === "complete";

          let normalizedAction = operation.action
            ? String(operation.action).toLowerCase()
            : null;

          // progress가 "completed"이고 action이 "receive"인 경우 "duplex"로 처리
          if (isProgressCompleted && normalizedAction === "receive") {
            normalizedAction = "duplex";
          }

          setOperationAction(normalizedAction);

          // Receive address 추출
          let receiveAddr = "";

          // 1. operation.action === "receive"인 경우 channel에서 추출
          if (operation.action === "receive" && operation.channel) {
            const channelRef = operation.channel.ref || "";
            const channelName = channelRef.replace("#/channels/", "");

            // Channel 정보 조회하여 실제 address 사용
            try {
              const channelResponse = await getWebSocketChannel(channelName);
              receiveAddr =
                channelResponse.data.channel?.address || channelName;
            } catch {
              receiveAddr = channelName;
            }
          } else if (
            operation.action === "send" ||
            String(operation.action) === "sendto"
          ) {
            // 2. SEND/SENDTO 타입의 경우, receive address는 tags[1]이나 path에서 추출
            // tags[1]에 receiver address가 저장되어 있음
            if (selectedEndpoint.tags?.[1]) {
              receiveAddr = selectedEndpoint.tags[1];
            } else if (selectedEndpoint.path) {
              const pathParts = selectedEndpoint.path.split(" - ");
              if (pathParts.length > 0) {
                receiveAddr = pathParts[0] || "";
              }
            }
          } else {
            // 3. 기타 경우: tags[1] 또는 path에서 추출 시도
            if (selectedEndpoint.tags?.[1]) {
              receiveAddr = selectedEndpoint.tags[1];
            } else if (selectedEndpoint.path) {
              const pathParts = selectedEndpoint.path.split(" - ");
              if (pathParts.length > 0) {
                receiveAddr = pathParts[0] || "";
              }
            }
          }
          setReceiveAddress(receiveAddr);

          // Reply address 추출
          let replyAddr = "";

          // 1. operation.reply에서 추출 (최우선)
          if (operation.reply && operation.reply.channel) {
            const replyChannelRef = operation.reply.channel.ref || "";
            const replyChannelName = replyChannelRef.replace("#/channels/", "");

            try {
              const channelResponse = await getWebSocketChannel(
                replyChannelName
              );
              replyAddr =
                channelResponse.data.channel?.address || replyChannelName;
            } catch {
              replyAddr = replyChannelName;
            }
          }

          // 2. operation.action === "send"인 경우 channel을 reply로 사용
          if (!replyAddr && operation.action === "send" && operation.channel) {
            const channelRef = operation.channel.ref || "";
            const channelName = channelRef.replace("#/channels/", "");

            try {
              const channelResponse = await getWebSocketChannel(channelName);
              replyAddr = channelResponse.data.channel?.address || channelName;
            } catch {
              replyAddr = channelName;
            }
          }

          // 3. path에서 reply address 추출 시도 ("receive - reply" 형태)
          // operation.action이 "receive"인 경우에도 path에서 추출 가능
          if (!replyAddr && selectedEndpoint.path) {
            const pathParts = selectedEndpoint.path.split(" - ");
            if (pathParts.length > 1) {
              replyAddr = pathParts[1] || "";
            }
          }

          // receive 액션만 있는 경우 Reply Address는 필요 없음

          setReplyAddress(replyAddr);

          // Receive operation의 messages에서 schema 로드
          if (operation.messages && operation.messages.length > 0) {
            const firstMessage = operation.messages[0];
            let schemaRef: string | null = null;

            // ref 형태인 경우
            if (firstMessage.ref) {
              const refValue = firstMessage.ref;
              schemaRef = refValue.includes("#/components/schemas/")
                ? refValue.replace("#/components/schemas/", "")
                : refValue;
            }

            // Schema 로드 및 필드 생성
            if (schemaRef) {
              try {
                const schemaResponse = await getWebSocketSchema(schemaRef);
                const schemaData = schemaResponse.data;

                if (schemaData.properties) {
                  const fields = Object.entries(schemaData.properties).map(
                    ([key, propSchema]: [string, unknown]) => {
                      return parseOpenAPISchemaToSchemaField(
                        key,
                        propSchema as Record<string, unknown>
                      );
                    }
                  );

                  // required 필드 설정
                  if (
                    schemaData.required &&
                    Array.isArray(schemaData.required)
                  ) {
                    fields.forEach((field) => {
                      if (schemaData.required!.includes(field.key)) {
                        field.required = true;
                      }
                    });
                  }

                  setMessageSchemaFields(fields);

                  // 기본값으로 formData 초기화
                  const defaultFormData: Record<string, unknown> = {};
                  fields.forEach((field) => {
                    // 기본값 생성 로직 (간단한 버전)
                    if (isPrimitiveSchema(field.schemaType)) {
                      switch (field.schemaType.type) {
                        case "string":
                          defaultFormData[field.key] = "";
                          break;
                        case "integer":
                        case "number":
                          defaultFormData[field.key] = 0;
                          break;
                        case "boolean":
                          defaultFormData[field.key] = false;
                          break;
                        default:
                          defaultFormData[field.key] = "";
                      }
                    } else {
                      defaultFormData[field.key] = "";
                    }
                  });
                  setMessageFormData(defaultFormData);
                } else {
                  setMessageSchemaFields([]);
                  setMessageFormData({});
                }
              } catch (error) {
                console.error("Schema 로드 실패:", error);
                setMessageSchemaFields([]);
                setMessageFormData({});
              }
            } else {
              setMessageSchemaFields([]);
              setMessageFormData({});
            }
          } else {
            setMessageSchemaFields([]);
            setMessageFormData({});
          }

          // 패턴 파라미터 초기화 (operation 로드 성공 시)
          const allParams = new Set<string>();
          if (receiveAddr) {
            extractPathParameters(receiveAddr).forEach((p) => allParams.add(p));
          }
          if (replyAddr) {
            extractPathParameters(replyAddr).forEach((p) => allParams.add(p));
          }

          const newPathParameters: Record<string, string> = {};
          allParams.forEach((param) => {
            newPathParameters[param] = "";
          });
          setPathParameters(newPathParameters);
        } catch (error) {
          console.error("WebSocket operation 정보 로드 실패:", error);
          // 실패 시 tags나 path에서 파싱 시도
          let receiveAddr = "";
          let replyAddr = "";

          // tags[1]에서 receive address 추출
          if (selectedEndpoint.tags?.[1]) {
            receiveAddr = selectedEndpoint.tags[1];
          }

          // path에서 추출 ("receive - reply" 형태)
          if (selectedEndpoint.path) {
            const pathParts = selectedEndpoint.path.split(" - ");
            if (pathParts.length > 0 && !receiveAddr) {
              receiveAddr = pathParts[0] || "";
            }
            if (pathParts.length > 1) {
              replyAddr = pathParts[1] || "";
            }
          }

          setReceiveAddress(receiveAddr);
          setReplyAddress(replyAddr);
          setOperationAction(null);

          // 패턴 파라미터 초기화 (에러 발생 시)
          const allParams = new Set<string>();
          if (receiveAddr) {
            extractPathParameters(receiveAddr).forEach((p) => allParams.add(p));
          }
          if (replyAddr) {
            extractPathParameters(replyAddr).forEach((p) => allParams.add(p));
          }

          const newPathParameters: Record<string, string> = {};
          allParams.forEach((param) => {
            newPathParameters[param] = "";
          });
          setPathParameters(newPathParameters);
        }
      } else {
        setEntryPoint("");
        setReceiveAddress("");
        setReplyAddress("");
        setOperationAction(null);
        setPathParameters({});
      }
    };

    loadWebSocketInfo();
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
      alert("Please enter an Entry Point.");
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
      if (
        !connectHeaders.some(
          (h) => h.key && h.key.toLowerCase() === "accept-version"
        )
      ) {
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
            content: JSON.stringify(
              { status: "Connected to STOMP server", url: entryPoint },
              null,
              2
            ),
          };
          addWsMessage(message);

          // Subscribe는 replyAddress로 구독 (서버가 클라이언트에게 메시지를 보낼 주소)
          // replyAddress가 /topic/ 또는 /queue/로 시작하는 경우에만 구독
          if (replyAddress && replyAddress.trim() !== "") {
            if (
              replyAddress.startsWith("/topic/") ||
              replyAddress.startsWith("/queue/")
            ) {
              // replyAddress로 구독 (패턴 치환 필요)
              let actualSubscribeAddress = replyAddress;
              if (hasPathParameter(replyAddress)) {
                actualSubscribeAddress = replacePathParameters(
                  replyAddress,
                  pathParameters
                );
                if (hasPathParameter(actualSubscribeAddress)) {
                  const missingParams = extractPathParameters(
                    actualSubscribeAddress
                  );
                  addWsMessage({
                    id: `msg-${Date.now()}-${Math.random()}`,
                    timestamp: Date.now(),
                    direction: "received" as const,
                    address: "WARNING",
                    content: JSON.stringify({
                      message: t("wsTest.subscriptionAddressHasPattern", {
                        params: missingParams.join(", "),
                      }),
                      replyAddress,
                      missingParameters: missingParams,
                    }),
                  });
                } else {
                  handleSubscribe(actualSubscribeAddress);
                }
              } else {
                handleSubscribe(actualSubscribeAddress);
              }
            } else {
              addWsMessage({
                id: `msg-${Date.now()}-${Math.random()}`,
                timestamp: Date.now(),
                direction: "received" as const,
                address: "INFO",
                content: JSON.stringify({
                  message: t("wsTest.replyAddressNotStartWithTopicOrQueue"),
                  replyAddress,
                }),
              });
            }
          } else {
            // replyAddress가 없으면 구독하지 않음
            // receive 액션만 있는 경우는 reply address가 없는 게 정상이므로 에러 메시지 표시하지 않음
            if (
              receiveAddress &&
              receiveAddress.trim() !== "" &&
              operationAction !== null &&
              operationAction !== "receive"
            ) {
              addWsMessage({
                id: `msg-${Date.now()}-${Math.random()}`,
                timestamp: Date.now(),
                direction: "received" as const,
                address: "INFO",
                content: JSON.stringify({
                  message: t("wsTest.replyAddressRequired"),
                  receiveAddress,
                }),
              });
            }
          }

          // Try 알림 구독 (백엔드: /user/queue/ouro/try)
          const tryNotificationDestination = "/user/queue/ouro/try";
          try {
            if (stompClientRef.current) {
              stompClientRef.current.subscribe(
                tryNotificationDestination,
                (frame) => {
                  // Try 알림 메시지에서 tryId 추출
                  // STOMP 프레임 헤더는 대소문자를 구분할 수 있으므로 모든 키를 확인
                  let tryIdHeader: string | undefined = undefined;

                  // 먼저 일반적인 키로 확인
                  tryIdHeader =
                    frame.headers["X-Ouroboros-Try-Id"] ||
                    frame.headers["x-ouroboros-try-id"] ||
                    frame.headers["X-OUROBOROS-TRY-ID"];

                  // 헤더에 tryId가 없으면 모든 헤더 키를 순회하면서 대소문자 구분 없이 찾기
                  if (!tryIdHeader && frame.headers) {
                    const headerKeys = Object.keys(frame.headers);
                    for (const key of headerKeys) {
                      if (key.toLowerCase() === "x-ouroboros-try-id") {
                        tryIdHeader = frame.headers[key];
                        break;
                      }
                    }
                  }

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
                      } catch {
                        // 파싱 실패 시 원본 문자열 유지
                        parsedPayload = dispatchMessage.payload;
                      }
                    }

                    // address는 headers의 destination을 사용, 없으면 tryNotificationDestination 사용
                    const destinationAddress =
                      dispatchMessage.headers?.destination ||
                      tryNotificationDestination;

                    // content는 payload만 표시
                    const payloadContent =
                      typeof parsedPayload === "object"
                        ? JSON.stringify(parsedPayload, null, 2)
                        : parsedPayload;

                    const tryMessage = {
                      id: `msg-${Date.now()}-${Math.random()}`,
                      timestamp: Date.now(),
                      direction: "sent" as const,
                      address: destinationAddress,
                      content: payloadContent,
                      tryId: tryIdHeader,
                    };

                    addWsMessage(tryMessage);
                    updateWsStats((prev) => ({
                      totalSent: (prev?.totalSent || 0) + 1,
                    }));
                  } catch {
                    // 파싱 실패 시 원본 body 그대로 표시
                    const tryMessage = {
                      id: `msg-${Date.now()}-${Math.random()}`,
                      timestamp: Date.now(),
                      direction: "sent" as const,
                      address: tryNotificationDestination,
                      content: frame.body,
                      tryId: tryIdHeader,
                    };
                    addWsMessage(tryMessage);
                    updateWsStats((prev) => ({
                      totalSent: (prev?.totalSent || 0) + 1,
                    }));
                  }
                }
              );
            }
          } catch {
            // Try 알림 구독 실패
          }
        },
        (error) => {
          setWsConnectionStatus("disconnected");
          setWsConnectionStartTime(null);
          updateWsStats({ connectionDuration: null });
          const errorMessage = error.message || "Unknown error";
          alert(
            `An error occurred during STOMP connection:\n\n${errorMessage}\n\nURL: ${entryPoint}`
          );

          // 에러 메시지도 로그에 추가
          const errorMsg = {
            id: `msg-${Date.now()}-${Math.random()}`,
            timestamp: Date.now(),
            direction: "received" as const,
            address: "ERROR",
            content: JSON.stringify(
              { error: errorMessage, url: entryPoint },
              null,
              2
            ),
          };
          addWsMessage(errorMsg);
        }
      );
    } catch {
      setWsConnectionStatus("disconnected");
      alert(t("wsTest.connectionFailed"));
    }
  };

  const handleDisconnect = () => {
    if (stompClientRef.current) {
      const client = stompClientRef.current;
      client.disconnect(() => {
        setWsConnectionStatus("disconnected");
        setWsConnectionStartTime(null);
        updateWsStats({ connectionDuration: null });
        setSubscriptions([]);
        stompClientRef.current = null;
      });
    } else {
      // 클라이언트가 없는 경우에도 상태 초기화
      setWsConnectionStatus("disconnected");
      setWsConnectionStartTime(null);
      updateWsStats({ connectionDuration: null });
      setSubscriptions([]);
    }
  };

  const handleSubscribe = (topic?: string) => {
    const destination = topic || newTopic.trim();

    if (!destination) {
      alert("Please enter a Topic.");
      return;
    }

    if (!stompClientRef.current || !stompClientRef.current.isConnected()) {
      alert(t("wsTest.pleaseConnectFirst"));
      return;
    }

    // 이미 구독 해제된 항목이 있는지 확인
    const existingSubscription = subscriptions.find(
      (sub) => sub.destination === destination && sub.subscriptionId === null
    );

    try {
      const subscriptionId = stompClientRef.current.subscribe(
        destination,
        (frame) => {
          // 응답 메시지에서 X-Ouroboros-Try-Id 헤더 추출
          // STOMP 프레임 헤더는 대소문자를 구분할 수 있으므로 모든 키를 확인
          let tryIdHeader: string | undefined = undefined;

          // 먼저 일반적인 키로 확인
          tryIdHeader =
            frame.headers["X-Ouroboros-Try-Id"] ||
            frame.headers["x-ouroboros-try-id"] ||
            frame.headers["X-OUROBOROS-TRY-ID"];

          // 헤더에 tryId가 없으면 모든 헤더 키를 순회하면서 대소문자 구분 없이 찾기
          if (!tryIdHeader && frame.headers) {
            const headerKeys = Object.keys(frame.headers);
            for (const key of headerKeys) {
              if (key.toLowerCase() === "x-ouroboros-try-id") {
                tryIdHeader = frame.headers[key];
                break;
              }
            }
          }

          // 헤더에 tryId가 없으면 메시지 본문에서 추출 시도
          if (!tryIdHeader && frame.body) {
            try {
              const bodyJson = JSON.parse(frame.body);
              // 본문에 tryId가 있는 경우 (예: { tryId: "...", ... })
              if (bodyJson.tryId) {
                tryIdHeader = bodyJson.tryId;
              }
              // 본문의 headers에 tryId가 있는 경우 (예: { headers: { "X-Ouroboros-Try-Id": "..." }, ... })
              else if (bodyJson.headers) {
                // headers 객체의 모든 키를 확인
                const headerKeys = Object.keys(bodyJson.headers);
                for (const key of headerKeys) {
                  if (key.toLowerCase() === "x-ouroboros-try-id") {
                    tryIdHeader = bodyJson.headers[key];
                    break;
                  }
                }
              }
            } catch {
              // JSON 파싱 실패 시 무시
            }
          }

          // tryId가 있으면 store에 저장
          if (tryIdHeader) {
            setTryId(tryIdHeader);
          }

          // 수신된 MESSAGE의 address는 frame.headers.destination을 사용 (서버가 보낸 실제 destination)
          // 이것은 replyAddress와 일치해야 함 (receiveAddress가 아님)
          const messageAddress = frame.headers.destination || destination;

          const message = {
            id: `msg-${Date.now()}-${Math.random()}`,
            timestamp: Date.now(),
            direction: "received" as const,
            address: messageAddress,
            content: frame.body,
            tryId: tryIdHeader || undefined,
          };
          addWsMessage(message);
          updateWsStats((prev) => ({
            totalReceived: (prev?.totalReceived || 0) + 1,
          }));
        }
      );

      if (existingSubscription) {
        // 기존 구독 해제된 항목을 다시 활성화
        setSubscriptions(
          subscriptions.map((s) =>
            s.id === existingSubscription.id ? { ...s, subscriptionId } : s
          )
        );
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
        content: JSON.stringify(
          { action: "SUBSCRIBE", destination: destination },
          null,
          2
        ),
      };
      addWsMessage(message);
    } catch {
      alert("Subscription failed.");
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
      setSubscriptions(
        subscriptions.map((s) =>
          s.id === subscription.id ? { ...s, subscriptionId: null } : s
        )
      );

      // 구독 해제 메시지 로그
      const message = {
        id: `msg-${Date.now()}-${Math.random()}`,
        timestamp: Date.now(),
        direction: "sent" as const,
        address: subscription.destination,
        content: JSON.stringify(
          { action: "UNSUBSCRIBE", destination: subscription.destination },
          null,
          2
        ),
      };
      addWsMessage(message);
    } catch {
      // 구독 해제 실패
    }
  };

  // 간단한 모드 메시지 전송
  const handleSimpleSend = () => {
    if (wsConnectionStatus !== "connected") {
      alert(t("wsTest.pleaseConnectFirst"));
      return;
    }

    if (!stompClientRef.current) {
      alert("STOMP client is not initialized.");
      return;
    }

    if (!stompClientRef.current.isConnected()) {
      alert(t("wsTest.connectionLostPleaseReconnect"));
      setWsConnectionStatus("disconnected");
      return;
    }

    // Send는 receiveAddress로 전송 (클라이언트→서버)
    // receiveAddress가 없으면 replyAddress로 전송 시도
    let destination = "";

    if (receiveAddress && receiveAddress.trim() !== "") {
      destination = receiveAddress;
    } else if (replyAddress && replyAddress.trim() !== "") {
      destination = replyAddress;
    }

    if (!destination) {
      alert(t("wsTest.cannotSendMessageSetAddress"));
      return;
    }

    // 패턴이 포함되어 있으면 실제 값으로 치환
    const actualDestination = hasPathParameter(destination)
      ? replacePathParameters(destination, pathParameters)
      : destination;

    // 패턴이 치환되지 않았으면 경고
    if (hasPathParameter(actualDestination)) {
      const params = extractPathParameters(actualDestination);
      const missingParams = params.filter((p) => !pathParameters[p]);
      if (missingParams.length > 0) {
        alert(
          t("wsTest.cannotSendMessagePatternInAddress", {
            destination: actualDestination,
            params: missingParams.join(", "),
          })
        );
        return;
      }
    }

    // 실제 destination 사용
    destination = actualDestination;

    // 자동 생성 필드인지 확인하는 헬퍼 함수
    const isAutoGeneratedField = (field: SchemaField): boolean => {
      const fieldKeyLower = field.key.toLowerCase();
      // 필드명 기반 판단
      const isAutoFieldName =
        fieldKeyLower === "sentat" ||
        fieldKeyLower === "sent_at" ||
        fieldKeyLower === "createdat" ||
        fieldKeyLower === "created_at" ||
        fieldKeyLower === "timestamp" ||
        field.key === "sentAt" ||
        field.key === "createdAt";

      // 타입 기반 판단 (date-time 또는 instant)
      const isDateTimeField =
        isPrimitiveSchema(field.schemaType) &&
        field.schemaType.type === "string" &&
        (field.schemaType.format === "date-time" ||
          field.schemaType.format === "instant" ||
          field.schemaType.format === "date");

      return isAutoFieldName || isDateTimeField;
    };

    // Schema 기반 메시지 생성
    let messageBody: string;
    if (messageSchemaFields.length > 0) {
      // Schema 필드가 있으면 formData를 JSON으로 변환
      const messageData: Record<string, unknown> = {};
      messageSchemaFields.forEach((field) => {
        // 자동 생성 필드는 항상 현재 시간으로 설정
        if (isAutoGeneratedField(field)) {
          messageData[field.key] = new Date().toISOString();
        } else {
          const value = messageFormData[field.key];
          // undefined나 빈 문자열이 아닌 경우만 포함
          if (value !== undefined && value !== "") {
            messageData[field.key] = value;
          }
        }
      });
      messageBody = JSON.stringify(messageData);
    } else {
      // Schema 필드가 없으면 기본 형식 사용 (하위 호환성)
      if (
        !messageFormData.content ||
        String(messageFormData.content).trim() === ""
      ) {
        alert("Please enter a message.");
        return;
      }
      messageBody = JSON.stringify({
        sender: messageFormData.sender || "tester",
        content: messageFormData.content,
        type: messageFormData.type || "TALK",
        sentAt: new Date().toISOString(),
      });
    }

    try {
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
      updateWsStats((prev) => ({
        totalSent: (prev?.totalSent || 0) + 1,
      }));

      // FormData 초기화 (기본값으로)
      if (messageSchemaFields.length > 0) {
        const defaultFormData: Record<string, unknown> = {};
        messageSchemaFields.forEach((field) => {
          // 자동 생성 필드는 초기화하지 않음 (항상 전송 시 현재 시간으로 설정됨)
          const fieldKeyLower = field.key.toLowerCase();
          const isAutoFieldName =
            fieldKeyLower === "sentat" ||
            fieldKeyLower === "sent_at" ||
            fieldKeyLower === "createdat" ||
            fieldKeyLower === "created_at" ||
            fieldKeyLower === "timestamp" ||
            field.key === "sentAt" ||
            field.key === "createdAt";

          const isDateTimeField =
            isPrimitiveSchema(field.schemaType) &&
            field.schemaType.type === "string" &&
            (field.schemaType.format === "date-time" ||
              field.schemaType.format === "instant" ||
              field.schemaType.format === "date");

          if (isAutoFieldName || isDateTimeField) {
            // 자동 생성 필드는 초기화하지 않음
            return;
          }

          if (isPrimitiveSchema(field.schemaType)) {
            switch (field.schemaType.type) {
              case "string":
                defaultFormData[field.key] = "";
                break;
              case "integer":
              case "number":
                defaultFormData[field.key] = 0;
                break;
              case "boolean":
                defaultFormData[field.key] = false;
                break;
              default:
                defaultFormData[field.key] = "";
            }
          } else {
            defaultFormData[field.key] = "";
          }
        });
        setMessageFormData(defaultFormData);
      }
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : "Unknown error";
      alert(`Failed to send message:\n\n${errorMessage}`);
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
                ? t("wsTest.connected")
                : wsConnectionStatus === "connecting"
                ? t("wsTest.connecting")
                : t("wsTest.disconnected")}
            </span>
          </div>
        </div>
      </div>

      <div className="p-4">
        {/* 연결 설정 Section */}
        <div className="mb-6 space-y-4">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3]">
            {t("wsTest.connectionSettings")}
          </h3>

          <div>
            <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
              {t("wsTest.wsEndpoint")}
            </label>
            <input
              type="text"
              value={entryPoint}
              onChange={(e) => setEntryPoint(e.target.value)}
              placeholder="ws://localhost:8080/ws"
              disabled={wsConnectionStatus === "connected"}
              className="w-full px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-500 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-0 focus-visible:outline-none text-sm font-mono disabled:opacity-50 disabled:cursor-not-allowed"
            />
          </div>

          {/* Receive Address - duplex 또는 receive일 때만 표시 */}
          {(operationAction === "duplex" || operationAction === "receive") && (
            <div>
              <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                {t("wsTest.receiveAddress")}
              </label>
              <input
                type="text"
                value={receiveAddress}
                onChange={(e) => {
                  setReceiveAddress(e.target.value);
                  // 패턴 파라미터 업데이트
                  const params = extractPathParameters(e.target.value);
                  const newParams = { ...pathParameters };
                  params.forEach((p) => {
                    if (!newParams[p]) {
                      newParams[p] = "";
                    }
                  });
                  setPathParameters(newParams);
                }}
                placeholder="/topic/chat/room1"
                disabled={wsConnectionStatus === "connected"}
                className="w-full px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-500 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-0 focus-visible:outline-none text-sm disabled:opacity-50 disabled:cursor-not-allowed"
              />
              {hasPathParameter(receiveAddress) && (
                <p className="mt-1 text-xs text-amber-600 dark:text-amber-400">
                  {t("wsTest.addressContainsPattern")}
                </p>
              )}
            </div>
          )}

          {/* Reply Address - duplex 또는 send/sendto일 때만 표시 */}
          {(operationAction === "duplex" ||
            operationAction === "send" ||
            operationAction === "sendto") && (
            <div>
              <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                {t("wsTest.replyAddress")}
              </label>
              <input
                type="text"
                value={replyAddress}
                onChange={(e) => {
                  setReplyAddress(e.target.value);
                  // 패턴 파라미터 업데이트
                  const params = extractPathParameters(e.target.value);
                  const newParams = { ...pathParameters };
                  params.forEach((p) => {
                    if (!newParams[p]) {
                      newParams[p] = "";
                    }
                  });
                  setPathParameters(newParams);
                }}
                placeholder="/app/chat/room1"
                disabled={wsConnectionStatus === "connected"}
                className="w-full px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-500 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-0 focus-visible:outline-none text-sm disabled:opacity-50 disabled:cursor-not-allowed"
              />
              {hasPathParameter(replyAddress) && (
                <p className="mt-1 text-xs text-amber-600 dark:text-amber-400">
                  패턴이 포함된 주소입니다. 아래 파라미터를 입력해주세요.
                </p>
              )}
            </div>
          )}

          {/* Path Parameters 입력 필드 - 패턴이 있을 때만 표시 */}
          {(() => {
            const allParams = new Set<string>();
            if (receiveAddress) {
              extractPathParameters(receiveAddress).forEach((p) =>
                allParams.add(p)
              );
            }
            if (replyAddress) {
              extractPathParameters(replyAddress).forEach((p) =>
                allParams.add(p)
              );
            }
            const paramArray = Array.from(allParams);

            if (paramArray.length === 0) return null;

            return (
              <div className="border border-amber-200 dark:border-amber-800 rounded-md p-3 bg-amber-50 dark:bg-amber-900/20">
                <label className="block text-xs font-medium text-amber-800 dark:text-amber-300 mb-2">
                  {t("wsTest.pathParameters")}
                </label>
                <p className="text-xs text-amber-700 dark:text-amber-400 mb-3">
                  {t("wsTest.pathPatternDescription")}
                </p>
                <div className="space-y-2">
                  {paramArray.map((param) => (
                    <div key={param}>
                      <label className="block text-xs font-medium text-amber-800 dark:text-amber-300 mb-1">
                        {param}
                      </label>
                      <input
                        type="text"
                        value={pathParameters[param] || ""}
                        onChange={(e) => {
                          const newParams = {
                            ...pathParameters,
                            [param]: e.target.value,
                          };
                          setPathParameters(newParams);

                          // 연결된 상태이고 receiveAddress에 패턴이 있으면 재구독
                          if (
                            wsConnectionStatus === "connected" &&
                            receiveAddress &&
                            hasPathParameter(receiveAddress)
                          ) {
                            const actualReceiveAddress = replacePathParameters(
                              receiveAddress,
                              newParams
                            );

                            // 모든 파라미터가 입력되었는지 확인
                            const allParams =
                              extractPathParameters(receiveAddress);
                            const allParamsFilled = allParams.every(
                              (p) => newParams[p] && newParams[p].trim() !== ""
                            );

                            if (
                              allParamsFilled &&
                              !hasPathParameter(actualReceiveAddress) &&
                              actualReceiveAddress
                            ) {
                              // 기존 구독 해제 - receiveAddress 패턴과 관련된 모든 구독 찾기
                              const existingSubs = subscriptions.filter(
                                (s) =>
                                  s.subscriptionId !== null &&
                                  (s.destination === receiveAddress ||
                                    (hasPathParameter(receiveAddress) &&
                                      s.destination.startsWith(
                                        receiveAddress.split("{")[0]
                                      )) ||
                                    // 이미 치환된 주소로 구독된 경우도 찾기
                                    s.destination === actualReceiveAddress)
                              );

                              // 모든 관련 구독 해제
                              existingSubs.forEach((sub) => {
                                if (sub.subscriptionId) {
                                  handleUnsubscribe(sub);
                                }
                              });

                              // 잠시 후 재구독 (구독 해제 완료 대기)
                              setTimeout(() => {
                                handleSubscribe(actualReceiveAddress);
                              }, 200);
                            }
                          }
                        }}
                        placeholder={t("wsTest.pathParameterPlaceholder", {
                          param,
                        })}
                        className="w-full px-3 py-2 rounded-md bg-white dark:bg-[#0D1117] border border-amber-300 dark:border-amber-700 text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-500 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-0 focus-visible:outline-none text-sm"
                      />
                      {/* 치환된 주소 미리보기 */}
                      {pathParameters[param] && (
                        <p className="mt-1 text-xs text-amber-600 dark:text-amber-400 font-mono">
                          {receiveAddress &&
                            hasPathParameter(receiveAddress) && (
                              <span>
                                {t("wsTest.receive")}:{" "}
                                {replacePathParameters(receiveAddress, {
                                  ...pathParameters,
                                  [param]: pathParameters[param],
                                })}
                                <br />
                              </span>
                            )}
                          {replyAddress && hasPathParameter(replyAddress) && (
                            <span>
                              {t("wsEditor.reply")}:{" "}
                              {replacePathParameters(replyAddress, {
                                ...pathParameters,
                                [param]: pathParameters[param],
                              })}
                            </span>
                          )}
                        </p>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            );
          })()}

          <div className="flex gap-2">
            {wsConnectionStatus === "disconnected" ? (
              <button
                onClick={handleConnect}
                disabled={!isConnectEnabled}
                className={`flex-1 px-4 py-2 rounded-md transition-all text-sm font-semibold active:translate-y-[1px] focus:outline-none focus-visible:outline-none md:flex-none md:w-auto w-full ${
                  !isConnectEnabled
                    ? "bg-gray-300 dark:bg-gray-700 text-gray-500 dark:text-gray-400 cursor-not-allowed"
                    : "bg-[#2563EB] hover:bg-[#1E40AF] text-white"
                }`}
              >
                {t("wsTest.connect")}
              </button>
            ) : (
              <button
                onClick={handleDisconnect}
                className="flex-1 px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-md transition-all text-sm font-semibold active:translate-y-[1px] focus:outline-none focus-visible:outline-none md:flex-none md:w-auto w-full"
              >
                {t("wsTest.disconnect")}
              </button>
            )}
          </div>
        </div>

        {/* STOMP CONNECT Headers - Collapsible */}
        {wsConnectionStatus === "disconnected" && (
          <div className="mb-4 border border-gray-200 dark:border-[#2D333B] rounded-md p-3">
            <div className="flex items-center justify-between mb-2">
              <label className="text-xs font-medium text-gray-600 dark:text-[#8B949E]">
                {t("wsTest.stompConnectHeaders")}
              </label>
              <button
                onClick={addConnectHeader}
                className="text-xs px-2 py-1 bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded-md transition-all active:translate-y-[1px] focus:outline-none focus-visible:outline-none"
              >
                {t("apiCard.addHeader")}
              </button>
            </div>
            {connectHeaders.length === 0 ? (
              <p className="text-xs text-gray-500 dark:text-[#8B949E] text-center py-2">
                {t("wsTest.connectWithDefaultSettings")}
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
                      placeholder={t("wsTest.key")}
                      className="flex-1 px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-500 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-0 focus-visible:outline-none text-sm"
                    />
                    <input
                      type="text"
                      value={header.value}
                      onChange={(e) =>
                        updateConnectHeader(index, header.key, e.target.value)
                      }
                      placeholder={t("wsTest.value")}
                      className="flex-1 px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-500 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-0 focus-visible:outline-none text-sm"
                    />
                    <button
                      onClick={() => removeConnectHeader(index)}
                      className="px-3 py-2 text-red-500 hover:text-red-700"
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
            )}
          </div>
        )}
      </div>

      {/* Main Content - Connected State */}
      {wsConnectionStatus === "connected" && operationAction !== "reply" && (
        <div className="border-t border-gray-200 dark:border-[#2D333B] p-4">
          <div className="space-y-4">
            <h3 className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3]">
              Send Message
            </h3>

            {/* Schema 기반 입력 필드 */}
            {messageSchemaFields.length > 0 ? (
              <div className="space-y-4">
                {messageSchemaFields.map((field) => {
                  // 자동 생성 필드인지 확인
                  const fieldKeyLower = field.key.toLowerCase();
                  const isAutoFieldName =
                    fieldKeyLower === "sentat" ||
                    fieldKeyLower === "sent_at" ||
                    fieldKeyLower === "createdat" ||
                    fieldKeyLower === "created_at" ||
                    fieldKeyLower === "timestamp" ||
                    field.key === "sentAt" ||
                    field.key === "createdAt";

                  const isDateTimeField =
                    isPrimitiveSchema(field.schemaType) &&
                    field.schemaType.type === "string" &&
                    (field.schemaType.format === "date-time" ||
                      field.schemaType.format === "instant" ||
                      field.schemaType.format === "date");

                  const isAutoGenerated = isAutoFieldName || isDateTimeField;

                  // 자동 생성 필드는 읽기 전용으로 표시
                  if (isAutoGenerated) {
                    return (
                      <div key={field.key}>
                        <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                          {field.key}
                          {field.required && (
                            <span className="text-red-500 ml-1">*</span>
                          )}
                          {field.description && (
                            <span className="text-xs text-gray-500 dark:text-[#8B949E] ml-2">
                              ({field.description})
                            </span>
                          )}
                          <span className="text-xs text-blue-600 dark:text-blue-400 ml-2">
                            (auto)
                          </span>
                        </label>
                        <input
                          type="text"
                          value={new Date().toISOString()}
                          readOnly
                          disabled
                          className="w-full px-3 py-2 rounded-md bg-gray-100 dark:bg-[#161B22] border border-gray-300 dark:border-[#2D333B] text-gray-600 dark:text-[#8B949E] cursor-not-allowed text-sm"
                        />
                      </div>
                    );
                  }

                  const value = messageFormData[field.key] ?? "";
                  let isBoolean = false;
                  let isNumber = false;
                  let primitiveType:
                    | "string"
                    | "integer"
                    | "number"
                    | "boolean"
                    | "file"
                    | null = null;

                  if (isPrimitiveSchema(field.schemaType)) {
                    primitiveType = field.schemaType.type;
                    isBoolean = field.schemaType.type === "boolean";
                    isNumber =
                      field.schemaType.type === "integer" ||
                      field.schemaType.type === "number";
                  }

                  return (
                    <div key={field.key}>
                      <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                        {field.key}
                        {field.required && (
                          <span className="text-red-500 ml-1">*</span>
                        )}
                        {field.description && (
                          <span className="text-xs text-gray-500 dark:text-[#8B949E] ml-2">
                            ({field.description})
                          </span>
                        )}
                      </label>
                      {isBoolean ? (
                        <select
                          value={String(value)}
                          onChange={(e) => {
                            setMessageFormData({
                              ...messageFormData,
                              [field.key]: e.target.value === "true",
                            });
                          }}
                          className="w-full px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] focus:outline-none focus:ring-0 focus-visible:outline-none text-sm"
                        >
                          <option value="false">false</option>
                          <option value="true">true</option>
                        </select>
                      ) : isNumber && primitiveType ? (
                        <input
                          type="number"
                          value={typeof value === "number" ? value : ""}
                          onChange={(e) => {
                            const numValue =
                              primitiveType === "integer"
                                ? parseInt(e.target.value) || 0
                                : parseFloat(e.target.value) || 0;
                            setMessageFormData({
                              ...messageFormData,
                              [field.key]: numValue,
                            });
                          }}
                          placeholder={
                            primitiveType === "integer" ? "0" : "0.0"
                          }
                          className="w-full px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-500 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-0 focus-visible:outline-none text-sm"
                        />
                      ) : (
                        <input
                          type="text"
                          value={String(value)}
                          onChange={(e) => {
                            setMessageFormData({
                              ...messageFormData,
                              [field.key]: e.target.value,
                            });
                          }}
                          placeholder={field.key}
                          onKeyDown={(e) => {
                            if (e.key === "Enter") {
                              handleSimpleSend();
                            }
                          }}
                          className="w-full px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-500 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-0 focus-visible:outline-none text-sm"
                        />
                      )}
                    </div>
                  );
                })}
              </div>
            ) : (
              // Schema 필드가 없을 때 기본 입력 필드 (하위 호환성)
              <>
                <div>
                  <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                    Sender
                  </label>
                  <input
                    type="text"
                    value={
                      typeof messageFormData.sender === "string"
                        ? messageFormData.sender
                        : ""
                    }
                    onChange={(e) => {
                      setMessageFormData({
                        ...messageFormData,
                        sender: e.target.value,
                      });
                    }}
                    placeholder="tester"
                    className="w-full px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-500 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-0 focus-visible:outline-none text-sm"
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                    Content
                  </label>
                  <input
                    type="text"
                    value={
                      typeof messageFormData.content === "string"
                        ? messageFormData.content
                        : ""
                    }
                    onChange={(e) => {
                      setMessageFormData({
                        ...messageFormData,
                        content: e.target.value,
                      });
                    }}
                    placeholder="Message"
                    onKeyDown={(e) => {
                      if (e.key === "Enter") {
                        handleSimpleSend();
                      }
                    }}
                    className="w-full px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-500 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-0 focus-visible:outline-none text-sm"
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-2">
                    Type
                  </label>
                  <select
                    value={
                      typeof messageFormData.type === "string"
                        ? messageFormData.type
                        : "TALK"
                    }
                    onChange={(e) => {
                      setMessageFormData({
                        ...messageFormData,
                        type: e.target.value,
                      });
                    }}
                    className="w-full px-3 py-2 rounded-md bg-gray-50 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] text-gray-900 dark:text-[#E6EDF3] focus:outline-none focus:ring-0 focus-visible:outline-none text-sm"
                  >
                    <option value="TALK">TALK</option>
                    <option value="ENTER">ENTER</option>
                    <option value="LEAVE">LEAVE</option>
                  </select>
                </div>
              </>
            )}

            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="enableTryHeader"
                checked={enableTryHeader}
                onChange={(e) => setEnableTryHeader(e.target.checked)}
                className="w-4 h-4 text-gray-600 dark:text-gray-400 bg-gray-100 border-gray-300 rounded focus:ring-0 focus-visible:outline-none"
              />
              <label
                htmlFor="enableTryHeader"
                className="text-xs font-medium text-gray-700 dark:text-[#E6EDF3]"
              >
                X-Ouroboros-Try
              </label>
              <span className="text-xs text-gray-500 dark:text-[#8B949E]">
                Add trace header
              </span>
            </div>

            <button
              onClick={handleSimpleSend}
              className="w-full md:w-auto px-4 py-2 bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded-md transition-all text-sm font-semibold active:translate-y-[1px] focus:outline-none focus-visible:outline-none"
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
                        <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-0 rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-gray-600"></div>
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
          <label className="flex items-center gap-2 text-xs font-medium text-gray-600 dark:text-[#8B949E] mb-3">
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
                d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
              />
            </svg>
            {t("wsTest.subscriptions", {
              active: subscriptions.filter((s) => s.subscriptionId !== null)
                .length,
              total: subscriptions.length,
            })}
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
                    <span
                      className={`text-sm font-mono ${
                        isActive
                          ? "text-gray-900 dark:text-[#E6EDF3]"
                          : "text-gray-500 dark:text-[#8B949E]"
                      }`}
                    >
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
                    <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-0 rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-gray-600"></div>
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
