import { create } from "zustand";
import { persist } from "zustand/middleware";
import { getAllRestApiSpecs, getAllWebSocketOperations, getAllWebSocketChannels } from "@/features/spec/services/api";
import type { RestApiSpecResponse, OperationResponse } from "@/features/spec/services/api";

export interface Endpoint {
  id: string;
  method: string;
  path: string;
  description: string;
  implementationStatus?: "not-implemented" | "in-progress" | "modifying";
  hasSpecError?: boolean;
  tags?: string[];
  progress?: string;
  tag?: string;
  diff?: string;
  protocol?: Protocol; // 프로토콜 정보 추가
  operationName?: string; // WebSocket operation name (조회용)
}

export interface EndpointData {
  [group: string]: Endpoint[];
}

export type Protocol = "REST" | "GraphQL" | "WebSocket" | null;

interface SidebarState {
  isOpen: boolean;
  toggle: () => void;
  isDarkMode: boolean;
  toggleDarkMode: () => void;
  selectedEndpoint: Endpoint | null;
  setSelectedEndpoint: (endpoint: Endpoint | null) => void;
  endpoints: EndpointData;
  setEndpoints: (endpoints: EndpointData) => void;
  updateEndpoint: (updatedEndpoint: Endpoint) => void;
  deleteEndpoint: (endpointId: string) => void;
  addEndpoint: (endpoint: Endpoint, group: string) => void;
  triggerNewForm: boolean;
  setTriggerNewForm: (trigger: boolean) => void;
  loadEndpoints: () => Promise<void>;
  isLoading: boolean;
  protocol: Protocol;
  setProtocol: (protocol: Protocol) => void;
}

// 백엔드 스펙을 프론트엔드 엔드포인트 형태로 변환
function convertSpecToEndpoint(spec: RestApiSpecResponse): Endpoint {
  // tag 매핑: none=미구현, implementing=구현중, bugfix=수정중
  const mapTagToStatus = (
    tag?: string,
    progress?: string
  ): Endpoint["implementationStatus"] => {
    if (progress?.toLowerCase() === "completed") return undefined;
    switch (tag) {
      case "implementing":
        return "in-progress";
      case "bugfix":
        return "modifying";
      case "none":
      default:
        return "not-implemented";
    }
  };

  return {
    id: spec.id,
    method: spec.method,
    path: spec.path,
    description: spec.description || spec.summary || "",
    implementationStatus: mapTagToStatus(spec.tag, spec.progress),
    hasSpecError: spec.diff && spec.diff !== "none" ? true : undefined,
    tags: spec.tags,
    progress: spec.progress,
    tag: spec.tag,
    diff: spec.diff,
    protocol: "REST", // 현재는 REST API만 지원하므로 기본값은 REST
  };
}

// WebSocket Operation을 프론트엔드 엔드포인트 형태로 변환
function convertOperationToEndpoint(
  operationResponse: OperationResponse,
  channelMap: Map<string, string>
): Endpoint {
  const { operationName, operation, tag: _tag } = operationResponse;
  const tag = _tag;
  
  // tag 매핑: receive, duplicate, sendto에 따라 상태 설정
  const mapTagToStatus = (
    _tag?: string,
    progress?: string
  ): Endpoint["implementationStatus"] => {
    if (progress?.toLowerCase() === "completed") return undefined;
    // progress가 "mock"이면 in-progress로 표시
    if (progress?.toLowerCase() === "mock") return "in-progress";
    // progress가 "none"이면 not-implemented로 표시
    return "not-implemented";
  };

  // tag에 따라 method 표시
  let method = "RECEIVE";
  if (tag === "duplicate") {
    method = "DUPLEX";
  } else if (tag === "receive") {
    method = "RECEIVE";
  } else if (tag === "sendto") {
    method = "SEND";
  }
  
  // entrypoint
  const entrypoint = operation.entrypoint || "/ws";

  // receiver address 추출
  const channelRef = operation.channel?.ref || "";
  const channelName = channelRef.replace('#/channels/', '');
  const receiverAddress = channelMap.get(channelName) || channelName || "/unknown";

  // reply address 추출 (있는 경우)
  let replyAddress = "";
  if (operation.reply && operation.reply.channel) {
    const replyChannelRef = operation.reply.channel.ref || "";
    const replyChannelName = replyChannelRef.replace('#/channels/', '');
    replyAddress = channelMap.get(replyChannelName) || replyChannelName || "/unknown";
  }

  // Path 생성: "receive address - reply address" 형태
  let path = receiverAddress;
  if (replyAddress) {
    path = `${receiverAddress} - ${replyAddress}`;
  }

  // Description 생성
  let description = "";
  if (tag === "sendto") {
    description = `Send to ${receiverAddress}`;
  } else if (tag === "duplicate") {
    description = `Duplex: ${receiverAddress} ⇄ ${replyAddress}`;
  } else {
    description = `Receive from ${receiverAddress}`;
  }

  return {
    id: operation.id || operationName,
    method: method,
    path: path,
    description: description,
    implementationStatus: mapTagToStatus(tag, operation.progress),
    hasSpecError: operation.diff && operation.diff !== "none" ? true : undefined,
    tags: [entrypoint, receiverAddress], // [entrypoint, receiverAddress] 저장 (그룹화용)
    progress: operation.progress,
    tag: tag,
    diff: operation.diff,
    protocol: "WebSocket",
    operationName: operationName,
  };
}

