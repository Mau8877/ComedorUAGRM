import { create } from 'zustand'

interface UiState {
  sidebarCollapsed: boolean
  toggleSidebarCollapsed: () => void
  mobileSidebarOpen: boolean
  setMobileSidebarOpen: (open: boolean) => void
}

export const useUiStore = create<UiState>((set) => ({
  sidebarCollapsed: false,
  toggleSidebarCollapsed: () =>
    set((s) => ({ sidebarCollapsed: !s.sidebarCollapsed })),
  mobileSidebarOpen: false,
  setMobileSidebarOpen: (open) => set({ mobileSidebarOpen: open }),
}))
