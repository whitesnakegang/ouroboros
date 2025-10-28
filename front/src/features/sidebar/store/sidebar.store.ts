import { create } from "zustand";
import { persist } from "zustand/middleware";

interface Endpoint {
  id: number;
  method: string;
  path: string;
  description: string;
  implementationStatus?: "not-implemented" | "in-progress" | "modifying";
  hasSpecError?: boolean;
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
  deleteEndpoint: (endpointId: number) => void;
  addEndpoint: (endpoint: Endpoint, group: string) => void;
  triggerNewForm: boolean;
  setTriggerNewForm: (trigger: boolean) => void;
}

const mockInitialEndpoints = {
  AUTH: [
    {
      id: 1,
      method: "POST",
      path: "/api/auth/login",
      description: "사용자 로그인",
      implementationStatus: "not-implemented" as const,
    },
    {
      id: 2,
      method: "POST",
      path: "/api/auth/register",
      description: "신규 사용자 등록",
      implementationStatus: "in-progress" as const,
    },
    {
      id: 3,
      method: "POST",
      path: "/api/auth/logout",
      description: "사용자 로그아웃",
      implementationStatus: "modifying" as const,
    },
  ],
  USERS: [
    {
      id: 4,
      method: "GET",
      path: "/api/users/:id",
      description: "ID로 사용자 조회",
      hasSpecError: false,
    },
    {
      id: 5,
      method: "GET",
      path: "/api/users",
      description: "전체 사용자 목록",
      hasSpecError: true,
    },
    {
      id: 6,
      method: "PUT",
      path: "/api/users/:id",
      description: "사용자 정보 수정",
      hasSpecError: false,
    },
  ],
};

export const useSidebarStore = create<SidebarState>()(
  persist(
    (set, get) => ({
      isOpen: true,
      toggle: () => set((state) => ({ isOpen: !state.isOpen })),
      isDarkMode: false,
      toggleDarkMode: () => set((state) => ({ isDarkMode: !state.isDarkMode })),
      selectedEndpoint: null,
      setSelectedEndpoint: (endpoint) => set({ selectedEndpoint: endpoint }),
      endpoints: mockInitialEndpoints,
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
    }),
    {
      name: "sidebar-storage",
    }
  )
);