export const useSidebarStore = create<SidebarState>()(
  persist(
    (set, get) => ({
      isOpen: true,
      toggle: () => set((state) => ({ isOpen: !state.isOpen })),
      isDarkMode: false,
      toggleDarkMode: () => set((state) => ({ isDarkMode: !state.isDarkMode })),
      selectedEndpoint: null,
      setSelectedEndpoint: (endpoint) => set({ selectedEndpoint: endpoint }),
      endpoints: {},
      setEndpoints: (endpoints) => set({ endpoints }),
      updateEndpoint: (updatedEndpoint) => {
        const { endpoints } = get();
        const newEndpoints = { ...endpoints };

        Object.keys(newEndpoints).forEach((group) => {
          newEndpoints[group] = newEndpoints[group].map((ep) =>
            ep.id === updatedEndpoint.id ? updatedEndpoint : ep
          );
        });

        set({ endpoints: newEndpoints });
      },
      deleteEndpoint: (endpointId) => {
        const { endpoints } = get();
        const newEndpoints = { ...endpoints };

        Object.keys(newEndpoints).forEach((group) => {
          newEndpoints[group] = newEndpoints[group].filter(
            (ep) => ep.id !== endpointId
          );
        });

        set({ endpoints: newEndpoints, selectedEndpoint: null });
      },
      addEndpoint: (endpoint, group) => {
        const { endpoints } = get();
        const newEndpoints = { ...endpoints };

        if (!newEndpoints[group]) {
          newEndpoints[group] = [];
        }

        newEndpoints[group] = [...newEndpoints[group], endpoint];
        set({ endpoints: newEndpoints, selectedEndpoint: endpoint });
      },
      triggerNewForm: false,
      setTriggerNewForm: (trigger) => set({ triggerNewForm: trigger }),
      isLoading: false,
      protocol: null,
      setProtocol: (protocol) => set({ protocol }),
      loadEndpoints: async () => {
        set({ isLoading: true });
        try {
          // REST API 스펙 로드
          const restResponse = await getAllRestApiSpecs();
          const restSpecs = restResponse.data;

          // 스펙을 그룹별로 분류
          const grouped: EndpointData = {};

          restSpecs.forEach((spec) => {
            // tags를 그룹 키로 사용 (첫 번째 태그 또는 기본값)
            const group =
              spec.tags && spec.tags.length > 0 ? spec.tags[0] : "OTHERS";

            if (!grouped[group]) {
              grouped[group] = [];
            }

            grouped[group].push(convertSpecToEndpoint(spec));
          });

          // WebSocket Operations 로드
          try {
            // 1. Channels 로드하여 channel name → address 매핑 생성
            const channelMap = new Map<string, string>();
            try {
              const channelsResponse = await getAllWebSocketChannels();
              channelsResponse.data.forEach((channelResponse) => {
                // ChannelResponse: { channelName, channel: { address, ... } }
                const channelName = channelResponse.channelName;
                const address = channelResponse.channel?.address;
                if (channelName && address) {
                  channelMap.set(channelName, address);
                }
              });
              console.log("✅ Loaded channel mappings:", channelMap);
            } catch (channelError) {
              console.warn("Channel 로드 실패, 추정 값 사용:", channelError);
            }

            // 2. Operations 로드
            const wsResponse = await getAllWebSocketOperations();
            const wsOperations = wsResponse.data;

            // 3. WebSocket Operations를 Entry Point > Receiver Address 계층으로 그룹화
            wsOperations.forEach((operation) => {
              console.log("🔍 Processing operation:", {
                operationName: operation.operationName,
                operationId: operation.operation?.id,
                action: operation.operation?.action,
                tag: operation.tag
              });
              
              const endpoint = convertOperationToEndpoint(operation, channelMap);
              
              console.log("✅ Converted endpoint:", {
                id: endpoint.id,
                method: endpoint.method,
                path: endpoint.path,
                operationName: endpoint.operationName
              });
              
              // tags[0] = entrypoint, tags[1] = receiverAddress
              const entrypoint = endpoint.tags?.[0] || "/ws";
              const receiverAddress = endpoint.tags?.[1] || "/unknown";
              
              // 그룹명: "Entry Point > Receiver Address"
              const wsGroup = `${entrypoint} > ${receiverAddress}`;

              if (!grouped[wsGroup]) {
                grouped[wsGroup] = [];
              }
              grouped[wsGroup].push(endpoint);
            });
          } catch (wsError) {
            console.warn("WebSocket Operations 로드 실패:", wsError);
            // WebSocket 로드 실패 시 에러만 로그
          }

          set({ endpoints: grouped, isLoading: false });
        } catch (error) {
          console.error("API 목록 로드 실패:", error);
          set({ isLoading: false });
          // 에러 발생 시 빈 객체로 설정
          set({ endpoints: {} });
        }
      },
    }),
    {
      name: "sidebar-storage",
    }
  )
);
