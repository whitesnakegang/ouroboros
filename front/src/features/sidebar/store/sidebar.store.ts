import { create } from "zustand";
import { persist } from "zustand/middleware";
import { getAllRestApiSpecs } from "@/features/spec/services/api";
import type { RestApiSpecResponse } from "@/features/spec/services/api";

interface Endpoint {
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
}

export interface EndpointData {
  [group: string]: Endpoint[];
}

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
}

// 백엔드 스펙을 프론트엔드 엔드포인트 형태로 변환
function convertSpecToEndpoint(spec: RestApiSpecResponse): Endpoint {
  // tag 매핑: none=미구현, implementing=구현중, bugfix=수정중
  const mapTagToStatus = (
    tag?: string,
    progress?: string
  ): Endpoint["implementationStatus"] => {
    if (progress === "completed") return undefined;
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
    hasSpecError: spec.isValid === false ? true : undefined,
    tags: spec.tags,
    progress: spec.progress,
    tag: spec.tag,
    diff: spec.diff,
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
      loadEndpoints: async () => {
        set({ isLoading: true });
        try {
          const response = await getAllRestApiSpecs();
          const specs = response.data;

          // 스펙을 그룹별로 분류
          const grouped: EndpointData = {};

          specs.forEach((spec) => {
            // tags를 그룹 키로 사용 (첫 번째 태그 또는 기본값)
            const group =
              spec.tags && spec.tags.length > 0 ? spec.tags[0] : "OTHERS";

            if (!grouped[group]) {
              grouped[group] = [];
            }

            grouped[group].push(convertSpecToEndpoint(spec));
          });

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
