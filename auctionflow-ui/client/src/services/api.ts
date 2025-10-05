import { useAuthStore } from '@/stores/auth';
import type { LoginRequest, LoginResponse, RegisterRequest, PlaceBidRequest, PlaceBidResponse } from '@/types/api';
import {
  getMockAuctions,
  getMockAuction,
  getMockBidHistory,
  placeMockBid,
  mockLogin,
  mockRegister,
  mockCategories,
  mockBidIncrements,
  generateMockResponse,
} from './mockData';

const API_BASE_URL = '/api/v1';
const USE_MOCKS = import.meta.env.VITE_USE_MOCKS === 'true';

async function fetchWithAuth(url: string, options: RequestInit = {}) {
  const token = useAuthStore.getState().token;
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(token && { Authorization: `Bearer ${token}` }),
    ...options.headers,
  };

  try {
    const response = await fetch(`${API_BASE_URL}${url}`, {
      ...options,
      headers,
      signal: AbortSignal.timeout(2000),
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({ error: 'UNKNOWN_ERROR', message: response.statusText }));
      throw error;
    }

    return response.json();
  } catch (error: any) {
    if (USE_MOCKS && (error.name === 'TypeError' || error.name === 'TimeoutError')) {
      console.warn('Backend unavailable, using mock data');
      throw { useMock: true };
    }
    throw error;
  }
}

export const api = {
  // Auth
  login: async (data: LoginRequest): Promise<LoginResponse> => {
    try {
      return await fetchWithAuth('/auth/login', {
        method: 'POST',
        body: JSON.stringify(data),
      });
    } catch (error: any) {
      if (error.useMock) return mockLogin(data.email, data.password);
      throw error;
    }
  },

  register: async (data: RegisterRequest) => {
    try {
      return await fetchWithAuth('/auth/register', {
        method: 'POST',
        body: JSON.stringify(data),
      });
    } catch (error: any) {
      if (error.useMock) return mockRegister(data);
      throw error;
    }
  },

  // Auctions
  getAuctions: async (params?: { category?: string; status?: string; limit?: number; offset?: number }) => {
    try {
      const queryParams = new URLSearchParams();
      if (params?.category) queryParams.append('category', params.category);
      if (params?.status) queryParams.append('status', params.status);
      if (params?.limit) queryParams.append('limit', params.limit.toString());
      if (params?.offset) queryParams.append('offset', params.offset.toString());
      
      const query = queryParams.toString();
      return await fetchWithAuth(`/auctions${query ? `?${query}` : ''}`);
    } catch (error: any) {
      if (error.useMock) return getMockAuctions(params);
      throw error;
    }
  },

  getAuction: async (id: string) => {
    try {
      return await fetchWithAuth(`/auctions/${id}`);
    } catch (error: any) {
      if (error.useMock) return getMockAuction(id);
      throw error;
    }
  },

  createAuction: async (data: any) => {
    try {
      return await fetchWithAuth('/auctions', {
        method: 'POST',
        body: JSON.stringify(data),
      });
    } catch (error: any) {
      if (error.useMock) {
        return generateMockResponse({
          id: 'auction-' + Date.now(),
          ...data,
          status: 'PENDING',
          currentHighestBid: null,
          bidCount: 0,
          createdAt: new Date().toISOString(),
        });
      }
      throw error;
    }
  },

  // Bidding
  placeBid: async (auctionId: string, data: PlaceBidRequest): Promise<PlaceBidResponse> => {
    try {
      return await fetchWithAuth(`/auctions/${auctionId}/bids`, {
        method: 'POST',
        body: JSON.stringify(data),
      });
    } catch (error: any) {
      if (error.useMock) return placeMockBid(auctionId, data.amount, data.idempotencyKey);
      throw error;
    }
  },

  getBidHistory: async (auctionId: string) => {
    try {
      return await fetchWithAuth(`/auctions/${auctionId}/bids`);
    } catch (error: any) {
      if (error.useMock) return getMockBidHistory(auctionId);
      throw error;
    }
  },

  // User
  getUserBids: async (userId: string) => {
    try {
      return await fetchWithAuth(`/users/${userId}/bids`);
    } catch (error: any) {
      if (error.useMock) {
        return generateMockResponse({
          bids: [
            {
              id: 'bid-1',
              auctionId: 'auction-1',
              auctionTitle: 'Vintage Leica M3 Camera',
              amount: 650,
              status: 'ACCEPTED',
              timestamp: new Date(Date.now() - 10 * 60 * 1000).toISOString(),
              auctionStatus: 'ACTIVE',
            },
          ],
          total: 1,
        });
      }
      throw error;
    }
  },

  // Watchlist
  addToWatchlist: async (auctionId: string) => {
    try {
      return await fetchWithAuth(`/auctions/${auctionId}/watch`, {
        method: 'POST',
      });
    } catch (error: any) {
      if (error.useMock) {
        return generateMockResponse({ message: 'Auction added to watchlist' });
      }
      throw error;
    }
  },

  removeFromWatchlist: async (auctionId: string) => {
    try {
      return await fetchWithAuth(`/auctions/${auctionId}/watch`, {
        method: 'DELETE',
      });
    } catch (error: any) {
      if (error.useMock) {
        return generateMockResponse({ message: 'Auction removed from watchlist' });
      }
      throw error;
    }
  },

  // Reference data
  getCategories: async () => {
    try {
      return await fetchWithAuth('/reference/categories');
    } catch (error: any) {
      if (error.useMock) {
        return generateMockResponse({ categories: mockCategories });
      }
      throw error;
    }
  },

  getBidIncrements: async () => {
    try {
      return await fetchWithAuth('/reference/bid-increments');
    } catch (error: any) {
      if (error.useMock) {
        return generateMockResponse({ increments: mockBidIncrements });
      }
      throw error;
    }
  },
};
