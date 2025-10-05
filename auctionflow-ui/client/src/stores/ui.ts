import { create } from 'zustand';

interface UIState {
  authModalOpen: boolean;
  authMode: 'login' | 'register';
  selectedAuctionId: string | null;
  notificationCount: number;
  setAuthModalOpen: (open: boolean) => void;
  setAuthMode: (mode: 'login' | 'register') => void;
  setSelectedAuctionId: (id: string | null) => void;
  setNotificationCount: (count: number) => void;
}

export const useUIStore = create<UIState>((set) => ({
  authModalOpen: false,
  authMode: 'login',
  selectedAuctionId: null,
  notificationCount: 0,
  setAuthModalOpen: (open) => set({ authModalOpen: open }),
  setAuthMode: (mode) => set({ authMode: mode }),
  setSelectedAuctionId: (id) => set({ selectedAuctionId: id }),
  setNotificationCount: (count) => set({ notificationCount: count }),
}));
