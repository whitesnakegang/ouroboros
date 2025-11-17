import { create } from "zustand";
import { persist } from "zustand/middleware";

type Protocol = "REST" | "WebSocket" | null;

interface SpecState {
  protocol: Protocol;
  setProtocol: (protocol: Protocol) => void;
  selectedEndpointId: number | null;
  setSelectedEndpointId: (id: number | null) => void;
  isEditing: boolean;
  setIsEditing: (isEditing: boolean) => void;
}

export const useSpecStore = create<SpecState>()(
  persist(
    (set) => ({
      protocol: "REST",
      setProtocol: (protocol) => set({ protocol }),
      selectedEndpointId: null,
      setSelectedEndpointId: (id) => set({ selectedEndpointId: id }),
      isEditing: false,
      setIsEditing: (isEditing) => set({ isEditing }),
    }),
    {
      name: "spec-storage",
    }
  )
);
