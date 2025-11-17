import { create } from "zustand";
import { persist } from "zustand/middleware";
import {
  getAllRestApiSpecs,
  getAllWebSocketOperations,
  getAllWebSocketChannels,
} from "@/features/spec/services/api";
import type {
  RestApiSpecResponse,
  OperationResponse,
} from "@/features/spec/services/api";

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
  entrypoint?: string; // WebSocket entrypoint (pathname)
  wsProtocol?: "ws" | "wss" | null; // WebSocket protocol (ws/wss/null)
}

export interface EndpointData {
  [group: string]: Endpoint[];
}

export type Protocol = "REST" | "WebSocket" | null;

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

// 주소에서 도메인 추출 (첫 번째 경로 세그먼트)
function extractDomainFromAddress(address: string): string {
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

  // receiver address 추출
  const channelRef = operation.channel?.ref || "";
  const channelName = channelRef.replace("#/channels/", "");
  const receiverAddress =
    channelMap.get(channelName) || channelName || "/unknown";

  // reply address 추출 (있는 경우)
  let replyAddress = "";
  if (operation.reply && operation.reply.channel) {
    const replyChannelRef = operation.reply.channel.ref || "";
    const replyChannelName = replyChannelRef.replace("#/channels/", "");
    replyAddress =
      channelMap.get(replyChannelName) || replyChannelName || "/unknown";
  }

  // Path 생성: "receive address - reply address" 형태
  let path = receiverAddress;
  if (replyAddress) {
    path = `${receiverAddress} - ${replyAddress}`;
  }

  // Summary 생성 (operation name을 읽기 쉽게)
  let summary = operationName
    .replace(/^_/, "")
    .replace(/_to_/g, " → ")
    .replace(/_/g, " ")
    .replace(/\./g, ".");

  // tags 결정: WebSocket의 경우 tags는 [{name: "TAG_NAME"}] 형태
  const operationTags = (operation as any).tags;
  let tags: string[] | undefined = undefined;

  if (
    operationTags &&
    Array.isArray(operationTags) &&
    operationTags.length > 0
  ) {
    // WebSocket tags는 객체 배열 형태 [{name: "TAG_NAME"}]이므로 name 값만 추출
    tags = operationTags
      .map((tag: any) => {
        if (typeof tag === "string") {
          return tag; // 이미 문자열인 경우 (호환성)
        } else if (tag && typeof tag === "object" && tag.name) {
          return tag.name; // {name: "TAG_NAME"} 형태에서 name 추출
        }
        return null;
      })
      .filter((tag: string | null): tag is string => tag !== null);

    // 유효한 tag가 없으면 undefined
    if (tags.length === 0) {
      tags = undefined;
    }
  }

  // entrypoint 추출 (operation.entrypoint 또는 기본값)
  const entrypoint = (operation as any).entrypoint || "/ws";

  // protocol 추출 (operationResponse.protocol 또는 기본값 "ws")
  const wsProtocol = operationResponse.protocol || "ws";

  return {
    id: operation.id || operationName,
    method: method,
    path: path,
    description: summary, // summary로 사용
    implementationStatus: mapTagToStatus(tag, operation.progress),
    hasSpecError:
      operation.diff && operation.diff !== "none" ? true : undefined,
    tags: tags,
    progress: operation.progress,
    tag: tag,
    diff: operation.diff,
    protocol: "WebSocket",
    operationName: operationName,
    entrypoint: entrypoint, // WebSocket entrypoint 저장
    wsProtocol: wsProtocol, // WebSocket protocol 저장 (ws/wss/null)
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
            } catch {
              // Channel 로드 실패 시 추정 값 사용
            }

            // 2. Operations 로드
            const wsResponse = await getAllWebSocketOperations();
            const wsOperations = wsResponse.data;

            // 3. WebSocket Operations를 그룹화
            // tags의 name 값으로 그룹화, 없으면 "OTHERS"
            wsOperations.forEach((operation) => {
              const endpoint = convertOperationToEndpoint(
                operation,
                channelMap
              );

              // 그룹 결정: tags의 첫 번째 name 값 사용, 없으면 "OTHERS"
              const group =
                endpoint.tags && endpoint.tags.length > 0
                  ? endpoint.tags[0] // tags의 첫 번째 값 사용
                  : "OTHERS"; // tags가 없으면 "OTHERS"

              if (!grouped[group]) {
                grouped[group] = [];
              }
              grouped[group].push(endpoint);
            });
          } catch {
            // WebSocket Operations 로드 실패 시 무시
          }

          set({ endpoints: grouped, isLoading: false });
        } catch {
          // API 목록 로드 실패
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
