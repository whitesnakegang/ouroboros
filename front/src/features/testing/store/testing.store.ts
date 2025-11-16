import { create } from "zustand";
import type { TryMethod } from "@/features/spec/services/api";

export type Protocol = "REST" | "WebSocket";

export interface TestRequest {
  method: string;
  url: string;
  description: string;
  headers: Array<{ key: string; value: string }>;
  queryParams: Array<{ key: string; value: string }>;
  body: string;
}

export interface TestResponse {
  status: number;
  statusText: string;
  headers: Record<string, string>;
  body: string;
  responseTime: number; // 응답 시간 (ms)
}

export type WebSocketConnectionStatus =
  | "disconnected"
  | "connecting"
  | "connected";

export interface WebSocketMessage {
  id: string;
  timestamp: number;
  direction: "sent" | "received";
  address: string;
  content: string;
  tryId?: string;
}

export interface WebSocketStats {
  totalSent: number;
  totalReceived: number;
  averageResponseTime: number | null; // Receiver → Reply 평균 응답 시간
  connectionDuration: number | null; // 연결 지속 시간 (ms)
}

interface TestingState {
  protocol: Protocol;
  setProtocol: (protocol: Protocol) => void;

  // Request State
  request: TestRequest;
  setRequest: (request: Partial<TestRequest>) => void;
  updateRequestHeader: (index: number, key: string, value: string) => void;
  addRequestHeader: () => void;
  removeRequestHeader: (index: number) => void;
  updateQueryParam: (index: number, key: string, value: string) => void;
  addQueryParam: () => void;
  removeQueryParam: (index: number) => void;

  // Authorization State
  authorization: string;
  setAuthorization: (authorization: string) => void;

  // Response State
  response: TestResponse | null;
  setResponse: (response: TestResponse | null) => void;

  // Test Execution State
  isLoading: boolean;
  setIsLoading: (isLoading: boolean) => void;

  // Method List State (for Test tab)
  methodList: TryMethod[] | null;
  setMethodList: (methods: TryMethod[] | null) => void;
  totalDurationMs: number | null;
  setTotalDurationMs: (duration: number | null) => void;

  // Try ID State
  tryId: string | null;
  setTryId: (tryId: string | null) => void;

  // WebSocket State
  wsConnectionStatus: WebSocketConnectionStatus;
  setWsConnectionStatus: (status: WebSocketConnectionStatus) => void;
  wsMessages: WebSocketMessage[];
  addWsMessage: (message: WebSocketMessage) => void;
  clearWsMessages: () => void;
  wsStats: WebSocketStats;
  updateWsStats: (stats: Partial<WebSocketStats>) => void;
  wsConnectionStartTime: number | null;
  setWsConnectionStartTime: (time: number | null) => void;
}

export const useTestingStore = create<TestingState>((set) => ({
  protocol: "REST",
  setProtocol: (protocol) => set({ protocol }),

  request: {
    method: "POST",
    url: "/api/auth/login",
    description: "사용자 로그인",
    headers: [{ key: "Content-Type", value: "application/json" }],
    queryParams: [],
    body: JSON.stringify({ email: "string", password: "string" }, null, 2),
  },
  setRequest: (partialRequest) =>
    set((state) => ({
      request: { ...state.request, ...partialRequest },
    })),
  updateRequestHeader: (index, key, value) =>
    set((state) => ({
      request: {
        ...state.request,
        headers: state.request.headers.map((h, i) =>
          i === index ? { key, value } : h
        ),
      },
    })),
  addRequestHeader: () =>
    set((state) => ({
      request: {
        ...state.request,
        headers: [...state.request.headers, { key: "", value: "" }],
      },
    })),
  removeRequestHeader: (index) =>
    set((state) => ({
      request: {
        ...state.request,
        headers: state.request.headers.filter((_, i) => i !== index),
      },
    })),
  updateQueryParam: (index, key, value) =>
    set((state) => ({
      request: {
        ...state.request,
        queryParams: state.request.queryParams.map((p, i) =>
          i === index ? { key, value } : p
        ),
      },
    })),
  addQueryParam: () =>
    set((state) => ({
      request: {
        ...state.request,
        queryParams: [...state.request.queryParams, { key: "", value: "" }],
      },
    })),
  removeQueryParam: (index) =>
    set((state) => ({
      request: {
        ...state.request,
        queryParams: state.request.queryParams.filter((_, i) => i !== index),
      },
    })),

  authorization: "",
  setAuthorization: (authorization) => set({ authorization }),

  response: null,
  setResponse: (response) => set({ response }),

  isLoading: false,
  setIsLoading: (isLoading) => set({ isLoading }),

  methodList: null,
  setMethodList: (methods) => set({ methodList: methods }),
  totalDurationMs: null,
  setTotalDurationMs: (duration) => set({ totalDurationMs: duration }),

  tryId: null,
  setTryId: (tryId) => set({ tryId }),

  // WebSocket State
  wsConnectionStatus: "disconnected",
  setWsConnectionStatus: (status) => set({ wsConnectionStatus: status }),
  wsMessages: [],
  addWsMessage: (message) =>
    set((state) => ({
      wsMessages: [...state.wsMessages, message],
    })),
  clearWsMessages: () => set({ wsMessages: [] }),
  wsStats: {
    totalSent: 0,
    totalReceived: 0,
    averageResponseTime: null,
    connectionDuration: null,
  },
  updateWsStats: (stats) =>
    set((state) => ({
      wsStats: { ...state.wsStats, ...stats },
    })),
  wsConnectionStartTime: null,
  setWsConnectionStartTime: (time) => set({ wsConnectionStartTime: time }),
}));
